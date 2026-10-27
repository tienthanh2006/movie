CREATE DATABASE IF NOT EXISTS user_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS movie_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS venue_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tài khoản ứng dụng (DEV). Khớp với DB_USER / DB_PASSWORD trong application-dev.properties.
CREATE USER IF NOT EXISTS 'cinema'@'%' IDENTIFIED BY 'cinema123';
GRANT ALL PRIVILEGES ON user_db.*    TO 'cinema'@'%';
GRANT ALL PRIVILEGES ON movie_db.*   TO 'cinema'@'%';
GRANT ALL PRIVILEGES ON venue_db.*   TO 'cinema'@'%';
GRANT ALL PRIVILEGES ON booking_db.* TO 'cinema'@'%';
FLUSH PRIVILEGES;
