package it.nutrizionista.restnutrizionista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import it.nutrizionista.restnutrizionista.dto.RetentionReport;
import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoNomeOverride;
import it.nutrizionista.restnutrizionista.entity.AlternativeMode;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.AuditLog;
import it.nutrizionista.restnutrizionista.entity.AvversionePersonale;
import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.Metodo;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.ObiettivoNutrizionale;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoNomeOverrideRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;
import it.nutrizionista.restnutrizionista.repository.AvversionePersonaleRepository;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.ObiettivoNutrizionaleRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.RetentionService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * A6 — Retention / storage limitation (art. 5(1)(e) + art. 17 GDPR). Gate di sicurezza sul job che, allo
 * scadere della conservazione dei dati clinici, mette in quarantena e poi <b>cancella fisicamente</b> i
 * clienti inattivi. Operazione irreversibile a massimo raggio → la direzione d'errore deve essere SEMPRE
 * verso l'over-retention: mai cancellare un cliente attivo.
 *
 * <p><b>Meccaniche del test</b> (non-transazionale: il ciclo apre tx proprie; il DB è troncato per-test):
 * <ul>
 *   <li>Config sul bean-proxy AOP → override via {@link AopTestUtils#getTargetObject} + {@link ReflectionTestUtils}
 *       sul <b>target</b> (settare i {@code @Value} sul proxy non li raggiungerebbe).</li>
 *   <li>Dati "vecchi" simulati retrodatando i timestamp {@code @CreatedDate}/{@code @LastModifiedDate} via
 *       <b>UPDATE nativa (JdbcTemplate)</b> — impossibile via entità (l'{@code AuditingEntityListener}
 *       ri-bumperebbe {@code updatedAt} al save). JdbcTemplate committa subito (autocommit) e le query del
 *       service leggono in tx nuove → nessun bisogno di {@code em.clear()}.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = RetentionServiceIntegrationTest.EMAIL)
class RetentionServiceIntegrationTest extends SafeTestDatabaseBase {

    static final String EMAIL = "nutriRetention@test.it";

    @Autowired private RetentionService retentionService;
    @Autowired private JdbcTemplate jdbc;

    @Autowired private UtenteRepository repoUtente;
    @Autowired private ClienteRepository repoCliente;
    @Autowired private SchedaRepository repoScheda;
    @Autowired private PastoRepository repoPasto;
    @Autowired private AlimentoBaseRepository repoAlimento;
    @Autowired private AlimentoPastoRepository repoAlimentoPasto;
    @Autowired private AlimentoAlternativoRepository repoAlternativa;
    @Autowired private AlimentoPastoNomeOverrideRepository repoNomeOverride;
    @Autowired private MisurazioneAntropometricaRepository repoMisurazione;
    @Autowired private PlicometriaRepository repoPlicometria;
    @Autowired private ObiettivoNutrizionaleRepository repoObiettivo;
    @Autowired private AvversionePersonaleRepository repoAvversione;
    @Autowired private AppuntamentoRepository repoAppuntamento;
    @Autowired private CalcoloTdeeRepository repoTdee;
    @Autowired private DocumentoFascicoloRepository repoFascicolo;
    @Autowired private AuditLogRepository repoAudit;

    private Utente nutrizionista;

    // Ancore temporali con margini enormi rispetto ai confini (10 anni / 90 giorni): eventuali derive di
    // fuso orario nella conversione JDBC↔Hibernate (poche ore) non attraversano mai queste soglie.
    private static final OffsetDateTime OLD = LocalDate.now(ZoneOffset.UTC).minusYears(15)
            .atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    private static final LocalDate OLD_DATE = LocalDate.now(ZoneOffset.UTC).minusYears(15);

    // Cutoff usato per interrogare DIRETTAMENTE la query (matrice di eleggibilità) con years=10 (default).
    private static final LocalDate CUTOFF_DATE = LocalDate.now(ZoneOffset.UTC).minusYears(10);
    private static final Instant CUTOFF = CUTOFF_DATE.atStartOfDay(ZoneOffset.UTC).toInstant();

    @BeforeEach
    void seedNutrizionistaEResetConfig() {
        // La superclasse ha già troncato tutte le tabelle. Ricrea l'attore (serve a getMe() nell'audit del purge).
        Utente u = new Utente();
        u.setNome("Nutri"); u.setCognome("Retention"); u.setCodiceFiscale("RTNRTN00A00A000A");
        u.setEmail(EMAIL); u.setPassword("x"); u.setTelefono("000"); u.setIndirizzo("x");
        nutrizionista = repoUtente.save(u);
        resetConfigDefaults();
    }

    @AfterEach
    void ripristinaConfig() {
        resetConfigDefaults(); // igiene cross-classe: il RetentionService è un singleton condiviso dal contesto
    }

