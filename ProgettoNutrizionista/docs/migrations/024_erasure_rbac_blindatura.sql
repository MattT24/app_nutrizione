-- =============================================================================
-- 024 — Erasure account: blindatura RBAC (invariante E, art. 17)
-- =============================================================================
-- Idempotente (guard NOT EXISTS / DELETE per alias). La lancia l'utente su TiDB
-- (ddl-auto NON esegue le migrazioni; e queste sono DATI su permessi/ruoli_permessi, non schema).
--
-- (1) SELF-SERVICE: concede UTENTE_DELETE_PROFILE al ruolo NUTRIZIONISTA (whitelist 005 non lo includeva).
--     Sblocca DELETE /api/utenti/profilo → deleteMyProfile (self-scoped by construction: opera su getMe()).
--     ⚠️ Le authority si caricano al LOGIN → il nutrizionista deve RI-LOGGARSI per ottenere il permesso.
--
-- (2) RETIRE UTENTE_DELETE: rimuove il permesso del vettore arbitrario-per-id (l'endpoint DELETE /api/utenti
--     è stato eliminato — Item 2b). Difesa in profondità: senza endpoint il permesso è inerte, ma lo si toglie
--     comunque perché RuoloPermessoController (POST /api/ruoli-permessi, gate RUOLO_UPDATE) potrebbe ri-armarlo a
--     runtime. Nessun seeder Java lo ricrea (era solo in 005, blocco ADMIN già ritirato da 019).
-- =============================================================================

-- (1) grant self-delete a NUTRIZIONISTA
-- NB: created_at è NOT NULL SENZA default sulla tabella (colonna @CreatedDate, popolata da JPA a runtime
--     ma un INSERT SQL grezzo la salta → ERROR 1364) → va valorizzata a mano con NOW(6). updated_at è nullable.
INSERT INTO ruoli_permessi (ruolo_id, permesso_id, created_at)
SELECT r.id, p.id, NOW(6)
FROM ruoli r, permessi p
WHERE r.alias = 'NUTRIZIONISTA'
  AND p.alias = 'UTENTE_DELETE_PROFILE'
  AND NOT EXISTS (
      SELECT 1 FROM ruoli_permessi x WHERE x.ruolo_id = r.id AND x.permesso_id = p.id
  );

-- (2) retire UTENTE_DELETE — prima i link (FK), poi il permesso
DELETE FROM ruoli_permessi WHERE permesso_id IN (SELECT id FROM permessi WHERE alias = 'UTENTE_DELETE');
DELETE FROM permessi WHERE alias = 'UTENTE_DELETE';
