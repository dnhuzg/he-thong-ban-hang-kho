-- S7-02: ghi nhận thanh toán và đối trừ vào hoá đơn / công nợ. Chạy lại an toàn.

USE oms;

-- Phiếu thu tiền của đại lý; đã ghi nhận thì không xoá
CREATE TABLE IF NOT EXISTS payments (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  code          VARCHAR(20)   NOT NULL,
  customer_id   BIGINT        NOT NULL,
  amount        DECIMAL(15,2) NOT NULL,
  method        ENUM('CASH','BANK_TRANSFER','OTHER') NOT NULL,
  paid_on       DATE          NOT NULL,
  collected_by  BIGINT        NOT NULL,
  note          VARCHAR(500)  NULL,
  created_by    BIGINT        NOT NULL,
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payments_code (code),
  KEY idx_payments_customer (customer_id, paid_on),
  CONSTRAINT fk_payments_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
  CONSTRAINT fk_payments_collected_by FOREIGN KEY (collected_by) REFERENCES users (id),
  CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_payments_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Phân bổ một phiếu thu vào các hoá đơn
CREATE TABLE IF NOT EXISTS payment_allocations (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  payment_id  BIGINT        NOT NULL,
  invoice_id  BIGINT        NOT NULL,
  amount      DECIMAL(15,2) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_payment_allocations_payment (payment_id),
  KEY idx_payment_allocations_invoice (invoice_id),
  CONSTRAINT fk_payment_allocations_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
  CONSTRAINT fk_payment_allocations_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
  CONSTRAINT chk_payment_allocations_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO document_sequences (name, last_number) VALUES ('PAYMENT', 0);

ALTER TABLE invoices MODIFY status ENUM('ISSUED','PARTIAL','PAID','CANCELLED') NOT NULL DEFAULT 'ISSUED';

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='invoices' AND column_name='paid_amount')=0,
 'ALTER TABLE invoices ADD paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER total_amount', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
