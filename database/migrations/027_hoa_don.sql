-- S7-01: phát hành hoá đơn từ phiếu xuất đã giao. Chạy lại an toàn.

USE oms;

-- Bộ đếm số chứng từ: khoá dòng trong transaction phát hành nên số liên tục, không trùng, không nhảy cóc
CREATE TABLE IF NOT EXISTS document_sequences (
  name        VARCHAR(30) NOT NULL,
  last_number  BIGINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dòng hoá đơn theo số thực giao trên phiếu xuất; đơn giá là giá đã chốt trên đơn sau chiết khấu, theo đơn vị cơ sở
CREATE TABLE IF NOT EXISTS invoice_lines (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  invoice_id   BIGINT        NOT NULL,
  shipment_id  BIGINT        NOT NULL,
  product_id   BIGINT        NOT NULL,
  quantity     DECIMAL(14,4) NOT NULL,
  unit_price   DECIMAL(15,2) NOT NULL,
  amount       DECIMAL(15,2) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_invoice_lines_invoice (invoice_id),
  CONSTRAINT fk_invoice_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
  CONSTRAINT fk_invoice_lines_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id),
  CONSTRAINT fk_invoice_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_invoice_lines_quantity CHECK (quantity > 0 AND unit_price >= 0 AND amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO document_sequences (name, last_number) VALUES ('INVOICE', 0);

-- Đại lý chưa có tài khoản cổng thì hoá đơn không có người nhận nhắc hạn
ALTER TABLE invoices MODIFY recipient_user_id BIGINT NULL;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='invoices' AND column_name='issued_by')=0,
 'ALTER TABLE invoices ADD issued_by BIGINT NULL AFTER paid_at, ADD CONSTRAINT fk_invoices_issued_by FOREIGN KEY (issued_by) REFERENCES users (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='shipments' AND column_name='invoice_id')=0,
 'ALTER TABLE shipments ADD invoice_id BIGINT NULL AFTER order_id, ADD KEY idx_shipments_invoice (invoice_id), ADD CONSTRAINT fk_shipments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
