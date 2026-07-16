package it.nutrizionista.restnutrizionista;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

import it.nutrizionista.restnutrizionista.entity.Appuntamento;
import it.nutrizionista.restnutrizionista.entity.AttivitaRecente;
import it.nutrizionista.restnutrizionista.entity.CalcoloTdee;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Metodo;
import it.nutrizionista.restnutrizionista.entity.MisurazioneAntropometrica;
import it.nutrizionista.restnutrizionista.entity.ObiettivoNutrizionale;
import it.nutrizionista.restnutrizionista.entity.Pasto;
import it.nutrizionista.restnutrizionista.entity.Plicometria;
import it.nutrizionista.restnutrizionista.entity.Scheda;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.repository.AppuntamentoRepository;
import it.nutrizionista.restnutrizionista.repository.AttivitaRecenteRepository;
import it.nutrizionista.restnutrizionista.repository.CalcoloTdeeRepository;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.MisurazioneAntropometricaRepository;
import it.nutrizionista.restnutrizionista.repository.ObiettivoNutrizionaleRepository;
import it.nutrizionista.restnutrizionista.repository.PastoRepository;
import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;
import it.nutrizionista.restnutrizionista.repository.SchedaRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.ClienteService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * Cancellazione cliente "data-rich": semina un cliente con UN figlio per ogni tipo con FK diretta
 * a `cliente` (schede+pasto, calcoli TDEE, documenti di fascicolo, misurazioni, plicometrie,
 * obiettivi, appuntamenti, attività recenti, tag) e verifica che `deleteMyCliente` NON lasci FK
 * residue (nessuna `DataIntegrityViolationException`).
 *
 * <p>La completezza della cancellazione in `deleteMyCliente` è oggi una checklist manuale (il
 * cascade ORM copre solo le collezioni mappate su `Cliente`; appuntamenti/TDEE/fascicolo/attività
 * recenti vanno rimossi a mano): questo test scova in un colpo TUTTE le lacune del cascade a livello
 * cliente, invece di sistemarle una alla volta a ogni FK che salta fuori. È il guard di regressione
 * del finding F-DEL-CASCADE.</p>
 *
 * <p>Non seminati qui: la blacklist (`AvversionePersonale`) — è `cascade=ALL, orphanRemoval` su
 * `Cliente`, quindi mai a rischio — e l'albero alimenti sotto la scheda (`AlimentoPasto`/alternative,
 * dipendono dal catalogo `AlimentoBase`): la loro rimozione è testata nel delete di `SchedaService`.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ClienteDeleteCascadeCompletoIntegrationTest extends SafeTestDatabaseBase {

	private static final String EMAIL = "nutriCascade@test.it";

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

		// ── Un figlio per ogni tipo con FK diretta a cliente ──
		// Schede (+ pasto): branch a delete manuale bottom-up + cascade sulla scheda vuota.
		Scheda s = new Scheda(); s.setCliente(cliente); s.setNome("Dieta storica"); s.setAttiva(true);
		Scheda scheda = repoScheda.save(s);
		Pasto p = new Pasto(); p.setNome("Colazione"); p.setScheda(scheda);
		repoPasto.save(p);

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

		// Attività recente (FK cliente_id — il buco corretto da questo intervento).
		AttivitaRecente ar = new AttivitaRecente();
		ar.setNutrizionista(nutrizionista); ar.setCliente(cliente);
		ar.setTipo("Nuovo Cliente"); ar.setDataAttivita(Instant.now());
		repoAttivita.save(ar);

		// ── Precondizioni ──
		assertTrue(repoCliente.findById(id).isPresent(), "precondizione: cliente presente");
		assertTrue(repoAttivita.findByNutrizionista_IdAndCliente_Id(nutrizionista.getId(), id).isPresent(),
				"precondizione: attività recente presente");

		// ── Azione: cancellazione del cliente con tutto l'albero.
		//    Non deve lanciare DataIntegrityViolationException (FK residua) né altro. ──
		assertDoesNotThrow(() -> clienteService.deleteMyCliente(id));

		// ── Verifiche: cliente e figli con FK diretta rimossi ──
		assertTrue(repoCliente.findById(id).isEmpty(), "il cliente deve essere cancellato");
		assertTrue(repoAttivita.findByNutrizionista_IdAndCliente_Id(nutrizionista.getId(), id).isEmpty(),
				"le attività recenti devono essere rimosse (FK cliente_id)");
		assertTrue(repoScheda.findIdsByCliente_Id(id).isEmpty(), "le schede devono essere rimosse");

		Files.deleteIfExists(file);
	}
}
