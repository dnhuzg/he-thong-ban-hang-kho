-- GHTK-78 / S8-03: Lap phieu dieu chinh ton kho co phe duyet
USE oms;

-- Phieu dieu chinh ton kho
CREATE TABLE IF NOT EXISTS inventory_adjustments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    status ENUM('DRAFT','PENDING','APPROVED','REJECTED')
        NOT NULL DEFAULT 'DRAFT',
    note VARCHAR(500) NULL,
    total_loss_value DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME NULL,
    approved_by BIGINT NULL,
    approved_at DATETIME NULL,
    rejected_by BIGINT NULL,
    rejected_at DATETIME NULL,
    rejection_reason VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_adjustment_code (code),
    KEY idx_inventory_adjustment_warehouse (warehouse_id, status),
    CONSTRAINT fk_inventory_adjustment_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_inventory_adjustment_creator
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_inventory_adjustment_approver
        FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT fk_inventory_adjustment_rejector
        FOREIGN KEY (rejected_by) REFERENCES users(id),
    CONSTRAINT chk_inventory_adjustment_loss
        CHECK (total_loss_value >= 0),
    CONSTRAINT chk_inventory_adjustment_separation
        CHECK (approved_by IS NULL OR approved_by <> created_by)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Chi tiet hang hong, mat, het han phai huy
CREATE TABLE IF NOT EXISTS inventory_adjustment_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    adjustment_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    reason ENUM('DAMAGED','LOST','EXPIRED')
        NOT NULL,
    reason_detail VARCHAR(500) NOT NULL,
    quantity DECIMAL(14,4) NOT NULL,
    loss_value DECIMAL(18,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_inventory_adjustment_lines_product (product_id),
    CONSTRAINT fk_inventory_adjustment_line_header
        FOREIGN KEY (adjustment_id)
        REFERENCES inventory_adjustments(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_inventory_adjustment_line_product
        FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_inventory_adjustment_line_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_inventory_adjustment_line_loss
        CHECK (loss_value >= 0),
    CONSTRAINT chk_inventory_adjustment_line_reason
        CHECK (CHAR_LENGTH(TRIM(reason_detail)) > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- So chung tu rieng cho phieu dieu chinh
INSERT IGNORE INTO document_sequences (name, last_number)
VALUES ('INVENTORY_ADJUSTMENT', 0);

-- Bo sung loai bien dong de phieu duyet xuat hien tren the kho.
-- Giu nguyen cac gia tri ENUM movement_type da co.
SET @movement_type = (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inventory_movements'
      AND COLUMN_NAME = 'movement_type'
);

SET @alter_movement = IF(
    @movement_type LIKE '%INVENTORY_ADJUSTMENT_LOSS%',
    'SELECT 1',
    CONCAT(
        'ALTER TABLE inventory_movements MODIFY movement_type ',
        LEFT(@movement_type, CHAR_LENGTH(@movement_type) - 1),
        ',''INVENTORY_ADJUSTMENT_LOSS'') NOT NULL'
    )
);

PREPARE stmt FROM @alter_movement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;