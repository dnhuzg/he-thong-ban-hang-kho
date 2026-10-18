-- GHTK-57 / S5-09: tồn tối thiểu theo SKU và theo kho; NULL là chưa khai báo (không cảnh báo).
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema='oms' AND table_name='warehouse_inventory'
                 AND column_name='min_quantity')=0,
  'ALTER TABLE warehouse_inventory ADD COLUMN min_quantity DECIMAL(14,4) NULL AFTER reserved',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
