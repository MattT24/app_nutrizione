INSERT INTO
    permessi (
        nome,
        alias,
        gruppo_id,
        created_at,
        updated_at
    )
VALUES (
        'Crea Alimento Personale',
        'ALIMENTO_PERSONALE_CREATE',
        2,
        NOW(),
        NOW()
    );

INSERT INTO
    ruoli_permessi (
        ruolo_id,
        permesso_id,
        created_at,
        updated_at
    )
VALUES (
        2,
        LAST_INSERT_ID(),
        NOW(),
        NOW()
    );