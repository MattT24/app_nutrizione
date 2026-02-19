-- pasto_id column already exists (auto-created by Hibernate)
-- Only need to make alimento_pasto_id nullable

ALTER TABLE alimenti_alternativi
MODIFY COLUMN alimento_pasto_id BIGINT NULL;