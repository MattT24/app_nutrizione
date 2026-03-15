-- =============================================================================
--  SEED: Ricette Suggerite  (v1 – 2026-03-15)
-- =============================================================================
--  Gli ingredienti sono collegati tramite sub-SELECT sul campo `nome`
--  dell'entità alimenti_base, in modo che lo script sia robusto rispetto
--  agli ID effettivi nel DB e non richieda conoscenza preventiva degli ID.
--
--  PREREQUISITI:
--    · Le tabelle `ricette` e `ricette_ingredienti` devono esistere
--      (create automaticamente da Hibernate con ddl-auto=update al primo avvio).
--    · Gli alimenti referenziati devono essere presenti in `alimenti_base`.
--
--  USO: eseguire una volta sull'ambiente target.
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
--  RICETTA 1 – Bowl Proteica al Pollo
-- ---------------------------------------------------------------------------
INSERT INTO ricette (titolo, descrizione, categoria, fonte, url_immagine, pubblica, created_at, updated_at)
VALUES (
  'Bowl Proteica al Pollo',
  'Un piatto bilanciato ad alto contenuto proteico: riso basmati cotto al vapore, petto di pollo grigliato con spezie, avocado a fette e pomodorini freschi. Ideale come pranzo o cena post-workout.',
  'Alto Proteico',
  NULL,
  NULL,
  TRUE,
  NOW(), NOW()
);

SET @r1 = LAST_INSERT_ID();

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r1, id, 100.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%riso basmati%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r1, id, 150.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%petto di pollo%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r1, id, 80.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%avocado%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r1, id, 100.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%pomodori%' AND LOWER(nome) NOT LIKE '%secchi%' LIMIT 1;

-- ---------------------------------------------------------------------------
--  RICETTA 2 – Insalata Mediterranea
-- ---------------------------------------------------------------------------
INSERT INTO ricette (titolo, descrizione, categoria, fonte, url_immagine, pubblica, created_at, updated_at)
VALUES (
  'Insalata Mediterranea',
  'Insalata ricca di omega-3 e antiossidanti: mix di lattuga e rucola, tonno al naturale sgocciolato, olive nere, feta greca e olio extravergine di oliva. Low-carb e saziante.',
  'Low Carb',
  NULL,
  NULL,
  TRUE,
  NOW(), NOW()
);

SET @r2 = LAST_INSERT_ID();

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r2, id, 80.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%lattuga%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r2, id, 30.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%rucola%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r2, id, 80.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%tonno%' AND LOWER(nome) NOT LIKE '%olio%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r2, id, 30.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%olive%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r2, id, 40.0, 'Feta' FROM alimenti_base WHERE LOWER(nome) LIKE '%feta%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r2, id, 10.0, 'Olio EVO' FROM alimenti_base WHERE LOWER(nome) LIKE '%olio%oliva%' LIMIT 1;

-- ---------------------------------------------------------------------------
--  RICETTA 3 – Porridge Energetico
-- ---------------------------------------------------------------------------
INSERT INTO ricette (titolo, descrizione, categoria, fonte, url_immagine, pubblica, created_at, updated_at)
VALUES (
  'Porridge Energetico',
  'Colazione completa e nutriente: fiocchi d''avena cotti nel latte, banana matura schiacciata per dolcezza naturale, noci per i grassi buoni e un filo di miele. Perfetto prima di un allenamento.',
  'Colazione',
  NULL,
  NULL,
  TRUE,
  NOW(), NOW()
);

SET @r3 = LAST_INSERT_ID();

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r3, id, 80.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%fiocchi%avena%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r3, id, 200.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%latte%' AND LOWER(nome) NOT LIKE '%condensato%' AND LOWER(nome) NOT LIKE '%mandorla%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r3, id, 100.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%banana%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r3, id, 20.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%noci%' AND LOWER(nome) NOT LIKE '%cocco%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r3, id, 15.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%miele%' LIMIT 1;

-- ---------------------------------------------------------------------------
--  RICETTA 4 – Salmone al Forno con Patate Dolci
-- ---------------------------------------------------------------------------
INSERT INTO ricette (titolo, descrizione, categoria, fonte, url_immagine, pubblica, created_at, updated_at)
VALUES (
  'Salmone al Forno con Patate Dolci',
  'Filetto di salmone atlantico cotto al forno con erbe aromatiche, abbinato a patate dolci arrosto e asparagi al vapore. Ricco di omega-3, vitamina D e carboidrati a basso indice glicemico.',
  'Omega-3',
  NULL,
  NULL,
  TRUE,
  NOW(), NOW()
);

SET @r4 = LAST_INSERT_ID();

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r4, id, 200.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%salmone%' AND LOWER(nome) NOT LIKE '%affumicato%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r4, id, 200.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%patate dolci%' OR LOWER(nome) LIKE '%patata dolce%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r4, id, 150.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%asparagi%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r4, id, 10.0, 'Olio EVO' FROM alimenti_base WHERE LOWER(nome) LIKE '%olio%oliva%' LIMIT 1;

