-- =====================================================================================
-- 013_audit_log.sql — A7: Audit log GDPR degli accessi ai dati sanitari
-- =====================================================================================
-- Crea la tabella append-only `audit_log` e semina i permessi RBAC di consultazione.
--
-- Perché serve: in PROD `spring.jpa.hibernate.ddl-auto=validate` NON crea/modifica lo schema
-- → la tabella deve preesistere. In DEV (`ddl-auto=update`) Hibernate la crea da sola al riavvio
-- (i `length` espliciti sull'entità AuditLog blindano la generazione). Eseguire questo script
-- PRIMA del deploy prod, e il seed permessi manualmente (come tutti i seed RBAC, vedi 005).
--
-- Design (vedi entity/AuditLog.java):
-- - NESSUNA FK su utente_id/cliente_id: l'audit deve sopravvivere alla cancellazione di
--   account/cliente (retention >= 24 mesi, accountability). Attore anche denormalizzato in utente_email.
-- - Immutabile/append-only: solo INSERT (via AuditService) e DELETE della retention; mai UPDATE.
-- - Enum come VARCHAR con length esplicito (coerente con 012, evita "Data truncated").
--
-- NOTA validate-parity (prod): il tipo di `created_at` generato da Hibernate per un campo `Instant`
-- dipende da dialect/timezone. Prima del deploy prod, verificare la DDL effettiva con
-- `SHOW CREATE TABLE audit_log` su un'istanza dev (ddl-auto=update) e allineare qui se necessario.
-- =====================================================================================

SELECT 'audit_log: START TRANSACTION' AS info;
START TRANSACTION;

SET @now := NOW();

-- =========================================================
-- 1) Tabella audit_log (append-only, no FK)
-- =========================================================
CREATE TABLE IF NOT EXISTS audit_log (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  utente_id    BIGINT       NULL,
  utente_email VARCHAR(255) NULL,
  action       VARCHAR(32)  NOT NULL,
  entity_type  VARCHAR(48)  NOT NULL,
  entity_id    BIGINT       NULL,
  cliente_id   BIGINT       NULL,
  esito        VARCHAR(16)  NOT NULL,
  ip_address   VARCHAR(45)  NULL,
  user_agent   VARCHAR(512) NULL,
  destinatario VARCHAR(255) NULL,
  created_at   DATETIME(6)  NOT NULL,
  KEY idx_audit_cliente_data (cliente_id, created_at),
  KEY idx_audit_utente_data  (utente_id, created_at),
  KEY idx_audit_action_data  (action, created_at),
  KEY idx_audit_created_at   (created_at)
) ENGINE=InnoDB;

-- =========================================================
-- 2) Seed permessi RBAC di consultazione (idempotente, stile 005)
--    AUDIT_READ      → vista globale (titolare/admin)
--    AUDIT_READ_OWN  → self-audit del nutrizionista (solo i propri accessi)
-- =========================================================
INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Audit & Compliance', 'AUDIT', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='AUDIT');

SET @g_audit := (SELECT id FROM gruppi WHERE alias='AUDIT' LIMIT 1);

INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Audit - Lettura globale', 'AUDIT_READ', @g_audit, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='AUDIT_READ');

INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Audit - Lettura propri accessi', 'AUDIT_READ_OWN', @g_audit, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='AUDIT_READ_OWN');

-- =========================================================
-- 3) Assegnazione permessi ai ruoli (idempotente)
--    ADMIN         → AUDIT_READ + AUDIT_READ_OWN
--    NUTRIZIONISTA → AUDIT_READ_OWN
-- =========================================================
SET @r_admin := (SELECT id FROM ruoli WHERE alias='ADMIN' LIMIT 1);
SET @r_nutri := (SELECT id FROM ruoli WHERE alias='NUTRIZIONISTA' LIMIT 1);

INSERT INTO ruoli_permessi (ruolo_id, permesso_id, created_at, updated_at)
SELECT @r_admin, p.id, @now, NULL
FROM permessi p
WHERE p.alias IN ('AUDIT_READ','AUDIT_READ_OWN')
  AND @r_admin IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ruoli_permessi rp WHERE rp.ruolo_id=@r_admin AND rp.permesso_id=p.id);

INSERT INTO ruoli_permessi (ruolo_id, permesso_id, created_at, updated_at)
SELECT @r_nutri, p.id, @now, NULL
FROM permessi p
WHERE p.alias='AUDIT_READ_OWN'
  AND @r_nutri IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ruoli_permessi rp WHERE rp.ruolo_id=@r_nutri AND rp.permesso_id=p.id);

-- =========================================================
-- 4) Verifica
-- =========================================================
SELECT r.alias AS ruolo, p.alias AS permesso
FROM ruoli_permessi rp
JOIN ruoli r ON r.id = rp.ruolo_id
JOIN permessi p ON p.id = rp.permesso_id
WHERE p.alias IN ('AUDIT_READ','AUDIT_READ_OWN')
ORDER BY r.alias, p.alias;

COMMIT;
SELECT 'audit_log: COMMIT OK' AS info;
