-- Account demo: schema richiesto dal profilo prod (ddl-auto=validate).
-- MySQL 8 / TiDB. Eseguire prima del deploy dell'applicazione.

ALTER TABLE utenti
    MODIFY COLUMN metodo_registrazione ENUM('EMAIL', 'GOOGLE', 'DEMO') NULL;

CREATE TABLE IF NOT EXISTS credenziali_demo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    utente_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    disabled_at DATETIME(6) NULL,
    token_version BIGINT NOT NULL DEFAULT 0,
    created_by_admin_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    version BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_credenziale_demo_username UNIQUE (username),
    CONSTRAINT uk_credenziale_demo_utente UNIQUE (utente_id),
    CONSTRAINT fk_credenziale_demo_utente
        FOREIGN KEY (utente_id) REFERENCES utenti(id),
    INDEX idx_demo_scadenza (expires_at),
    INDEX idx_demo_disabilitato_scadenza (disabled_at, expires_at)
);

CREATE TABLE IF NOT EXISTS audit_account_demo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    target_user_id BIGINT NULL,
    demo_credential_id BIGINT NULL,
    azione VARCHAR(40) NOT NULL,
    esito VARCHAR(16) NOT NULL,
    motivo VARCHAR(500) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(512) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_demo_admin_data (admin_id, created_at),
    INDEX idx_audit_demo_target_data (target_user_id, created_at)
);
