-- ============================================================================
-- 022 — Bonifica tabelle legacy orfane (MANUALE, idempotente)
-- ============================================================================
-- Contesto: tabelle "cruft" lasciate da `ddl-auto=update` (che NON droppa mai le
-- tabelle rimosse dal modello). Nessuna entità/repository/SQL/migrazione le usa
-- più — il modello attuale è:
--   • allergeni del paziente  -> cliente_tag_standard (tag ALL_*) + AvversionePersonale
--   • blacklist alimenti      -> avversione_personale_cliente
--   • obiettivi               -> obiettivi_nutrizionali
--
-- ⚠️ Perché è necessaria (verificato sullo schema reale TiDB v8.5.3, 2026-07-22):
--   Le 3 tabelle hanno una FK verso `clienti` con regola **NO ACTION (RESTRICT)** e
--   `foreign_key_checks=1` → cancellare un cliente che ha una riga figlia in una di
--   queste tabelle FALLISCE con FK violation (l'ORM non le svuota: non sono mappate).
--   `cliente_allergeni` conteneva 1 riga (dato art. 9) orfana e irraggiungibile
--   dall'app → un cliente reale NON era cancellabile = bug art. 17 pre-esistente,
--   invisibile ai test H2 (H2 ricostruisce lo schema dalle sole entità correnti).
--   Il DROP chiude il blocco FK ed erasa il dato orfano (minimizzazione art. 5(1)(c)
--   + erasure art. 17). Verificato: nessun'altra tabella referenzia queste 3.
--
-- Separata dalla 021 (additiva di A6): responsabilità distinta (bonifica schema vs
-- colonne retention). Idempotente (DROP TABLE IF EXISTS).
-- ============================================================================

DROP TABLE IF EXISTS cliente_allergeni;
DROP TABLE IF EXISTS alimenti_da_evitare_per_cliente;
DROP TABLE IF EXISTS storico_obiettivi;
