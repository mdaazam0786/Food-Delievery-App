-- V3__rename_admin_to_restaurant_add_driver.sql
-- Rename ROLE_ADMIN to ROLE_RESTAURANT and ensure ROLE_DRIVER is a valid value

-- Update any existing users with ROLE_ADMIN to ROLE_RESTAURANT
UPDATE users SET role = 'ROLE_RESTAURANT' WHERE role = 'ROLE_ADMIN';