    // ───────────────────────────── 0. Prova che l'override config sul proxy AOP FUNZIONA ─────────────────────────────

    /** Sanity: con dry-run=false (settato sul TARGET via AopTestUtils) una quarantena reale viene scritta. */
    @Test
    void overrideConfig_dryRunFalse_marcaQuarantenaReale() {
        Cliente c = seedClienteBase("A");
        retrodataClienteRow(c.getId(), OLD);
        cfg("dryRun", false);

        RetentionReport r = retentionService.eseguiCicloRetention();

        assertNotNull(r, "il ciclo deve produrre un report");
        assertFalse(r.dryRun(), "l'override dryRun=false deve avere effetto sul target del proxy");
        assertEquals(1, r.eseguitiQuarantena(), "il cliente inattivo deve essere quarantenato");
        assertNotNull(reload(c.getId()).getDataQuarantena(), "dataQuarantena scritta ⇒ override raggiunge il bean");
    }

    // ───────────────────────────── 1. Matrice per-clausola (criterio VINCOLANTE) ─────────────────────────────

    /**
     * Un cliente "vecchio" con UN figlio recente a OGNI livello del grafo NON è eleggibile; tutto vecchio →
     * eleggibile. Dimostra esplicitamente che l'attività sui SOLI figli tiene il cliente FUORI dal purge
     * (un figlio non coperto dalla query → un cliente attivo verrebbe cancellato: difetto di sicurezza).
     */
    @Test
    void matricePerClausola_attivitaSuOgniFiglioTieneFuoriDalPurge() {
        Cliente c = seedClienteBase("Matrix");
        Long id = c.getId();
        seedSottoAlberoCompleto(c); // schede→pasti→alimenti_pasto→{alternativa,nomeOverride} + mis/plico/ob/avv/fasc/app/tdee

        Map<String, Runnable> clausole = new LinkedHashMap<>();
        clausole.put("scheda", () -> recentTs("schede"));
        clausole.put("pasto", () -> recentTs("pasti"));
        clausole.put("alimentoPasto", () -> recentTs("alimenti_pasto"));
        clausole.put("alimentoAlternativo", () -> recentTs("alimenti_alternativi"));
        clausole.put("nomeOverride", () -> recentTs("alimenti_pasto_nome_override"));
        clausole.put("misurazione", () -> recentTs("misurazioni_antropometriche"));
        clausole.put("plicometria", () -> recentTs("plicometrie"));
        clausole.put("obiettivo", () -> recentTs("obiettivi_nutrizionali"));
        clausole.put("avversionePersonale", () -> recentTs("avversione_personale_cliente"));
        clausole.put("documentoFascicolo", () -> jdbc.update("UPDATE documenti_fascicolo SET data_creazione=?", nowOdt()));
        clausole.put("appuntamento", () -> recentTs("appuntamenti"));
        clausole.put("calcoloTdee", () -> jdbc.update("UPDATE calcoli_tdee SET data_calcolo=?", LocalDate.now(ZoneOffset.UTC)));
        clausole.put("ultimoContattoClinico",
                () -> jdbc.update("UPDATE clienti SET ultimo_contatto_clinico=? WHERE id=?", nowOdt(), id));

        for (Map.Entry<String, Runnable> e : clausole.entrySet()) {
            retrodataCompleto(id);          // tutto vecchio
            e.getValue().run();             // un solo segnale recente
            assertFalse(idsEleggibili().contains(id),
                    "attività recente su '" + e.getKey() + "' deve tenere il cliente FUORI dal purge (query incompleta?)");
        }

        retrodataCompleto(id); // tutto vecchio → eleggibile
        assertTrue(idsEleggibili().contains(id), "con tutti i timestamp vecchi il cliente deve essere eleggibile");
    }

    // ───────────────────────────── 2. Floor childless ─────────────────────────────

    @Test
    void floorChildless_createdAtRecenteNonEleggibile_vecchioEleggibile() {
        Cliente c = seedClienteBase("Floor");
        Long id = c.getId();

        // createdAt recente (appena salvato), nessun figlio → NON eleggibile (floor immutabile).
        assertFalse(idsEleggibili().contains(id), "cliente creato di recente non è mai eleggibile");

        // createdAt vecchio + ultimoContattoClinico vecchio/null → eleggibile.
        retrodataClienteRow(id, OLD);
        assertTrue(idsEleggibili().contains(id), "cliente vecchio senza figli è eleggibile");
    }

    // ───────────────────────────── 3. Segnale d'aggregato (ultimoContattoClinico) ─────────────────────────────

