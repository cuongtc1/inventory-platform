-- Who created and last changed each row (docs/conventions.md section 5). Filled by Spring Data JPA auditing:
-- the user from the JWT once login exists (Phase 3), "system" until then.
-- Existing rows get "system" through the temporary DEFAULT, which is then removed so new rows must set it.

ALTER TABLE categories
    ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'system';
ALTER TABLE categories
    ALTER COLUMN created_by DROP DEFAULT,
    ALTER COLUMN updated_by DROP DEFAULT;

ALTER TABLE suppliers
    ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'system';
ALTER TABLE suppliers
    ALTER COLUMN created_by DROP DEFAULT,
    ALTER COLUMN updated_by DROP DEFAULT;

ALTER TABLE products
    ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'system';
ALTER TABLE products
    ALTER COLUMN created_by DROP DEFAULT,
    ALTER COLUMN updated_by DROP DEFAULT;
