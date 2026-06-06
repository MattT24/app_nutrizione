-- 011_crea_allergens_patch.sql
-- Patch per risolvere i falsi negativi (senza_glutine = NULL) e altri allergeni base
-- basandoci sulla categoria testuale CREA degli alimenti.

-- 1. Imposta senza_glutine = TRUE per categorie sicure per natura (non trasformate)
UPDATE alimenti_base 
SET senza_glutine = TRUE 
WHERE senza_glutine IS NULL 
  AND (
    categoria IN (
        'Frutta fresca', 'Frutta secca', 'Ortaggi', 'Verdura', 'Carni fresche', 'Pollame',
        'Pesce fresco e surgelato', 'Molluschi', 'Crostacei', 'Latte', 'Yogurt', 'Formaggi stagionati',
        'Uova', 'Legumi', 'Oli e grassi', 'Acqua e bevande analcoliche', 'Spezie'
    )
    OR categoria LIKE '%verdure%'
    OR categoria LIKE '%frutta%'
    OR categoria LIKE '%carni%'
    OR categoria LIKE '%pesc%'
    OR categoria LIKE '%legum%'
  );

-- 2. Imposta senza_lattosio = TRUE per categorie sicure
UPDATE alimenti_base 
SET senza_lattosio = TRUE 
WHERE senza_lattosio IS NULL 
  AND (
    categoria IN (
        'Frutta fresca', 'Frutta secca', 'Ortaggi', 'Verdura', 'Carni fresche', 'Pollame', 
        'Pesce', 'Uova', 'Legumi', 'Oli vegetali', 
        'Acqua e bevande analcoliche', 'Cereali', 'Riso', 'Pasta'
    )
    OR categoria LIKE '%verdure%'
    OR categoria LIKE '%frutta%'
    OR categoria LIKE '%carni%'
    OR categoria LIKE '%pesc%'
    OR categoria LIKE '%legum%'
  );

-- 3. Imposta vegano = TRUE per categorie puramente vegetali
UPDATE alimenti_base 
SET vegano = TRUE 
WHERE vegano IS NULL 
  AND (
    categoria IN (
        'Frutta fresca', 'Frutta secca', 'Ortaggi', 'Verdura',
        'Legumi', 'Oli vegetali', 
        'Acqua e bevande analcoliche', 'Cereali', 'Riso', 'Pasta'
    )
    OR categoria LIKE '%verdure%'
    OR categoria LIKE '%frutta%'
    OR categoria LIKE '%legum%'
  );

-- 4. Imposta senza_glutine = FALSE palesemente per i derivati del grano
UPDATE alimenti_base 
SET senza_glutine = FALSE 
WHERE senza_glutine IS NULL 
  AND (
    categoria IN ('Prodotti da forno', 'Cereali e derivati', 'Pasta', 'Pane', 'Pizza', 'Biscotti')
    OR nome LIKE '%frumento%' 
    OR nome LIKE '%kamut%'
    OR nome LIKE '%farro%'
    OR nome LIKE '%orzo%'
  );
