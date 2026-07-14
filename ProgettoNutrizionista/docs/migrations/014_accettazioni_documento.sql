-- =====================================================================================
-- 014_accettazioni_documento.sql — A4/A9: accettazione documenti legali alla registrazione
-- =====================================================================================
-- Tabella append-only delle accettazioni (Privacy/Termini/DPA) versionate, per Utente.
-- Base giuridica account B2B = contratto (art. 6(1)(b)): è "presa visione/accettazione di documento
-- versionato", NON consenso al trattamento. Una riga per accettazione; revoca = valorizza revocato_at.
--
-- Perché serve: in PROD ddl-auto=validate NON crea lo schema → la tabella deve preesistere. In DEV
-- (ddl-auto=update) Hibernate la crea al riavvio. Nessun seed RBAC (accettazioni self-service).
-- Le versioni correnti dei documenti sono in codice (AccettazioneService, @Value con default), non qui.
-- =====================================================================================

CREATE TABLE IF NOT EXISTS accettazioni_documento (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  utente_id    BIGINT       NOT NULL,
  tipo         VARCHAR(32)  NOT NULL,
  versione     VARCHAR(32)  NOT NULL,
  accettato_at DATETIME(6)  NOT NULL,
  revocato_at  DATETIME(6)  NULL,
  KEY idx_accettazione_utente (utente_id, tipo),
  CONSTRAINT fk_accettazione_utente FOREIGN KEY (utente_id) REFERENCES utenti(id)
) ENGINE=InnoDB;
