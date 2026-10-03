-- GHTK-39 / S4-01: lưu giá chỉnh thủ công của dòng hàng theo đơn vị cơ sở.
-- NULL nghĩa là dòng hàng tiếp tục dùng giá tự động từ bảng giá đang hiệu lực.
-- Dùng cột riêng để không xung đột với unit_price dùng khi chốt đơn ở Sprint 4.
USE oms;

SET @sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = 'oms'
     AND table_name = 'order_lines'
     AND column_name = 'manual_base_price') = 0,
  'ALTER TABLE order_lines ADD COLUMN manual_base_price DECIMAL(15,2) NULL AFTER base_quantity',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
