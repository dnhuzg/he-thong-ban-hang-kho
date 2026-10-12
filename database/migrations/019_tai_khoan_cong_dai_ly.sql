-- Backend Sprint 4; chạy lại an toàn, không xoá dữ liệu.
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='orders' AND column_name='customer_submitted')=0,
 'ALTER TABLE orders ADD customer_submitted BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


CREATE TABLE IF NOT EXISTS customer_accounts (
  customer_id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  CONSTRAINT fk_customer_account_customer FOREIGN KEY(customer_id) REFERENCES customers(id),
  CONSTRAINT fk_customer_account_user FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
