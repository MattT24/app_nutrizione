package it.nutrizionista.restnutrizionista;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoNomeOverride;
import it.nutrizionista.restnutrizionista.entity.AlternativeMode;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.AttivitaRecente;
import it.nutrizionista.restnutrizionista.entity.AuditLog;
import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.EventoGamification;
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
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.enums.TipoEventoGamification;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoNomeOverrideRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.AttivitaRecenteRepository;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.EventoGamificationRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.ObiettivoNutrizionaleRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.ClienteService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * F-DEL-CASCADE — regression-guard della cancellazione cliente (art. 17 GDPR: nessun dato sanitario
 * orfano). Semina un cliente con UN figlio per OGNI tipo del grafo — diretti (calcoli TDEE, fascicolo
 * + file su disco, misurazioni, plicometrie, obiettivi, appuntamenti, attività recenti, tag),
 * transitivi (Scheda→Pasto→AlimentoPasto→{AlimentoAlternativo con ENTRAMBE le FK, NomeOverride}) e i
 * due riferimenti denormalizzati SENZA FK (EventoGamification.clienteId, AuditLog.clienteId) — poi
 * cancella e verifica in un colpo tutte le lacune del cascade.
 *
 * <p>La completezza di {@code deleteMyCliente} è oggi <b>sistematica</b> (ORM-first ibrido:
 * cascade/orphanRemoval sulle collezioni di {@code Cliente} + delete espliciti nel service per i figli
 * a FK non-mappata + unlink file su disco). Questo test è il guard che rende rossa qualunque
 * regressione (una nuova entità figlia non gestita → orfano rilevato subito).</p>
 *
 * <p><b>Trattamento OPPOSTO dei due {@code clienteId} denormalizzati (verificato qui):</b></p>
 * <ul>
 *   <li>{@code EventoGamification.clienteId} → <b>anonimizzato a NULL</b> (recide il legame col paziente
 *       cancellato, art. 17), l'evento sopravvive;</li>
 *   <li>{@code AuditLog.clienteId} → <b>preservato INTATTO</b> (retention A7, obbligo legale
 *       art. 17(3)(b)). L'asserzione colpisce la riga pre-seminata <b>per id</b>, non un
 *       {@code exists(clienteId=id)}: {@code deleteMyCliente} scrive essa stessa una riga A7 DELETE con
 *       lo stesso {@code clienteId=id}, che maschererebbe la sparizione di quella seminata.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class ClienteDeleteCascadeCompletoIntegrationTest extends SafeTestDatabaseBase {

	private static final String EMAIL = "nutriCascade@test.it";
	/** clienteId sintetico (senza FK) usato come controllo di scoping dell'anonimizzazione. */
	private static final long ALTRO_CLIENTE_ID = 999_999L;

	@Autowired private ClienteService clienteService;
	@Autowired private UtenteRepository repoUtente;
	@Autowired private ClienteRepository repoCliente;
	@Autowired private SchedaRepository repoScheda;
	@Autowired private PastoRepository repoPasto;
	@Autowired private CalcoloTdeeRepository repoTdee;
	@Autowired private DocumentoFascicoloRepository repoFascicolo;
	@Autowired private MisurazioneAntropometricaRepository repoMisurazione;
	@Autowired private PlicometriaRepository repoPlicometria;
	@Autowired private ObiettivoNutrizionaleRepository repoObiettivo;
	@Autowired private AppuntamentoRepository repoAppuntamento;
	@Autowired private AttivitaRecenteRepository repoAttivita;
	// F-DEL-CASCADE: sotto-albero transitivo + i due riferimenti denormalizzati no-FK.
	@Autowired private AlimentoBaseRepository repoAlimento;
	@Autowired private AlimentoPastoRepository repoAlimentoPasto;
	@Autowired private AlimentoAlternativoRepository repoAlternativa;
	@Autowired private AlimentoPastoNomeOverrideRepository repoNomeOverride;
	@Autowired private EventoGamificationRepository repoEvento;
	@Autowired private AuditLogRepository repoAudit;

	@Test
	@WithMockUser(username = EMAIL)
	void deleteCliente_conTuttiIFigli_nonLasciaFkResidue() throws Exception {
		// ── Nutrizionista proprietario ──
		Utente u = new Utente();
		u.setNome("Nutri"); u.setCognome("Cascade"); u.setCodiceFiscale("CSCCSC00A00A000A");
		u.setEmail(EMAIL); u.setPassword("x"); u.setTelefono("000"); u.setIndirizzo("x");
		Utente nutrizionista = repoUtente.save(u);

		// ── Cliente (con tag clinici: ElementCollection) ──
		Cliente c = new Cliente();
		c.setSesso(Sesso.Maschio);
		c.setNome("Mario"); c.setCognome("Storico");
		c.setCodiceFiscale("MRASTR00A00A000A"); c.setEmail("clienteCascade@test.it");
		c.setDataNascita(LocalDate.of(1990, 1, 1));
		c.setPeso(70.0); c.setAltezza(175);
		c.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
		c.setIntolleranze("N"); c.setFunzioniIntestinali("N"); c.setProblematicheSalutari("N");
		c.setQuantitaEQualitaDelSonno("N"); c.setAssunzioneFarmaci("N");
		c.setBeveAlcol(false); c.setFuma(false);
		c.setNutrizionista(nutrizionista);
		c.setTagStandard(new HashSet<>(Set.of(TagStandard.ALL_GLUTINE)));
		Cliente cliente = repoCliente.save(c);
		final Long id = cliente.getId();

		// ── Sotto-albero transitivo: Scheda → Pasto → AlimentoPasto → {AlimentoAlternativo (ENTRAMBE
		//    le FK, come il codice reale) + NomeOverride}. Il catalogo AlimentoBase NON è figlio del
		//    cliente e deve SOPRAVVIVERE. ──
		Scheda s = new Scheda(); s.setCliente(cliente); s.setNome("Dieta storica"); s.setAttiva(true);
		Scheda scheda = repoScheda.save(s);
		Pasto p = new Pasto(); p.setNome("Colazione"); p.setScheda(scheda);
		Pasto pasto = repoPasto.save(p);

		AlimentoBase alimento = repoAlimento.save(aliment("Pane"));
		AlimentoBase alimentoAlt = repoAlimento.save(aliment("Riso"));
		AlimentoPasto ap = repoAlimentoPasto.save(new AlimentoPasto(alimento, pasto, 100));

		AlimentoAlternativo alt = new AlimentoAlternativo();
		alt.setAlimentoPasto(ap);   // FK legacy
		alt.setPasto(pasto);        // FK per-pasto → entrambe valorizzate (dual-FK)
		alt.setAlimentoAlternativo(alimentoAlt);
		alt.setQuantita(120); alt.setPriorita(1); alt.setMode(AlternativeMode.CALORIE);
		repoAlternativa.save(alt);

		AlimentoPastoNomeOverride ovr = new AlimentoPastoNomeOverride();
		ovr.setAlimentoPasto(ap); ovr.setNomeCustom("Pane integrale");
		repoNomeOverride.save(ovr);

		// Calcolo TDEE.
		CalcoloTdee calc = new CalcoloTdee();
		calc.setCliente(cliente); calc.setDataCalcolo(LocalDate.now());
		calc.setSesso("M"); calc.setEta(30); calc.setPeso(80.0); calc.setAltezza(180.0);
		calc.setLivelloAttivita(1.55); calc.setBmr(1500.0); calc.setTdee(2000.0);
		repoTdee.save(calc);

		// Documento di fascicolo (record + file reale su disco).
		Path file = Files.createTempFile("cascade-test-", ".pdf");
		Files.write(file, new byte[] { 1, 2, 3 });
		DocumentoFascicolo doc = new DocumentoFascicolo();
		doc.setCliente(cliente); doc.setTitolo("Doc"); doc.setTipoDocumento(TipoDocumento.SCHEDA);
		doc.setPercorsoFile(file.toString());
		repoFascicolo.save(doc);

		// Misurazione, plicometria, obiettivo (collezioni cascade su Cliente).
		MisurazioneAntropometrica m = new MisurazioneAntropometrica();
		m.setCliente(cliente); repoMisurazione.save(m);
		Plicometria pl = new Plicometria();
		pl.setCliente(cliente); pl.setMetodo(Metodo.JACKSON_POLLOCK_3); repoPlicometria.save(pl);
		ObiettivoNutrizionale ob = new ObiettivoNutrizionale();
		ob.setCliente(cliente); repoObiettivo.save(ob);

		// Appuntamento (FK cliente_id, delete manuale).
		Appuntamento app = new Appuntamento();
		app.setCliente(cliente); app.setNutrizionista(nutrizionista);
		app.setData(LocalDate.now()); app.setEndData(LocalDate.now());
		app.setModalita(Appuntamento.Modalita.IN_STUDIO);
		app.setStato(Appuntamento.StatoAppuntamento.PRENOTATO);
		repoAppuntamento.save(app);

		// Attività recente (FK cliente_id, delete manuale).
		AttivitaRecente ar = new AttivitaRecente();
		ar.setNutrizionista(nutrizionista); ar.setCliente(cliente);
		ar.setTipo("Nuovo Cliente"); ar.setDataAttivita(Instant.now());
		repoAttivita.save(ar);

		// ── Riferimenti denormalizzati SENZA FK: trattamento OPPOSTO ──
		// EventoGamification riferito a QUESTO cliente → dopo il delete deve sopravvivere con clienteId=NULL.
		EventoGamification evtCliente = new EventoGamification();
		evtCliente.setNutrizionista(nutrizionista); evtCliente.setTipoEvento(TipoEventoGamification.NUOVO_CLIENTE);
		evtCliente.setPunti(20); evtCliente.setClienteId(id);
		final Long evtClienteId = repoEvento.save(evtCliente).getId();

		// Controllo di scoping: evento con clienteId DIVERSO → l'anonimizzazione NON deve toccarlo.
		EventoGamification evtAltro = new EventoGamification();
		evtAltro.setNutrizionista(nutrizionista); evtAltro.setTipoEvento(TipoEventoGamification.SCHEDA_CREATA);
		evtAltro.setPunti(10); evtAltro.setClienteId(ALTRO_CLIENTE_ID);
		final Long evtAltroId = repoEvento.save(evtAltro).getId();

		// AuditLog pre-esistente riferito a questo cliente → deve sopravvivere INTATTO (clienteId valorizzato).
		AuditLog seed = new AuditLog(nutrizionista.getId(), EMAIL, AuditAction.ACCESS, AuditEntityType.CLIENTE,
				id, id, AuditOutcome.SUCCESS, null, null, null);
		final Long auditSeedId = repoAudit.save(seed).getId();

		// ── Precondizioni ──
		assertTrue(repoCliente.findById(id).isPresent(), "precondizione: cliente presente");
		assertTrue(repoAttivita.findByNutrizionista_IdAndCliente_Id(nutrizionista.getId(), id).isPresent(),
				"precondizione: attività recente presente");

		// ── Azione: cancellazione del cliente con tutto l'albero.
		//    Non deve lanciare DataIntegrityViolationException (FK residua) né altro. ──
		assertDoesNotThrow(() -> clienteService.deleteMyCliente(id));

		// ── Verifiche: cliente rimosso ──
		assertTrue(repoCliente.findById(id).isEmpty(), "il cliente deve essere cancellato");

		// ── Zero orfani in OGNI tabella figlia (il DB è troncato pre-test → solo questo test scrive) ──
		assertEquals(0, repoScheda.findIdsByCliente_Id(id).size(), "schede rimosse");
		assertEquals(0, repoPasto.count(), "pasti rimossi");
		assertEquals(0, repoAlimentoPasto.count(), "alimenti_pasto rimossi");
		assertEquals(0, repoAlternativa.count(), "alternative rimosse (dual-FK)");
		assertEquals(0, repoNomeOverride.count(), "nome_override rimossi");
		assertEquals(0, repoTdee.count(), "calcoli TDEE rimossi");
		assertEquals(0, repoFascicolo.count(), "documenti fascicolo rimossi");
		assertEquals(0, repoMisurazione.count(), "misurazioni rimosse");
		assertEquals(0, repoPlicometria.count(), "plicometrie rimosse");
		assertEquals(0, repoObiettivo.count(), "obiettivi rimossi");
		assertEquals(0, repoAppuntamento.count(), "appuntamenti rimossi");
		assertTrue(repoAttivita.findByNutrizionista_IdAndCliente_Id(nutrizionista.getId(), id).isEmpty(),
				"attività recenti rimosse (FK cliente_id)");
		assertTrue(Files.notExists(file), "il PDF del fascicolo deve essere rimosso dal disco");

		// ── Catalogo AlimentoBase NON è figlio del cliente → sopravvive ──
		assertEquals(2, repoAlimento.count(), "il catalogo alimenti non deve essere toccato");

		// ── EventoGamification: sopravvive; clienteId del paziente cancellato → NULL, l'altro intatto ──
		assertEquals(2, repoEvento.count(), "gli eventi gamification non vanno cancellati");
		assertNull(repoEvento.findById(evtClienteId).orElseThrow().getClienteId(),
				"l'evento del cliente cancellato deve essere anonimizzato (clienteId=NULL)");
		assertEquals(ALTRO_CLIENTE_ID, repoEvento.findById(evtAltroId).orElseThrow().getClienteId(),
				"l'anonimizzazione deve essere scoped: eventi di altri clienti non toccati");

		// ── AuditLog: la riga pre-seminata sopravvive INTATTA. Target PER ID (non exists(clienteId=id)):
		//    deleteMyCliente scrive anche una riga A7 DELETE con lo stesso clienteId, che maschererebbe
		//    la sparizione di quella seminata. ──
		AuditLog auditDopo = repoAudit.findById(auditSeedId).orElse(null);
		assertNotNull(auditDopo, "la riga di audit pre-esistente deve sopravvivere alla cancellazione");
		assertEquals(id, auditDopo.getClienteId(),
				"il clienteId dell'audit deve restare INTATTO (retention A7, art. 17(3)(b))");
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
