-- =====================================================================================
-- 016_clienti_cf_email_opzionali.sql — Codice fiscale ed email del cliente FACOLTATIVI
-- =====================================================================================
-- Rende NULLABLE `codice_fiscale` ed `email` sulla tabella `clienti` (prima NOT NULL).
-- Il nutrizionista può creare un cliente senza CF/email e completarli in seguito.
--
-- Perché serve: in PROD `spring.jpa.hibernate.ddl-auto=validate` NON modifica lo schema; e anche
-- in DEV (`ddl-auto=update`) Hibernate NON rilascia i vincoli NOT NULL preesistenti. Quindi, pur
-- avendo messo nullable=true nell'entity, senza questo ALTER un INSERT con CF/email null fallirebbe
-- ("Column '...' cannot be null"). Stesso pattern di 001-clienti-campi-opzionali.sql.
--
-- Gli indici UNIQUE su codice_fiscale ed email RESTANO: su MySQL/TiDB un unique index ammette più
-- righe NULL (i NULL sono distinti), quindi più clienti senza CF/email sono ammessi. Le stringhe
-- vuote sono normalizzate a NULL a livello applicativo (DtoMapper.blankToNull) per non violare
-- l'unique (che invece NON ammette più "").
-- =====================================================================================

ALTER TABLE clienti MODIFY COLUMN codice_fiscale VARCHAR(255) NULL;
ALTER TABLE clienti MODIFY COLUMN email          VARCHAR(255) NULL;
