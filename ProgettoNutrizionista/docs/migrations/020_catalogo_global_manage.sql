-- =========================================================
-- 020_catalogo_global_manage.sql — RBAC: governance del catalogo globale (Batch 2, D3/E3)
-- Rif.: docs/RBAC-TARGET.md (D3, E1-E3), docs/HANDOFF-RBAC-P0.md (Batch 2).
--
-- Separa il catalogo GLOBALE (control-plane) dal PERSONALE (nutrizionista):
--  - nuovo permesso CATALOGO_GLOBAL_MANAGE → SUPER_ADMIN: gate di create/update/delete GLOBALI di AlimentoBase.
--  - revoca ALIMENTO_CREATE/UPDATE/DELETE (globali) dal ruolo NUTRIZIONISTA (che tiene ALIMENTO_READ +
--    ALIMENTO_PERSONALE_CREATE) → over-grant chiuso alla radice (difesa in profondità oltre al guard service di Batch 0).
--
-- ⚠️ Seed RBAC IDEMPOTENTE, portabile. ddl-auto NON esegue la seed → lanciare a mano (dev+prod), come 005/013/019.
-- ⚠️ FE-safe: verificato (2 agenti sul codice FE) che il frontend NON chiama mai gli endpoint globali
--    (POST/PUT `/api/alimenti_base`, DELETE `/api/alimenti_base/{id}`) — usa solo `/personale` + import OFF →
--    la revoca dei permessi globali NON rompe alcun flusso FE. Il nutrizionista modifica i propri via il nuovo
--    PUT `/api/alimenti_base/personale` (gate ALIMENTO_PERSONALE_CREATE).
-- =========================================================

SET @now        := NOW();
SET @g_alimenti := (SELECT id FROM gruppi WHERE alias = 'ALIMENTI'      LIMIT 1);
SET @r_super    := (SELECT id FROM ruoli  WHERE alias = 'SUPER_ADMIN'   LIMIT 1);
SET @r_nutri    := (SELECT id FROM ruoli  WHERE alias = 'NUTRIZIONISTA' LIMIT 1);

-- 1) Permesso control-plane CATALOGO_GLOBAL_MANAGE (gruppo ALIMENTI). Idempotente.
INSERT INTO permessi (nome, alias, gruppo_id, created_at, updated_at)
SELECT 'Catalogo globale - Gestione (create/update/delete)', 'CATALOGO_GLOBAL_MANAGE', @g_alimenti, @now, NULL
WHERE NOT EXISTS (SELECT 1 FROM permessi WHERE alias = 'CATALOGO_GLOBAL_MANAGE');

-- 2) Assegna CATALOGO_GLOBAL_MANAGE a SUPER_ADMIN. Idempotente.
INSERT INTO ruoli_permessi (ruolo_id, permesso_id, created_at, updated_at)
SELECT @r_super, p.id, @now, NULL
FROM permessi p
WHERE p.alias = 'CATALOGO_GLOBAL_MANAGE'
  AND @r_super IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ruoli_permessi rp WHERE rp.ruolo_id = @r_super AND rp.permesso_id = p.id);

-- 3) Revoca i 3 permessi GLOBALI del catalogo dal ruolo NUTRIZIONISTA (tiene ALIMENTO_READ + ALIMENTO_PERSONALE_CREATE).
--    NB: ALIMENTO_ALTERNATIVO_* / ALIMENTO_DA_EVITARE_* sono famiglie diverse → NON toccate.
DELETE rp FROM ruoli_permessi rp
JOIN permessi p ON p.id = rp.permesso_id
WHERE @r_nutri IS NOT NULL
  AND rp.ruolo_id = @r_nutri
  AND p.alias IN ('ALIMENTO_CREATE', 'ALIMENTO_UPDATE', 'ALIMENTO_DELETE');

-- Verifica attesa post-migrazione:
--   SELECT p.alias FROM ruoli_permessi rp JOIN permessi p ON p.id=rp.permesso_id
--     WHERE rp.ruolo_id=@r_nutri AND p.alias LIKE 'ALIMENTO%';   -- → solo ALIMENTO_READ, ALIMENTO_PERSONALE_CREATE
--   SELECT 1 FROM ruoli_permessi rp JOIN permessi p ON p.id=rp.permesso_id JOIN ruoli r ON r.id=rp.ruolo_id
--     WHERE r.alias='SUPER_ADMIN' AND p.alias='CATALOGO_GLOBAL_MANAGE';  -- → 1 riga
