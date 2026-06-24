-- =====================================================================================
-- 012_widen_enum_string_columns.sql — Fix "Data truncated for column 'tag'"
-- =====================================================================================
-- La colonna cliente_tag_standard.tag era troppo corta per i valori TagStandard più lunghi
-- (es. INT_GLUTINE_NCGS=16, FARM_ANTICOAGULANTI=19) → insert falliva con "Data truncated".
--
-- La tabella ha PRIMARY KEY composta (cliente_id, tag): su TiDB NON si può fare
-- `ALTER ... MODIFY COLUMN tag` su una colonna con flag di PRIMARY KEY (ERROR 8200), né
-- droppare una PK clustered. Soluzione robusta: RICREARE la tabella con `tag` VARCHAR(64)
-- e copiare i dati. La FK cliente_id→cliente verrà ripristinata da Hibernate (ddl-auto=update)
-- al riavvio del backend (aggiunge le FK mancanti).
--
-- Eseguire su TiDB/MySQL. La tabella contiene solo coppie (cliente_id, tag), copia rapida.
-- =====================================================================================

-- (opzionale) ispezione struttura attuale prima di procedere:
-- SHOW CREATE TABLE cliente_tag_standard;

CREATE TABLE cliente_tag_standard_new (
    cliente_id BIGINT NOT NULL,
    tag        VARCHAR(64) NOT NULL,
    PRIMARY KEY (cliente_id, tag)
);

INSERT INTO cliente_tag_standard_new (cliente_id, tag)
    SELECT cliente_id, tag FROM cliente_tag_standard;

DROP TABLE cliente_tag_standard;

RENAME TABLE cliente_tag_standard_new TO cliente_tag_standard;

-- Verifica post-migrazione:
-- SHOW COLUMNS FROM cliente_tag_standard LIKE 'tag';   -- atteso: varchar(64)
-- SELECT COUNT(*) FROM cliente_tag_standard;           -- stesso conteggio di prima

-- NOTA: le colonne enum-string delle tabelle NUOVE (alimento_allergene.allergene/stato,
-- alimenti_base.fonte_allergeni) sono già state create da Hibernate con lunghezza adeguata
-- ai valori enum attuali → nessun ALTER necessario ora. I `length` espliciti aggiunti alle
-- entità servono solo a blindare la generazione schema per ambienti nuovi / valori futuri.
