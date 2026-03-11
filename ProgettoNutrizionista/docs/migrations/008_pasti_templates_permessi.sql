-- =========================================================
-- Script aggiornamento permessi: PASTI TEMPLATES
-- =========================================================

SET @now := NOW();

START TRANSACTION;

-- 1. Inserimento nuovo permesso (se non esiste)
INSERT INTO
    permessi (
        nome,
        alias,
        gruppo_id,
        created_at,
        updated_at
    )
SELECT 'Pasti Template - Gestione', 'PASTI_TEMPLATE_MANAGE', NULL, @now, NULL
WHERE
    NOT EXISTS (
        SELECT 1
        FROM permessi
        WHERE
            alias = 'PASTI_TEMPLATE_MANAGE'
    );

-- 2. Recupero ID dei Ruoli
SET
    @id_admin := (
        SELECT id
        FROM ruoli
        WHERE
            alias = 'ADMIN'
        LIMIT 1
    );

SET
    @id_nutrizionista := (
        SELECT id
        FROM ruoli
        WHERE
            alias = 'NUTRIZIONISTA'
        LIMIT 1
    );

-- 3. Recupero ID del nuovo Permesso
SET
    @p_manage := (
        SELECT id
        FROM permessi
        WHERE
            alias = 'PASTI_TEMPLATE_MANAGE'
        LIMIT 1
    );

-- 4. Associazione Permesso ai Ruoli (Insert idempotente)
INSERT INTO
    ruoli_permessi (
        ruolo_id,
        permesso_id,
        created_at,
        updated_at
    )
SELECT @id_admin, @p_manage, @now, NULL
FROM DUAL
WHERE
    @id_admin IS NOT NULL
    AND @p_manage IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM ruoli_permessi
        WHERE
            ruolo_id = @id_admin
            AND permesso_id = @p_manage
    );

INSERT INTO
    ruoli_permessi (
        ruolo_id,
        permesso_id,
        created_at,
        updated_at
    )
SELECT @id_nutrizionista, @p_manage, @now, NULL
FROM DUAL
WHERE
    @id_nutrizionista IS NOT NULL
    AND @p_manage IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM ruoli_permessi
        WHERE
            ruolo_id = @id_nutrizionista
            AND permesso_id = @p_manage
    );

COMMIT;

SELECT 'Permesso PASTI_TEMPLATE_MANAGE inserito e assegnato correttamente.' AS info;

