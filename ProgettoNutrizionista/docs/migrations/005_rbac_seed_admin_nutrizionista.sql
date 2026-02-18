-- RBAC seed completo (MySQL/MariaDB)
-- Ruoli: ADMIN (tutti i permessi) e NUTRIZIONISTA (permessi funzionalità sito)
--
-- Fonte permessi:
-- - alias usati nei controller tramite @PreAuthorize("hasAuthority('...')")
-- - moduli raggruppati nella tabella gruppi
--
-- Nota schema:
-- - Il backend usa tabelle: ruoli, gruppi, permessi, ruoli_permessi
-- - Questa seed è idempotente per gli INSERT (non duplica record se rilanciata)
--
-- Esecuzione:
-- - consigliato: backup DB, poi esecuzione in transazione
-- - verifica con le query di controllo in fondo

SET @now := NOW();

SELECT 'RBAC seed: START TRANSACTION' AS info;
START TRANSACTION;

-- =========================================================
-- 1) Definizione tabelle RBAC (se mancanti)
-- =========================================================
SELECT 'RBAC seed: ensure tables exist' AS info;

CREATE TABLE IF NOT EXISTS ruoli (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(255) NOT NULL,
  alias VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ruoli_alias (alias)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gruppi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(255) NOT NULL,
  alias VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_gruppi_alias (alias)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS permessi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(255) NOT NULL,
  alias VARCHAR(255) NOT NULL,
  gruppo_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_permessi_alias (alias),
  KEY ix_permessi_gruppo_id (gruppo_id),
  CONSTRAINT fk_permessi_gruppi
    FOREIGN KEY (gruppo_id) REFERENCES gruppi(id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ruoli_permessi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ruolo_id BIGINT NOT NULL,
  permesso_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ruolo_permesso (ruolo_id, permesso_id),
  KEY ix_rp_ruolo_id (ruolo_id),
  KEY ix_rp_permesso_id (permesso_id),
  CONSTRAINT fk_rp_ruoli
    FOREIGN KEY (ruolo_id) REFERENCES ruoli(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_rp_permessi
    FOREIGN KEY (permesso_id) REFERENCES permessi(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 2) Creazione gruppi (categorizzazione per modulo)
-- =========================================================
SELECT 'RBAC seed: insert module groups' AS info;

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'RBAC (Ruoli/Permessi)', 'RBAC', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='RBAC');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Alimenti base', 'ALIMENTI', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='ALIMENTI');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Alternative alimenti', 'ALIMENTI_ALTERNATIVI', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='ALIMENTI_ALTERNATIVI');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Alimenti da evitare', 'ALIMENTI_DA_EVITARE', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='ALIMENTI_DA_EVITARE');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Schede dieta', 'SCHEDE', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='SCHEDE');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Pasti', 'PASTI', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='PASTI');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Pasti custom (meals)', 'MEALS', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='MEALS');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Clienti', 'CLIENTI', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='CLIENTI');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Appuntamenti', 'APPUNTAMENTI', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='APPUNTAMENTI');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Plicometria', 'PLICOMETRIA', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='PLICOMETRIA');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Misurazioni antropometriche', 'MISURAZIONI_ANTROPOMETRICHE', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='MISURAZIONI_ANTROPOMETRICHE');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Nutrienti (macro/micro)', 'NUTRIENTI', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='NUTRIENTI');

INSERT INTO gruppi (nome, alias, created_at, updated_at)
SELECT 'Utenti', 'UTENTI', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM gruppi WHERE alias='UTENTI');

-- =========================================================
-- 3) Creazione ruoli
-- =========================================================
SELECT 'RBAC seed: insert roles ADMIN, NUTRIZIONISTA' AS info;

INSERT INTO ruoli (nome, alias, created_at, updated_at)
SELECT 'Amministratore (tutti i permessi)', 'ADMIN', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM ruoli WHERE alias='ADMIN');

INSERT INTO ruoli (nome, alias, created_at, updated_at)
SELECT 'Nutrizionista (funzionalità sito)', 'NUTRIZIONISTA', @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM ruoli WHERE alias='NUTRIZIONISTA');

-- =========================================================
-- 3.1) Creazione utenti seed (1 admin + 1 nutrizionista)
-- =========================================================
SELECT 'RBAC seed: insert seed users (ADMIN, NUTRIZIONISTA)' AS info;

SET @r_admin := (SELECT id FROM ruoli WHERE alias='ADMIN' LIMIT 1);
SET @r_nutri := (SELECT id FROM ruoli WHERE alias='NUTRIZIONISTA' LIMIT 1);

