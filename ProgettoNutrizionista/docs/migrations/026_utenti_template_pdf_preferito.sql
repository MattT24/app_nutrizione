-- ============================================================================
-- Migrazione 025 — Template PDF preferito per le schede (utenti)
-- ============================================================================
-- Aggiunge la colonna `template_pdf_preferito` alla tabella `utenti`: preferenza
-- di default (DETTAGLIATO/ESSENZIALE) usata da PdfService.resolveTemplatePdf
-- quando il chiamante non specifica un template esplicito (download diretto,
-- invio email, fascicolo). Campo additivo, nullable con default applicativo
-- DETTAGLIATO (stesso comportamento delle righe esistenti = NULL trattato
-- come DETTAGLIATO in PdfService, quindi zero impatto visivo sulle righe già
-- presenti anche senza backfill).
--
-- ⚠️ PROD-FIRST: in produzione `ddl-auto=validate` NON modifica lo schema →
-- eseguire QUESTO script PRIMA del deploy. In DEV `ddl-auto=update` Hibernate
-- aggiunge la colonna al riavvio; questo file resta la fonte di verità per PROD.
-- ============================================================================

ALTER TABLE utenti
    ADD COLUMN template_pdf_preferito VARCHAR(24) NULL DEFAULT 'DETTAGLIATO';
