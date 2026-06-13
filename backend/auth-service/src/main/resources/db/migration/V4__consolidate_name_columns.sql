-- V4__consolidate_name_columns.sql
-- Consolidate first_name and last_name into a single full_name column
-- This aligns the database schema with the Java entity definition

-- 1. Add full_name column if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(200);

-- 2. Migrate data from first_name + last_name to full_name
UPDATE users 
SET full_name = CONCAT(
    COALESCE(first_name, ''), 
    CASE WHEN COALESCE(first_name, '') != '' AND COALESCE(last_name, '') != '' THEN ' ' ELSE '' END,
    COALESCE(last_name, '')
)
WHERE full_name IS NULL OR full_name = '';

-- 3. Drop old columns (optional, but recommended for cleanliness)
ALTER TABLE users DROP COLUMN IF EXISTS first_name;
ALTER TABLE users DROP COLUMN IF EXISTS last_name;

-- 4. Add the role column as an ENUM (Flyway will validate this)
-- Note: The role column should already exist from V2, but adding here for completeness
-- If it causes an error "Duplicate column name", you can safely ignore it
