package it.nutrizionista.restnutrizionista;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.entity.AccettazioneDocumento;
import it.nutrizionista.restnutrizionista.entity.AlimentoAlternativo;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AlimentoPasto;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoNomeOverride;
import it.nutrizionista.restnutrizionista.entity.AlimentoPastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.AlimentoSchedaTemplateAlternativa;
import it.nutrizionista.restnutrizionista.entity.AlternativeMode;
import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.AttivitaRecente;
import it.nutrizionista.restnutrizionista.entity.AuditLog;
import it.nutrizionista.restnutrizionista.entity.BadgeSbloccato;
import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.CredenzialeDemo;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.EventoGamification;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.MeseGratisRiscattato;
import it.nutrizionista.restnutrizionista.entity.MessaggioAssistenza;
import it.nutrizionista.restnutrizionista.entity.Metodo;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.ObiettivoNutrizionale;
import it.nutrizionista.restnutrizionista.entity.OrariStudio;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.PastoSchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.PastoTemplate;
import it.nutrizionista.restnutrizionista.entity.PastoTemplateAlimento;
import it.nutrizionista.restnutrizionista.entity.PastoTemplateAlternativo;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.PremioRiscattato;
import it.nutrizionista.restnutrizionista.entity.PresetObiettivo;
import it.nutrizionista.restnutrizionista.entity.ProgressioneNutrizionista;
import it.nutrizionista.restnutrizionista.entity.Promemoria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.SchedaTemplate;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TicketAssistenza;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.entity.UtentePreferito;
import it.nutrizionista.restnutrizionista.enums.AuditAction;
import it.nutrizionista.restnutrizionista.enums.AuditEntityType;
import it.nutrizionista.restnutrizionista.enums.AuditOutcome;
import it.nutrizionista.restnutrizionista.enums.StatoTicket;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.enums.TipoEventoGamification;
import it.nutrizionista.restnutrizionista.enums.TipoPremio;
import it.nutrizionista.restnutrizionista.repository.AccettazioneDocumentoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoAlternativoRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoNomeOverrideRepository;
import it.nutrizionista.restnutrizionista.repository.AlimentoPastoRepository;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.AttivitaRecenteRepository;
import it.nutrizionista.restnutrizionista.repository.AuditLogRepository;
import it.nutrizionista.restnutrizionista.repository.BadgeSbloccatoRepository;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.CredenzialeDemoRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.EventoGamificationRepository;
import it.nutrizionista.restnutrizionista.repository.MeseGratisRiscattatoRepository;
import it.nutrizionista.restnutrizionista.repository.MessaggioAssistenzaRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.ObiettivoNutrizionaleRepository;
import it.nutrizionista.restnutrizionista.repository.OrariStudioRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PastoTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.PremioRiscattatoRepository;
import it.nutrizionista.restnutrizionista.repository.PresetObiettivoRepository;
import it.nutrizionista.restnutrizionista.repository.ProgressioneNutrizionistaRepository;
import it.nutrizionista.restnutrizionista.repository.PromemoriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaTemplateRepository;
import it.nutrizionista.restnutrizionista.repository.TicketAssistenzaRepository;
import it.nutrizionista.restnutrizionista.repository.UtentePreferitoRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.UtenteService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * F-USER-DEL / A6 — regression-guard "seed-complete" della cancellazione ACCOUNT
 * ({@code UtenteService.deleteAccount}). Semina DUE nutrizionisti:
 * <ul>
 *   <li><b>U1</b> (da cancellare): 1 cliente con sotto-albero clinico COMPLETO (come in
 *       {@code ClienteDeleteCascadeCompletoIntegrationTest}) + UNA riga per OGNI figlio utente-owned
 *       non-cascade svuotato da {@code eliminaFigliNonCascadeUtente} (assistenza, preferiti, template
 *       pasto/scheda con l'intero sotto-albero, i 5 gamification, appuntamento manuale, promemoria,
 *       orari studio, preset obiettivo, attività recente residua, accettazione documento, credenziale
 *       demo, alimento PERSONALE). Punto critico: un {@code UtentePreferito} di U1 punta all'alimento
 *       personale di U1 → senza il delete dei preferiti PRIMA degli alimenti si avrebbe FK violation.</li>
 *   <li><b>U2</b> (deve restare INTATTO): 1 cliente + alcuni figli (appuntamento, preset, evento
 *       gamification, alimento personale) per verificare che l'erasure di U1 è scoped e non li tocca.</li>
 * </ul>
 * Il cuore del guard è {@code assertDoesNotThrow(deleteAccount(u1))}: se una tabella figlia con FK verso
 * {@code utenti} non viene svuotata (o nell'ordine sbagliato) la delete fallisce con FK violation e il test
 * diventa rosso. Poi si verifica: U1 sparito, zero residui in ogni tabella figlio di U1, alimenti globali
 * + dati di U2 intatti, e le righe A7 {@code DELETE} scritte dall'erasure PRESERVATE (retention art. 17(3)(b)).
 */
@SpringBootTest
@ActiveProfiles("test")
class UtenteDeleteAccountCascadeIntegrationTest extends SafeTestDatabaseBase {

	private static final String EMAIL_U1 = "nutriAccount@test.it";
	private static final String EMAIL_U2 = "nutriAltro@test.it";

	@Autowired private UtenteService utenteService;

	// ── Repository: cliente + sotto-albero clinico (riuso del template ClienteDeleteCascade...) ──
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
	@Autowired private AlimentoBaseRepository repoAlimento;
	@Autowired private AlimentoPastoRepository repoAlimentoPasto;
	@Autowired private AlimentoAlternativoRepository repoAlternativa;
	@Autowired private AlimentoPastoNomeOverrideRepository repoNomeOverride;
	@Autowired private EventoGamificationRepository repoEvento;
	@Autowired private AuditLogRepository repoAudit;

	// ── Repository: figli utente-owned non-cascade (F-USER-DEL) ──
	@Autowired private TicketAssistenzaRepository repoTicket;
	@Autowired private MessaggioAssistenzaRepository repoMessaggio;
	@Autowired private UtentePreferitoRepository repoPreferito;
	@Autowired private PastoTemplateRepository repoPastoTemplate;
	@Autowired private SchedaTemplateRepository repoSchedaTemplate;
	@Autowired private BadgeSbloccatoRepository repoBadge;
	@Autowired private ProgressioneNutrizionistaRepository repoProgressione;
	@Autowired private PremioRiscattatoRepository repoPremio;
	@Autowired private MeseGratisRiscattatoRepository repoMeseGratis;
	@Autowired private PromemoriaRepository repoPromemoria;
	@Autowired private OrariStudioRepository repoOrari;
	@Autowired private PresetObiettivoRepository repoPreset;
	@Autowired private AccettazioneDocumentoRepository repoAccettazione;
	@Autowired private CredenzialeDemoRepository repoCredenziale;

	/** Per le tabelle figlie dei template (nessun repository dedicato) e verifica zero-orfani a DB. */
	@Autowired private JdbcTemplate jdbc;

	@Test
	@WithMockUser(username = EMAIL_U1)
	void deleteAccount_conTuttiIFigliUtente_nonLasciaFkResidueEPreservaAltroTenant() throws Exception {

		// ══════════════════════════════════════════════════════════════════════════════════════════
		//  Catalogo AlimentoBase condiviso
		// ══════════════════════════════════════════════════════════════════════════════════════════
		// Globali (created_by=NULL): devono SOPRAVVIVERE alla cancellazione dell'account.
		AlimentoBase gPane = repoAlimento.save(aliment("Pane"));                 // usato in AlimentoPasto del cliente
		AlimentoBase gRiso = repoAlimento.save(aliment("Riso"));                 // usato in AlimentoAlternativo del cliente
		AlimentoBase gGlobale = repoAlimento.save(aliment("Globale Sopravvive")); // usato nei template + assert survivor
		final Long gGlobaleId = gGlobale.getId();

		// ── Nutrizionista U1 (proprietario, verrà cancellato) ──
		Utente u1 = new Utente();
		u1.setNome("Uno"); u1.setCognome("DaCancellare"); u1.setCodiceFiscale("AAAAAA00A00A000A");
		u1.setEmail(EMAIL_U1); u1.setPassword("x"); u1.setTelefono("111"); u1.setIndirizzo("via 1");
		u1 = repoUtente.save(u1);
		final Long u1Id = u1.getId();

		// ── Nutrizionista U2 (altro tenant, deve restare INTATTO) ──
		Utente u2 = new Utente();
		u2.setNome("Due"); u2.setCognome("DaPreservare"); u2.setCodiceFiscale("BBBBBB00A00A000B");
		u2.setEmail(EMAIL_U2); u2.setPassword("x"); u2.setTelefono("222"); u2.setIndirizzo("via 2");
		u2 = repoUtente.save(u2);
		final Long u2Id = u2.getId();

		// Alimenti personali: quello di U1 va cancellato, quello di U2 sopravvive.
		AlimentoBase pU1 = repoAlimento.save(alimentPersonale("Personale U1", u1));
		final Long pU1Id = pU1.getId();
		AlimentoBase pU2 = repoAlimento.save(alimentPersonale("Personale U2", u2));
		final Long pU2Id = pU2.getId();

		// ══════════════════════════════════════════════════════════════════════════════════════════
		//  U1 — cliente con sotto-albero clinico COMPLETO
		// ══════════════════════════════════════════════════════════════════════════════════════════
		Cliente c = new Cliente();
		c.setSesso(Sesso.Maschio);
		c.setNome("Mario"); c.setCognome("Storico");
		c.setCodiceFiscale("MRASTR00A00A000A"); c.setEmail("clienteU1@test.it");
		c.setDataNascita(LocalDate.of(1990, 1, 1));
		c.setPeso(70.0); c.setAltezza(175);
		c.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
		c.setIntolleranze("N"); c.setFunzioniIntestinali("N"); c.setProblematicheSalutari("N");
		c.setQuantitaEQualitaDelSonno("N"); c.setAssunzioneFarmaci("N");
		c.setBeveAlcol(false); c.setFuma(false);
		c.setNutrizionista(u1);
		c.setTagStandard(new HashSet<>(Set.of(TagStandard.ALL_GLUTINE)));
		Cliente cliente = repoCliente.save(c);
		final Long clienteId = cliente.getId();

		// Scheda → Pasto → AlimentoPasto → {AlimentoAlternativo (dual-FK) + NomeOverride}
		Scheda s = new Scheda(); s.setCliente(cliente); s.setNome("Dieta storica"); s.setAttiva(true);
		Scheda scheda = repoScheda.save(s);
		Pasto p = new Pasto(); p.setNome("Colazione"); p.setScheda(scheda);
		Pasto pasto = repoPasto.save(p);

		AlimentoPasto ap = repoAlimentoPasto.save(new AlimentoPasto(gPane, pasto, 100));
		AlimentoAlternativo alt = new AlimentoAlternativo();
		alt.setAlimentoPasto(ap);   // FK legacy
		alt.setPasto(pasto);        // FK per-pasto → dual-FK
		alt.setAlimentoAlternativo(gRiso);
		alt.setQuantita(120); alt.setPriorita(1); alt.setMode(AlternativeMode.CALORIE);
		repoAlternativa.save(alt);

		AlimentoPastoNomeOverride ovr = new AlimentoPastoNomeOverride();
		ovr.setAlimentoPasto(ap); ovr.setNomeCustom("Pane integrale");
		repoNomeOverride.save(ovr);

		// Calcolo TDEE
		CalcoloTdee calc = new CalcoloTdee();
		calc.setCliente(cliente); calc.setDataCalcolo(LocalDate.now());
		calc.setSesso("M"); calc.setEta(30); calc.setPeso(80.0); calc.setAltezza(180.0);
		calc.setLivelloAttivita(1.55); calc.setBmr(1500.0); calc.setTdee(2000.0);
		repoTdee.save(calc);

		// Documento fascicolo (record + file reale su disco)
		Path file = Files.createTempFile("account-cascade-test-", ".pdf");
		Files.write(file, new byte[] { 1, 2, 3 });
		DocumentoFascicolo doc = new DocumentoFascicolo();
		doc.setCliente(cliente); doc.setTitolo("Doc"); doc.setTipoDocumento(TipoDocumento.SCHEDA);
		doc.setPercorsoFile(file.toString());
		repoFascicolo.save(doc);

		// Misurazione, plicometria, obiettivo (collezioni cascade su Cliente)
		MisurazioneAntropometrica m = new MisurazioneAntropometrica();
		m.setCliente(cliente); repoMisurazione.save(m);
		Plicometria pl = new Plicometria();
		pl.setCliente(cliente); pl.setMetodo(Metodo.JACKSON_POLLOCK_3); repoPlicometria.save(pl);
		ObiettivoNutrizionale ob = new ObiettivoNutrizionale();
		ob.setCliente(cliente); repoObiettivo.save(ob);

		// Appuntamento cliente-linked (FK cliente_id, delete via erasure cliente)
		Appuntamento appCliente = new Appuntamento();
		appCliente.setCliente(cliente); appCliente.setNutrizionista(u1);
		appCliente.setData(LocalDate.now()); appCliente.setEndData(LocalDate.now());
		appCliente.setModalita(Appuntamento.Modalita.IN_STUDIO);
		appCliente.setStato(Appuntamento.StatoAppuntamento.PRENOTATO);
		repoAppuntamento.save(appCliente);

		// Attività recente cliente-linked
		AttivitaRecente arCliente = new AttivitaRecente();
		arCliente.setNutrizionista(u1); arCliente.setCliente(cliente);
		arCliente.setTipo("Nuovo Cliente"); arCliente.setDataAttivita(Instant.now());
		repoAttivita.save(arCliente);

		// EventoGamification cliente-linked (clienteId → anonimizzato in cliente-delete, poi rimosso in account-delete)
		EventoGamification evtCliente = new EventoGamification();
		evtCliente.setNutrizionista(u1); evtCliente.setTipoEvento(TipoEventoGamification.NUOVO_CLIENTE);
		evtCliente.setPunti(20); evtCliente.setClienteId(clienteId);
		repoEvento.save(evtCliente);

		// AuditLog pre-esistente riferito al cliente di U1 → deve SOPRAVVIVERE INTATTO (retention A7).
		AuditLog seed = new AuditLog(u1Id, EMAIL_U1, AuditAction.ACCESS, AuditEntityType.CLIENTE,
				clienteId, clienteId, AuditOutcome.SUCCESS, null, null, null);
		final Long auditSeedId = repoAudit.save(seed).getId();

		// ══════════════════════════════════════════════════════════════════════════════════════════
		//  U1 — figli utente-owned non-cascade (uno per ciascuna tabella svuotata dall'erasure account)
		// ══════════════════════════════════════════════════════════════════════════════════════════
		// (1) Assistenza: ticket + messaggio
		TicketAssistenza ticket = new TicketAssistenza();
		ticket.setNutrizionista(u1); ticket.setOggetto("Problema"); ticket.setDescrizione("Descrizione");
		ticket.setStato(StatoTicket.IN_ATTESA);
		ticket = repoTicket.save(ticket);
		MessaggioAssistenza messaggio = new MessaggioAssistenza();
		messaggio.setTicket(ticket); messaggio.setMittente(u1); messaggio.setTesto("Ciao");
		repoMessaggio.save(messaggio);

		// (2) Preferito → punta all'alimento PERSONALE di U1 (il "catch": FK violation senza delete-first dei preferiti)
		repoPreferito.save(new UtentePreferito(u1, pU1));

		// (2b) PastoTemplate (createdBy=U1) con alimento → alternativo (albero orphanRemoval)
		PastoTemplate pt = new PastoTemplate();
		pt.setNome("Template Pasto"); pt.setCreatedBy(u1);
		PastoTemplateAlimento pta = new PastoTemplateAlimento();
		pta.setTemplate(pt); pta.setAlimento(gGlobale); pta.setQuantita(100.0);
		PastoTemplateAlternativo ptAlt = new PastoTemplateAlternativo();
		ptAlt.setTemplateAlimento(pta); ptAlt.setAlimentoAlternativo(gPane);
		ptAlt.setQuantita(80); ptAlt.setPriorita(1); ptAlt.setMode(AlternativeMode.CALORIE);
		pta.getAlternative().add(ptAlt);
		pt.getAlimenti().add(pta);
		repoPastoTemplate.save(pt);

		// (2c) SchedaTemplate (createdBy=U1) con pasti → alimenti → alternative (albero orphanRemoval)
		SchedaTemplate st = new SchedaTemplate();
		st.setNome("Template Scheda"); st.setCreatedBy(u1);
		PastoSchedaTemplate pst = new PastoSchedaTemplate();
		pst.setNome("Colazione"); pst.setSchedaTemplate(st); pst.setOrdineVisualizzazione(0);
		AlimentoPastoSchedaTemplate apst = new AlimentoPastoSchedaTemplate();
		apst.setAlimento(gGlobale); apst.setPastoSchedaTemplate(pst); apst.setQuantita(100);
		AlimentoSchedaTemplateAlternativa asta = new AlimentoSchedaTemplateAlternativa();
		asta.setAlimentoPastoSchedaTemplate(apst); asta.setAlimentoAlternativo(gRiso);
		asta.setQuantita(90); asta.setPriorita(1); asta.setMode(AlternativeMode.CALORIE);
		apst.getAlternative().add(asta);
		pst.getAlimenti().add(apst);
		st.getPasti().add(pst);
		repoSchedaTemplate.save(st);

		// (3) Gamification (5 tabelle): evento puro + badge + progressione + premio + mese gratis
		EventoGamification evtU1 = new EventoGamification();
		evtU1.setNutrizionista(u1); evtU1.setTipoEvento(TipoEventoGamification.ACCESSO_GIORNALIERO); evtU1.setPunti(2);
		repoEvento.save(evtU1);
		BadgeSbloccato badge = new BadgeSbloccato();
		badge.setNutrizionista(u1); badge.setCodiceBadge("PRIMO_CLIENTE");
		repoBadge.save(badge);
		ProgressioneNutrizionista prog = new ProgressioneNutrizionista();
		prog.setNutrizionista(u1); prog.setPuntiTotali(100); prog.setPuntiRiscattabili(50);
		repoProgressione.save(prog);
		PremioRiscattato premio = new PremioRiscattato();
		premio.setNutrizionista(u1); premio.setTipoPremio(TipoPremio.SCONTO_20); premio.setPuntiSpesi(1500);
		repoPremio.save(premio);
		MeseGratisRiscattato mese = new MeseGratisRiscattato();
		mese.setNutrizionista(u1); mese.setPuntiSpesi(5000);
		repoMeseGratis.save(mese);

		// (4) Calendario/studio/preset/attività/accettazioni/demo
		Appuntamento appManuale = new Appuntamento();  // manuale, senza cliente (FK cliente_id null)
		appManuale.setNutrizionista(u1);
		appManuale.setData(LocalDate.now()); appManuale.setEndData(LocalDate.now());
		appManuale.setModalita(Appuntamento.Modalita.ONLINE);
		appManuale.setStato(Appuntamento.StatoAppuntamento.PRENOTATO);
		repoAppuntamento.save(appManuale);

		Promemoria promemoria = new Promemoria();
		promemoria.setNutrizionista(u1); promemoria.setTesto("Chiama fornitore"); promemoria.setData(LocalDate.now());
		repoPromemoria.save(promemoria);

		OrariStudio orari = new OrariStudio();
		orari.setNutrizionista(u1); orari.setGiornoSettimana(DayOfWeek.MONDAY); orari.setGiornoLavorativo(true);
		repoOrari.save(orari);

		PresetObiettivo preset = new PresetObiettivo();
		preset.setNutrizionista(u1); preset.setNome("Preset U1");
		preset.setPctProteine(30.0); preset.setPctCarboidrati(40.0); preset.setPctGrassi(30.0);
		preset.setMoltiplicatoreTdee(1.0);
		repoPreset.save(preset);

		AttivitaRecente arResidua = new AttivitaRecente();  // residua non-cliente (cliente_id null)
		arResidua.setNutrizionista(u1); arResidua.setTipo("Login"); arResidua.setDataAttivita(Instant.now());
		repoAttivita.save(arResidua);

		AccettazioneDocumento accettazione = new AccettazioneDocumento(
				u1, it.nutrizionista.restnutrizionista.enums.TipoDocumento.PRIVACY_POLICY, "v1", Instant.now());
		repoAccettazione.save(accettazione);

		CredenzialeDemo demo = new CredenzialeDemo();
		demo.setUtente(u1); demo.setUsername("demo-u1"); demo.setPasswordHash("hash");
		demo.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS)); demo.setCreatedByAdminId(999L);
		repoCredenziale.save(demo);

		// ══════════════════════════════════════════════════════════════════════════════════════════
		//  U2 — dati dell'altro tenant (devono restare intatti)
		// ══════════════════════════════════════════════════════════════════════════════════════════
		Cliente c2 = new Cliente();
		c2.setSesso(Sesso.Femmina);
		c2.setNome("Anna"); c2.setCognome("Altra");
		c2.setCodiceFiscale("NNAALT00A00A000B"); c2.setEmail("clienteU2@test.it");
		c2.setDataNascita(LocalDate.of(1992, 2, 2));
		c2.setPeso(60.0); c2.setAltezza(165);
		c2.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
		c2.setIntolleranze("N"); c2.setFunzioniIntestinali("N"); c2.setProblematicheSalutari("N");
		c2.setQuantitaEQualitaDelSonno("N"); c2.setAssunzioneFarmaci("N");
		c2.setBeveAlcol(false); c2.setFuma(false);
		c2.setNutrizionista(u2);
		c2.setTagStandard(new HashSet<>());
		Cliente cliente2 = repoCliente.save(c2);
		final Long cliente2Id = cliente2.getId();

		Appuntamento app2 = new Appuntamento();
		app2.setNutrizionista(u2); app2.setCliente(cliente2);
		app2.setData(LocalDate.now()); app2.setEndData(LocalDate.now());
		app2.setModalita(Appuntamento.Modalita.IN_STUDIO);
		app2.setStato(Appuntamento.StatoAppuntamento.PRENOTATO);
		repoAppuntamento.save(app2);

		PresetObiettivo preset2 = new PresetObiettivo();
		preset2.setNutrizionista(u2); preset2.setNome("Preset U2");
		preset2.setPctProteine(25.0); preset2.setPctCarboidrati(50.0); preset2.setPctGrassi(25.0);
		preset2.setMoltiplicatoreTdee(1.2);
		repoPreset.save(preset2);

		EventoGamification evtU2 = new EventoGamification();
		evtU2.setNutrizionista(u2); evtU2.setTipoEvento(TipoEventoGamification.SCHEDA_CREATA); evtU2.setPunti(10);
		final Long evtU2Id = repoEvento.save(evtU2).getId();

		// ── Precondizioni ──
		assertTrue(repoUtente.findById(u1Id).isPresent(), "precondizione: U1 presente");
		assertEquals(2, repoUtente.count(), "precondizione: due nutrizionisti");
		assertEquals(2, repoCliente.count(), "precondizione: due clienti");

		// ══════════════════════════════════════════════════════════════════════════════════════════
		//  AZIONE: cancellazione dell'account U1. Non deve lanciare FK violation né altro.
		// ══════════════════════════════════════════════════════════════════════════════════════════
		final Utente toDelete = u1;
		assertDoesNotThrow(() -> utenteService.deleteAccount(toDelete));

		// ══════════════════════════════════════════════════════════════════════════════════════════
		//  VERIFICHE — U1 e tutti i suoi figli spariti
		// ══════════════════════════════════════════════════════════════════════════════════════════
		assertTrue(repoUtente.findById(u1Id).isEmpty(), "U1 deve essere cancellato");

		// Sotto-albero clinico del cliente (U2 non ne ha) → zero
		assertEquals(0, repoScheda.count(), "schede rimosse");
		assertEquals(0, repoPasto.count(), "pasti rimossi");
		assertEquals(0, repoAlimentoPasto.count(), "alimenti_pasto rimossi");
		assertEquals(0, repoAlternativa.count(), "alternative rimosse (dual-FK)");
		assertEquals(0, repoNomeOverride.count(), "nome_override rimossi");
		assertEquals(0, repoTdee.count(), "calcoli TDEE rimossi");
		assertEquals(0, repoFascicolo.count(), "documenti fascicolo rimossi");
		assertEquals(0, repoMisurazione.count(), "misurazioni rimosse");
		assertEquals(0, repoPlicometria.count(), "plicometrie rimosse");
		assertEquals(0, repoObiettivo.count(), "obiettivi rimossi");
		assertTrue(Files.notExists(file), "il PDF del fascicolo deve essere rimosso dal disco");

		// Figli utente-owned SOLO di U1 → zero
		assertEquals(0, repoMessaggio.count(), "messaggi assistenza rimossi");
		assertEquals(0, repoTicket.count(), "ticket assistenza rimossi");
		assertEquals(0, repoPreferito.count(), "preferiti rimossi (catch FK: prima degli alimenti)");
		assertEquals(0, repoPastoTemplate.count(), "pasto-template rimossi");
		assertEquals(0, repoSchedaTemplate.count(), "scheda-template rimossi");
		assertEquals(0, repoBadge.count(), "badge rimossi");
		assertEquals(0, repoProgressione.count(), "progressione rimossa");
		assertEquals(0, repoPremio.count(), "premi rimossi");
		assertEquals(0, repoMeseGratis.count(), "mesi gratis rimossi");
		assertEquals(0, repoPromemoria.count(), "promemoria rimossi");
		assertEquals(0, repoOrari.count(), "orari studio rimossi");
		assertEquals(0, repoAccettazione.count(), "accettazioni rimosse");
		assertEquals(0, repoCredenziale.count(), "credenziali demo rimosse");

		// Sotto-alberi dei template: nessun orfano nelle tabelle figlie (nessun repo dedicato → JDBC)
		assertEquals(0, countRows("pasti_template_alimenti"), "pasti_template_alimenti orfani");
		assertEquals(0, countRows("pasti_template_alternative"), "pasti_template_alternative orfani");
		assertEquals(0, countRows("pasti_scheda_template"), "pasti_scheda_template orfani");
		assertEquals(0, countRows("alimenti_pasto_scheda_template"), "alimenti_pasto_scheda_template orfani");
		assertEquals(0, countRows("alimenti_scheda_template_alternative"), "alimenti_scheda_template_alternative orfani");

		// ══════════════════════════════════════════════════════════════════════════════════════════
		//  VERIFICHE — U2 (altro tenant) INTATTO + scoping delle tabelle condivise
		// ══════════════════════════════════════════════════════════════════════════════════════════
		assertTrue(repoUtente.findById(u2Id).isPresent(), "U2 deve restare");
		assertEquals(1, repoUtente.count(), "resta solo U2");
		assertTrue(repoCliente.findById(cliente2Id).isPresent(), "il cliente di U2 deve restare");
		assertEquals(1, repoCliente.count(), "resta solo il cliente di U2");

		// Tabelle CONDIVISE con U2: resta esattamente la riga di U2.
		assertEquals(1, repoAppuntamento.count(), "resta solo l'appuntamento di U2");
		assertEquals(1, repoPreset.count(), "resta solo il preset di U2");
		assertEquals(1, repoEvento.count(), "resta solo l'evento gamification di U2");
		assertTrue(repoEvento.findById(evtU2Id).isPresent(), "l'evento di U2 deve restare");
		// AttivitaRecente: U1 aveva la cliente-linked + la residua; U2 nessuna → zero.
		assertEquals(0, repoAttivita.count(), "attività recenti (solo U1) rimosse");

		// AlimentoBase: globali + personale di U2 sopravvivono; personale di U1 sparito.
		assertTrue(repoAlimento.findById(pU1Id).isEmpty(), "l'alimento personale di U1 deve essere cancellato");
		assertTrue(repoAlimento.findById(gGlobaleId).isPresent(), "gli alimenti globali devono sopravvivere");
		assertTrue(repoAlimento.findById(pU2Id).isPresent(), "l'alimento personale di U2 deve sopravvivere");
		assertEquals(4, repoAlimento.count(), "restano i 3 globali + il personale di U2");

		// ══════════════════════════════════════════════════════════════════════════════════════════
		//  VERIFICHE — Audit A7 (retention art. 17(3)(b)): NON si cancella
		// ══════════════════════════════════════════════════════════════════════════════════════════
		// La riga pre-esistente sopravvive INTATTA (target PER ID: l'erasure ne scrive un'altra con lo stesso clienteId).
		AuditLog auditDopo = repoAudit.findById(auditSeedId).orElse(null);
		assertNotNull(auditDopo, "la riga di audit pre-esistente deve sopravvivere alla cancellazione dell'account");
		assertEquals(clienteId, auditDopo.getClienteId(),
				"il clienteId dell'audit deve restare INTATTO (retention A7)");
		// L'erasure del cliente (dentro deleteAccount) ha scritto la riga A7 DELETE, anch'essa preservata.
		assertTrue(repoAudit.search(clienteId, null, AuditAction.DELETE, null, null, PageRequest.of(0, 10))
				.getTotalElements() >= 1,
				"deve esistere la riga A7 DELETE scritta dall'erasure del cliente (art. 17)");
	}

	// ── Helpers ──

	/** AlimentoBase GLOBALE (created_by=NULL): catalogo condiviso, sopravvive alla cancellazione account. */
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

	/** AlimentoBase PERSONALE (created_by valorizzato): del solo nutrizionista, cancellato con l'account. */
	private static AlimentoBase alimentPersonale(String nome, Utente owner) {
		AlimentoBase a = aliment(nome);
		a.setCreatedBy(owner);
		return a;
	}

	/** COUNT(*) su una tabella figlia priva di repository dedicato (nome lowercase, DATABASE_TO_UPPER=false). */
	private long countRows(String table) {
		Long n = jdbc.queryForObject("SELECT COUNT(*) FROM \"" + table + "\"", Long.class);
		return n == null ? 0L : n;
	}
}
