-- Runs once, when the MySQL data volume is created for the first time.
-- One schema per service. Tables are created later by each service's Flyway migrations.
-- The 'stockline' user is created by the MySQL image from MYSQL_USER / MYSQL_PASSWORD.

CREATE DATABASE IF NOT EXISTS auth_db         CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS product_db      CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS inventory_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS order_db        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

GRANT ALL PRIVILEGES ON auth_db.*         TO 'stockline'@'%';
GRANT ALL PRIVILEGES ON product_db.*      TO 'stockline'@'%';
GRANT ALL PRIVILEGES ON inventory_db.*    TO 'stockline'@'%';
GRANT ALL PRIVILEGES ON order_db.*        TO 'stockline'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'stockline'@'%';

FLUSH PRIVILEGES;
