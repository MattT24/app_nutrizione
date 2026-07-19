package it.nutrizionista.restnutrizionista.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Allinea in modo idempotente l'ENUM legacy usato da MySQL/TiDB.
 * Hibernate ddl-auto non aggiorna in modo affidabile i valori ammessi da un ENUM esistente.
 */
@Component
public class DemoSchemaMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoSchemaMigration.class);
    private static final String ALTER_ENUM = """
            ALTER TABLE utenti
            MODIFY COLUMN metodo_registrazione ENUM('EMAIL','GOOGLE','DEMO') NULL
            """;

    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String prodotto = metadata.getDatabaseProductName();
            if (prodotto == null || !(prodotto.toLowerCase().contains("mysql")
                    || prodotto.toLowerCase().contains("tidb"))) {
                log.debug("Migrazione ENUM demo non necessaria sul database {}", prodotto);
                return;
            }
        }

        String columnType = jdbcTemplate.queryForObject("""
                SELECT COLUMN_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'utenti'
                  AND COLUMN_NAME = 'metodo_registrazione'
                """, String.class);
        if (columnType == null) {
            throw new IllegalStateException("Colonna utenti.metodo_registrazione non trovata");
        }
        if (columnType.toUpperCase().contains("'DEMO'")) {
            log.debug("ENUM metodo_registrazione gia allineato");
            return;
        }
        if (!columnType.toLowerCase().startsWith("enum(")) {
            log.info("metodo_registrazione non e un ENUM: nessuna migrazione necessaria");
            return;
        }

        jdbcTemplate.execute(ALTER_ENUM);
        log.info("Schema account demo allineato: metodo_registrazione ammette DEMO");
    }
}
