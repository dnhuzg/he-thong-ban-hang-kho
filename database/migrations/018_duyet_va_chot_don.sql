-- Backend Sprint 4; chạy lại an toàn, không xoá dữ liệu.
USE oms;

ALTER TABLE orders MODIFY status VARCHAR(32) NOT NULL DEFAULT 'DRAFT';
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='orders' AND column_name='approval_reason')=0,
 'ALTER TABLE orders ADD approval_reason VARCHAR(1000) NULL, ADD warehouse_id BIGINT NULL, ADD CONSTRAINT fk_order_warehouse FOREIGN KEY(warehouse_id) REFERENCES warehouses(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='order_lines' AND column_name='unit_price')=0,
 'ALTER TABLE order_lines ADD unit_price DECIMAL(15,2) NULL, ADD gross_amount DECIMAL(18,2) NULL, ADD discount_amount DECIMAL(18,2) NULL, ADD discount_note VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


CREATE TABLE IF NOT EXISTS order_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(32) NOT NULL,
  to_status VARCHAR(32) NOT NULL,
  action VARCHAR(30) NOT NULL,
  opinion VARCHAR(500) NULL,
  actor_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_history_order FOREIGN KEY(order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_history_actor FOREIGN KEY(actor_id) REFERENCES users(id),
  KEY idx_order_history_order(order_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
