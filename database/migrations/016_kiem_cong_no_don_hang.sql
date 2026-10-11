-- Backend Sprint 4; chạy lại an toàn, không xoá dữ liệu.
USE oms;


CREATE TABLE IF NOT EXISTS customer_receivables (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  document_code VARCHAR(50) NOT NULL UNIQUE,
  issued_on DATE NOT NULL,
  outstanding DECIMAL(15,2) NOT NULL,
  CONSTRAINT fk_receivable_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT chk_receivable_amount CHECK (outstanding >= 0),
  KEY idx_receivable_customer (customer_id, issued_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS order_credit_reservations (
  order_id BIGINT NOT NULL PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  CONSTRAINT fk_credit_reservation_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_credit_reservation_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT chk_credit_reservation_amount CHECK (amount >= 0),
  KEY idx_credit_reservation_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
