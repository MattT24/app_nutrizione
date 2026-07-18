-- =====================================================================================
-- 017_clienti_trattamento_limitato.sql — Limitazione del trattamento (art. 18 GDPR, A5.3)
-- =====================================================================================
-- Aggiunge lo stato "cliente limitato" alla tabella `clienti`:
--   trattamento_limitato : flag; quando true il dato resta CONSERVATO e LEGGIBILE dal titolare,
--                          ma le operazioni che scrivono/producono/inviano dati del cliente sono
--                          bloccate a livello service (LimitazioneTrattamentoValidator → 423 Locked).
--   data_limitazione     : quando è stata attivata la limitazione (opzionale).
--   motivo_limitazione   : motivazione dell'atto (opzionale, registrata anche nell'audit A7 dettaglio).
--
-- Migrazione ADDITIVA. Perché serve: in PROD `spring.jpa.hibernate.ddl-auto=validate` NON modifica
-- lo schema → eseguire PRIMA del deploy. In DEV (`ddl-auto=update`) Hibernate aggiunge le colonne al
-- riavvio; su MySQL/TiDB la colonna NOT NULL riceve il DEFAULT esplicito FALSE per le righe esistenti.
-- Nessun vincolo su PK/enum nativi → compatibile TiDB (< v8.5) e H2 (test, ricreato da zero).
-- =====================================================================================

ALTER TABLE clienti
  ADD COLUMN trattamento_limitato BOOLEAN      NOT NULL DEFAULT FALSE,
  ADD COLUMN data_limitazione     DATETIME     NULL,
  ADD COLUMN motivo_limitazione   VARCHAR(512) NULL;
