package it.nutrizionista.restnutrizionista.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.entity.LivelloDiAttivita;
import it.nutrizionista.restnutrizionista.entity.Sesso;
import it.nutrizionista.restnutrizionista.repository.ObiettivoNutrizionaleRepository;
import it.nutrizionista.restnutrizionista.service.ObiettivoNutrizionaleService.CalcoloResult;

/**
 * Unit test (Mockito, niente Spring/DB) sul calcolo BMR/TDEE.
 * Copre la regressione del bug: cliente con peso/altezza null non deve generare
 * NullPointerException ma una risposta "campi mancanti" pulita.
 */
@ExtendWith(MockitoExtension.class)
class ObiettivoNutrizionaleServiceTest {

	@Mock
	private ObiettivoNutrizionaleRepository repo;
	@Mock
	private OwnershipValidator ownershipValidator;

	@InjectMocks
	private ObiettivoNutrizionaleService service;

	@Test
	void calcola_clienteSenzaPeso_ritornaCampiMancantiSenzaEccezione() {
		Cliente c = new Cliente();
		// peso = null (caso che prima generava NPE in verificaCampiCalcolo)
		c.setAltezza(180);
		c.setSesso(Sesso.Maschio);
		c.setDataNascita(LocalDate.of(1990, 1, 1));
		c.setLivelloDiAttivita(LivelloDiAttivita.MODERATAMENTE_ATTIVO);
		when(ownershipValidator.getOwnedCliente(1L)).thenReturn(c);

		CalcoloResult res = service.calcola(1L);

		assertFalse(res.isSuccesso(), "Con peso null il calcolo non deve riuscire");
		assertTrue(res.campiMancanti().contains("peso"), "Deve segnalare 'peso' tra i campi mancanti");
	}

	@Test
	void calcola_clienteCompleto_calcolaBmrETdeeCorretti() {
		Cliente c = new Cliente();
		c.setPeso(80.0);
		c.setAltezza(180);
		c.setSesso(Sesso.Maschio);
		c.setDataNascita(LocalDate.now().minusYears(30));
		c.setLivelloDiAttivita(LivelloDiAttivita.MODERATAMENTE_ATTIVO); // LAF 1.55
		when(ownershipValidator.getOwnedCliente(1L)).thenReturn(c);
		lenient().when(repo.findByCliente_IdAndAttivoTrue(1L)).thenReturn(Optional.empty());

		CalcoloResult res = service.calcola(1L);

		assertTrue(res.isSuccesso(), "Con dati completi il calcolo deve riuscire");
		// Mifflin-St Jeor (M): BMR = 10*80 + 6.25*180 - 5*30 + 5 = 1780
		assertEquals(1780.0, res.obiettivo().getBmr(), 0.01);
		// TDEE = BMR * LAF = 1780 * 1.55 = 2759.0
		assertEquals(2759.0, res.obiettivo().getTdee(), 0.01);
	}
}
