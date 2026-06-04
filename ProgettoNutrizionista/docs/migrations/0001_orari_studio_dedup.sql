-- =====================================================================
-- 0001_orari_studio_dedup.sql
-- Bonifica righe duplicate in orari_studio e impedisce future duplicazioni.
--
-- Contesto: l'entità OrariStudio non aveva un vincolo unique su
-- (nutrizionista_id, giorno_settimana). Righe duplicate per lo stesso giorno
-- facevano fallire i lookup Optional (NonUniqueResultException), causando
-- HTTP 400 "Query did not return a unique result: 2 results were returned"
-- alla creazione/modifica di un appuntamento (AppuntamentoService.validaSlotOrario)
-- e al salvataggio orari (OrariStudioService.upsertOrariStudioMe).
--
-- Eseguire UNA TANTUM su TiDB (MySQL-compatible) PRIMA di riavviare il backend.
-- Idempotente: rilanciarlo non causa danni (il DELETE non trova più duplicati;
-- l'ALTER fallisce solo se il vincolo esiste già — vedi nota sotto).
-- =====================================================================

-- 1) Elimina i duplicati tenendo, per ogni (nutrizionista, giorno), la riga con id minimo.
DELETE o1
FROM orari_studio o1
JOIN orari_studio o2
  ON o1.nutrizionista_id = o2.nutrizionista_id
 AND o1.giorno_settimana = o2.giorno_settimana
 AND o1.id > o2.id;

-- 2) Vincolo unique per impedire future duplicazioni.
--    NB: se la tabella ha già un vincolo/indice equivalente, questa ALTER darà
--    errore "Duplicate key name": in tal caso ignorarlo (vincolo già presente).
ALTER TABLE orari_studio
  ADD CONSTRAINT uq_orari_studio_nutri_giorno UNIQUE (nutrizionista_id, giorno_settimana);
