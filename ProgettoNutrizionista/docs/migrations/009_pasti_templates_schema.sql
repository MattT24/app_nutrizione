-- =========================================================
-- Schema: PASTI TEMPLATES (alimenti + alternative + nome_custom)
-- =========================================================

START TRANSACTION;

-- A) Estendo righe alimento template con nome custom
ALTER TABLE pasti_template_alimenti
    ADD COLUMN IF NOT EXISTS nome_custom VARCHAR(255) NULL;

-- B) Tabelle alternative per alimento template
CREATE TABLE IF NOT EXISTS pasti_template_alternative (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_alimento_id BIGINT NOT NULL,
    alimento_alternativo_id BIGINT NOT NULL,
    quantita INT NOT NULL DEFAULT 100,
    priorita INT NOT NULL DEFAULT 1,
    mode VARCHAR(30) NOT NULL DEFAULT 'CALORIE',
    manual TINYINT(1) NOT NULL DEFAULT 1,
    note VARCHAR(500) NULL,
    nome_custom VARCHAR(255) NULL,
    CONSTRAINT fk_template_alt_template_alimento
        FOREIGN KEY (template_alimento_id) REFERENCES pasti_template_alimenti(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_template_alt_alimento
        FOREIGN KEY (alimento_alternativo_id) REFERENCES alimenti_base(id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_template_alt UNIQUE (template_alimento_id, alimento_alternativo_id)
);

COMMIT;
