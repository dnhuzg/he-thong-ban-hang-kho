-- Backend Sprint 4; chạy lại an toàn, không xoá dữ liệu.
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='customers' AND column_name='serving_warehouse_id')=0,
 'ALTER TABLE customers ADD serving_warehouse_id BIGINT NULL, ADD CONSTRAINT fk_customer_serving_warehouse FOREIGN KEY (serving_warehouse_id) REFERENCES warehouses(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


CREATE TABLE IF NOT EXISTS warehouse_inventory (
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  on_hand DECIMAL(14,4) NOT NULL DEFAULT 0,
  reserved DECIMAL(14,4) NOT NULL DEFAULT 0,
  PRIMARY KEY (warehouse_id, product_id),
  CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT chk_inventory_nonnegative CHECK (on_hand >= 0 AND reserved >= 0 AND reserved <= on_hand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS order_inventory_reservations (
  order_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity DECIMAL(14,4) NOT NULL,
  PRIMARY KEY (order_id, warehouse_id, product_id),
  CONSTRAINT fk_stock_reservation_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_stock_reservation_inventory FOREIGN KEY (warehouse_id,product_id) REFERENCES warehouse_inventory(warehouse_id,product_id),
  CONSTRAINT chk_stock_reservation_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