    @Test
    void segnaleAggregato_soloUltimoContattoRecenteNonEleggibile() {
        Cliente c = seedClienteBase("Aggregato");
        Long id = c.getId();
        // createdAt vecchio ma ultimoContattoClinico recente (nessun figlio) → NON eleggibile.
        jdbc.update("UPDATE clienti SET created_at=?, ultimo_contatto_clinico=? WHERE id=?", OLD, nowOdt(), id);
        assertFalse(idsEleggibili().contains(id),
                "un contatto clinico d'aggregato recente deve tenere il cliente fuori dal purge");
    }

    // ───────────────────────────── 4. Legal hold / trattamentoLimitato ─────────────────────────────

    @Test
    void legalHoldOTrattamentoLimitato_scadutoNonPurgato_compareInHold() {
        Cliente hold = seedClienteBase("Hold");
        Cliente limitato = seedClienteBase("Limitato");
        Cliente normale = seedClienteBase("Normale");
        retrodataClienteRow(hold.getId(), OLD);
        retrodataClienteRow(limitato.getId(), OLD);
        retrodataClienteRow(normale.getId(), OLD);
        jdbc.update("UPDATE clienti SET legal_hold=TRUE WHERE id=?", hold.getId());
        jdbc.update("UPDATE clienti SET trattamento_limitato=TRUE WHERE id=?", limitato.getId());
        cfg("dryRun", false);

        RetentionReport r = retentionService.eseguiCicloRetention();

        assertEquals(2, r.inHold(), "hold + limitato devono essere entrambi in hold");
        assertTrue(r.idInHold().contains(hold.getId()), "legalHold in idInHold");
        assertTrue(r.idInHold().contains(limitato.getId()), "trattamentoLimitato in idInHold");
        assertNull(reload(hold.getId()).getDataQuarantena(), "il cliente in legal hold NON va quarantenato");
        assertNull(reload(limitato.getId()).getDataQuarantena(), "il cliente limitato NON va quarantenato");
        assertNotNull(reload(normale.getId()).getDataQuarantena(), "il cliente senza hold va quarantenato");
    }

    // ───────────────────────────── 5. Dry-run: zero scritture ─────────────────────────────

    @Test
    void dryRun_default_nessunaScrittura_reportPianificato() {
        Cliente c = seedClienteBase("DryRun");
        retrodataClienteRow(c.getId(), OLD);
        // dryRun resta true (default)

        RetentionReport r = retentionService.eseguiCicloRetention();

        assertTrue(r.dryRun(), "il report deve dichiarare la modalità dry-run");
        assertEquals(1, r.pianificatiQuarantena(), "would-quarantena calcolato");
        assertEquals(0, r.eseguitiQuarantena(), "in dry-run nessuna quarantena eseguita");
        assertNull(reload(c.getId()).getDataQuarantena(), "in dry-run NON deve essere scritto dataQuarantena");
    }

    // ───────────────────────────── 6. Quarantena → grace → purge ─────────────────────────────

    @Test
    void quarantena_grace_purge_percorsoCompleto() {
        Cliente c = seedClienteBase("Grace");
        Long id = c.getId();
        retrodataClienteRow(id, OLD);
        cfg("dryRun", false);

        // Run 1: marca la quarantena (dataQuarantena = adesso) + audit RETENTION_QUARANTENA.
        RetentionReport r1 = retentionService.eseguiCicloRetention();
        assertEquals(1, r1.eseguitiQuarantena());
        assertEquals(0, r1.eseguitiPurge(), "appena quarantenato non è ancora oltre la grace");
        assertNotNull(reload(id).getDataQuarantena(), "quarantena marcata");
        assertTrue(auditPresente(AuditAction.RETENTION_QUARANTENA, id), "audit RETENTION_QUARANTENA scritto");

        // Run 2 (subito dopo): dataQuarantena è adesso, dentro la grace → NON purga.
        RetentionReport r2 = retentionService.eseguiCicloRetention();
        assertEquals(0, r2.eseguitiPurge(), "dentro la grace non deve purgare");
        assertTrue(repoCliente.findById(id).isPresent(), "cliente ancora presente prima della grace");

        // Retrodata la quarantena oltre la grace (90 gg) → purga.
        jdbc.update("UPDATE clienti SET data_quarantena=? WHERE id=?", odt(Instant.now().minus(Duration.ofDays(200))), id);
        RetentionReport r3 = retentionService.eseguiCicloRetention();
        assertEquals(1, r3.eseguitiPurge(), "oltre la grace deve purgare");
        assertTrue(r3.idPurge().contains(id));
        assertTrue(repoCliente.findById(id).isEmpty(), "cliente cancellato dopo la grace");
        assertTrue(auditPresente(AuditAction.RETENTION_PURGE, id), "audit RETENTION_PURGE per-id scritto");
    }