-- Credenziali seed (cambiare subito dopo la prima login):
-- - admin@demo.local / Admin123!
-- - nutrizionista@demo.local / Nutri123!
INSERT INTO utenti (nome, cognome, codice_fiscale, email, password, telefono, indirizzo, ruolo_id, created_at, updated_at)
SELECT 'Admin', 'Demo', 'ADMDMO00A00A000A', 'm',
       '$2a$12$/q8/EF8PAnMxh52Mwtaxgeq39SIkAtT/vERR4BuueUXVYbJwyIiGO',
       '0000000000', 'Demo', @r_admin, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM utenti WHERE email='admin@demo.local');

INSERT INTO utenti (nome, cognome, codice_fiscale, email, password, telefono, indirizzo, ruolo_id, created_at, updated_at)
SELECT 'Nutrizionista', 'Demo', 'NTDMO00A00A000A', 'nutrizionista@demo.local',
       '$2a$12$/q8/EF8PAnMxh52Mwtaxgeq39SIkAtT/vERR4BuueUXVYbJwyIiGO',
       '0000000000', 'Demo', @r_nutri, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM utenti WHERE email='nutrizionista@demo.local');

SELECT CONCAT('RBAC seed: utenti seed presenti = ', (SELECT COUNT(*) FROM utenti WHERE email IN ('admin@demo.local','nutrizionista@demo.local'))) AS info;
SELECT id, email, ruolo_id FROM utenti WHERE email IN ('admin@demo.local','nutrizionista@demo.local');

-- =========================================================
-- 4) Inserimento permessi (CRUD + permessi speciali)
-- =========================================================
SELECT 'RBAC seed: insert permissions' AS info;

SET @g_rbac := (SELECT id FROM gruppi WHERE alias='RBAC' LIMIT 1);
SET @g_alimenti := (SELECT id FROM gruppi WHERE alias='ALIMENTI' LIMIT 1);
SET @g_alt := (SELECT id FROM gruppi WHERE alias='ALIMENTI_ALTERNATIVI' LIMIT 1);
SET @g_ev := (SELECT id FROM gruppi WHERE alias='ALIMENTI_DA_EVITARE' LIMIT 1);
SET @g_schede := (SELECT id FROM gruppi WHERE alias='SCHEDE' LIMIT 1);
SET @g_pasti := (SELECT id FROM gruppi WHERE alias='PASTI' LIMIT 1);
SET @g_meals := (SELECT id FROM gruppi WHERE alias='MEALS' LIMIT 1);
SET @g_clienti := (SELECT id FROM gruppi WHERE alias='CLIENTI' LIMIT 1);
SET @g_app := (SELECT id FROM gruppi WHERE alias='APPUNTAMENTI' LIMIT 1);
SET @g_plico := (SELECT id FROM gruppi WHERE alias='PLICOMETRIA' LIMIT 1);
SET @g_misu := (SELECT id FROM gruppi WHERE alias='MISURAZIONI_ANTROPOMETRICHE' LIMIT 1);
SET @g_nutr := (SELECT id FROM gruppi WHERE alias='NUTRIENTI' LIMIT 1);
SET @g_utenti := (SELECT id FROM gruppi WHERE alias='UTENTI' LIMIT 1);

-- --- RBAC: gruppi / permessi / ruoli ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Gruppi - Creazione', 'GRUPPO_CREATE', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='GRUPPO_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Gruppi - Lettura', 'GRUPPO_READ', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='GRUPPO_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Gruppi - Aggiornamento', 'GRUPPO_UPDATE', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='GRUPPO_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Gruppi - Eliminazione', 'GRUPPO_DELETE', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='GRUPPO_DELETE');

INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Permessi - Creazione', 'PERMESSO_CREATE', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PERMESSO_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Permessi - Lettura', 'PERMESSO_READ', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PERMESSO_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Permessi - Aggiornamento', 'PERMESSO_UPDATE', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PERMESSO_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Permessi - Eliminazione', 'PERMESSO_DELETE', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PERMESSO_DELETE');

INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Ruoli - Creazione', 'RUOLO_CREATE', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='RUOLO_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Ruoli - Lettura', 'RUOLO_READ', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='RUOLO_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Ruoli - Aggiornamento', 'RUOLO_UPDATE', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='RUOLO_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Ruoli - Eliminazione', 'RUOLO_DELETE', @g_rbac, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='RUOLO_DELETE');

