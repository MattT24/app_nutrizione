-- ============================================================================
-- Migrazione: rendere opzionali i campi non più obbligatori della tabella clienti
-- ----------------------------------------------------------------------------
-- Contesto: i campi sesso, data_nascita, peso, altezza sono diventati opzionali
-- (obbligatori restano solo nome, cognome, codice_fiscale, email).
-- Hibernate `ddl-auto=update` NON rilascia i vincoli NOT NULL preesistenti,
-- quindi l'INSERT con questi campi a NULL fallisce con:
--   SQL Error 1048 (23000): Column 'altezza' cannot be null
-- Eseguire UNA TANTUM su TiDB. Idempotente (rieseguibile senza danni).
--
-- Nota: i tipi sotto corrispondono a quelli generati da Hibernate per l'entità
-- Cliente. Se il DB usasse tipi diversi, verificarli con: SHOW COLUMNS FROM clienti;
-- ============================================================================

ALTER TABLE clienti MODIFY COLUMN sesso        VARCHAR(255) NULL;
ALTER TABLE clienti MODIFY COLUMN data_nascita DATE         NULL;
ALTER TABLE clienti MODIFY COLUMN peso         DOUBLE       NULL;
ALTER TABLE clienti MODIFY COLUMN altezza      INT          NULL;