    // ───────────────────────────── 7. TOCTOU: ri-verifica in-tx sotto lock ─────────────────────────────

    /**
     * {@code purgaCliente} è invocato direttamente (metodo di sicurezza): un cliente quarantenato-oltre-grace
     * a cui, dopo lo snapshot, riappare attività recente deve essere SALTATO dalla ri-verifica in-tx. Con la
     * stessa query e senza concorrenza il ciclo completo non può divergere; testare il guard in isolamento è
     * la verifica deterministica del meccanismo TOCTOU (#1).
     */
    @Test
    void toctou_attivitaRiappareDopoSnapshot_purgaClienteSalta() {
        LocalDate cutoffDate = LocalDate.now(ZoneOffset.UTC).minusYears(10);
        Instant cutoff = cutoffDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant graceCutoff = Instant.now().minus(Duration.ofDays(90));

        // Controllo POSITIVO: cliente davvero inattivo + quarantenato oltre grace → purgaCliente ritorna true.
        Cliente ok = seedClienteBase("PurgeOk");
        retrodataClienteRow(ok.getId(), OLD);
        jdbc.update("UPDATE clienti SET data_quarantena=? WHERE id=?", odt(Instant.now().minus(Duration.ofDays(200))), ok.getId());
        boolean purgato = retentionService.purgaCliente(ok.getId(), cutoff, cutoffDate, graceCutoff);
        assertTrue(purgato, "un cliente realmente inattivo e oltre-grace deve essere purgato (positivo)");
        assertTrue(repoCliente.findById(ok.getId()).isEmpty(), "il controllo positivo deve essere cancellato");

        // TOCTOU: cliente quarantenato oltre grace MA con attività recente (una misurazione) → SALTA.
        Cliente c = seedClienteBase("Toctou");
        MisurazioneAntropometrica m = new MisurazioneAntropometrica();
        m.setCliente(c); repoMisurazione.save(m); // misurazione recente = attività riapparsa dopo lo snapshot
        retrodataClienteRow(c.getId(), OLD);
        jdbc.update("UPDATE clienti SET data_quarantena=? WHERE id=?", odt(Instant.now().minus(Duration.ofDays(200))), c.getId());

        boolean saltato = retentionService.purgaCliente(c.getId(), cutoff, cutoffDate, graceCutoff);

        assertFalse(saltato, "la ri-verifica in-tx deve SALTARE (attività recente riapparsa)");
        assertTrue(repoCliente.findById(c.getId()).isPresent(), "il cliente con attività recente NON deve essere cancellato");
    }

    // ───────────────────────────── 8a. Breaker catastrofico (gatea mark+purge, non il clear) ─────────────────────────────

    @Test
    void breaker_troppiEleggibili_saltaMarkEPurge_maIlClearGira() {
        // 12 clienti eleggibili + 1 di controllo (recente) quarantenato-stale da ripulire → total=13.
        for (int i = 0; i < 12; i++) {
            Cliente c = seedClienteBase("Breaker" + i);
            retrodataClienteRow(c.getId(), OLD);
        }
        // Controllo: recente (NON eleggibile) ma con dataQuarantena stantia → il clear deve ripulirlo (R1).
        Cliente stale = seedClienteBase("Stale");
        jdbc.update("UPDATE clienti SET data_quarantena=? WHERE id=?", odt(Instant.now().minus(Duration.ofDays(10))), stale.getId());
        cfg("dryRun", false);

        RetentionReport r = retentionService.eseguiCicloRetention();

        assertTrue(r.breakerScattato(), "12 eleggibili su 13 (>20% e >10) deve far scattare il breaker");
        assertEquals(0, r.eseguitiQuarantena(), "il breaker deve saltare la marcatura");
        assertEquals(0, r.eseguitiPurge(), "il breaker deve saltare il purge");
        assertNull(reload(stale.getId()).getDataQuarantena(),
                "il clear protettivo deve girare ANCHE a breaker scattato (R1): quarantena stantia rimossa");
    }

    // ───────────────────────────── 8b. Rate-limit: cap oldest-first + backlog ─────────────────────────────

