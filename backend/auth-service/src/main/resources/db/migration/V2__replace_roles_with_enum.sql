-- V2__replace_roles_with_enum.sql
-- Replace roles/permissions join tables with a single role enum column on users

-- 1. Add the role column (nullable first so existing rows don't fail)
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER';

-- 2. Migrate existing role assignments
UPDATE users u
    INNER JOIN user_roles ur ON ur.user_id = u.id
    INNER JOIN roles r       ON r.id = ur.role_id
SET u.role = r.name;

-- 3. Drop join tables (FK constraints first)
ALTER TABLE user_roles      DROP FOREIGN KEY fk_ur_user;
ALTER TABLE user_roles      DROP FOREIGN KEY fk_ur_role;
ALTER TABLE role_permissions DROP FOREIGN KEY fk_rp_role;
ALTER TABLE role_permissions DROP FOREIGN KEY fk_rp_permission;

DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS permissions;
