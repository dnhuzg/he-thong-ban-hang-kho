-- S3-07: khoá / mở giao dịch với một đại lý (EP-03). Đại lý bị khoá (status = LOCKED) không tạo được đơn mới;
-- lý do khoá, người khoá, thời điểm khoá lưu ngay trên đại lý để hiện cảnh báo, còn lịch sử khoá / mở lưu ở
-- customer_lock_history (chỉ thêm, không sửa / xoá). Chạy lại vẫn an toàn.
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'oms' AND table_name = 'customers' AND column_name = 'lock_reason') = 0,
              'ALTER TABLE customers ADD COLUMN lock_reason VARCHAR(500) NULL AFTER credit_days, ADD COLUMN locked_at DATETIME NULL AFTER lock_reason, ADD COLUMN locked_by BIGINT NULL AFTER locked_at, ADD CONSTRAINT fk_customers_locked_by FOREIGN KEY (locked_by) REFERENCES users (id)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS customer_lock_history (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  customer_id   BIGINT        NOT NULL,
  action        ENUM('LOCK','UNLOCK') NOT NULL,
  reason        VARCHAR(500)  NULL,
  changed_by    BIGINT        NOT NULL,
  changed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_lock_history_customer (customer_id, changed_at),
  CONSTRAINT fk_lock_history_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
  CONSTRAINT fk_lock_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
