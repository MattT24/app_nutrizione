package it.nutrizionista.restnutrizionista;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import it.nutrizionista.restnutrizionista.dto.AlimentoBaseFormDto;
import it.nutrizionista.restnutrizionista.dto.MacroDto;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.Macro;
import it.nutrizionista.restnutrizionista.entity.Ruolo;
import it.nutrizionista.restnutrizionista.entity.Utente;
import it.nutrizionista.restnutrizionista.exception.ForbiddenException;
import it.nutrizionista.restnutrizionista.repository.AlimentoBaseRepository;
import it.nutrizionista.restnutrizionista.repository.RuoloRepository;
import it.nutrizionista.restnutrizionista.repository.UtenteRepository;
import it.nutrizionista.restnutrizionista.service.AlimentoBaseService;
import it.nutrizionista.restnutrizionista.support.SafeTestDatabaseBase;

/**
 * P0 (Batch 0) — IDOR write/delete sul catalogo MISTO {@code AlimentoBase}. Difesa in profondità nel service:
 * un nutrizionista, pur avendo il permesso {@code ALIMENTO_UPDATE/DELETE} (gate {@code @PreAuthorize} soddisfatto
 * — qui il service è invocato direttamente), NON può modificare/cancellare alimenti <b>globali</b>
 * ({@code created_by == null}) né <b>personali di un altro</b> → {@link ForbiddenException} (403). Discriminante
 * platform-admin = <b>ruolo sull'entità</b> ({@code getRuolo().getAlias() ∈ {ADMIN, SUPER_ADMIN}}), non le
 * authority. L'ADMIN storico NON deve regredire sul catalogo globale.
 *
 * <p>Il guard {@code assertPuoModificareCatalogo} è condiviso da {@code update}/{@code delete}/{@code deletePersonale}.
 * La direzione "block" è provata su update+delete (403); la direzione "allow" (proprietario / admin) è provata
 * sui casi <b>delete-OK</b> (stesso helper). Non si asserisce un update-OK end-to-end perché il path {@code update}
 * ricostruisce il {@code Macro} via {@code toMacro} (privo di {@code created_at}, non presente in {@code MacroDto})
 * → il merge tenta {@code created_at=NULL}: comportamento <b>pre-esistente e indipendente da P0</b>, segnalato a
 * parte come possibile bug del path update.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class AlimentoBaseIdorIntegrationTest extends SafeTestDatabaseBase {

	private static final String EMAIL_A = "idorNutriA@test.it";
	private static final String EMAIL_B = "idorNutriB@test.it";
	private static final String EMAIL_ADMIN = "idorAdmin@test.it";
	private static final String EMAIL_SUPER = "idorSuper@test.it";

	@Autowired private AlimentoBaseService service;
	@Autowired private AlimentoBaseRepository repoAlimento;
	@Autowired private UtenteRepository repoUtente;
	@Autowired private RuoloRepository repoRuolo;

	private Long globaleId;
	private Long personaleAId;
	private Long personaleBId;

	@BeforeEach
	void seed() {
		Ruolo ruoloNutri = ruolo("NUTRIZIONISTA");
		Ruolo ruoloAdmin = ruolo("ADMIN");
		Ruolo ruoloSuper = ruolo("SUPER_ADMIN");

		Utente nutriA = repoUtente.save(utente(EMAIL_A, "AAAAAA00A00A000A", ruoloNutri));
		repoUtente.save(utente(EMAIL_B, "BBBBBB00B00B000B", ruoloNutri));
		repoUtente.save(utente(EMAIL_ADMIN, "CCCCCC00C00C000C", ruoloAdmin));
		repoUtente.save(utente(EMAIL_SUPER, "DDDDDD00D00D000D", ruoloSuper));

		globaleId = repoAlimento.save(aliment("Zucchero (globale)", null)).getId();   // catalogo globale
		personaleAId = repoAlimento.save(aliment("Alimento di A", nutriA)).getId();    // personale di A
		Utente nutriB = repoUtente.findByEmail(EMAIL_B).orElseThrow();
		personaleBId = repoAlimento.save(aliment("Alimento di B", nutriB)).getId();    // personale di B
	}

	// ── Nutrizionista: BLOCCATO su globale e su personale-altrui (update + delete) ──

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_UPDATE" })
	void nutrizionista_nonPuoAggiornareGlobale() {
		assertThrows(ForbiddenException.class, () -> service.update(form(globaleId)));
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_UPDATE" })
	void nutrizionista_nonPuoAggiornarePersonaleAltrui() {
		assertThrows(ForbiddenException.class, () -> service.update(form(personaleAId)));
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_DELETE" })
	void nutrizionista_nonPuoCancellareGlobale() {
		assertThrows(ForbiddenException.class, () -> service.delete(globaleId));
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_DELETE" })
	void nutrizionista_nonPuoCancellarePersonaleAltrui() {
		assertThrows(ForbiddenException.class, () -> service.delete(personaleAId));
	}

	// ── Direzione "allow" del guard (nessun falso 403): proprietario e admin ──

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_DELETE" })
	void nutrizionista_puoCancellareIlProprioPersonale() {
		assertDoesNotThrow(() -> service.delete(personaleBId));
	}

	@Test
	@WithMockUser(username = EMAIL_ADMIN, authorities = { "ALIMENTO_DELETE" })
	void admin_puoCancellareGlobale() {
		assertDoesNotThrow(() -> service.delete(globaleId));   // ADMIN storico non regredisce
	}

	@Test
	@WithMockUser(username = EMAIL_SUPER, authorities = { "ALIMENTO_DELETE" })
	void superAdmin_puoCancellareGlobale() {
		// Forward-looking: dopo il ritiro di ADMIN (Batch 1) il platform-admin è SUPER_ADMIN → copre
		// il ramo SUPER_ADMIN di isPlatformAdmin.
		assertDoesNotThrow(() -> service.delete(globaleId));
	}

	// ── Batch 2: guard su create globale ──

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_PERSONALE_CREATE" })
	void nutrizionista_nonPuoCreareGlobale() {
		assertThrows(ForbiddenException.class, () -> service.create(form(null)));
	}

	@Test
	@WithMockUser(username = EMAIL_SUPER, authorities = { "CATALOGO_GLOBAL_MANAGE" })
	void superAdmin_puoCreareGlobale() {
		assertDoesNotThrow(() -> service.create(form(null)));
	}

	// ── Batch 2: updatePersonale (F-MACRO-UPDATE sblocca l'update-OK end-to-end) ──

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_PERSONALE_CREATE" })
	void nutrizionista_puoAggiornareIlProprioPersonale() {
		assertDoesNotThrow(() -> service.updatePersonale(form(personaleBId)));
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_PERSONALE_CREATE" })
	void nutrizionista_nonPuoAggiornarePersonaleAltruiViaPersonale() {
		assertThrows(ForbiddenException.class, () -> service.updatePersonale(form(personaleAId)));
	}

	@Test
	@WithMockUser(username = EMAIL_B, authorities = { "ALIMENTO_PERSONALE_CREATE" })
	void nutrizionista_nonPuoAggiornareGlobaleViaPersonale() {
		assertThrows(ForbiddenException.class, () -> service.updatePersonale(form(globaleId)));
	}

	@Test
	@WithMockUser(username = EMAIL_SUPER, authorities = { "CATALOGO_GLOBAL_MANAGE" })
	void superAdmin_puoAggiornareGlobaleConMacroInPlace() {
		// F-MACRO-UPDATE: l'update globale completa senza violare created_at (Macro aggiornato in-place).
		assertDoesNotThrow(() -> service.update(form(globaleId)));
	}

	// ── Helpers ──

	private Ruolo ruolo(String alias) {
		Ruolo r = new Ruolo();
		r.setNome(alias);
		r.setAlias(alias);
		return repoRuolo.save(r);
	}

	private Utente utente(String email, String cf, Ruolo ruolo) {
		Utente u = new Utente();
		u.setNome("Test");
		u.setCognome(email);
		u.setCodiceFiscale(cf);
		u.setEmail(email);
		u.setPassword("x");
		u.setTelefono("000");
		u.setIndirizzo("x");
		u.setRuolo(ruolo);
		return u;
	}

	/** createdBy == null → alimento globale; altrimenti personale del proprietario indicato. */
	private static AlimentoBase aliment(String nome, Utente createdBy) {
		AlimentoBase a = new AlimentoBase();
		a.setNome(nome);
		a.setMisuraInGrammi(100.0);
		Macro m = new Macro();
		m.setAlimento(a);
		m.setCalorie(100.0);
		m.setProteine(10.0);
		m.setCarboidrati(10.0);
		m.setGrassi(10.0);
		a.setMacronutrienti(m);
		a.setCreatedBy(createdBy);
		return a;
	}

	/** Form minimo per {@code update}: il guard scatta PRIMA del mapping, quindi basta l'id (+ campi validi). */
	private static AlimentoBaseFormDto form(Long id) {
		AlimentoBaseFormDto f = new AlimentoBaseFormDto();
		f.setId(id);
		f.setNome("Aggiornato");
		f.setMisuraInGrammi(100.0);
		MacroDto macro = new MacroDto();
		macro.setCalorie(100.0);
		macro.setProteine(10.0);
		macro.setCarboidrati(10.0);
		macro.setGrassi(10.0);
		f.setMacroNutrienti(macro);
		return f;
	}
}
