-- GHTK-58 / S6-01: mở rộng bảng lô hiện có, không xóa dữ liệu hoặc tự suy đoán hạn dùng lô cũ.
-- Áp sau 028. Chạy lại an toàn; SKU mặc định chưa bật quản lý lô.
USE oms;

SET @lot_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='products' AND column_name='track_lots')=0, 'ALTER TABLE products ADD COLUMN track_lots BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE lot_stmt FROM @lot_sql; EXECUTE lot_stmt; DEALLOCATE PREPARE lot_stmt;

SET @lot_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='products' AND column_name='expiry_warning_days')=0, 'ALTER TABLE products ADD COLUMN expiry_warning_days INT NOT NULL DEFAULT 30', 'SELECT 1');
PREPARE lot_stmt FROM @lot_sql; EXECUTE lot_stmt; DEALLOCATE PREPARE lot_stmt;

SET @lot_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='inventory_lots' AND column_name='lot_no')=0, 'ALTER TABLE inventory_lots ADD COLUMN lot_no VARCHAR(50) NULL', 'SELECT 1');
PREPARE lot_stmt FROM @lot_sql; EXECUTE lot_stmt; DEALLOCATE PREPARE lot_stmt;

SET @lot_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='inventory_lots' AND column_name='manufactured_date')=0, 'ALTER TABLE inventory_lots ADD COLUMN manufactured_date DATE NULL', 'SELECT 1');
PREPARE lot_stmt FROM @lot_sql; EXECUTE lot_stmt; DEALLOCATE PREPARE lot_stmt;

SET @lot_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='inventory_lots' AND column_name='expiry_date')=0, 'ALTER TABLE inventory_lots ADD COLUMN expiry_date DATE NULL', 'SELECT 1');
PREPARE lot_stmt FROM @lot_sql; EXECUTE lot_stmt; DEALLOCATE PREPARE lot_stmt;

SET @lot_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='goods_receipt_lines' AND column_name='manufactured_date')=0, 'ALTER TABLE goods_receipt_lines ADD COLUMN manufactured_date DATE NULL', 'SELECT 1');
PREPARE lot_stmt FROM @lot_sql; EXECUTE lot_stmt; DEALLOCATE PREPARE lot_stmt;

SET @lot_sql = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='products' AND constraint_name='chk_product_expiry_warning')=0, 'ALTER TABLE products ADD CONSTRAINT chk_product_expiry_warning CHECK (expiry_warning_days BETWEEN 0 AND 365)', 'SELECT 1');
PREPARE lot_stmt FROM @lot_sql; EXECUTE lot_stmt; DEALLOCATE PREPARE lot_stmt;

-- Giữ lại lô đã xuất hết với lượng bằng 0 để truy nguyên và hoàn lại đúng lô khi giao thất bại.
SET @lot_sql = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='inventory_lots' AND constraint_name='chk_inventory_lots_quantity')>0,
 'ALTER TABLE inventory_lots DROP CHECK chk_inventory_lots_quantity', 'SELECT 1');
PREPARE lot_stmt FROM @lot_sql; EXECUTE lot_stmt; DEALLOCATE PREPARE lot_stmt;
ALTER TABLE inventory_lots ADD CONSTRAINT chk_inventory_lots_quantity CHECK (remaining_quantity >= 0);

CREATE TABLE IF NOT EXISTS shipment_lot_allocations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  shipment_id BIGINT NOT NULL,
  lot_id BIGINT NOT NULL,
  quantity DECIMAL(14,4) NOT NULL,
  override_reason VARCHAR(500) NULL,
  override_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shipment_lot (shipment_id,lot_id),
  CONSTRAINT fk_shipment_lot_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
  CONSTRAINT fk_shipment_lot_lot FOREIGN KEY (lot_id) REFERENCES inventory_lots(id),
  CONSTRAINT fk_shipment_lot_actor FOREIGN KEY (override_by) REFERENCES users(id),
  CONSTRAINT chk_shipment_lot_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