-- --- Alimenti base ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alimenti base - Creazione', 'ALIMENTO_CREATE', @g_alimenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alimenti base - Lettura', 'ALIMENTO_READ', @g_alimenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alimenti base - Aggiornamento', 'ALIMENTO_UPDATE', @g_alimenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alimenti base - Eliminazione', 'ALIMENTO_DELETE', @g_alimenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_DELETE');

-- --- Alternative alimenti ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alternative alimenti - Creazione', 'ALIMENTO_ALTERNATIVO_CREATE', @g_alt, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_ALTERNATIVO_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alternative alimenti - Lettura', 'ALIMENTO_ALTERNATIVO_READ', @g_alt, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_ALTERNATIVO_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alternative alimenti - Aggiornamento', 'ALIMENTO_ALTERNATIVO_UPDATE', @g_alt, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_ALTERNATIVO_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alternative alimenti - Eliminazione', 'ALIMENTO_ALTERNATIVO_DELETE', @g_alt, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_ALTERNATIVO_DELETE');

-- --- Alimenti da evitare ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alimenti da evitare - Creazione', 'ALIMENTO_DA_EVITARE_CREATE', @g_ev, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_DA_EVITARE_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alimenti da evitare - Lettura', 'ALIMENTO_DA_EVITARE_READ', @g_ev, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_DA_EVITARE_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alimenti da evitare - Aggiornamento', 'ALIMENTO_DA_EVITARE_UPDATE', @g_ev, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_DA_EVITARE_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Alimenti da evitare - Eliminazione', 'ALIMENTO_DA_EVITARE_DELETE', @g_ev, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='ALIMENTO_DA_EVITARE_DELETE');

-- --- Schede ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Schede - Creazione', 'SCHEDA_CREATE', @g_schede, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='SCHEDA_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Schede - Lettura', 'SCHEDA_READ', @g_schede, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='SCHEDA_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Schede - Aggiornamento', 'SCHEDA_UPDATE', @g_schede, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='SCHEDA_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Schede - Eliminazione', 'SCHEDA_DELETE', @g_schede, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='SCHEDA_DELETE');

-- --- Pasti ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Pasti - Creazione', 'PASTO_CREATE', @g_pasti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PASTO_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Pasti - Lettura', 'PASTO_READ', @g_pasti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PASTO_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Pasti - Aggiornamento', 'PASTO_UPDATE', @g_pasti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PASTO_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Pasti - Eliminazione', 'PASTO_DELETE', @g_pasti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PASTO_DELETE');

-- --- Meals (pasti custom) ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Meals - Creazione', 'MEAL_CREATE', @g_meals, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='MEAL_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Meals - Aggiornamento', 'MEAL_UPDATE', @g_meals, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='MEAL_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Meals - Eliminazione', 'MEAL_DELETE', @g_meals, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='MEAL_DELETE');

-- --- Clienti ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Clienti - Creazione', 'CLIENTE_CREATE', @g_clienti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='CLIENTE_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Clienti - Lettura', 'CLIENTE_READ', @g_clienti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='CLIENTE_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Clienti - Aggiornamento', 'CLIENTE_UPDATE', @g_clienti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='CLIENTE_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Clienti - Eliminazione (mio cliente)', 'CLIENTE_MY_DELETE', @g_clienti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='CLIENTE_MY_DELETE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Clienti - Dettaglio esteso', 'CLIENTE_DETTAGLIO', @g_clienti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='CLIENTE_DETTAGLIO');

-- --- Appuntamenti ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Appuntamenti - Creazione', 'APPUNTAMENTO_CREATE', @g_app, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='APPUNTAMENTO_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Appuntamenti - Lettura', 'APPUNTAMENTO_READ', @g_app, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='APPUNTAMENTO_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Appuntamenti - Aggiornamento', 'APPUNTAMENTO_UPDATE', @g_app, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='APPUNTAMENTO_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Appuntamenti - Eliminazione', 'APPUNTAMENTO_DELETE', @g_app, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='APPUNTAMENTO_DELETE');

-- --- Plicometria ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Plicometria - Creazione', 'PLICOMETRIA_CREATE', @g_plico, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PLICOMETRIA_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Plicometria - Lettura', 'PLICOMETRIA_READ', @g_plico, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PLICOMETRIA_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Plicometria - Aggiornamento', 'PLICOMETRIA_UPDATE', @g_plico, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PLICOMETRIA_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Plicometria - Eliminazione', 'PLICOMETRIA_DELETE', @g_plico, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='PLICOMETRIA_DELETE');

