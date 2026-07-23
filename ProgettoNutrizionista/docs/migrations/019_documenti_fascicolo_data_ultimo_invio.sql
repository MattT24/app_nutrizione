-- ============================================================================
-- Migrazione 019 — Data ultimo invio per i documenti del fascicolo
-- ============================================================================
-- Aggiunge la colonna `data_ultimo_invio` alla tabella `documenti_fascicolo`,
-- valorizzata dal backend a ogni condivisione via email andata a buon fine.
-- La UI la usa per mostrare il banner "documento già inviato" nella modale di
-- condivisione. Campo additivo, nullable: nessun impatto sulle righe esistenti
-- (NULL = mai inviato).
--
-- ⚠️ PROD-FIRST: in produzione `ddl-auto=validate` NON modifica lo schema →
-- eseguire QUESTO script PRIMA del deploy. In DEV `ddl-auto=update` Hibernate
-- aggiunge la colonna al riavvio; questo file resta la fonte di verità per PROD.
-- Coerente con la colonna entity `Instant` → DATETIME(6) (come `data_creazione`).
-- ============================================================================

ALTER TABLE documenti_fascicolo
    ADD COLUMN data_ultimo_invio DATETIME(6) NULL;
