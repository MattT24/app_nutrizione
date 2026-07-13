package it.nutrizionista.restnutrizionista.support;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base per gli integration test su DB H2 in-memory. Due garanzie, nel @BeforeEach della superclasse
 * (che gira PRIMA del @BeforeEach delle sottoclassi):
 * <ol>
 *   <li><b>Safety:</b> blocca l'esecuzione se il datasource non è {@code jdbc:h2:mem:} (mai sul DB reale).</li>
 *   <li><b>Cleanup FK-agnostico:</b> svuota TUTTE le tabelle prima di ogni test. Disabilita l'integrità
 *       referenziale ({@code SET REFERENTIAL_INTEGRITY FALSE}), tronca ogni tabella di {@code PUBLIC}
 *       (ricavata da {@code information_schema}), poi la ripristina. È <b>order-independent per
 *       costruzione</b>: nessun ordinamento manuale figli-prima-padri, nessun whack-a-mole al variare
 *       delle FK (es. le ~14 tabelle che referenziano {@code utenti}: eventi_gamification,
 *       attivita_recente, badge_sbloccato, clienti, appuntamenti, ...). Elimina la fragilità per cui
 *       l'ordine dei test faceva fallire il {@code deleteAll(utenti)} su FK residue.</li>
 * </ol>
 * Gli @SpringBootTest condividono lo stesso DB H2 (stessa URL): questo cleanup rende ogni test
 * indipendente dallo stato lasciato dai precedenti, qualunque sia l'ordine di esecuzione.
 */
public abstract class SafeTestDatabaseBase {
	@Autowired private Environment env;
	@Autowired private JdbcTemplate jdbc;

	@BeforeEach
	void assertSafeTestDatabaseAndClean() {
		String url = env.getProperty("spring.datasource.url");
		if (url == null) throw new IllegalStateException("spring.datasource.url mancante");
		String normalized = url.toLowerCase();
		if (!normalized.startsWith("jdbc:h2:mem:")) {
			throw new IllegalStateException("Test bloccato: datasource non sicuro: " + url);
		}

		// Cleanup FK-agnostico: RI off → TRUNCATE di tutte le tabelle base di PUBLIC → RI on.
		jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
		try {
			// Identificatori in MAIUSCOLO: il DB test usa DATABASE_TO_UPPER=false → 'information_schema'
			// minuscolo non risolverebbe. I literal ('PUBLIC','BASE TABLE') e i nomi tabella restano
			// come memorizzati (Hibernate/MySQL li crea lowercase → TRUNCATE quotato preserva il case).
			List<String> tables = jdbc.queryForList(
					"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
							+ "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
					String.class);
			for (String table : tables) {
				jdbc.execute("TRUNCATE TABLE \"" + table + "\"");
			}
		} finally {
			jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
		}
	}
}
