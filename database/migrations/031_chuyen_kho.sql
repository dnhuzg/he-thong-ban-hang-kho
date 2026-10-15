-- S5-06: phiếu chuyển kho nội bộ. Chạy lại an toàn.
USE oms;
CREATE TABLE IF NOT EXISTS warehouse_transfers (
 id BIGINT NOT NULL AUTO_INCREMENT, code VARCHAR(20) NOT NULL, transfer_date DATE NOT NULL,
 source_warehouse_id BIGINT NOT NULL, destination_warehouse_id BIGINT NOT NULL, note VARCHAR(500) NULL,
 status ENUM('DRAFT','IN_TRANSIT','RECEIVED','DISCREPANCY') NOT NULL DEFAULT 'DRAFT', discrepancy_reason VARCHAR(500) NULL,
 created_by BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_at DATETIME NULL, received_at DATETIME NULL, received_by BIGINT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_warehouse_transfer_code(code), KEY idx_transfer_status(status),
 CONSTRAINT fk_transfer_source FOREIGN KEY(source_warehouse_id) REFERENCES warehouses(id), CONSTRAINT fk_transfer_destination FOREIGN KEY(destination_warehouse_id) REFERENCES warehouses(id),
 CONSTRAINT fk_transfer_created_by FOREIGN KEY(created_by) REFERENCES users(id), CONSTRAINT fk_transfer_received_by FOREIGN KEY(received_by) REFERENCES users(id),
 CONSTRAINT chk_transfer_warehouses CHECK(source_warehouse_id<>destination_warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS warehouse_transfer_lines (
 id BIGINT NOT NULL AUTO_INCREMENT, transfer_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
 sent_quantity DECIMAL(14,4) NOT NULL, received_quantity DECIMAL(14,4) NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_transfer_product(transfer_id,product_id),
 CONSTRAINT fk_transfer_line_header FOREIGN KEY(transfer_id) REFERENCES warehouse_transfers(id) ON DELETE CASCADE,
 CONSTRAINT fk_transfer_line_product FOREIGN KEY(product_id) REFERENCES products(id),
 CONSTRAINT chk_transfer_line_quantities CHECK(sent_quantity>0 AND (received_quantity IS NULL OR received_quantity>=0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT IGNORE INTO document_sequences(name,last_number) VALUES('WAREHOUSE_TRANSFER',0);
SET @sql=IF((SELECT column_type FROM information_schema.columns WHERE table_schema='oms' AND table_name='inventory_movements' AND column_name='movement_type') NOT LIKE '%TRANSFER_OUT%', 'ALTER TABLE inventory_movements MODIFY movement_type ENUM(''SHIPMENT_RETURN'',''GOODS_RECEIPT'',''SHIPMENT_OUT'',''CUSTOMER_RETURN'',''TRANSFER_OUT'',''TRANSFER_IN'') NOT NULL','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
