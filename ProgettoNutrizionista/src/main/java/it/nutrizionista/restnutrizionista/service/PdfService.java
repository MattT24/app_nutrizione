package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.GiornoSettimana;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoScheda;
import it.nutrizionista.restnutrizionista.enums.TemaPdf;
import it.nutrizionista.restnutrizionista.exception.UnprocessableEntityException;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PdfService {

    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("HH:mm");

    // Guardrail sulla dimensione del documento: senza limite, una scheda con pasti/alimenti
    // illimitati genererebbe un rendering Chromium molto pesante (e un PDF enorme) su un endpoint
    // sincrono senza retry lato server. Margine ampio sopra l'uso realistico (7 giorni × 6 pasti/
    // giorno; 400 alimenti totali) per non intralciare casi legittimi.
    private static final int MAX_PASTI_PER_SCHEDA = 42;
    private static final int MAX_ALIMENTI_TOTALI_SCHEDA = 400;

    private final TemplateEngine templateEngine;
    private final MisurazioneAntropometricaRepository misurazioneRepository;
    private final PlicometriaRepository plicometriaRepository;
    private final PlicometriaCalcoliService plicometriaCalcoli;
    private final OwnershipValidator ownershipValidator;
    private final ChromiumPdfRenderer chromiumPdfRenderer;

    public PdfService(TemplateEngine templateEngine,
                      MisurazioneAntropometricaRepository misurazioneRepository,
                      PlicometriaRepository plicometriaRepository,
                      PlicometriaCalcoliService plicometriaCalcoli,
                      OwnershipValidator ownershipValidator,
                      ChromiumPdfRenderer chromiumPdfRenderer) {
        this.templateEngine = templateEngine;
        this.misurazioneRepository = misurazioneRepository;
        this.plicometriaRepository = plicometriaRepository;
        this.plicometriaCalcoli = plicometriaCalcoli;
        this.ownershipValidator = ownershipValidator;
        this.chromiumPdfRenderer = chromiumPdfRenderer;
    }

    // ─────────────────────────────────────────── MISURAZIONI ───────────────────────────────────────────

    /** Definizione statica delle misure antropometriche: ordine di presentazione, label, icona, unità, getter. */
    private record MisuraDef(String label, String icon, String unita, Function<MisurazioneAntropometrica, Double> get) {}

    private static final List<MisuraDef> MISURE = List.of(
            new MisuraDef("Peso", "weight", "kg", MisurazioneAntropometrica::getPeso),
            new MisuraDef("Vita", "ruler", "cm", MisurazioneAntropometrica::getVita),
            new MisuraDef("Fianchi", "ruler", "cm", MisurazioneAntropometrica::getFianchi),
            new MisuraDef("Spalle", "arrows", "cm", MisurazioneAntropometrica::getSpalle),
            new MisuraDef("Torace", "lungs", "cm", MisurazioneAntropometrica::getTorace),
            new MisuraDef("Bicipite SX", "dumbbell", "cm", MisurazioneAntropometrica::getBicipiteS),
            new MisuraDef("Bicipite DX", "dumbbell", "cm", MisurazioneAntropometrica::getBicipiteD),
            new MisuraDef("Gamba SX", "walk", "cm", MisurazioneAntropometrica::getGambaS),
            new MisuraDef("Gamba DX", "walk", "cm", MisurazioneAntropometrica::getGambaD)
    );

    @Transactional(readOnly = true)
    public byte[] generaPdfMisurazione(Long id) {
        MisurazioneAntropometrica corrente = ownershipValidator.getOwnedMisurazioneAntropometrica(id);
        MisurazioneAntropometrica precedente = null;
        if (corrente.getCliente() != null && corrente.getDataMisurazione() != null) {
            precedente = misurazioneRepository
                    .findFirstByCliente_IdAndDataMisurazioneLessThanOrderByDataMisurazioneDesc(
                            corrente.getCliente().getId(), corrente.getDataMisurazione())
                    .orElse(null);
        }
        return renderMisurazione(corrente, precedente);
    }

    byte[] renderMisurazione(MisurazioneAntropometrica corrente, MisurazioneAntropometrica precedente) {
        Context context = new Context();
        context.setVariable("misurazione", corrente);
        context.setVariable("haPrecedente", precedente != null);
        context.setVariable("dataCorrente", PdfFormat.dataBreve(corrente.getDataMisurazione()));
        context.setVariable("dataPrecedente", precedente != null ? PdfFormat.dataBreve(precedente.getDataMisurazione()) : null);

        List<Map<String, Object>> righe = new ArrayList<>();
        List<Map<String, Object>> trend = new ArrayList<>();
        int rilevate = 0;
        for (MisuraDef def : MISURE) {
            Double cur = def.get().apply(corrente);
            Double prev = precedente != null ? def.get().apply(precedente) : null;
            Map<String, Object> r = rigaConfronto(def.label(), def.icon(), def.unita(), cur, prev);
            righe.add(r);
            if (cur != null) rilevate++;
            if (cur != null && prev != null) trend.add(rigaTrend(def.label(), def.unita(), cur, prev));
        }
        context.setVariable("righe", righe);
        context.setVariable("trend", trend);
        context.setVariable("rilevate", rilevate);
        context.setVariable("totaleMisure", MISURE.size());

        // KPI = le prime 3 misure (Peso, Vita, Fianchi); la prima (Peso) è la card "hero" del layout
        List<Map<String, Object>> kpi = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MisuraDef def = MISURE.get(i);
            Map<String, Object> row = rigaConfronto(def.label(), def.icon(), def.unita(),
                    def.get().apply(corrente), precedente != null ? def.get().apply(precedente) : null);
            if (i == 0) row.put("hero", true);
            kpi.add(row);
        }
        context.setVariable("kpi", kpi);

        if (corrente.getCliente() != null) addProfessionalHeaderData(context, corrente.getCliente());
        return renderPdf(templateEngine.process("pdf/misurazione", context));
    }

    // ─────────────────────────────────────────── PLICOMETRIE ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generaPdfPlicometria(Long id) {
        Plicometria corrente = ownershipValidator.getOwnedPlicometria(id);
        Plicometria precedente = null;
        if (corrente.getCliente() != null && corrente.getDataMisurazione() != null) {
            precedente = plicometriaRepository
                    .findFirstByCliente_IdAndDataMisurazioneLessThanOrderByDataMisurazioneDesc(
                            corrente.getCliente().getId(), corrente.getDataMisurazione())
                    .orElse(null);
        }
        return renderPlicometria(corrente, precedente);
    }

    byte[] renderPlicometria(Plicometria corrente, Plicometria precedente) {
        Cliente cliente = corrente.getCliente();
        Sesso sesso = cliente != null ? cliente.getSesso() : null;

        Context context = new Context();
        context.setVariable("plicometria", corrente);
        context.setVariable("metodoLabel", PdfFormat.metodoLabel(corrente.getMetodo()));
        context.setVariable("numeroPliche", PdfFormat.numeroPliche(corrente.getMetodo()));
        context.setVariable("haPrecedente", precedente != null);
        context.setVariable("dataCorrente", PdfFormat.dataBreve(corrente.getDataMisurazione()));
        context.setVariable("dataPrecedente", precedente != null ? PdfFormat.dataBreve(precedente.getDataMisurazione()) : null);
        if (cliente != null) {
            context.setVariable("sessoLabel", PdfFormat.sesso(cliente.getSesso()));
            context.setVariable("eta", PdfFormat.eta(cliente.getDataNascita(), corrente.getDataMisurazione()));
            context.setVariable("dataNascita", PdfFormat.data(cliente.getDataNascita()));
        }

        // Pliche pertinenti al metodo (method-aware, JP3 sesso-dipendente)
        List<Map<String, Object>> pliche = new ArrayList<>();
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String key : plicometriaCalcoli.getPlicheUsate(corrente.getMetodo(), sesso)) {
            Double cur = plicometriaCalcoli.getValorePlica(corrente, key);
            Double prev = precedente != null ? plicometriaCalcoli.getValorePlica(precedente, key) : null;
            pliche.add(rigaConfronto(PdfFormat.plicaLabel(key), "plica", "mm", cur, prev));
        }
        context.setVariable("pliche", pliche);

        // Risultati (già persistiti nell'entity)
        boolean manuale = pliche.isEmpty();
        context.setVariable("manuale", manuale);

        Double curMG = corrente.getPercentualeMassaGrassa();
        Double prevMG = precedente != null ? precedente.getPercentualeMassaGrassa() : null;
        Double curSomma = corrente.getSommaPliche();
        Double prevSomma = precedente != null ? precedente.getSommaPliche() : null;
        Double curDens = corrente.getDensitaCorporea();
        Double prevDens = precedente != null ? precedente.getDensitaCorporea() : null;
        Double curMGkg = corrente.getMassaGrassaKg();
        Double prevMGkg = precedente != null ? precedente.getMassaGrassaKg() : null;
        Double curMM = corrente.getMassaMagraKg();
        Double prevMM = precedente != null ? precedente.getMassaMagraKg() : null;

        List<Map<String, Object>> risultati = new ArrayList<>();
        if (!manuale) {
            risultati.add(risultatoRow("Somma Pliche", "sum", "mm", curSomma, prevSomma, "sum"));
            risultati.add(risultatoRow("Densità Corporea", "drop", "g/ml", curDens, prevDens, "result"));
        }
        risultati.add(risultatoRow("Massa Grassa", "percent", "%", curMG, prevMG, "result"));
        risultati.add(risultatoRow("Massa Magra", "muscle", "kg", curMM, prevMM, "result"));
        context.setVariable("risultati", risultati);

        // KPI: Massa Grassa (hero) + Somma/Densità (o MG kg / MM kg per i metodi manuali)
        List<Map<String, Object>> kpi = new ArrayList<>();
        Map<String, Object> hero = rigaConfronto("Massa Grassa", "percent", "%", curMG, prevMG);
        hero.put("hero", true);
        kpi.add(hero);
        if (!manuale) {
            kpi.add(rigaConfronto("Somma Pliche", "sum", "mm", curSomma, prevSomma));
            kpi.add(rigaConfronto("Densità Corporea", "drop", "g/ml", curDens, prevDens));
        } else {
            kpi.add(rigaConfronto("Massa Grassa (kg)", "muscle", "kg", curMGkg, prevMGkg));
            kpi.add(rigaConfronto("Massa Magra", "muscle", "kg", curMM, prevMM));
        }
        context.setVariable("kpi", kpi);

        // Trend: Massa Grassa e Somma Pliche (se c'è confronto)
        if (precedente != null) {
            trend.add(rigaTrend("Massa Grassa", "%", corrente.getPercentualeMassaGrassa(), precedente.getPercentualeMassaGrassa()));
            if (!manuale) trend.add(rigaTrend("Somma Pliche", "mm", corrente.getSommaPliche(), precedente.getSommaPliche()));
        }
        context.setVariable("trend", trend);

        if (cliente != null) addProfessionalHeaderData(context, cliente);
        return renderPdf(templateEngine.process("pdf/plicometria", context));
    }

    // ─────────────────────────────────────────── SCHEDE ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generaPdfScheda(Long id) {
        return generaPdfScheda(id, true);
    }

    @Transactional(readOnly = true)
    public byte[] generaPdfScheda(Long id, boolean mostraMacro) {
        Scheda scheda = ownershipValidator.getOwnedScheda(id);
        forzaFetchScheda(scheda);
        assertDimensioneRenderizzabile(scheda);
        return renderScheda(scheda, mostraMacro);
    }

    /** Guardrail contro schede abnormi (vedi MAX_PASTI_PER_SCHEDA/MAX_ALIMENTI_TOTALI_SCHEDA):
     *  copre tutti i percorsi di ingresso (CRUD pasto, copy-bulk, import) con un solo controllo,
     *  qui perché il grafo pasti/alimenti è già interamente caricato da forzaFetchScheda.
     *  Package-private per consentire il test diretto senza DB. */
    void assertDimensioneRenderizzabile(Scheda scheda) {
        int numPasti = scheda.getPasti().size();
        if (numPasti > MAX_PASTI_PER_SCHEDA) {
            throw new UnprocessableEntityException(
                    "La scheda ha troppi pasti (%d, massimo %d) per generare il PDF".formatted(numPasti, MAX_PASTI_PER_SCHEDA));
        }
        int totAlimenti = scheda.getPasti().stream()
                .mapToInt(p -> p.getAlimentiPasto() != null ? p.getAlimentiPasto().size() : 0)
                .sum();
        if (totAlimenti > MAX_ALIMENTI_TOTALI_SCHEDA) {
            throw new UnprocessableEntityException(
                    "La scheda ha troppi alimenti (%d, massimo %d) per generare il PDF".formatted(totAlimenti, MAX_ALIMENTI_TOTALI_SCHEDA));
        }
    }

    private void forzaFetchScheda(Scheda scheda) {
        scheda.getPasti().size();
        scheda.getPasti().forEach(pasto -> {
            if (pasto.getAlimentiPasto() != null) {
                pasto.getAlimentiPasto().size();
                pasto.getAlimentiPasto().forEach(ap -> {
                    if (ap.getAlimento() != null) {
                        ap.getAlimento().getNome();
                        ap.getAlimento().getMacronutrienti(); // per i totali del donut
                    }
                    if (ap.getNomeOverride() != null) ap.getNomeOverride().getNomeCustom();
                    if (ap.getAlternative() != null) {
                        ap.getAlternative().size();
                        ap.getAlternative().forEach(alt -> {
                            if (alt.getAlimentoAlternativo() != null) alt.getAlimentoAlternativo().getNome();
                        });
                    }
                });
            }
        });
    }

    byte[] renderScheda(Scheda scheda, boolean mostraMacro) {
        List<Pasto> pastiOrdinati = scheda.getPasti().stream()
                .sorted(Comparator.comparing((Pasto p) -> p.getGiorno() != null ? p.getGiorno().ordinal() : -1)
                        .thenComparing(Pasto::getOrdineVisualizzazione, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        boolean settimanale = scheda.getTipo() == TipoScheda.SETTIMANALE;

        Context context = new Context();
        context.setVariable("scheda", scheda);
        context.setVariable("dataCreazione", PdfFormat.data(scheda.getDataCreazione()));
        context.setVariable("tipoLabel", settimanale ? "Settimanale" : "Giornaliero");
        context.setVariable("mostraMacro", mostraMacro);

        Cliente cl = scheda.getCliente();
        String nome = cl != null && cl.getNome() != null ? cl.getNome() : "";
        String cognome = cl != null && cl.getCognome() != null ? cl.getCognome() : "";
        context.setVariable("pazienteNome", (nome + " " + cognome).trim());
        String ini = "";
        if (!nome.isEmpty()) ini += nome.charAt(0);
        if (!cognome.isEmpty()) ini += cognome.charAt(0);
        context.setVariable("iniziali", ini.toUpperCase());

        int totAlimenti = pastiOrdinati.stream()
                .mapToInt(p -> p.getAlimentiPasto() != null ? p.getAlimentiPasto().size() : 0).sum();
        context.setVariable("totalePasti", pastiOrdinati.size());
        context.setVariable("totaleAlimenti", totAlimenti);

        if (settimanale) {
            Map<GiornoSettimana, List<Map<String, Object>>> perGiorno = new LinkedHashMap<>();
            for (Pasto p : pastiOrdinati) {
                if (p.getGiorno() == null) continue;
                perGiorno.computeIfAbsent(p.getGiorno(), k -> new ArrayList<>()).add(pastoView(p));
            }
            List<Map<String, Object>> giorni = new ArrayList<>();
            perGiorno.forEach((g, pasti) -> {
                Map<String, Object> gv = new LinkedHashMap<>();
                gv.put("nome", PdfFormat.giorno(g));
                gv.put("pasti", pasti);
                giorni.add(gv);
            });
            context.setVariable("giorni", giorni);
        } else {
            context.setVariable("pasti", pastiOrdinati.stream().map(this::pastoView).collect(Collectors.toList()));
        }

        aggiungiTotaliMacro(context, pastiOrdinati);

        if (scheda.getCliente() != null) addProfessionalHeaderData(context, scheda.getCliente());
        String template = settimanale ? "pdf/scheda_settimanale" : "pdf/scheda_giornaliera";
        return renderPdf(templateEngine.process(template, context));
    }

    /** View-model di un pasto: nome, orario, alimenti (con nome-override, quantità e alternative). */
    private Map<String, Object> pastoView(Pasto pasto) {
        Map<String, Object> pv = new LinkedHashMap<>();
        pv.put("nome", pasto.getNome());
        pv.put("descrizione", pasto.getDescrizione());
        String orario = "";
        if (pasto.getOrarioInizio() != null) {
            orario = pasto.getOrarioInizio().format(ORA);
            if (pasto.getOrarioFine() != null) orario += " – " + pasto.getOrarioFine().format(ORA);
        }
        pv.put("orario", orario);

        List<Map<String, Object>> alimenti = new ArrayList<>();
        if (pasto.getAlimentiPasto() != null) {
            for (AlimentoPasto ap : pasto.getAlimentiPasto()) {
                Map<String, Object> av = new LinkedHashMap<>();
                String nome = ap.getNomeOverride() != null && ap.getNomeOverride().getNomeCustom() != null
                        ? ap.getNomeOverride().getNomeCustom()
                        : (ap.getAlimento() != null ? ap.getAlimento().getNome() : "N/D");
                av.put("nome", nome);
                av.put("categoria", ap.getAlimento() != null ? ap.getAlimento().getCategoria() : "");
                av.put("qty", ap.getQuantita() > 0 ? PdfFormat.num((double) ap.getQuantita()) + " g" : "A volontà");
                List<Map<String, Object>> alts = new ArrayList<>();
                if (ap.getAlternative() != null) {
                    ap.getAlternative().forEach(alt -> {
                        Map<String, Object> altv = new LinkedHashMap<>();
                        String an = alt.getNomeCustom() != null ? alt.getNomeCustom()
                                : (alt.getAlimentoAlternativo() != null ? alt.getAlimentoAlternativo().getNome() : "N/D");
                        altv.put("nome", an);
                        altv.put("qty", alt.getQuantita() != null && alt.getQuantita() > 0 ? alt.getQuantita() + " g" : "");
                        alts.add(altv);
                    });
                }
                av.put("alternative", alts);
                alimenti.add(av);
            }
        }
        pv.put("alimenti", alimenti);
        return pv;
    }

    /** Totali nutrizionali della scheda (somma alimenti) + percentuali kcal per il donut macro. */
    private void aggiungiTotaliMacro(Context context, List<Pasto> pasti) {
        double kcal = 0, prot = 0, carb = 0, grassi = 0, fibre = 0;
        for (Pasto p : pasti) {
            if (p.getAlimentiPasto() == null) continue;
            for (AlimentoPasto ap : p.getAlimentiPasto()) {
                if (ap.getAlimento() == null || ap.getAlimento().getMacronutrienti() == null) continue;
                Macro m = ap.getAlimento().getMacronutrienti();
                double f = ap.getQuantita() / 100.0;
                kcal += nz(m.getCalorie()) * f;
                prot += nz(m.getProteine()) * f;
                carb += nz(m.getCarboidrati()) * f;
                grassi += nz(m.getGrassi()) * f;
                fibre += nz(m.getFibre()) * f;
            }
        }
        double kcalMacro = prot * 4 + carb * 4 + grassi * 9;
        boolean hasMacro = kcalMacro > 0;
        context.setVariable("hasMacro", hasMacro);
        if (!hasMacro) return;

        int pctProt = (int) Math.round(prot * 4 / kcalMacro * 100);
        int pctCarb = (int) Math.round(carb * 4 / kcalMacro * 100);
        int pctGrassi = 100 - pctProt - pctCarb;

        context.setVariable("kcalTot", PdfFormat.num((double) Math.round(kcal)));
        context.setVariable("macro", List.of(
                macroRow("Proteine", "#3b82f6", prot, pctProt),
                macroRow("Carboidrati", "#f59e0b", carb, pctCarb),
                macroRow("Grassi", "#8b5cf6", grassi, pctGrassi)
        ));
        context.setVariable("fibreG", PdfFormat.num((double) Math.round(fibre)));

        // Segmenti del donut SVG (circonferenza normalizzata a 100)
        List<Map<String, Object>> segs = new ArrayList<>();
        double offset = 0;
        for (int[] pc : new int[][]{{pctProt, 0}, {pctCarb, 1}, {pctGrassi, 2}}) {
            String color = pc[1] == 0 ? "#3b82f6" : pc[1] == 1 ? "#f59e0b" : "#8b5cf6";
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("color", color);
            s.put("len", pc[0]);
            s.put("gap", 100 - pc[0]);
            s.put("offset", -offset);
            segs.add(s);
            offset += pc[0];
        }
        context.setVariable("donut", segs);
    }

    private Map<String, Object> macroRow(String label, String color, double grammi, int pct) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("color", color);
        m.put("grammi", PdfFormat.num((double) Math.round(grammi)));
        m.put("pct", pct);
        return m;
    }

    // ─────────────────────────────────────────── VIEW-MODEL COMUNI ───────────────────────────────────────────

    /** Riga di confronto corrente/precedente con delta e direzione (usata da misure, pliche, risultati, KPI). */
    private Map<String, Object> rigaConfronto(String label, String icon, String unita, Double cur, Double prev) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("label", label);
        r.put("icon", icon);
        r.put("unita", unita);
        r.put("pending", cur == null);
        r.put("cur", cur != null ? PdfFormat.num(cur) : "—");
        r.put("prev", prev != null ? PdfFormat.num(prev) : "—");
        r.put("hasDelta", cur != null && prev != null);
        r.put("delta", PdfFormat.delta(cur, prev, unita));
        r.put("deltaDir", PdfFormat.deltaDir(cur, prev));
        // Sempre presente (anche a false): con Thymeleaf+SpEL (motore usato in produzione, a
        // differenza del vecchio motore OGNL) l'accesso a una chiave ASSENTE da una Map con
        // notazione a punto (es. ${k.hero}) lancia un'eccezione, non restituisce null come
        // farebbe un semplice Map.get(...) — a differenza di un valore null su una chiave presente.
        r.put("hero", false);
        return r;
    }

    private Map<String, Object> risultatoRow(String label, String icon, String unita, Double cur, Double prev, String rowClass) {
        Map<String, Object> r = rigaConfronto(label, icon, unita, cur, prev);
        r.put("rowClass", rowClass);
        return r;
    }

    /** Riga trend a 2 punti: sub "prev → cur unità", valore finale, posizioni y% dei due dot. */
    private Map<String, Object> rigaTrend(String label, String unita, Double cur, Double prev) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("name", label);
        t.put("sub", PdfFormat.num(prev) + " → " + PdfFormat.num(cur) + " " + unita);
        t.put("cur", PdfFormat.num(cur));
        t.put("unita", unita);
        t.put("delta", PdfFormat.delta(cur, prev, unita));
        String dir = PdfFormat.deltaDir(cur, prev);
        t.put("dir", dir);
        // In SVG y cresce verso il basso: valore che scende → linea discendente
        double y1 = "down".equals(dir) ? 22 : "up".equals(dir) ? 75 : 48;
        double y2 = "down".equals(dir) ? 75 : "up".equals(dir) ? 22 : 48;
        t.put("y1", y1);
        t.put("y2", y2);
        return t;
    }

    private static double nz(Double v) { return v == null ? 0.0 : v; }

    // ─────────────────────────────────────────── HEADER + RENDER ───────────────────────────────────────────

    /** Logo Statera di brand (fallback quando il nutrizionista non ha caricato un proprio logo),
     *  caricato una sola volta dal classpath e riusato per ogni render (asset statico). */
    private static final String STATERA_LOGO_BASE64 = loadStaticLogoBase64();

    private static String loadStaticLogoBase64() {
        try (var in = PdfService.class.getResourceAsStream("/branding/statera-logo.png")) {
            if (in == null) {
                System.err.println("PDF Service: logo Statera di default non trovato nel classpath (/branding/statera-logo.png)");
                return null;
            }
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (Exception e) {
            System.err.println("PDF Service: errore caricando il logo Statera di default: " + e.getMessage());
            return null;
        }
    }

    private void addProfessionalHeaderData(Context context, Cliente cliente) {
        if (cliente.getNutrizionista() != null) {
            var nutrizionista = cliente.getNutrizionista();
            nutrizionista.getNome();
            context.setVariable("nutrizionista", nutrizionista);

            // Tema colore (preferenza del nutrizionista): il template applica la classe 't-red' se ROSSO
            context.setVariable("temaRosso", nutrizionista.getPdfTemaColore() == TemaPdf.ROSSO);

            // Logo del nutrizionista se caricato; altrimenti (o se il file non è più raggiungibile
            // sul disco) il logo Statera di brand, non la sola icona a bilancia del fragment mast().
            String logoBase64 = STATERA_LOGO_BASE64;
            if (nutrizionista.getFilePathLogo() != null && !nutrizionista.getFilePathLogo().isEmpty()) {
                try {
                    String logoPathStr = nutrizionista.getFilePathLogo();
                    java.nio.file.Path logoPath = java.nio.file.Paths.get(logoPathStr);
                    if (!logoPath.isAbsolute() && !java.nio.file.Files.exists(logoPath)) {
                        logoPath = java.nio.file.Paths.get(System.getProperty("user.dir"), logoPathStr);
                    }
                    if (java.nio.file.Files.exists(logoPath)) {
                        byte[] logoBytes = java.nio.file.Files.readAllBytes(logoPath);
                        String base64 = java.util.Base64.getEncoder().encodeToString(logoBytes);
                        String mimeType = java.nio.file.Files.probeContentType(logoPath);
                        if (mimeType == null) mimeType = "image/png";
                        logoBase64 = "data:" + mimeType + ";base64," + base64;
                    } else {
                        System.err.println("PDF Service: Logo non trovato al percorso: " + logoPath.toAbsolutePath());
                    }
                } catch (Exception e) {
                    System.err.println("PDF Service: Errore durante il caricamento del logo: " + e.getMessage());
                }
            }
            if (logoBase64 != null) context.setVariable("logoBase64", logoBase64);
        }
    }

    // package-private per consentire il test diretto del rendering senza DB
    byte[] renderPdf(String html) {
        return chromiumPdfRenderer.render(html);
    }
}
