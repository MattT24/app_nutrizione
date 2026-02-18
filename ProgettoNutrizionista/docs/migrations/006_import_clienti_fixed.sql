-- Import clienti (fix "Column count doesn't match value count")
--
-- Fonte: clienti.sql esportato da phpMyAdmin
-- Fix applicato:
-- - l’ultima riga (id=67) aveva 1 valore mancante per la colonna `telefono`
-- - aggiunto NULL prima del valore `sesso`
--
-- Nota:
-- - Verificare che gli `utente_id` presenti nel dump esistano nella tabella `utenti`.
-- - La tabella `clienti` ha UNIQUE su `codice_fiscale` ed `email`: se nel DB target esistono già,
--   l’import fallirà per duplicati.

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

INSERT INTO `clienti` (`altezza`, `beve_alcol`, `data_nascita`, `peso`, `created_at`, `id`, `updated_at`, `utente_id`, `assunzione_farmaci`, `codice_fiscale`, `cognome`, `email`, `funzioni_intestinali`, `intolleranze`, `nome`, `num_allenamenti_settimanali`, `problematiche_salutari`, `quantita_qualita_sonno`, `telefono`, `sesso`) VALUES
(180, b'0', '1980-01-01', 75.5, '2025-12-08 19:20:07.000000', 1,  '2025-12-08 19:20:07.000000', 1, 'Nessun farmaco', 'RSSMRA80A01H501A', 'Rossi', 'mario.rossi@email.com', 'Regolari', 'Nessuna intolleranza', 'Mario', '3 volte a settimana', 'Nessuna problematica', '7-8 ore per notte, buona qualità', '3331234567', 'Maschio'),
(180, b'0', '1980-01-01', 75.5, '2025-12-08 19:25:24.000000', 2,  '2025-12-08 19:25:24.000000', 1, 'Nessuna', 'RSSMRA80A01H501U', 'Rossi', 'mario.rossi@example.com', 'Normali', 'Lattosio', 'Mario', '3', 'Nessuna', 'Buona', '3331234567', 'Maschio'),
(180, b'0', '1980-01-01', 75.5, '2025-12-09 13:32:45.000000', 5,  '2025-12-09 13:32:45.000000', 1, 'Nessuna', 'RSSMRA80A01H501I', 'Rossi', 'mario.rossiopis@example.com', 'Normali', 'Lattosio', 'Mario', '3', 'Nessuna', 'Buona', '3331234567', 'Maschio'),
(165, b'1', '1992-05-15', 62.3, '2025-12-09 14:00:00.000000', 6,  '2025-12-09 14:00:00.000000', 1, 'Contraccettivi orali', 'BNCLRD92E55F205A', 'Bianchi', 'chiara.bianchi@email.com', 'Regolari', 'Glutine', 'Chiara', '4', 'Emicrania', '6-7 ore per notte, media qualità', '3479876543', 'Femmina'),
(178, b'0', '1985-11-30', 82.1, '2025-12-09 14:05:00.000000', 7,  '2025-12-09 14:05:00.000000', 1, 'Antidepressivo SSRI', 'VRDGPP85T30A001B', 'Verdi', 'giuseppe.verdi@email.com', 'Irregolari', 'Nessuna', 'Giuseppe', '2', 'Ansia lieve', '8 ore per notte, buona qualità', '3391234567', 'Maschio'),
(170, b'1', '1990-08-22', 68.5, '2025-12-09 14:10:00.000000', 8,  '2025-12-09 14:10:00.000000', 1, 'Antistaminico stagionale', 'RSSLCU90M22H501C', 'Russo', 'lucia.russo@email.com', 'Normali', 'Nichel', 'Lucia', '5', 'Rinite allergica', '7 ore per notte, ottima qualità', '3331112222', 'Femmina'),
(182, b'1', '1978-03-10', 88.7, '2025-12-09 14:15:00.000000', 9,  '2025-12-09 14:15:00.000000', 1, 'Antipertensivo', 'FRRMRA78C10F205D', 'Ferrari', 'marco.ferrari@email.com', 'Regolari', 'Nessuna', 'Marco', '3', 'Ipertensione lieve', '6 ore per notte, scarsa qualità', '3472223333', 'Maschio'),
(160, b'0', '1995-12-05', 55.2, '2025-12-09 14:20:00.000000', 10,  '2025-12-09 14:20:00.000000', 1, 'Nessun farmaco', 'ESPGNY95D45F205E', 'Esposito', 'gianna.esposito@email.com', 'Normali', 'Lattosio', 'Gianna', '4', 'Nessuna', '8-9 ore per notte, ottima qualità', '3293334444', 'Femmina'),
(175, b'1', '1982-07-18', 77.4, '2025-12-09 14:25:00.000000', 11,  '2025-12-09 14:25:00.000000', 1, 'Antinfiammatori occasionali', 'RSSNDR82L18H501F', 'Rossini', 'andrea.rossini@email.com', 'Irregolari', 'Solfiti', 'Andrea', '6', 'Artrosi ginocchio', '7 ore per notte, buona qualità', '3384445555', 'Maschio'),
(168, b'0', '1988-09-25', 65.8, '2025-12-09 14:30:00.000000', 12,  '2025-12-09 14:30:00.000000', 1, 'Vitamine e integratori', 'MNTFRN88P25F205G', 'Monti', 'francesca.monti@email.com', 'Regolari', 'Nessuna', 'Francesca', '3', 'Carenza vitamina D', '7-8 ore per notte, media qualità', '3405556666', 'Femmina'),
(190, b'1', '1975-01-12', 95.2, '2025-12-09 14:35:00.000000', 13,  '2025-12-09 14:35:00.000000', 1, 'Statine', 'CLBGPP75A12F205H', 'Colombo', 'paolo.colombo@email.com', 'Normali', 'Nessuna', 'Paolo', '2', 'Colesterolo alto', '6 ore per notte, scarsa qualità', '3316667777', 'Maschio'),
(172, b'0', '1993-04-08', 70.1, '2025-12-09 14:40:00.000000', 14,  '2025-12-09 14:40:00.000000', 1, 'Contraccettivi orali', 'RCCSNZ93D48F205I', 'Ricci', 'sara.ricci@email.com', 'Regolari', 'Frutta secca', 'Sara', '5', 'Nessuna', '8 ore per notte, ottima qualità', '3457778888', 'Femmina'),
(185, b'1', '1987-06-20', 85.6, '2025-12-09 14:45:00.000000', 15,  '2025-12-09 14:45:00.000000', 1, 'Nessun farmaco', 'MRTDMN87H20F205J', 'Moretti', 'domenico.moretti@email.com', 'Normali', 'Lattosio', 'Domenico', '4', 'Reflusso gastrico', '7 ore per notte, buona qualità', '3338889999', 'Maschio'),
(165, b'1', '1992-05-15', 62.3, '2025-12-09 14:00:00.000000', 26,  '2025-12-09 14:00:00.000000', 1, 'Contraccettivi orali', 'BNCLRD92E55F295A', 'Bianchi', 'chiara.bianchini@email.com', 'Regolari', 'Glutine', 'Chiara', '4', 'Emicrania', '6-7 ore per notte, media qualità', '3479876543', 'Femmina'),
(178, b'0', '1985-11-30', 82.1, '2025-12-09 14:05:00.000000', 27,  '2025-12-09 14:05:00.000000', 1, 'Antidepressivo SSRI', 'VRDGPP85T30U001B', 'Verdi', 'giuseppele.verdi@email.com', 'Irregolari', 'Nessuna', 'Giuseppe', '2', 'Ansia lieve', '8 ore per notte, buona qualità', '3354567890', 'Maschio'),
(165, b'0', '1990-05-20', 60.2, '2025-12-10 16:41:52.000000', 50,  '2025-12-10 16:41:52.000000', 2, 'Nessun farmaco', 'BRNGNN90E20F205A', 'Bianchi', 'anna.bianchi@email.com', 'Regolari', 'Lattosio', 'Anna', '4', 'Nessuna problematica', '7-8 ore per notte, buona qualità', '3471112222', 'Femmina'),
(178, b'1', '1985-08-15', 80.5, '2025-12-10 16:41:52.000000', 51,  '2025-12-10 16:41:52.000000', 2, 'Antistaminico stagionale', 'VRDGPP85M15F205B', 'Verdi', 'giuseppe.i@email.com', 'Normali', 'Nessuna intolleranza', 'Giuseppe', '3', 'Rinite allergica', '6-7 ore per notte, media qualità', '3352223333', 'Maschio'),
(172, b'0', '1992-03-10', 65.8, '2025-12-10 16:41:52.000000', 52,  '2025-12-10 16:41:52.000000', 2, 'Contraccettivi orali', 'RSSLCU92C10F205C', 'Russo', 'lucia.russoee@email.com', 'Regolari', 'Glutine', 'Lucia', '5', 'Nessuna problematica', '8 ore per notte, ottima qualità', '3333334444', 'Femmina'),
(182, b'1', '1978-11-25', 85.3, '2025-12-10 16:41:52.000000', 53,  '2025-12-10 16:41:52.000000', 2, 'Antipertensivo', 'FRRMRA78S25F205D', 'Ferrari', 'marco.ferrweari@email.com', 'Irregolari', 'Nessuna intolleranza', 'Marco', '2', 'Ipertensione lieve', '6 ore per notte, scarsa qualità', '3474445555', 'Maschio'),
(168, b'0', '1995-07-30', 58.7, '2025-12-10 16:41:52.000000', 54,  '2025-12-10 16:41:52.000000', 2, 'Vitamine e integratori', 'ESPGNY95L30F205E', 'Esposito', 'giulia.esssposito@email.com', 'Normali', 'Frutta secca', 'Giulia', '4', 'Carenza di ferro', '7-8 ore per notte, buona qualità', '3295556666', 'Femmina'),
(170, b'1', '1997-07-09', 70, '2025-12-10 15:46:24.000000', 55,  '2025-12-10 15:46:24.000000', 2, 'farmaci', 'RSSMRA80A01H501T', 'Prisco', 'pizzonia.tommaso.2@gmail.com', 'regolari', 'lattosio', 'Giuseppe', '2 volte', 'diabete', 'buona', '3917076370', 'Maschio'),
(0, b'0', '2025-12-31', 0, '2026-01-25 18:00:44.000000', 56,  '2026-01-25 18:00:44.000000', 1, '', '', 'cdscv', '', '', '', 'fede', '', '', '', '', 'Maschio'),
(5, b'0', '2014-02-03', 0.4, '2026-01-25 18:01:35.000000', 57,  '2026-01-25 18:01:35.000000', 1, '', 'RRRRRRRRRRRRRRRR', 'cdscv', 'tommasopizzonia0@gmail.com', '', '', 'fede', '', '', '', '333333333333333333', 'Maschio'),
(5, b'0', '2026-01-15', 0.4, '2026-01-25 18:25:11.000000', 58,  '2026-01-25 18:25:11.000000', 1, '', 'EEEEEEEEEEEEEEEE', 'rrr', 'tommaso@gmail.com', '', '', 'fede', '', '', '', '333333333333333333', 'Femmina'),
(170, b'1', '2003-03-12', 70, '2026-01-25 18:45:43.000000', 59,  '2026-01-25 18:45:43.000000', 1, 'malox', 'EEEEEEEEEEEEEEF', 'Golia', 'tommao@gmail.com', '\nirregolari', 'lattosio', 'Giulio', '2 volte', 'diabete', '8 ore', '33333333333333333', 'Maschio'),
(78, b'0', '2003-02-27', 99, '2026-01-26 00:16:13.000000', 60,  '2026-01-26 00:16:13.000000', 1, '', 'EEEEEEEEEEEEEPPP', 'Golia', 'maso@gmail.com', '', '', 'Giulio', '', '', '', '333333333333333300000', 'Maschio'),
(175, b'0', '1990-01-01', 70, '2026-02-15 11:16:16.000000', 67,  '2026-02-15 11:16:16.000000', 1, 'N', 'MRARSS00A00A000A', 'Rossi', 'cliente@test.it', 'N', 'N', 'Mario', '0', 'N', 'N', NULL, 'Maschio');

COMMIT;

