-- S7-05: thu tiền theo tuyến và xác nhận nộp công ty.
USE oms;
CREATE TABLE IF NOT EXISTS route_collections (
 id BIGINT NOT NULL AUTO_INCREMENT, payment_id BIGINT NOT NULL, receipt_path VARCHAR(255) NULL,
 status ENUM('PENDING','CONFIRMED') NOT NULL DEFAULT 'PENDING', confirmed_by BIGINT NULL, confirmed_at DATETIME NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_route_collection_payment(payment_id), KEY idx_route_collection_status(status),
 CONSTRAINT fk_route_collection_payment FOREIGN KEY(payment_id) REFERENCES payments(id), CONSTRAINT fk_route_collection_confirmed_by FOREIGN KEY(confirmed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
