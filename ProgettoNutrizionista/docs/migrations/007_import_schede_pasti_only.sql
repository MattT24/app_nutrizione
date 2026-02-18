-- Import SOLO schede e pasti (da usare dopo import_crea_final_with_categories.sql)
--
-- Motivo:
-- - import_crea_final_with_categories.sql già importa alimenti_base, macro, micro, valori_micronutrienti
-- - questo script importa soltanto le tabelle funzionali "schede" e "pasti"
-- - usa INSERT IGNORE per essere rilanciabile senza errori di duplicato
--
-- Prerequisiti:
-- - clienti e utenti devono essere già presenti (FK su schede.cliente_id, clienti.utente_id)
--
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

INSERT IGNORE INTO `schede` (`attiva`, `cliente_id`, `created_at`, `id`, `updated_at`, `data_creazione`, `nome`) VALUES
(b'1', 67, '2026-02-15 11:16:16.000000', 11, '2026-02-15 11:16:16.000000', '2026-02-15', 'Scheda Test'),
(b'1', 1, '2026-02-15 11:27:56.000000', 12, '2026-02-15 11:27:56.000000', '2026-02-15', 'Nuova Dieta 15/02/2026');

INSERT IGNORE INTO `pasti` (`orario_fine`, `orario_inizio`, `created_at`, `id`, `scheda_id`, `updated_at`, `nome`, `default_code`, `descrizione`, `eliminabile`, `ordine_visualizzazione`) VALUES
(NULL, NULL, '2026-02-15 11:16:16.000000', 26, 11, '2026-02-15 11:16:16.000000', 'Colazione', 'Colazione', NULL, b'0', 1),
(NULL, NULL, '2026-02-15 11:16:16.000000', 27, 11, '2026-02-15 11:16:16.000000', 'Pranzo', 'Pranzo', NULL, b'0', 2),
(NULL, NULL, '2026-02-15 11:16:16.000000', 28, 11, '2026-02-15 11:16:16.000000', 'Merenda', 'Merenda', NULL, b'0', 3),
(NULL, NULL, '2026-02-15 11:16:16.000000', 29, 11, '2026-02-15 11:16:16.000000', 'Cena', 'Cena', NULL, b'0', 4),
('11:29:00.000000', '12:28:00.000000', '2026-02-15 11:27:56.000000', 30, 12, '2026-02-15 11:28:11.000000', 'Colazione', 'Colazione', NULL, b'0', 1),
(NULL, NULL, '2026-02-15 11:27:56.000000', 31, 12, '2026-02-15 11:27:56.000000', 'Pranzo', 'Pranzo', NULL, b'0', 2),
(NULL, NULL, '2026-02-15 11:27:56.000000', 32, 12, '2026-02-15 11:27:56.000000', 'Merenda', 'Merenda', NULL, b'0', 3),
(NULL, NULL, '2026-02-15 11:27:56.000000', 33, 12, '2026-02-15 11:27:56.000000', 'Cena', 'Cena', NULL, b'0', 4);

COMMIT;

