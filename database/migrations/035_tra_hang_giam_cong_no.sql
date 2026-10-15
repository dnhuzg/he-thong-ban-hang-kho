-- S8-01/S8-02: trả hàng đại lý và chứng từ giảm công nợ. Chạy lại an toàn.
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='invoices' AND column_name='credited_amount')=0,
 'ALTER TABLE invoices ADD credited_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER paid_amount', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sales_returns (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(20) NOT NULL,
  invoice_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  status ENUM('PENDING','RECEIVED') NOT NULL DEFAULT 'PENDING',
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  received_by BIGINT NULL,
  received_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sales_returns_code (code),
  KEY idx_sales_returns_invoice (invoice_id, status),
  KEY idx_sales_returns_warehouse (warehouse_id, status),
  CONSTRAINT fk_sales_returns_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
  CONSTRAINT fk_sales_returns_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_sales_returns_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_sales_returns_created_by FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT fk_sales_returns_received_by FOREIGN KEY (received_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_return_lines (
  id BIGINT NOT NULL AUTO_INCREMENT,
  return_id BIGINT NOT NULL,
  invoice_line_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity DECIMAL(14,4) NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  item_condition ENUM('GOOD','DAMAGED') NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_return_invoice_line (return_id, invoice_line_id),
  KEY idx_return_lines_invoice_line (invoice_line_id),
  CONSTRAINT fk_return_lines_return FOREIGN KEY (return_id) REFERENCES sales_returns(id) ON DELETE CASCADE,
  CONSTRAINT fk_return_lines_invoice_line FOREIGN KEY (invoice_line_id) REFERENCES invoice_lines(id),
  CONSTRAINT fk_return_lines_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT chk_return_lines_quantity CHECK (quantity > 0 AND unit_price >= 0 AND amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS warehouse_damaged_inventory (
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  on_hand DECIMAL(14,4) NOT NULL DEFAULT 0,
  PRIMARY KEY (warehouse_id, product_id),
  CONSTRAINT fk_damaged_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_damaged_inventory_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT chk_damaged_inventory_nonnegative CHECK (on_hand >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS return_credit_notes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(20) NOT NULL,
  return_id BIGINT NOT NULL,
  invoice_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_return_credit_code (code),
  UNIQUE KEY uk_return_credit_return (return_id),
  KEY idx_return_credit_customer (customer_id, created_at),
  CONSTRAINT fk_return_credit_return FOREIGN KEY (return_id) REFERENCES sales_returns(id),
  CONSTRAINT fk_return_credit_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
  CONSTRAINT fk_return_credit_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_return_credit_created_by FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT chk_return_credit_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO document_sequences (name, last_number) VALUES ('SALES_RETURN', 0), ('RETURN_CREDIT', 0);

SET @sql = IF((SELECT column_type FROM information_schema.columns WHERE table_schema='oms' AND table_name='inventory_movements' AND column_name='movement_type') NOT LIKE '%CUSTOMER_RETURN%',
 'ALTER TABLE inventory_movements MODIFY movement_type ENUM(''SHIPMENT_RETURN'',''GOODS_RECEIPT'',''SHIPMENT_OUT'',''CUSTOMER_RETURN'') NOT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
