-- GHTK-78 / S8-03: phiếu điều chỉnh giảm tồn (hàng hỏng, mất, hết hạn phải huỷ) có phê duyệt.
-- Áp sau 035. Chạy lại an toàn.
USE oms;

SET @sql = IF((SELECT column_type FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='inventory_movements' AND column_name='movement_type') NOT LIKE '%ADJUSTMENT_OUT%',
 'ALTER TABLE inventory_movements MODIFY movement_type ENUM(''SHIPMENT_RETURN'',''GOODS_RECEIPT'',''SHIPMENT_OUT'',''CUSTOMER_RETURN'',''TRANSFER_OUT'',''TRANSFER_IN'',''INVENTORY_COUNT_GAIN'',''INVENTORY_COUNT_LOSS'',''ADJUSTMENT_OUT'') NOT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS stock_adjustments (
  id             BIGINT NOT NULL AUTO_INCREMENT,
  code           VARCHAR(20) NOT NULL,
  warehouse_id   BIGINT NOT NULL,
  product_id     BIGINT NOT NULL,
  lot_id         BIGINT NULL,
  quantity       DECIMAL(14,4) NOT NULL,
  reason         ENUM('DAMAGED','LOST','EXPIRED') NOT NULL,
  note           VARCHAR(500) NOT NULL,
  unit_cost      DECIMAL(15,2) NOT NULL,
  loss_value     DECIMAL(15,2) NOT NULL,
  status         ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  created_by     BIGINT NOT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_by     BIGINT NULL,
  decided_at     DATETIME NULL,
  reject_reason  VARCHAR(500) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_stock_adjustment_code (code),
  KEY idx_stock_adjustment_status (status, warehouse_id),
  CONSTRAINT fk_stock_adjustment_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
  CONSTRAINT fk_stock_adjustment_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_stock_adjustment_lot FOREIGN KEY (lot_id) REFERENCES inventory_lots (id),
  CONSTRAINT fk_stock_adjustment_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_stock_adjustment_decider FOREIGN KEY (decided_by) REFERENCES users (id),
  CONSTRAINT chk_stock_adjustment_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO document_sequences (name, last_number) VALUES ('STOCK_ADJUSTMENT', 0);
