-- S5-04: phiếu nhập kho từ nhà cung cấp. Phiếu nháp không ảnh hưởng tồn;
-- xác nhận phiếu mới cộng warehouse_inventory, tạo inventory_lots và ghi inventory_movements.
-- Chạy lại an toàn.

USE oms;

CREATE TABLE IF NOT EXISTS goods_receipts (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  code          VARCHAR(20)   NULL,
  supplier_id   BIGINT        NOT NULL,
  document_no   VARCHAR(50)   NOT NULL,
  received_date DATE          NOT NULL,
  warehouse_id  BIGINT        NOT NULL,
  note          VARCHAR(500)  NULL,
  status        ENUM('DRAFT','CONFIRMED') NOT NULL DEFAULT 'DRAFT',
  created_by    BIGINT        NOT NULL,
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  confirmed_by  BIGINT        NULL,
  confirmed_at  DATETIME      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_goods_receipts_code (code),
  UNIQUE KEY uk_goods_receipts_document (supplier_id, document_no),
  KEY idx_goods_receipts_warehouse_date (warehouse_id, received_date),
  CONSTRAINT fk_goods_receipts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
  CONSTRAINT fk_goods_receipts_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
  CONSTRAINT fk_goods_receipts_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_goods_receipts_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lưu cả số lượng theo đơn vị trên chứng từ lẫn bản chụp hệ số, như quy ước của S2-07
CREATE TABLE IF NOT EXISTS goods_receipt_lines (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  receipt_id     BIGINT        NOT NULL,
  line_no        INT           NOT NULL,
  product_id     BIGINT        NOT NULL,
  unit_id        BIGINT        NOT NULL,
  quantity       DECIMAL(15,4) NOT NULL,
  factor         DECIMAL(12,4) NOT NULL,
  base_quantity  DECIMAL(15,4) NOT NULL,
  lot_no         VARCHAR(50)   NULL,
  expiry_date    DATE          NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_goods_receipt_lines_no (receipt_id, line_no),
  KEY idx_goods_receipt_lines_product (product_id),
  CONSTRAINT fk_goods_receipt_lines_receipt FOREIGN KEY (receipt_id) REFERENCES goods_receipts (id) ON DELETE CASCADE,
  CONSTRAINT fk_goods_receipt_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_goods_receipt_lines_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT chk_goods_receipt_lines_quantity CHECK (quantity > 0 AND base_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE inventory_movements
  MODIFY movement_type ENUM('SHIPMENT_RETURN','GOODS_RECEIPT') NOT NULL;