-- --- Misurazioni antropometriche ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Misurazioni antropometriche - Creazione', 'MISURAZIONE_ANTROPOMETRICA_CREATE', @g_misu, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='MISURAZIONE_ANTROPOMETRICA_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Misurazioni antropometriche - Lettura', 'MISURAZIONE_ANTROPOMETRICA_READ', @g_misu, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='MISURAZIONE_ANTROPOMETRICA_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Misurazioni antropometriche - Aggiornamento', 'MISURAZIONE_ANTROPOMETRICA_UPDATE', @g_misu, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='MISURAZIONE_ANTROPOMETRICA_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Misurazioni antropometriche - Eliminazione', 'MISURAZIONE_ANTROPOMETRICA_DELETE', @g_misu, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='MISURAZIONE_ANTROPOMETRICA_DELETE');

-- --- Nutrienti (macro/micro) ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Macro - Lettura', 'MACRO_READ', @g_nutr, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='MACRO_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Micro - Lettura', 'MICRO_READ', @g_nutr, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='MICRO_READ');

-- --- Utenti ---
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Utenti - Creazione', 'UTENTE_CREATE', @g_utenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='UTENTE_CREATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Utenti - Lettura', 'UTENTE_READ', @g_utenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='UTENTE_READ');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Utenti - Aggiornamento', 'UTENTE_UPDATE', @g_utenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='UTENTE_UPDATE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Utenti - Eliminazione', 'UTENTE_DELETE', @g_utenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='UTENTE_DELETE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Utenti - Profilo (lettura/aggiornamento profilo)', 'UTENTE_PROFILE', @g_utenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='UTENTE_PROFILE');
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Utenti - Eliminazione profilo', 'UTENTE_DELETE_PROFILE', @g_utenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias='UTENTE_DELETE_PROFILE');

-- =========================================================
-- 5) Assegnazione permessi ai ruoli
-- =========================================================
SELECT 'RBAC seed: assign permissions to roles' AS info;

SET @r_admin := (SELECT id FROM ruoli WHERE alias='ADMIN' LIMIT 1);
SET @r_nutri := (SELECT id FROM ruoli WHERE alias='NUTRIZIONISTA' LIMIT 1);

-- 5.1 ADMIN: tutti i permessi, senza eccezioni
INSERT INTO ruoli_permessi (ruolo_id, permesso_id, created_at, updated_at)
SELECT @r_admin, p.id, @now, NULL
FROM permessi p
WHERE NOT EXISTS (
  SELECT 1 FROM ruoli_permessi rp
  WHERE rp.ruolo_id = @r_admin AND rp.permesso_id = p.id
);

SELECT CONCAT('RBAC seed: ADMIN assigned perms = ', (SELECT COUNT(*) FROM ruoli_permessi WHERE ruolo_id=@r_admin)) AS info;

-- 5.2 NUTRIZIONISTA: permessi funzionalità sito (NO RBAC admin)
-- Include:
-- - gestione contenuti e dati nutrizionali: ALIMENTO*, alternative, da evitare, macro/micro, schede, pasti, meals
-- - gestione dati cliente e visite: clienti, appuntamenti, plicometria, misurazioni antropometriche
-- - profilo utente: UTENTE_PROFILE
INSERT INTO ruoli_permessi (ruolo_id, permesso_id, created_at, updated_at)
SELECT @r_nutri, p.id, @now, NULL
FROM permessi p
WHERE p.alias IN (
  'ALIMENTO_CREATE','ALIMENTO_READ','ALIMENTO_UPDATE','ALIMENTO_DELETE',
  'ALIMENTO_ALTERNATIVO_CREATE','ALIMENTO_ALTERNATIVO_READ','ALIMENTO_ALTERNATIVO_UPDATE','ALIMENTO_ALTERNATIVO_DELETE',
  'ALIMENTO_DA_EVITARE_CREATE','ALIMENTO_DA_EVITARE_READ','ALIMENTO_DA_EVITARE_UPDATE','ALIMENTO_DA_EVITARE_DELETE',
  'SCHEDA_CREATE','SCHEDA_READ','SCHEDA_UPDATE','SCHEDA_DELETE',
  'PASTO_CREATE','PASTO_READ','PASTO_UPDATE','PASTO_DELETE',
  'MEAL_CREATE','MEAL_UPDATE','MEAL_DELETE',
  'CLIENTE_CREATE','CLIENTE_READ','CLIENTE_UPDATE','CLIENTE_MY_DELETE','CLIENTE_DETTAGLIO',
  'APPUNTAMENTO_CREATE','APPUNTAMENTO_READ','APPUNTAMENTO_UPDATE','APPUNTAMENTO_DELETE',
  'PLICOMETRIA_CREATE','PLICOMETRIA_READ','PLICOMETRIA_UPDATE','PLICOMETRIA_DELETE',
  'MISURAZIONE_ANTROPOMETRICA_CREATE','MISURAZIONE_ANTROPOMETRICA_READ','MISURAZIONE_ANTROPOMETRICA_UPDATE','MISURAZIONE_ANTROPOMETRICA_DELETE',
  'MACRO_READ','MICRO_READ',
  'UTENTE_PROFILE'
)
AND NOT EXISTS (
  SELECT 1 FROM ruoli_permessi rp
  WHERE rp.ruolo_id = @r_nutri AND rp.permesso_id = p.id
);

