-- 023_utente_subject_id.sql — Migrazione Keycloak Fase 2 (link identità app ↔ IdP).
-- ADDITIVA. `subject_id` = Keycloak `sub` (identificatore stabile dell'utente nell'IdP).
-- NULL finché non avviene il primo login OIDC (link gated su email_verified in KeycloakUserLinkService).
-- Chiave DOPPIA: risoluzione del principal (CurrentUserService.getMe) + erasure key (delete cross-store art. 17, I10).
--
-- UNIQUE: due Utente non possono condividere lo stesso `sub`. In MySQL/TiDB più NULL sono ammessi su una colonna
-- UNIQUE nullable → i record pre-link (subject_id NULL) NON collidono (stesso pattern di codice_fiscale/email opzionali).
--
-- ⚠️ Manuale (prod/dev): ddl-auto=update in dev aggiunge la colonna dall'entità; in prod (validate) va eseguita a mano.
-- Idempotente su TiDB (supporta IF NOT EXISTS su ADD COLUMN / CREATE INDEX). I test H2 NON eseguono questa migrazione
-- (schema ricreato da @Entity: il vincolo unique arriva da @Column(unique=true) su Utente.subjectId).

ALTER TABLE utenti ADD COLUMN IF NOT EXISTS subject_id VARCHAR(255) NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_utenti_subject_id ON utenti (subject_id);
