-- S2-04: nhật ký lưu giá trị trước và sau của mỗi thao tác. Chạy lại nhiều lần vẫn an toàn.
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'oms' AND table_name = 'audit_logs' AND column_name = 'old_value') = 0,
              'ALTER TABLE audit_logs ADD COLUMN old_value TEXT NULL AFTER detail, ADD COLUMN new_value TEXT NULL AFTER old_value',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = 'oms' AND table_name = 'audit_logs' AND index_name = 'idx_audit_actor_created') = 0,
              'CREATE INDEX idx_audit_actor_created ON audit_logs (actor_id, created_at)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