    @Test
    void rateLimit_purgabiliOltreCap_processatiOldestFirst_conBacklog() {
        cfg("dryRun", false);
        cfg("maxPurgePerRun", 2);

        // 4 clienti eleggibili e già quarantenati oltre grace, con dataQuarantena a distanze crescenti.
        // 4 su 4 NON fa scattare il breaker (4 non è > safetyMinAbs=10).
        int[] giorni = { 400, 300, 200, 100 };
        Long[] ids = new Long[4];
        for (int i = 0; i < 4; i++) {
            Cliente c = seedClienteBase("Rate" + i);
            ids[i] = c.getId();
            retrodataClienteRow(c.getId(), OLD);
            jdbc.update("UPDATE clienti SET data_quarantena=? WHERE id=?",
                    odt(Instant.now().minus(Duration.ofDays(giorni[i]))), c.getId());
        }

        RetentionReport r = retentionService.eseguiCicloRetention();

        assertFalse(r.breakerScattato(), "4 eleggibili non devono far scattare il breaker (floor 10)");
        assertEquals(2, r.eseguitiPurge(), "solo maxPurgePerRun=2 purgati per run");
        assertEquals(2, r.backlogPurge(), "gli altri 2 restano come backlog (nessun troncamento silenzioso)");
        // Oldest-first (R4): i due dataQuarantena più vecchi (400, 300 gg) purgati per primi.
        assertTrue(repoCliente.findById(ids[0]).isEmpty(), "il più vecchio (400 gg) deve essere purgato");
        assertTrue(repoCliente.findById(ids[1]).isEmpty(), "il secondo più vecchio (300 gg) deve essere purgato");
        assertTrue(repoCliente.findById(ids[2]).isPresent(), "il più recente (200 gg) resta nel backlog");
        assertTrue(repoCliente.findById(ids[3]).isPresent(), "il più recente (100 gg) resta nel backlog");
        assertTrue(r.idPurge().contains(ids[0]) && r.idPurge().contains(ids[1]), "idPurge = i due più vecchi");
    }

    // ───────────────────────────── 9. Recovery / clear durante la grace ─────────────────────────────

    @Test
    void clear_recuperoAttivitaOHold_durantLaGrace() {
        cfg("dryRun", false);

        // (a) quarantenato che torna attivo (createdAt recente) → non più inattivo → clear.
        Cliente recuperato = seedClienteBase("Recuperato");
        jdbc.update("UPDATE clienti SET data_quarantena=? WHERE id=?", odt(Instant.now().minus(Duration.ofDays(10))), recuperato.getId());
        // createdAt resta recente (appena salvato) → findInattivoById empty → clear.

        // (b) quarantenato entrato in hold → clear (ramo distinto), pur essendo vecchio.
        Cliente inHold = seedClienteBase("InHold");
        retrodataClienteRow(inHold.getId(), OLD);
        jdbc.update("UPDATE clienti SET data_quarantena=?, legal_hold=TRUE WHERE id=?",
                odt(Instant.now().minus(Duration.ofDays(10))), inHold.getId());

        retentionService.eseguiCicloRetention();

        assertNull(reload(recuperato.getId()).getDataQuarantena(), "attività riapparsa durante la grace → quarantena azzerata");
        assertNull(reload(inHold.getId()).getDataQuarantena(), "hold durante la quarantena → quarantena azzerata");
    }

    // ───────────────────────────── 10. Purge completo: nessun orfano, PDF rimosso, audit isolato ─────────────────────────────

    @Test
    void purgeCompleto_nessunOrfano_pdfRimosso_auditAltroClienteSopravvive() throws Exception {
        // Cliente da purgare: sotto-albero completo + PDF su disco.
        Cliente vittima = seedClienteBase("Vittima");
        Long vittimaId = vittima.getId();
        Path pdf = seedSottoAlberoCompleto(vittima);
        retrodataCompleto(vittimaId);
        jdbc.update("UPDATE clienti SET data_quarantena=? WHERE id=?", odt(Instant.now().minus(Duration.ofDays(200))), vittimaId);

        // Cliente di CONTROLLO (recente, senza figli) + una riga di audit pre-esistente → devono sopravvivere.
        Cliente altro = seedClienteBase("AltroControllo");
        Long altroId = altro.getId();
        AuditLog seed = new AuditLog(nutrizionista.getId(), EMAIL, AuditAction.ACCESS, AuditEntityType.CLIENTE,
                altroId, altroId, AuditOutcome.SUCCESS, null, null, null);
        Long auditAltroId = repoAudit.save(seed).getId();

        cfg("dryRun", false);
        RetentionReport r = retentionService.eseguiCicloRetention();

        assertEquals(1, r.eseguitiPurge(), "solo la vittima è purgabile (l'altro è recente)");
        // Nessun orfano: la vittima possedeva tutto il sotto-albero (l'altro non ha figli).
        assertTrue(repoCliente.findById(vittimaId).isEmpty(), "vittima cancellata");
        assertEquals(0, repoScheda.findIdsByCliente_Id(vittimaId).size(), "schede rimosse");
        assertEquals(0, repoPasto.count(), "pasti rimossi");
        assertEquals(0, repoAlimentoPasto.count(), "alimenti_pasto rimossi");
        assertEquals(0, repoAlternativa.count(), "alternative rimosse (dual-FK)");
        assertEquals(0, repoNomeOverride.count(), "nome_override rimossi");
        assertEquals(0, repoMisurazione.count(), "misurazioni rimosse");
        assertEquals(0, repoPlicometria.count(), "plicometrie rimosse");
        assertEquals(0, repoObiettivo.count(), "obiettivi rimossi");
        assertEquals(0, repoAvversione.count(), "blacklist rimossa");
        assertEquals(0, repoAppuntamento.count(), "appuntamenti rimossi");
        assertEquals(0, repoTdee.count(), "calcoli TDEE rimossi");
        assertEquals(0, repoFascicolo.count(), "documenti fascicolo rimossi");
        assertTrue(Files.notExists(pdf), "il PDF del fascicolo deve essere rimosso dal disco (afterCommit)");

        // Asserzioni negative: il cliente di controllo e la sua riga di audit sopravvivono.
        assertTrue(repoCliente.findById(altroId).isPresent(), "il cliente di controllo NON deve essere toccato");
        assertNotNull(repoAudit.findById(auditAltroId).orElse(null), "l'audit di un ALTRO cliente deve sopravvivere");
        assertTrue(auditPresente(AuditAction.RETENTION_PURGE, vittimaId), "riga RETENTION_PURGE per-id della vittima");
    }

