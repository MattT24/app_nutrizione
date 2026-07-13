package it.nutrizionista.restnutrizionista;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.DocumentoFascicolo;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.entity.TipoDocumento;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.repository.ClienteRepository;
import it.nutrizionista.restnutrizionista.repository.DocumentoFascicoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.ClienteService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * A5.1 — la cancellazione del cliente deve rimuovere anche i documenti di fascicolo (record) e i
 * relativi file su disco: nessun record orfano (FK cliente_id NOT NULL), nessun PDF sanitario
 * lasciato sul filesystem.
 */
@SpringBootTest
@ActiveProfiles("test")
class ClienteDeleteFascicoloIntegrationTest extends SafeTestDatabaseBase {

	private static final String EMAIL = "nutriDel@test.it";

	@Autowired private ClienteService clienteService;
	@Autowired private UtenteRepository repoUtente;
	@Autowired private ClienteRepository repoCliente;
	@Autowired private DocumentoFascicoloRepository repoFascicolo;

	@Test
	@WithMockUser(username = EMAIL)
	void deleteCliente_rimuoveDocumentiFascicoloEFileSuDisco() throws Exception {
		Utente u = new Utente();
		u.setNome("Nutri"); u.setCognome("Del"); u.setCodiceFiscale("DELDEL00A00A000A");
		u.setEmail(EMAIL); u.setPassword("x"); u.setTelefono("000"); u.setIndirizzo("x");
		Utente nutrizionista = repoUtente.save(u);

		Cliente c = new Cliente();
		c.setSesso(Sesso.Maschio);
		c.setNome("Mario"); c.setCognome("Rossi");
		c.setCodiceFiscale("MRARSS00A00A000A"); c.setEmail("clienteDel@test.it");
		c.setDataNascita(LocalDate.of(1990, 1, 1));
		c.setPeso(70.0); c.setAltezza(175);
		c.setLivelloDiAttivita(LivelloDiAttivita.SEDENTARIO);
		c.setIntolleranze("N"); c.setFunzioniIntestinali("N"); c.setProblematicheSalutari("N");
		c.setQuantitaEQualitaDelSonno("N"); c.setAssunzioneFarmaci("N");
		c.setBeveAlcol(false); c.setFuma(false);
		c.setNutrizionista(nutrizionista);
		Cliente cliente = repoCliente.save(c);

		// File fisico reale che rappresenta il PDF del documento
		Path file = Files.createTempFile("fascicolo-test-", ".pdf");
		Files.write(file, new byte[] { 1, 2, 3 });

		DocumentoFascicolo doc = new DocumentoFascicolo();
		doc.setCliente(cliente);
		doc.setTitolo("Documento Test");
		doc.setTipoDocumento(TipoDocumento.SCHEDA);
		doc.setPercorsoFile(file.toString());
		repoFascicolo.save(doc);

		assertTrue(Files.exists(file), "precondizione: il file esiste");
		assertFalse(repoFascicolo.findByClienteIdOrderByDataCreazioneDesc(cliente.getId()).isEmpty(),
				"precondizione: il documento è presente");

		// Azione: cancellazione del cliente (come nutrizionista proprietario)
		clienteService.deleteMyCliente(cliente.getId());

		// Verifiche: nessun documento orfano, nessun file su disco, cliente rimosso
		assertTrue(repoFascicolo.findByClienteIdOrderByDataCreazioneDesc(cliente.getId()).isEmpty(),
				"i documenti di fascicolo devono essere cancellati (no orfani)");
		assertFalse(Files.exists(file), "il file PDF su disco deve essere stato rimosso");
		assertTrue(repoCliente.findById(cliente.getId()).isEmpty(), "il cliente deve essere cancellato");
	}
}
