-- S5-03: thông tin kho, vị trí lưu trữ và kho phục vụ đại lý. Chạy lại an toàn.
USE oms;
SET @sql=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='warehouses' AND column_name='address')=0,'ALTER TABLE warehouses ADD address VARCHAR(255) NULL','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='warehouses' AND column_name='manager_name')=0,'ALTER TABLE warehouses ADD manager_name VARCHAR(150) NULL','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='warehouses' AND column_name='status')=0,'ALTER TABLE warehouses ADD status ENUM(''ACTIVE'',''INACTIVE'') NOT NULL DEFAULT ''ACTIVE''','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
CREATE TABLE IF NOT EXISTS warehouse_locations (
 id BIGINT NOT NULL AUTO_INCREMENT, warehouse_id BIGINT NOT NULL, code VARCHAR(30) NOT NULL,
 name VARCHAR(150) NOT NULL, type ENUM('ZONE','SHELF') NOT NULL, parent_id BIGINT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_warehouse_location_code(warehouse_id,code), KEY idx_location_parent(parent_id),
 CONSTRAINT fk_location_warehouse FOREIGN KEY(warehouse_id) REFERENCES warehouses(id),
 CONSTRAINT fk_location_parent FOREIGN KEY(parent_id) REFERENCES warehouse_locations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