    // ───────────────────────────── 11. Config-driven: retention.clinical.years rispettato ─────────────────────────────

    @Test
    void configDriven_yearsRispettato() {
        cfg("clinicalYears", 5); // cutoff = adesso - 5 anni

        Cliente vecchio = seedClienteBase("Sette");  // creato 7 anni fa → eleggibile con years=5
        Cliente recente = seedClienteBase("Tre");     // creato 3 anni fa → NON eleggibile con years=5
        jdbc.update("UPDATE clienti SET created_at=? WHERE id=?",
                odt(LocalDate.now(ZoneOffset.UTC).minusYears(7).atStartOfDay(ZoneOffset.UTC).toInstant()), vecchio.getId());
        jdbc.update("UPDATE clienti SET created_at=? WHERE id=?",
                odt(LocalDate.now(ZoneOffset.UTC).minusYears(3).atStartOfDay(ZoneOffset.UTC).toInstant()), recente.getId());

        RetentionReport r = retentionService.eseguiCicloRetention(); // dry-run: conta soltanto

        assertEquals(1, r.inattivi(), "con years=5 solo il cliente di 7 anni è inattivo");
    }

    // ───────────────────────────── F1. Disgiunto updatedAt esercitato DA SOLO (over-deletion) ─────────────────────────────

    /**
     * Il caso più comune di attività: un record clinico VECCHIO ri-editato di RECENTE (created_at vecchio,
     * updated_at recente). Per ognuna delle 10 clausole a doppio-timestamp, se sparisse il disgiunto
     * {@code OR x.updatedAt >= :cutoff} il cliente tornerebbe eleggibile → un paziente attivo verrebbe
     * PURGATO. Qui created_at ed updated_at sono settati SEPARATAMENTE (vecchio/recente), a differenza di
     * {@link #matricePerClausola_attivitaSuOgniFiglioTieneFuoriDalPurge} che li muove insieme: così tolto
     * un qualsiasi {@code OR updatedAt} un'asserzione diventa rossa.
     */
    @Test
    void f1_disgiuntoUpdatedAt_recordVecchioRieditatoDiRecenteNonEleggibile() {
        Cliente c = seedClienteBase("F1");
        Long id = c.getId();
        seedSottoAlberoCompleto(c);

        List<String> tabelleDoppioTs = List.of(
                "schede", "pasti", "alimenti_pasto", "alimenti_alternativi", "alimenti_pasto_nome_override",
                "misurazioni_antropometriche", "plicometrie", "obiettivi_nutrizionali",
                "avversione_personale_cliente", "appuntamenti");

        for (String t : tabelleDoppioTs) {
            retrodataCompleto(id);              // cliente + tutti i figli: created_at ED updated_at VECCHI
            oldCreatedRecentUpdated(t);         // solo QUESTA clausola: created_at vecchio, updated_at RECENTE
            assertFalse(idsEleggibili().contains(id),
                    "record vecchio ri-editato di recente su '" + t + "' (solo updatedAt recente) deve tenere il "
                            + "cliente FUORI dal purge: disgiunto 'OR updatedAt >= cutoff' mancante?");
        }

        // DocumentoFascicolo è 2-signal ma NON usa created_at/updated_at: data_creazione + data_ultimo_invio
        // (merge #104: data ultimo invio email). Documento VECCHIO ma RE-INVIATO di recente → il disgiunto
        // data_ultimo_invio deve tenere il cliente fuori dal purge (mutation-killer del nuovo OR).
        retrodataCompleto(id);
        jdbc.update("UPDATE documenti_fascicolo SET data_creazione=?, data_ultimo_invio=?", OLD, nowOdt());
        assertFalse(idsEleggibili().contains(id),
                "documento fascicolo vecchio ma con data_ultimo_invio recente deve tenere il cliente FUORI dal "
                        + "purge: disgiunto 'OR d.dataUltimoInvio >= cutoff' mancante?");
    }

