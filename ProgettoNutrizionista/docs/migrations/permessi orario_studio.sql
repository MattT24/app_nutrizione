-- =========================================================
-- Script aggiornamento permessi: ORARI STUDIO
-- =========================================================

SET @now := NOW();

START TRANSACTION;

-- 1. Inserimento nuovi permessi (se non esistono)
INSERT INTO
    permessi (
        nome,
        alias,
        gruppo_id,
        created_at,
        updated_at
    )
SELECT 'Orari - Lettura', 'ORARI_STUDIO_READ', NULL, @now, NULL
WHERE
    NOT EXISTS (
        SELECT 1
        FROM permessi
        WHERE
            alias = 'ORARI_STUDIO_READ'
    );

INSERT INTO
    permessi (
        nome,
        alias,
        gruppo_id,
        created_at,
        updated_at
    )
SELECT 'Orari - Aggiornamento', 'ORARI_STUDIO_UPDATE', NULL, @now, NULL
WHERE
    NOT EXISTS (
        SELECT 1
        FROM permessi
        WHERE
            alias = 'ORARI_STUDIO_UPDATE'
    );

-- 2. Recupero ID dei Ruoli (CORRETTO: Uso SET invece di SELECT INTO)
SET
    @id_admin := (
        SELECT id
        FROM ruoli
        WHERE
            alias = 'ROLE_ADMIN'
        LIMIT 1
    );

SET
    @id_nutrizionista := (
        SELECT id
        FROM ruoli
        WHERE
            alias = 'ROLE_NUTRIZIONISTA'
        LIMIT 1
    );

-- 3. Recupero ID dei nuovi Permessi (CORRETTO: Uso SET invece di SELECT INTO)
SET
    @p_orari_read := (
        SELECT id
        FROM permessi
        WHERE
            alias = 'ORARI_STUDIO_READ'
        LIMIT 1
    );

SET
    @p_orari_update := (
        SELECT id
        FROM permessi
        WHERE
            alias = 'ORARI_STUDIO_UPDATE'
        LIMIT 1
    );

-- 4. Associazione Permessi ai Ruoli (Insert idempotente)

-- A) ADMIN: Può Leggere e Aggiornare
INSERT INTO
    ruoli_permessi (ruolo_id, permesso_id)
SELECT @id_admin, @p_orari_read
FROM DUAL
WHERE
    @id_admin IS NOT NULL
    AND @p_orari_read IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM ruoli_permessi
        WHERE
            ruolo_id = @id_admin
            AND permesso_id = @p_orari_read
    );

INSERT INTO
    ruoli_permessi (ruolo_id, permesso_id)
SELECT @id_admin, @p_orari_update
FROM DUAL
WHERE
    @id_admin IS NOT NULL
    AND @p_orari_update IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM ruoli_permessi
        WHERE
            ruolo_id = @id_admin
            AND permesso_id = @p_orari_update
    );

-- B) NUTRIZIONISTA: Può Leggere e Aggiornare
INSERT INTO
    ruoli_permessi (ruolo_id, permesso_id)
SELECT
    @id_nutrizionista,
    @p_orari_read
FROM DUAL
WHERE
    @id_nutrizionista IS NOT NULL
    AND @p_orari_read IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM ruoli_permessi
        WHERE
            ruolo_id = @id_nutrizionista
            AND permesso_id = @p_orari_read
    );

INSERT INTO
    ruoli_permessi (ruolo_id, permesso_id)
SELECT
    @id_nutrizionista,
    @p_orari_update
FROM DUAL
WHERE
    @id_nutrizionista IS NOT NULL
    AND @p_orari_update IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM ruoli_permessi
        WHERE
            ruolo_id = @id_nutrizionista
            AND permesso_id = @p_orari_update
    );

COMMIT;