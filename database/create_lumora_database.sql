-- Run once as a MySQL/MariaDB administrator before starting Lumora.
-- XAMPP:  C:\xampp\mysql\bin\mysql.exe -u root < database\create_lumora_database.sql
CREATE DATABASE IF NOT EXISTS lumora_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Replace the password before running in any non-local environment.
CREATE USER IF NOT EXISTS 'lumora_user'@'localhost' IDENTIFIED BY 'lumora_dev_pass';
ALTER USER 'lumora_user'@'localhost' IDENTIFIED BY 'lumora_dev_pass';
GRANT ALL PRIVILEGES ON lumora_db.* TO 'lumora_user'@'localhost';
FLUSH PRIVILEGES;
