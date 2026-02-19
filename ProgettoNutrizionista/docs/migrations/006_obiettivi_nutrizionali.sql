-- =============================================
-- Migration 006: Obiettivi Nutrizionali
-- =============================================
-- 1. Aggiunge colonna livello_attivita alla tabella clienti
-- 2. Migra i dati da num_allenamenti_settimanali (best-effort)
-- 3. Crea la tabella obiettivi_nutrizionali
-- 4. Aggiunge permessi RBAC

-- ─── STEP 1: Aggiungi colonna enum a clienti ─────────────────────────

ALTER TABLE clienti
ADD COLUMN livello_attivita VARCHAR(30) DEFAULT NULL;

-- ─── STEP 2: Migra valori esistenti (best-effort) ────────────────────

UPDATE clienti
SET
    livello_attivita = CASE
        WHEN num_allenamenti_settimanali IN ('0', 'nessuno', 'sedentario') THEN 'SEDENTARIO'
        WHEN num_allenamenti_settimanali IN ('1', '2', '1-2') THEN 'LEGGERMENTE_ATTIVO'
        WHEN num_allenamenti_settimanali IN ('3', '4', '3-4') THEN 'MODERATAMENTE_ATTIVO'
        WHEN num_allenamenti_settimanali IN ('5', '6', '5-6') THEN 'MOLTO_ATTIVO'
        WHEN num_allenamenti_settimanali IN ('7', '7+', 'ogni giorno') THEN 'ESTREMAMENTE_ATTIVO'
        ELSE NULL
    END
WHERE
    num_allenamenti_settimanali IS NOT NULL;

-- NOTA: NON rimuoviamo la colonna vecchia per sicurezza.
-- Dopo verifica dati si può eseguire:
-- ALTER TABLE clienti DROP COLUMN num_allenamenti_settimanali;

-- ─── STEP 3: Crea tabella obiettivi_nutrizionali ─────────────────────

CREATE TABLE IF NOT EXISTS obiettivi_nutrizionali (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL UNIQUE,
    obiettivo VARCHAR(30) NOT NULL DEFAULT 'MANTENIMENTO',
    bmr DOUBLE DEFAULT NULL,
    tdee DOUBLE DEFAULT NULL,
    laf DOUBLE DEFAULT 1.55,
    target_calorie DOUBLE DEFAULT NULL,
    target_proteine DOUBLE DEFAULT NULL,
    target_carboidrati DOUBLE DEFAULT NULL,
    target_grassi DOUBLE DEFAULT NULL,
    target_fibre DOUBLE DEFAULT NULL,
    pct_proteine DOUBLE DEFAULT NULL,
    pct_carboidrati DOUBLE DEFAULT NULL,
    pct_grassi DOUBLE DEFAULT NULL,
    note TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_obiettivi_cliente FOREIGN KEY (cliente_id) REFERENCES clienti (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ─── STEP 4: Permessi RBAC ───────────────────────────────────────────

-- Inserisci permessi (idempotente)
INSERT IGNORE INTO
    permessi (nome, descrizione, gruppo_id)
SELECT 'OBIETTIVO_READ', 'Visualizza obiettivo nutrizionale cliente', g.id
FROM gruppi g
WHERE
    g.nome = 'Clienti';

INSERT IGNORE INTO
    permessi (nome, descrizione, gruppo_id)
SELECT 'OBIETTIVO_WRITE', 'Crea/modifica obiettivo nutrizionale cliente', g.id
FROM gruppi g
WHERE
    g.nome = 'Clienti';

-- Assegna ai ruoli ADMIN e NUTRIZIONISTA
INSERT IGNORE INTO
    ruoli_permessi (ruolo_id, permesso_id)
SELECT r.id, p.id
FROM ruoli r, permessi p
WHERE
    r.nome IN ('ADMIN', 'NUTRIZIONISTA')
    AND p.nome = 'OBIETTIVO_READ';

INSERT IGNORE INTO
    ruoli_permessi (ruolo_id, permesso_id)
SELECT r.id, p.id
FROM ruoli r, permessi p
WHERE
    r.nome IN ('ADMIN', 'NUTRIZIONISTA')
    AND p.nome = 'OBIETTIVO_WRITE';

-- ─── VERIFICA ────────────────────────────────────────────────────────

-- SELECT p.nome, GROUP_CONCAT(r.nome) AS ruoli
-- FROM permessi p
-- JOIN ruoli_permessi rp ON rp.permesso_id = p.id
-- JOIN ruoli r ON r.id = rp.ruolo_id
-- WHERE p.nome LIKE 'OBIETTIVO%'
-- GROUP BY p.nome;