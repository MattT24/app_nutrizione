-- =====================================================================================
-- 011_off_allergeni.sql — Integrazione OpenFoodFacts (PR-1: fondazione dati)  [rev. 2]
-- =====================================================================================
-- IMPORTANTE: Hibernate (ddl-auto=update), all'avvio del backend, crea AUTOMATICAMENTE:
--   - colonne su alimenti_base: barcode, fonte_allergeni, nutriscore_grade, nova_group,
--       environmental_score_grade, ingredients_text, serving_quantity_g, completezza_off, needs_review
--   - colonne su macro: sale, energia_kj, zuccheri_aggiunti, grassi_trans, colesterolo
--   - tabelle: alimento_allergene(alimento_id, allergene, stato),
--              alimento_nutrient_level(alimento_id, nutriente, livello),
--              alimento_additivo(alimento_id, additivo)
--   - **l'indice UNIQUE composto `uq_alimento_owner_barcode (created_by, barcode)`**
--       (è dichiarato nell'annotazione @Table dell'entità AlimentoBase) → NON crearlo a mano.
--
-- => L'UNICA cosa che Hibernate NON fa è rimuovere il vecchio UNIQUE su `nome`.
--    Questo script serve SOLO a quello (rende possibili nomi duplicati e copie per-utente
--    dello stesso prodotto, su cui si basa il dedup per barcode).
--
-- Ordine consigliato: avviare prima il backend (Hibernate crea schema + unique composto),
-- poi eseguire i passi 1-2 qui sotto.
-- =====================================================================================

-- 1) Trovare il nome reale dell'indice UNIQUE su `nome`
--    (Hibernate lo genera con un nome tipo `UK<hash>`; non è detto sia "nome").
SHOW INDEX FROM alimenti_base WHERE Non_unique = 0 AND Column_name = 'nome';

-- 2) Rimuoverlo, sostituendo <idx> col valore della colonna Key_name trovato al passo 1.
--    Rimuovere un vincolo è sempre sicuro lato dati (nessun pre-flight necessario).
-- ALTER TABLE alimenti_base DROP INDEX `<idx>`;

-- --- Variante "automatica" (best-effort; richiede che TiDB accetti DDL via PREPARE) -------------
-- SET @idx := (SELECT INDEX_NAME FROM information_schema.STATISTICS
--              WHERE TABLE_SCHEMA = 'statera' AND TABLE_NAME = 'alimenti_base'
--                AND COLUMN_NAME = 'nome' AND NON_UNIQUE = 0 LIMIT 1);
-- SET @ddl := IF(@idx IS NULL, 'DO 0', CONCAT('ALTER TABLE alimenti_base DROP INDEX `', @idx, '`'));
-- PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
-- ----------------------------------------------------------------------------------------------

-- NB: l'indice UNIQUE composto (created_by, barcode) è già creato da Hibernate — NON ri-crearlo
--     (un ADD CONSTRAINT/CREATE INDEX su un indice esistente dà ERROR 1061 "Duplicate key name").
--     In MySQL/TiDB i NULL multipli sono ammessi nell'indice UNIQUE → CREA/manuali (barcode NULL) non collidono.

-- 3) (PR-3) Backfill allergeni dei ~900 alimenti CREA in alimento_allergene
--    da crea_alimenti_arricchiti.csv. Verifica post-backfill:  SELECT COUNT(*) FROM alimento_allergene;
