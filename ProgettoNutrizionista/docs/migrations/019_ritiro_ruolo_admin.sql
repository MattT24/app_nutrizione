-- =========================================================
-- 019_ritiro_ruolo_admin.sql — RBAC: ritiro del ruolo ADMIN (D1/D2)
-- Rif.: docs/RBAC-TARGET.md, docs/HANDOFF-RBAC-P0.md (Batch 1).
--
-- Modello target a 2 ruoli: NUTRIZIONISTA (tenant/app) + SUPER_ADMIN (control plane /api/admin/**).
-- Il ruolo ADMIN (storico, "tutti i permessi", authority-string INERTE — nessun hasRole/hasAuthority('ADMIN')
-- nel codice) viene RITIRATO. L'unico utente su ADMIN è l'account dev → migrato a NUTRIZIONISTA (D2: è un
-- tester dell'app che naviga il gestionale).
--
-- ⚠️ Seed RBAC PER-RUOLO (non per-email), IDEMPOTENTE, portabile (nessun ON DELETE CASCADE nuovo).
--    ddl-auto NON esegue la seed → questo SQL va lanciato A MANO (dev e prod), come 005/013.
-- ⚠️ Effetto collaterale operativo (dev): dopo il ritiro NESSUN account dev raggiunge /api/admin/**
--    finché non si configura l'utente SUPER_ADMIN (SuperAdminSeeder: superadmin.email + password-hash).
--    L'ex-admin dev è ora NUTRIZIONISTA (naviga l'app, non il control plane).
-- =========================================================

SET @now   := NOW();
SET @r_admin := (SELECT id FROM ruoli WHERE alias = 'ADMIN'         LIMIT 1);
SET @r_nutri := (SELECT id FROM ruoli WHERE alias = 'NUTRIZIONISTA' LIMIT 1);
SET @r_super := (SELECT id FROM ruoli WHERE alias = 'SUPER_ADMIN'   LIMIT 1);

-- 1) Migra gli utenti dal ruolo ADMIN a NUTRIZIONISTA (D2).
--    PRIMA del drop del ruolo (FK utenti.ruolo_id → ruoli, senza ON DELETE): un utente residuo su
--    ADMIN bloccherebbe il DELETE del ruolo.
UPDATE utenti
SET ruolo_id = @r_nutri
WHERE @r_admin IS NOT NULL AND @r_nutri IS NOT NULL AND ruolo_id = @r_admin;

-- 2) E1 — control plane: SUPER_ADMIN eredita AUDIT_READ (vista globale audit A7, GET /api/audit), finora
--    solo su ADMIN (013). Idempotente. (Gli endpoint /api/admin/** + assistenza/demo gate-ano su
--    hasAuthority('SUPER_ADMIN') → già coperti dal permesso SUPER_ADMIN; AUDIT_READ è l'unica lacuna.)
INSERT INTO ruoli_permessi (ruolo_id, permesso_id, created_at, updated_at)
SELECT @r_super, p.id, @now, NULL
FROM permessi p
WHERE p.alias = 'AUDIT_READ'
  AND @r_super IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ruoli_permessi rp WHERE rp.ruolo_id = @r_super AND rp.permesso_id = p.id
  );

-- 3) Rimuovi le associazioni ruolo↔permesso del ruolo ADMIN, poi il ruolo. Idempotente.
DELETE FROM ruoli_permessi WHERE @r_admin IS NOT NULL AND ruolo_id = @r_admin;
DELETE FROM ruoli WHERE alias = 'ADMIN';

-- 4) Bonifica stray 'ROLE_ADMIN' (nota): 'permessi orario_studio.sql:50' referenzia l'alias 'ROLE_ADMIN'
--    che NON è mai esistito (i ruoli reali sono 'ADMIN'/'NUTRIZIONISTA', senza prefisso 'ROLE_') → quella
--    riga era già un NO-OP (@id_admin NULL → grant saltato). Nessuna azione sul DB necessaria; il file
--    storico resta immutabile. Annotato per evitare falsi allarmi in audit futuri.

-- Verifica attesa post-migrazione:
--   SELECT alias FROM ruoli ORDER BY alias;                   -- → NUTRIZIONISTA, SUPER_ADMIN
--   SELECT COUNT(*) FROM utenti WHERE ruolo_id = @r_admin;    -- → 0
--   -- SUPER_ADMIN possiede AUDIT_READ.
