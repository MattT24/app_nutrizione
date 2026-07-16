-- =====================================================================================
-- 015_audit_log_add_dettaglio.sql — D1: dettaglio testuale sugli eventi di audit
-- =====================================================================================
-- Aggiunge la colonna `dettaglio` (nullable) alla tabella `audit_log`.
--
-- Perché serve: l'audit dell'override consapevole di un blocco clinico grave
-- (AuditAction.OVERRIDE_ALERT_GRAVE, finding D1) deve registrare anche i MOTIVI del blocco e
-- l'alimento forzato — informazioni che action/entity_type/entity_id non catturano. La colonna è
-- generica e riusabile per futuri eventi che richiedono un dettaglio testuale.
--
-- In PROD `spring.jpa.hibernate.ddl-auto=validate` NON modifica lo schema → eseguire PRIMA del deploy.
-- In DEV (`ddl-auto=update`) Hibernate aggiunge la colonna da solo al riavvio (length esplicito
-- sull'entità AuditLog). Colonna NULLABLE senza vincoli su PRIMARY KEY → nessun limite DDL TiDB.
-- =====================================================================================

ALTER TABLE audit_log
  ADD COLUMN dettaglio VARCHAR(1024) NULL AFTER destinatario;
