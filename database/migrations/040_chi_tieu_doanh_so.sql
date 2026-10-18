-- GHTK-79 / S8-04: chỉ tiêu doanh số theo tháng, nhân viên và khu vực
USE oms;

CREATE TABLE IF NOT EXISTS sales_targets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    area_id BIGINT NOT NULL,
    target_month DATE NOT NULL,
    target_amount DECIMAL(18,2) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_sales_targets_employee_area_month
        (employee_id, area_id, target_month),

    KEY idx_sales_targets_month (target_month),
    KEY idx_sales_targets_area_month (area_id, target_month),

    CONSTRAINT fk_sales_targets_employee
        FOREIGN KEY (employee_id) REFERENCES users (id),

    CONSTRAINT fk_sales_targets_area
        FOREIGN KEY (area_id) REFERENCES areas (id),

    CONSTRAINT fk_sales_targets_creator
        FOREIGN KEY (created_by) REFERENCES users (id),

    CONSTRAINT chk_sales_targets_amount
        CHECK (target_amount >= 0)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
