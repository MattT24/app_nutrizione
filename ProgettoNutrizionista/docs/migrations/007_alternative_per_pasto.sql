-- pasto_id column already exists (auto-created by Hibernate)
-- Only need to make alimento_pasto_id nullable
-- 1. Ricreiamo la Foreign Key per mantenere il collegamento col cliente (usando "clienti")
ALTER TABLE obiettivi_nutrizionali
ADD CONSTRAINT FK_obiettivo_cliente FOREIGN KEY (cliente_id) REFERENCES clienti (id);

-- 2. Aggiungiamo le nuove colonne per la gestione dello storico
ALTER TABLE obiettivi_nutrizionali
ADD COLUMN attivo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE obiettivi_nutrizionali ADD COLUMN data_creazione DATE;

-- 3. Popoliamo la data di creazione per gli obiettivi già esistenti
UPDATE obiettivi_nutrizionali SET data_creazione = DATE(created_at);