-- S3-05: hạn mức công nợ và số ngày nợ cho phép của từng đại lý (EP-03).
-- Mặc định 0 = không cho nợ. Mọi lần đổi bắt buộc có lý do và ghi vào customer_credit_history (chỉ thêm, không sửa / xoá).
-- Đơn hàng (EP-04) kiểm hạn mức ngay khi chốt đơn. Chạy lại vẫn an toàn.
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'oms' AND table_name = 'customers' AND column_name = 'credit_limit') = 0,
              'ALTER TABLE customers ADD COLUMN credit_limit DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER status, ADD COLUMN credit_days INT NOT NULL DEFAULT 0 AFTER credit_limit, ADD CONSTRAINT chk_customers_credit CHECK (credit_limit >= 0 AND credit_days >= 0)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS customer_credit_history (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  customer_id   BIGINT        NOT NULL,
  old_limit     DECIMAL(15,2) NOT NULL,
  new_limit     DECIMAL(15,2) NOT NULL,
  old_days      INT           NOT NULL,
  new_days      INT           NOT NULL,
  reason        VARCHAR(500)  NOT NULL,
  changed_by    BIGINT        NOT NULL,
  changed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_credit_history_customer (customer_id, changed_at),
  CONSTRAINT fk_credit_history_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
  CONSTRAINT fk_credit_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
