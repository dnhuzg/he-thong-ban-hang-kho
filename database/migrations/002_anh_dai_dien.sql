-- S2-03: ảnh đại diện người dùng. Chạy lại nhiều lần vẫn an toàn (MySQL 8 không có ADD COLUMN IF NOT EXISTS).
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'oms' AND table_name = 'users' AND column_name = 'avatar_path') = 0,
              'ALTER TABLE users ADD COLUMN avatar_path VARCHAR(255) NULL AFTER phone',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