-- ---------------------------------------------------------------------------
--  RICETTA 5 – Pasta Integrale con Tonno e Pomodorini
-- ---------------------------------------------------------------------------
INSERT INTO ricette (titolo, descrizione, categoria, fonte, url_immagine, pubblica, created_at, updated_at)
VALUES (
  'Pasta Integrale con Tonno e Pomodorini',
  'Piatto classico della dieta mediterranea rivisitato con pasta integrale per un maggiore apporto di fibre. Tonno al naturale, pomodorini freschi, capperi e un filo d''olio EVO a crudo.',
  'Mediterraneo',
  NULL,
  NULL,
  TRUE,
  NOW(), NOW()
);

SET @r5 = LAST_INSERT_ID();

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r5, id, 90.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%pasta%integrale%' OR (LOWER(nome) LIKE '%spaghetti%' AND LOWER(nome) LIKE '%integrale%') LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r5, id, 80.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%tonno%' AND LOWER(nome) NOT LIKE '%olio%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r5, id, 120.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%pomodori%' AND LOWER(nome) NOT LIKE '%secchi%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r5, id, 10.0, 'Olio EVO' FROM alimenti_base WHERE LOWER(nome) LIKE '%olio%oliva%' LIMIT 1;

-- ---------------------------------------------------------------------------
--  RICETTA 6 – Frittata di Albumi e Verdure
-- ---------------------------------------------------------------------------
INSERT INTO ricette (titolo, descrizione, categoria, fonte, url_immagine, pubblica, created_at, updated_at)
VALUES (
  'Frittata di Albumi e Verdure',
  'Frittata leggerissima ad alto contenuto proteico: albumi d''uovo, spinaci freschi, peperoni colorati e cipolla. Cotta in padella antiaderente con pochissimo olio. Ideale per cena proteica leggera.',
  'Alto Proteico',
  NULL,
  NULL,
  TRUE,
  NOW(), NOW()
);

SET @r6 = LAST_INSERT_ID();

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r6, id, 200.0, 'Albumi d''uovo' FROM alimenti_base WHERE LOWER(nome) LIKE '%albume%' OR LOWER(nome) LIKE '%albumi%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r6, id, 100.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%spinaci%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r6, id, 80.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%peperoni%' OR LOWER(nome) LIKE '%peperone%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r6, id, 50.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%cipolla%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r6, id, 5.0, 'Olio EVO' FROM alimenti_base WHERE LOWER(nome) LIKE '%olio%oliva%' LIMIT 1;

-- ---------------------------------------------------------------------------
--  RICETTA 7 – Yogurt Greco con Frutta e Granola
-- ---------------------------------------------------------------------------
INSERT INTO ricette (titolo, descrizione, categoria, fonte, url_immagine, pubblica, created_at, updated_at)
VALUES (
  'Yogurt Greco con Frutta e Granola',
  'Colazione o spuntino bilanciato: yogurt greco intero per le proteine, mirtilli e fragole fresche per gli antiossidanti, granola croccante per i carboidrati complessi. Veloce e nutriente.',
  'Colazione',
  NULL,
  NULL,
  TRUE,
  NOW(), NOW()
);

SET @r7 = LAST_INSERT_ID();

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r7, id, 150.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%yogurt%greco%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r7, id, 60.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%mirtilli%' OR LOWER(nome) LIKE '%mirtillo%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r7, id, 60.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%fragole%' OR LOWER(nome) LIKE '%fragola%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r7, id, 40.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%granola%' LIMIT 1;

-- ---------------------------------------------------------------------------
--  RICETTA 8 – Zuppa di Legumi e Verdure
-- ---------------------------------------------------------------------------
INSERT INTO ricette (titolo, descrizione, categoria, fonte, url_immagine, pubblica, created_at, updated_at)
VALUES (
  'Zuppa di Legumi e Verdure',
  'Zuppa invernale ricca di proteine vegetali e fibre: fagioli cannellini, lenticchie, carote, sedano e cipolla. Cotta in brodo vegetale con rosmarino. Ideale per chi segue un''alimentazione vegana.',
  'Vegano',
  NULL,
  NULL,
  TRUE,
  NOW(), NOW()
);

SET @r8 = LAST_INSERT_ID();

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r8, id, 80.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%fagioli%' AND LOWER(nome) NOT LIKE '%borlotti%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r8, id, 80.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%lenticchie%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r8, id, 100.0, NULL FROM alimenti_base WHERE LOWER(nome) LIKE '%carote%' OR LOWER(nome) LIKE '%carota%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r8, id, 60.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%sedano%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r8, id, 60.0, NULL  FROM alimenti_base WHERE LOWER(nome) LIKE '%cipolla%' LIMIT 1;

INSERT INTO ricette_ingredienti (ricetta_id, alimento_id, quantita, nome_custom)
SELECT @r8, id, 10.0, 'Olio EVO' FROM alimenti_base WHERE LOWER(nome) LIKE '%olio%oliva%' LIMIT 1;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
--  VERIFICA INSERIMENTO
-- =============================================================================
-- SELECT r.id, r.titolo, r.categoria, COUNT(ri.id) AS num_ingredienti
-- FROM ricette r LEFT JOIN ricette_ingredienti ri ON ri.ricetta_id = r.id
-- GROUP BY r.id, r.titolo, r.categoria
-- ORDER BY r.id;
