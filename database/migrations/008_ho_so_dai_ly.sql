-- S3-03: hồ sơ đại lý (EP-03): thêm mã số thuế, trạng thái giao dịch và thời điểm sửa vào bảng customers.
-- Đại lý đã phát sinh đơn hàng (S3-09 trở đi) bị khoá ngoại chặn xoá, chỉ chuyển sang Bị khoá giao dịch. Chạy lại vẫn an toàn.
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'oms' AND table_name = 'customers' AND column_name = 'tax_code') = 0,
              'ALTER TABLE customers ADD COLUMN tax_code VARCHAR(14) NULL AFTER phone',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'oms' AND table_name = 'customers' AND column_name = 'status') = 0,
              'ALTER TABLE customers ADD COLUMN status ENUM(''ACTIVE'',''LOCKED'') NOT NULL DEFAULT ''ACTIVE'' AFTER sales_rep_id',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'oms' AND table_name = 'customers' AND column_name = 'updated_at') = 0,
              'ALTER TABLE customers ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = 'oms' AND table_name = 'customers' AND index_name = 'uk_customers_tax_code') = 0,
              'ALTER TABLE customers ADD UNIQUE KEY uk_customers_tax_code (tax_code)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = 'oms' AND table_name = 'customers' AND index_name = 'idx_customers_name') = 0,
              'ALTER TABLE customers ADD KEY idx_customers_name (name)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