SELECT CONCAT('RBAC seed: NUTRIZIONISTA assigned perms = ', (SELECT COUNT(*) FROM ruoli_permessi WHERE ruolo_id=@r_nutri)) AS info;

-- =========================================================
-- 6) Query di verifica (output)
-- =========================================================
SELECT 'RBAC seed: verification queries' AS info;

-- Verifica ruoli creati
SELECT id, nome, alias FROM ruoli WHERE alias IN ('ADMIN','NUTRIZIONISTA');

-- Verifica permessi mancanti (se ci sono controller nuovi non coperti)
SELECT p.alias
FROM permessi p
WHERE p.alias NOT IN (
  'GRUPPO_CREATE','GRUPPO_READ','GRUPPO_UPDATE','GRUPPO_DELETE',
  'PERMESSO_CREATE','PERMESSO_READ','PERMESSO_UPDATE','PERMESSO_DELETE',
  'RUOLO_CREATE','RUOLO_READ','RUOLO_UPDATE','RUOLO_DELETE',
  'ALIMENTO_CREATE','ALIMENTO_READ','ALIMENTO_UPDATE','ALIMENTO_DELETE',
  'ALIMENTO_ALTERNATIVO_CREATE','ALIMENTO_ALTERNATIVO_READ','ALIMENTO_ALTERNATIVO_UPDATE','ALIMENTO_ALTERNATIVO_DELETE',
  'ALIMENTO_DA_EVITARE_CREATE','ALIMENTO_DA_EVITARE_READ','ALIMENTO_DA_EVITARE_UPDATE','ALIMENTO_DA_EVITARE_DELETE',
  'SCHEDA_CREATE','SCHEDA_READ','SCHEDA_UPDATE','SCHEDA_DELETE',
  'PASTO_CREATE','PASTO_READ','PASTO_UPDATE','PASTO_DELETE',
  'MEAL_CREATE','MEAL_UPDATE','MEAL_DELETE',
  'CLIENTE_CREATE','CLIENTE_READ','CLIENTE_UPDATE','CLIENTE_MY_DELETE','CLIENTE_DETTAGLIO',
  'APPUNTAMENTO_CREATE','APPUNTAMENTO_READ','APPUNTAMENTO_UPDATE','APPUNTAMENTO_DELETE',
  'PLICOMETRIA_CREATE','PLICOMETRIA_READ','PLICOMETRIA_UPDATE','PLICOMETRIA_DELETE',
  'MISURAZIONE_ANTROPOMETRICA_CREATE','MISURAZIONE_ANTROPOMETRICA_READ','MISURAZIONE_ANTROPOMETRICA_UPDATE','MISURAZIONE_ANTROPOMETRICA_DELETE',
  'MACRO_READ','MICRO_READ',
  'UTENTE_CREATE','UTENTE_READ','UTENTE_UPDATE','UTENTE_DELETE','UTENTE_PROFILE','UTENTE_DELETE_PROFILE'
)
ORDER BY p.alias;

-- Elenco permessi per ruolo
SELECT r.alias AS ruolo, p.alias AS permesso, p.nome AS descrizione
FROM ruoli_permessi rp
JOIN ruoli r ON r.id = rp.ruolo_id
JOIN permessi p ON p.id = rp.permesso_id
WHERE r.alias IN ('ADMIN','NUTRIZIONISTA')
ORDER BY r.alias, p.alias;

COMMIT;
SELECT 'RBAC seed: COMMIT OK' AS info;
