-- S7-06: biên bản đối chiếu công nợ và phản hồi đại lý.
USE oms;
CREATE TABLE IF NOT EXISTS debt_reconciliations (
 id BIGINT NOT NULL AUTO_INCREMENT, code VARCHAR(20) NOT NULL, customer_id BIGINT NOT NULL,
 period_start DATE NOT NULL, period_end DATE NOT NULL, opening_balance DECIMAL(15,2) NOT NULL,
 increase_amount DECIMAL(15,2) NOT NULL, decrease_amount DECIMAL(15,2) NOT NULL, closing_balance DECIMAL(15,2) NOT NULL,
 status ENUM('PENDING','ACCEPTED','DISPUTED') NOT NULL DEFAULT 'PENDING', dispute_reason VARCHAR(1000) NULL,
 created_by BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, responded_at DATETIME NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_reconciliation_code(code), UNIQUE KEY uk_reconciliation_period(customer_id,period_start,period_end),
 CONSTRAINT fk_reconciliation_customer FOREIGN KEY(customer_id) REFERENCES customers(id), CONSTRAINT fk_reconciliation_created_by FOREIGN KEY(created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT IGNORE INTO document_sequences(name,last_number) VALUES('RECONCILIATION',0);
