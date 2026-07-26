package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.entity.*;
import it.nutrizionista.restnutrizionista.enums.TemaPdf;
import it.nutrizionista.restnutrizionista.enums.TemplatePdf;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Generatore di anteprime PDF su dati mock (NESSUN DB): produce i PDF di tutti i casi rappresentativi
 * e un'unica pagina HTML con le anteprime PNG, per la revisione del design (checkpoint).
 * Attivo solo con -Dpdf.preview=true, così non incide sulla CI.
 */
@EnabledIfSystemProperty(named = "pdf.preview", matches = "true")
class PdfPreviewGeneratorTest {

    private final PlicometriaCalcoliService calcoli = new PlicometriaCalcoliService();
    private PdfService pdf;
    private ChromiumPdfRenderer renderer;
    private final StringBuilder html = new StringBuilder();

    @AfterEach
    void tearDown() {
        if (renderer != null) renderer.stop();
    }

    @Test
    void generaAnteprime() throws Exception {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        // SpringTemplateEngine (SpEL), non TemplateEngine (OGNL): deve combaciare con il motore
        // autoconfigurato da Spring Boot usato realmente in produzione. Sono comportamenti diversi
        // sull'accesso a chiave assente di una Map con notazione a punto (es. ${k.hero}): OGNL
        // restituisce null, SpEL lancia un'eccezione — un test con OGNL non avrebbe intercettato
        // il bug reale di produzione che ha causato l'introduzione di questo commento.
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        renderer = new ChromiumPdfRenderer(2, 5_000, 20_000);
        renderer.start();
        // render* non usano repository né OwnershipValidator → null è sufficiente per questo generatore di anteprime
        pdf = new PdfService(engine, null, null, calcoli, null, renderer);

        Path dir = Paths.get(System.getProperty("pdf.preview.dir", "target/pdf-preview"));
        Files.createDirectories(dir);

        Utente nut = new Utente();
        nut.setNome("Gabriel"); nut.setCognome("Spena"); nut.setEmail("gabriel.spena@statera.it");

        Cliente uomo = cliente("Vincenzo", "Gallo", Sesso.Maschio, LocalDate.of(1990, 5, 12), 92.0, nut);
        Cliente donna = cliente("Angela", "Rossi", Sesso.Femmina, LocalDate.of(1995, 9, 25), 63.0, nut);

        // --- MISURAZIONI ---
        MisurazioneAntropometrica mPrev = mis(uomo, LocalDate.of(2026, 3, 18), 92.0, 92.0, 103.0);
        MisurazioneAntropometrica mCur = mis(uomo, LocalDate.of(2026, 5, 15), 90.0, 90.0, 100.0);
        sezione(dir, "misurazione-confronto", "Misurazione antropometrica — con confronto",
                pdf.renderMisurazione(mCur, mPrev));
        sezione(dir, "misurazione-singola", "Misurazione antropometrica — prima rilevazione (senza confronto)",
                pdf.renderMisurazione(mCur, null));

        // --- PLICOMETRIE (ogni metodo) ---
        // Durnin-Womersley (4 pliche) con confronto
        Plicometria dw1 = plicDurnin(uomo, LocalDate.of(2026, 3, 18), 18, 10, 9, 20);
        Plicometria dw2 = plicDurnin(uomo, LocalDate.of(2026, 5, 15), 17, 8, 9, 16);
        sezione(dir, "plico-durnin", "Plicometria — Durnin-Womersley (4 pliche) con confronto",
                pdf.renderPlicometria(dw2, dw1));

        // Jackson-Pollock 7 (uomo)
        Plicometria jp7 = plicJP7(uomo, LocalDate.of(2026, 5, 15), 12, 10, 16, 14, 20, 18, 22);
        sezione(dir, "plico-jp7", "Plicometria — Jackson-Pollock 7 pliche (uomo)",
                pdf.renderPlicometria(jp7, null));

        // Jackson-Pollock 3 (donna): tricipite + sovrailiaca + coscia
        Plicometria jp3 = plicJP3donna(donna, LocalDate.of(2026, 5, 15), 18, 16, 24);
        sezione(dir, "plico-jp3-donna", "Plicometria — Jackson-Pollock 3 pliche (donna)",
                pdf.renderPlicometria(jp3, null));

        // Manuale (Parillo)
        Plicometria man = new Plicometria();
        man.setCliente(uomo); man.setMetodo(Metodo.PARILLO); man.setDataMisurazione(LocalDate.of(2026, 5, 15));
        man.setPercentualeMassaGrassa(22.5); man.setMassaGrassaKg(92.0 * 0.225); man.setMassaMagraKg(92.0 - 92.0 * 0.225);
        man.setNote("Valore stimato manualmente dal professionista.");
        sezione(dir, "plico-manuale", "Plicometria — metodo manuale (Parillo)",
                pdf.renderPlicometria(man, null));

        // --- SCHEDE (matrice: tema × macro × template) ---
        sezione(dir, "scheda-giornaliera", "Scheda giornaliera — tema emerald, con macro, dettagliato",
                pdf.renderScheda(schedaGiornaliera(uomo), true, null));
        nut.setPdfTemaColore(TemaPdf.ROSSO);
        sezione(dir, "scheda-giornaliera-rosso", "Scheda giornaliera — tema rosso",
                pdf.renderScheda(schedaGiornaliera(uomo), true, null));
        nut.setPdfTemaColore(TemaPdf.EMERALD);
        sezione(dir, "scheda-giornaliera-senza-macro", "Scheda giornaliera — senza grafico macro",
                pdf.renderScheda(schedaGiornaliera(uomo), false, null));
        sezione(dir, "scheda-giornaliera-essenziale", "Scheda giornaliera — template Essenziale",
                pdf.renderScheda(schedaGiornaliera(uomo), true, TemplatePdf.ESSENZIALE));
        sezione(dir, "scheda-settimanale", "Scheda settimanale — tema emerald, con macro, dettagliato",
                pdf.renderScheda(schedaSettimanale(uomo), true, null));
        nut.setPdfTemaColore(TemaPdf.ROSSO);
        sezione(dir, "scheda-settimanale-rosso", "Scheda settimanale — tema rosso",
                pdf.renderScheda(schedaSettimanale(uomo), true, null));
        nut.setPdfTemaColore(TemaPdf.EMERALD);
        sezione(dir, "scheda-settimanale-essenziale", "Scheda settimanale — template Essenziale",
                pdf.renderScheda(schedaSettimanale(uomo), true, TemplatePdf.ESSENZIALE));

        Path out = dir.resolve("anteprima.html");
        Files.writeString(out, html.toString());
        System.out.println("ANTEPRIME PDF GENERATE IN: " + out.toAbsolutePath());
    }

