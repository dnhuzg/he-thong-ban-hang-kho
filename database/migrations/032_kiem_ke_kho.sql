-- S5-07: kiểm kê và điều chỉnh tồn kho. Chạy lại an toàn.
USE oms;
CREATE TABLE IF NOT EXISTS inventory_counts (
 id BIGINT NOT NULL AUTO_INCREMENT, code VARCHAR(20) NOT NULL, warehouse_id BIGINT NOT NULL,
 status ENUM('DRAFT','COUNTED','POSTED') NOT NULL DEFAULT 'DRAFT', note VARCHAR(500) NULL,
 created_by BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, posted_by BIGINT NULL, posted_at DATETIME NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_inventory_count_code(code), KEY idx_inventory_count_warehouse(warehouse_id,status),
 CONSTRAINT fk_inventory_count_warehouse FOREIGN KEY(warehouse_id) REFERENCES warehouses(id),
 CONSTRAINT fk_inventory_count_created_by FOREIGN KEY(created_by) REFERENCES users(id), CONSTRAINT fk_inventory_count_posted_by FOREIGN KEY(posted_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS inventory_count_lines (
 id BIGINT NOT NULL AUTO_INCREMENT, count_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
 system_quantity DECIMAL(14,4) NOT NULL, actual_quantity DECIMAL(14,4) NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_inventory_count_product(count_id,product_id),
 CONSTRAINT fk_inventory_count_line_header FOREIGN KEY(count_id) REFERENCES inventory_counts(id) ON DELETE CASCADE,
 CONSTRAINT fk_inventory_count_line_product FOREIGN KEY(product_id) REFERENCES products(id),
 CONSTRAINT chk_inventory_count_quantity CHECK(system_quantity>=0 AND (actual_quantity IS NULL OR actual_quantity>=0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT IGNORE INTO document_sequences(name,last_number) VALUES('INVENTORY_COUNT',0);
SET @sql=IF((SELECT column_type FROM information_schema.columns WHERE table_schema='oms' AND table_name='inventory_movements' AND column_name='movement_type') NOT LIKE '%INVENTORY_COUNT_GAIN%', 'ALTER TABLE inventory_movements MODIFY movement_type ENUM(''SHIPMENT_RETURN'',''GOODS_RECEIPT'',''SHIPMENT_OUT'',''CUSTOMER_RETURN'',''TRANSFER_OUT'',''TRANSFER_IN'',''INVENTORY_COUNT_GAIN'',''INVENTORY_COUNT_LOSS'') NOT NULL','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
