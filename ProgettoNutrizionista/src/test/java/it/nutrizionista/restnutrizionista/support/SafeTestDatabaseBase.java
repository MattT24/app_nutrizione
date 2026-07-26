package it.nutrizionista.restnutrizionista.support;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base per gli integration test su DB di test effimero. Due garanzie, nel @BeforeEach della
 * superclasse (che gira PRIMA del @BeforeEach delle sottoclassi):
 * <ol>
 *   <li><b>Safety:</b> blocca l'esecuzione se il datasource non è un DB di test effimero —
 *       {@code jdbc:h2:mem:} (H2 in-memory) oppure {@code jdbc:tc:mysql:} (MySQL EFFIMERO in
 *       container, Testcontainers). Un {@code jdbc:mysql://} <b>reale</b> (TiDB/Clever) NON inizia
 *       con {@code jdbc:tc:} → resta bloccato: il cleanup non tronca MAI un DB vero.</li>
 *   <li><b>Cleanup FK-agnostico:</b> svuota TUTTE le tabelle prima di ogni test. Disabilita
 *       l'integrità referenziale, tronca ogni tabella base dello schema corrente, poi la ripristina.
 *       È <b>order-independent per costruzione</b>: nessun ordinamento manuale figli-prima-padri,
 *       nessun whack-a-mole al variare delle FK (es. le ~14 tabelle che referenziano {@code utenti}:
 *       eventi_gamification, attivita_recente, badge_sbloccato, clienti, appuntamenti, ...). Elimina
 *       la fragilità per cui l'ordine dei test faceva fallire il {@code deleteAll(utenti)} su FK
 *       residue.</li>
 * </ol>
 * Gli @SpringBootTest condividono lo stesso DB (stessa URL): questo cleanup rende ogni test
 * indipendente dallo stato lasciato dai precedenti, qualunque sia l'ordine di esecuzione. I due rami
 * (H2 / MySQL) differiscono solo per sintassi (FK-off, schema, quoting), non per logica.
 */
public abstract class SafeTestDatabaseBase {
	@Autowired private Environment env;
	@Autowired private JdbcTemplate jdbc;

	@BeforeEach
	void assertSafeTestDatabaseAndClean() {
		String url = env.getProperty("spring.datasource.url");
		if (url == null) throw new IllegalStateException("spring.datasource.url mancante");
		String normalized = url.toLowerCase();

		boolean h2 = normalized.startsWith("jdbc:h2:mem:");
		// Testcontainers = MySQL EFFIMERO in container. Un jdbc:mysql:// REALE non ha il prefisso tc:.
		boolean tcMysql = normalized.startsWith("jdbc:tc:mysql:");
		if (!h2 && !tcMysql) {
			throw new IllegalStateException("Test bloccato: datasource non sicuro: " + url);
		}

		if (h2) {
			// Identificatori in MAIUSCOLO: il DB test usa DATABASE_TO_UPPER=false → 'information_schema'
			// minuscolo non risolverebbe. I literal ('PUBLIC','BASE TABLE') e i nomi tabella restano
			// come memorizzati (Hibernate li crea lowercase → TRUNCATE quotato preserva il case).
			cleanTruncateAll("SET REFERENTIAL_INTEGRITY FALSE", "SET REFERENTIAL_INTEGRITY TRUE",
					"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
							+ "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'", '"');
		} else {
			// MySQL (Testcontainers): schema = DATABASE() (nome DB del container, non 'PUBLIC'),
			// FK-off via FOREIGN_KEY_CHECKS, identificatori con backtick.
			cleanTruncateAll("SET FOREIGN_KEY_CHECKS = 0", "SET FOREIGN_KEY_CHECKS = 1",
					"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
							+ "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'", '`');
		}
	}

	/** Cleanup FK-agnostico: FK-off → TRUNCATE di ogni tabella base dello schema corrente → FK-on. */
	private void cleanTruncateAll(String fkOff, String fkOn, String tablesQuery, char quote) {
		jdbc.execute(fkOff);
		try {
			List<String> tables = jdbc.queryForList(tablesQuery, String.class);
			for (String table : tables) {
				jdbc.execute("TRUNCATE TABLE " + quote + table + quote);
			}
		} finally {
			jdbc.execute(fkOn);
		}
	}
}