    // ---- rendering PDF → PNG base64 nella pagina HTML ----
    private void sezione(Path dir, String file, String titolo, byte[] pdfBytes) throws Exception {
        Files.write(dir.resolve(file + ".pdf"), pdfBytes);
        html.append("<h2 style=\"font:700 15px sans-serif;margin:28px 0 10px\">").append(titolo).append("</h2>");
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 96);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                html.append("<img style=\"display:block;width:100%;max-width:780px;border:1px solid #d0d0d0;")
                    .append("box-shadow:0 2px 8px rgba(0,0,0,.12);margin:0 0 18px\" src=\"data:image/png;base64,")
                    .append(b64).append("\"/>");
            }
        }
    }

    // ---- factory entità mock ----
    private Cliente cliente(String nome, String cognome, Sesso sesso, LocalDate nascita, double peso, Utente nut) {
        Cliente c = new Cliente();
        c.setNome(nome); c.setCognome(cognome); c.setSesso(sesso);
        c.setDataNascita(nascita); c.setPeso(peso); c.setNutrizionista(nut);
        return c;
    }

    private MisurazioneAntropometrica mis(Cliente c, LocalDate data, double peso, double vita, double fianchi) {
        MisurazioneAntropometrica m = new MisurazioneAntropometrica();
        m.setCliente(c); m.setDataMisurazione(data);
        m.setPeso(peso); m.setVita(vita); m.setFianchi(fianchi);
        return m;
    }

    private Plicometria plicBase(Cliente c, Metodo metodo, LocalDate data) {
        Plicometria p = new Plicometria();
        p.setCliente(c); p.setMetodo(metodo); p.setDataMisurazione(data);
        return p;
    }

    private void calcolaESetta(Plicometria p, Cliente c) {
        PlicometriaCalcoliService.Risultati r = calcoli.calcola(p, c);
        p.setSommaPliche(r.sommaPliche());
        p.setDensitaCorporea(r.densitaCorporea());
        p.setPercentualeMassaGrassa(r.percentualeMassaGrassa());
        p.setMassaGrassaKg(r.massaGrassaKg());
        p.setMassaMagraKg(r.massaMagraKg());
    }

    private Plicometria plicDurnin(Cliente c, LocalDate data, double tri, double bic, double sscap, double soil) {
        Plicometria p = plicBase(c, Metodo.DURNIN_WOMERSLEY, data);
        p.setTricipite(tri); p.setBicipite(bic); p.setSottoscapolare(sscap); p.setSovrailiaca(soil);
        calcolaESetta(p, c);
        return p;
    }

    private Plicometria plicJP7(Cliente c, LocalDate data, double pett, double asc, double tri, double sscap, double add, double soil, double cos) {
        Plicometria p = plicBase(c, Metodo.JACKSON_POLLOCK_7, data);
        p.setPettorale(pett); p.setAscellare(asc); p.setTricipite(tri); p.setSottoscapolare(sscap);
        p.setAddominale(add); p.setSovrailiaca(soil); p.setCoscia(cos);
        calcolaESetta(p, c);
        return p;
    }

    private Plicometria plicJP3donna(Cliente c, LocalDate data, double tri, double soil, double cos) {
        Plicometria p = plicBase(c, Metodo.JACKSON_POLLOCK_3, data);
        p.setTricipite(tri); p.setSovrailiaca(soil); p.setCoscia(cos);
        calcolaESetta(p, c);
        return p;
    }

    // ---- schede mock ----
    private AlimentoBase alimento(String nome, String categoria, double kcal, double prot, double carb, double grassi) {
        AlimentoBase a = new AlimentoBase();
        a.setNome(nome); a.setCategoria(categoria);
        Macro m = new Macro();
        m.setCalorie(kcal); m.setProteine(prot); m.setCarboidrati(carb); m.setGrassi(grassi); m.setFibre(2.0);
        a.setMacronutrienti(m);
        return a;
    }

    private AlimentoPasto ap(AlimentoBase a, int q, AlimentoAlternativo... alts) {
        AlimentoPasto x = new AlimentoPasto();
        x.setAlimento(a); x.setQuantita(q);
        if (alts.length > 0) {
            Set<AlimentoAlternativo> s = new LinkedHashSet<>();
            for (AlimentoAlternativo alt : alts) s.add(alt);
            x.setAlternative(s);
        }
        return x;
    }

    private AlimentoAlternativo alt(String nome, int q) {
        AlimentoAlternativo a = new AlimentoAlternativo();
        a.setNomeCustom(nome); a.setQuantita(q);
        return a;
    }

    private Pasto pasto(String nome, GiornoSettimana giorno, int ordine, LocalTime inizio, LocalTime fine, AlimentoPasto... alimenti) {
        Pasto p = new Pasto();
        p.setNome(nome); p.setGiorno(giorno); p.setOrdineVisualizzazione(ordine);
        p.setOrarioInizio(inizio); p.setOrarioFine(fine);
        Set<AlimentoPasto> s = new LinkedHashSet<>();
        for (AlimentoPasto a : alimenti) s.add(a);
        p.setAlimentiPasto(s);
        return p;
    }

    private Scheda schedaGiornaliera(Cliente c) {
        Scheda s = new Scheda();
        s.setCliente(c); s.setNome("Nuova Dieta 19/06/2026"); s.setTipo(TipoScheda.GIORNALIERA);
        s.setDataCreazione(LocalDate.of(2026, 6, 19));
        Set<Pasto> pasti = new LinkedHashSet<>();
        pasti.add(pasto("Colazione", GiornoSettimana.LUNEDI, 1, LocalTime.of(7, 0), LocalTime.of(9, 0),
                ap(alimento("Rice Pudding proteico", "Dairy desserts", 120, 9, 15, 3), 200),
                ap(alimento("Muesli con frutta", "Mueslis", 360, 10, 60, 8), 30)));
        pasti.add(pasto("Pranzo", GiornoSettimana.LUNEDI, 2, LocalTime.of(12, 30), LocalTime.of(14, 30),
                ap(alimento("Pasta", "Plant-based", 350, 12, 72, 2), 150),
                ap(alimento("Olio di oliva", "Oli e grassi", 900, 0, 0, 100), 10),
                ap(alimento("Pollo, fuso, con pelle", "Carni fresche", 200, 20, 0, 13), 200)));
        pasti.add(pasto("Cena", GiornoSettimana.LUNEDI, 3, LocalTime.of(19, 30), LocalTime.of(21, 30),
                ap(alimento("Olio di oliva", "Oli e grassi", 900, 0, 0, 100), 10),
                ap(alimento("Pasta", "Plant-based", 350, 12, 72, 2), 100,
                        alt("Pane bianco", 135), alt("Patate, crude", 500), alt("Riso Basmati", 100)),
                ap(alimento("Pollo, fuso, con pelle", "Carni fresche", 200, 20, 0, 13), 200,
                        alt("Bovino, filetto", 200), alt("Orata, surgelata", 270), alt("Gamberetti", 230))));
        s.setPasti(pasti);
        return s;
    }

    private Scheda schedaSettimanale(Cliente c) {
        Scheda s = new Scheda();
        s.setCliente(c); s.setNome("Piano Settimanale 19/06/2026"); s.setTipo(TipoScheda.SETTIMANALE);
        s.setDataCreazione(LocalDate.of(2026, 6, 19));
        Set<Pasto> pasti = new LinkedHashSet<>();
        GiornoSettimana[] giorni = {GiornoSettimana.LUNEDI, GiornoSettimana.MARTEDI, GiornoSettimana.MERCOLEDI};
        for (GiornoSettimana g : giorni) {
            pasti.add(pasto("Colazione", g, 1, LocalTime.of(7, 30), LocalTime.of(8, 30),
                    ap(alimento("Yogurt greco", "Latticini", 90, 9, 5, 4), 170),
                    ap(alimento("Avena", "Cereali", 370, 13, 60, 7), 40)));
            pasti.add(pasto("Pranzo", g, 2, LocalTime.of(13, 0), LocalTime.of(14, 0),
                    ap(alimento("Riso", "Cereali", 350, 7, 78, 1), 120),
                    ap(alimento("Petto di pollo", "Carni fresche", 165, 31, 0, 4), 180,
                            alt("Merluzzo", 220), alt("Tofu", 200))));
            pasti.add(pasto("Cena", g, 3, LocalTime.of(20, 0), LocalTime.of(21, 0),
                    ap(alimento("Uova", "Uova", 155, 13, 1, 11), 120),
                    ap(alimento("Verdure miste", "Ortaggi", 40, 2, 6, 1), 250)));
        }
        s.setPasti(pasti);
        return s;
    }
}