    // ───────────────────────────── F2. Dry-run blinda il ramo purge (over-deletion) ─────────────────────────────

    /**
     * {@code purgaCliente} è dry-run-blind: l'unica protezione è l'early-return in {@code eseguiInterno}.
     * Un cliente quarantenato OLTRE la grace (che in un run reale verrebbe cancellato) NON deve essere
     * toccato in dry-run. Blinda il ramo purge contro futuri refactor.
     */
    @Test
    void f2_dryRun_nonPurgaClienteOltreLaGrace() {
        Cliente c = seedClienteBase("F2");
        Long id = c.getId();
        retrodataClienteRow(id, OLD);
        // Quarantenato oltre la grace (90 gg) → in un ciclo REALE sarebbe purgato.
        jdbc.update("UPDATE clienti SET data_quarantena=? WHERE id=?",
                odt(Instant.now().minus(Duration.ofDays(200))), id);
        // dryRun resta true (default sicuro)

        RetentionReport r = retentionService.eseguiCicloRetention();

        assertTrue(r.dryRun(), "il report deve dichiarare la modalità dry-run");
        assertEquals(1, r.pianificatiPurge(), "il ramo purge SAREBBE scattato in un run reale (would-purge=1)");
        assertEquals(0, r.eseguitiPurge(), "in dry-run nessun purge reale");
        assertTrue(repoCliente.findById(id).isPresent(), "in dry-run il cliente NON deve essere cancellato");
        assertFalse(auditPresente(AuditAction.RETENTION_PURGE, id), "in dry-run nessuna riga RETENTION_PURGE");
    }

    // ═════════════════════════════════ Helpers ═════════════════════════════════

    private List<Long> idsEleggibili() {
        return repoCliente.findInattiviPerRetention(CUTOFF, CUTOFF_DATE).stream().map(Cliente::getId).toList();
    }

    private Cliente reload(Long id) {
        return repoCliente.findById(id).orElseThrow();
    }

    private boolean auditPresente(AuditAction action, Long clienteId) {
        return repoAudit.findAll().stream()
                .anyMatch(a -> a.getAction() == action && clienteId.equals(a.getClienteId()));
    }

    private void cfg(String field, Object value) {
        // Il target reale dietro il proxy AOP (i @Value settati sul proxy non lo raggiungerebbero).
        // Tipizzato a RetentionService: evita che l'inferenza generica scelga l'overload setField(Class,..).
        RetentionService target = AopTestUtils.getTargetObject(retentionService);
        ReflectionTestUtils.setField(target, field, value);
    }

    private void resetConfigDefaults() {
        cfg("clinicalYears", 10);
        cfg("quarantineDays", 90);
        cfg("dryRun", true);
        cfg("safetyPercent", 20);
        cfg("safetyMinAbs", 10);
        cfg("maxPurgePerRun", 50);
    }

    private static OffsetDateTime nowOdt() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static OffsetDateTime odt(Instant i) {
        return i.atOffset(ZoneOffset.UTC);
    }

    /** Retrodata la sola riga clienti (created_at + ultimo_contatto_clinico) — per clienti childless. */
    private void retrodataClienteRow(Long id, OffsetDateTime ts) {
        jdbc.update("UPDATE clienti SET created_at=?, ultimo_contatto_clinico=? WHERE id=?", ts, ts, id);
    }

    /** Retrodata la riga clienti + TUTTE le tabelle figlie (il cliente possiede l'intero sotto-albero). */
    private void retrodataCompleto(Long id) {
        retrodataClienteRow(id, OLD);
        oldTs("schede");
        oldTs("pasti");
        oldTs("alimenti_pasto");
        oldTs("alimenti_alternativi");
        oldTs("alimenti_pasto_nome_override");
        oldTs("misurazioni_antropometriche");
        oldTs("plicometrie");
        oldTs("obiettivi_nutrizionali");
        oldTs("avversione_personale_cliente");
        oldTs("appuntamenti");
        jdbc.update("UPDATE documenti_fascicolo SET data_creazione=?", OLD);
        jdbc.update("UPDATE calcoli_tdee SET data_calcolo=?", OLD_DATE);
    }

    private void oldTs(String table) {
        jdbc.update("UPDATE " + table + " SET created_at=?, updated_at=?", OLD, OLD);
    }

    private void recentTs(String table) {
        OffsetDateTime now = nowOdt();
        jdbc.update("UPDATE " + table + " SET created_at=?, updated_at=?", now, now);
    }

