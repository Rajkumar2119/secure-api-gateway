-- One MySQL server, one database per service (each service owns its data).
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE USER IF NOT EXISTS 'auth'@'%' IDENTIFIED BY 'auth';
GRANT ALL PRIVILEGES ON auth_db.* TO 'auth'@'%';

CREATE DATABASE IF NOT EXISTS catalog_db;
CREATE USER IF NOT EXISTS 'catalog'@'%' IDENTIFIED BY 'catalog';
GRANT ALL PRIVILEGES ON catalog_db.* TO 'catalog'@'%';

FLUSH PRIVILEGES;
