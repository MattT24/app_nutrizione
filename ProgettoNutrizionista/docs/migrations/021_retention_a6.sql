-- ============================================================================
-- 021 — A6 Retention / storage limitation (art. 5(1)(e)) — ADDITIVA, MANUALE
-- ============================================================================
-- Aggiunge il segnale di attività clinica + i marker di quarantena/legal-hold.
-- `ddl-auto=update` crea già queste colonne in DEV → uso ADD COLUMN IF NOT EXISTS
-- (TiDB v8.5) per rendere la migrazione idempotente e non-collidente col dev.
-- In PROD (`ddl-auto=validate`) è questa migrazione a creare le colonne.
-- Tipi allineati a Hibernate 6 (Instant -> datetime(6); boolean -> bit(1)).
--
-- Direzione d'errore SEMPRE verso l'over-retention (mai cancellare un cliente attivo):
-- i backfill sono conservativi.
-- ============================================================================

-- ── clienti: segnale aggregato + quarantena + legal hold ──
ALTER TABLE clienti ADD COLUMN IF NOT EXISTS ultimo_contatto_clinico DATETIME(6) NULL;
ALTER TABLE clienti ADD COLUMN IF NOT EXISTS data_quarantena         DATETIME(6) NULL;
ALTER TABLE clienti ADD COLUMN IF NOT EXISTS legal_hold              BIT(1) NOT NULL DEFAULT b'0';

-- Backfill conservativo: i clienti esistenti ereditano l'ultimo aggiornamento noto come "ultimo contatto".
UPDATE clienti SET ultimo_contatto_clinico = COALESCE(updated_at, created_at) WHERE ultimo_contatto_clinico IS NULL;

-- ── avversione_personale_cliente (blacklist): timestamp per entrare nella query di eleggibilità ──
ALTER TABLE avversione_personale_cliente ADD COLUMN IF NOT EXISTS created_at DATETIME(6) NULL;
ALTER TABLE avversione_personale_cliente ADD COLUMN IF NOT EXISTS updated_at DATETIME(6) NULL;

-- Backfill a data-migrazione: le avversioni esistenti contano come attività clinica recente (conservativo).
UPDATE avversione_personale_cliente SET created_at = NOW(6), updated_at = NOW(6) WHERE created_at IS NULL;