    /** created_at VECCHIO, updated_at RECENTE: esercita da solo il disgiunto {@code OR updatedAt >= cutoff} (F1). */
    private void oldCreatedRecentUpdated(String table) {
        jdbc.update("UPDATE " + table + " SET created_at=?, updated_at=?", OLD, nowOdt());
    }

    private Cliente seedClienteBase(String nome) {
        Cliente c = new Cliente();
        c.setSesso(Sesso.Maschio);
        c.setNome(nome); c.setCognome("Retention");
        c.setDataNascita(LocalDate.of(1980, 1, 1));
        c.setPeso(70.0); c.setAltezza(175);
        c.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
        c.setIntolleranze("N"); c.setFunzioniIntestinali("N"); c.setProblematicheSalutari("N");
        c.setQuantitaEQualitaDelSonno("N"); c.setAssunzioneFarmaci("N");
        c.setBeveAlcol(false); c.setFuma(false);
        c.setNutrizionista(nutrizionista);
        return repoCliente.save(c);
    }

    /** Semina un figlio per OGNI livello del grafo; ritorna il path del PDF di fascicolo su disco. */
    private Path seedSottoAlberoCompleto(Cliente cliente) {
        try {
            Scheda s = new Scheda(); s.setCliente(cliente); s.setNome("Dieta"); s.setAttiva(true);
            Scheda scheda = repoScheda.save(s);
            Pasto p = new Pasto(); p.setNome("Colazione"); p.setScheda(scheda);
            Pasto pasto = repoPasto.save(p);

            AlimentoBase alimento = repoAlimento.save(aliment("Pane"));
            AlimentoBase alimentoAlt = repoAlimento.save(aliment("Riso"));
            AlimentoPasto ap = repoAlimentoPasto.save(new AlimentoPasto(alimento, pasto, 100));

            AlimentoAlternativo alt = new AlimentoAlternativo();
            alt.setAlimentoPasto(ap); alt.setPasto(pasto); alt.setAlimentoAlternativo(alimentoAlt);
            alt.setQuantita(120); alt.setPriorita(1); alt.setMode(AlternativeMode.CALORIE);
            repoAlternativa.save(alt);

            AlimentoPastoNomeOverride ovr = new AlimentoPastoNomeOverride();
            ovr.setAlimentoPasto(ap); ovr.setNomeCustom("Pane integrale");
            repoNomeOverride.save(ovr);

            MisurazioneAntropometrica m = new MisurazioneAntropometrica();
            m.setCliente(cliente); repoMisurazione.save(m);
            Plicometria pl = new Plicometria();
            pl.setCliente(cliente); pl.setMetodo(Metodo.JACKSON_POLLOCK_3); repoPlicometria.save(pl);
            ObiettivoNutrizionale ob = new ObiettivoNutrizionale();
            ob.setCliente(cliente); repoObiettivo.save(ob);
            repoAvversione.save(new AvversionePersonale(cliente, alimento, LivelloAllerta.WARNING, "n"));

            Appuntamento app = new Appuntamento();
            app.setCliente(cliente); app.setNutrizionista(nutrizionista);
            app.setData(LocalDate.now()); app.setEndData(LocalDate.now());
            app.setModalita(Appuntamento.Modalita.IN_STUDIO);
            app.setStato(Appuntamento.StatoAppuntamento.PRENOTATO);
            repoAppuntamento.save(app);

            CalcoloTdee calc = new CalcoloTdee();
            calc.setCliente(cliente); calc.setDataCalcolo(LocalDate.now());
            calc.setSesso("M"); calc.setEta(30); calc.setPeso(80.0); calc.setAltezza(180.0);
            calc.setLivelloAttivita(1.55); calc.setBmr(1500.0); calc.setTdee(2000.0);
            repoTdee.save(calc);

            Path file = Files.createTempFile("retention-test-", ".pdf");
            Files.write(file, new byte[] { 1, 2, 3 });
            DocumentoFascicolo doc = new DocumentoFascicolo();
            doc.setCliente(cliente); doc.setTitolo("Doc"); doc.setTipoDocumento(TipoDocumento.SCHEDA);
            doc.setPercorsoFile(file.toString());
            repoFascicolo.save(doc);
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static AlimentoBase aliment(String nome) {
        AlimentoBase a = new AlimentoBase();
        a.setNome(nome);
        a.setMisuraInGrammi(100.0);
        Macro macro = new Macro();
        macro.setAlimento(a);
        macro.setCalorie(100.0); macro.setProteine(10.0); macro.setCarboidrati(10.0); macro.setGrassi(10.0);
        a.setMacroNutrienti(macro);
        return a;
    }
}
