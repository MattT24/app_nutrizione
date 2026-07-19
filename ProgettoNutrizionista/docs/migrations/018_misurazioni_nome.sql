-- ============================================================================
-- Migrazione 018 — Nome opzionale per le misurazioni antropometriche
-- ============================================================================
-- Aggiunge la colonna `nome` (etichetta libera, rinominabile dalla UI con doppio
-- click) alla tabella `misurazioni_antropometriche`. Campo additivo, nullable:
-- nessun impatto sulle righe esistenti (nome = NULL → la UI mostra la data).
--
-- ⚠️ PROD-FIRST: in produzione `ddl-auto=validate` NON modifica lo schema →
-- eseguire QUESTO script PRIMA del deploy. In DEV `ddl-auto=update` Hibernate
-- aggiunge la colonna al riavvio; questo file resta la fonte di verità per PROD.
-- Coerente con la colonna entity `@Column(length = 100)` → VARCHAR(100).
-- ============================================================================

ALTER TABLE misurazioni_antropometriche
    ADD COLUMN nome VARCHAR(100) NULL;
