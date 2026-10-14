-- S6-03: phiếu soạn hàng từ đơn đã duyệt, gộp được nhiều đơn; ghi số lượng thực soạn và lý do soạn thiếu.
-- Chạy lại an toàn.

USE oms;

CREATE TABLE IF NOT EXISTS picking_lists (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  code          VARCHAR(20)  NULL,
  warehouse_id  BIGINT       NOT NULL,
  status        ENUM('OPEN','DONE') NOT NULL DEFAULT 'OPEN',
  created_by    BIGINT       NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_by  BIGINT       NULL,
  completed_at  DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_picking_lists_code (code),
  KEY idx_picking_lists_warehouse_status (warehouse_id, status),
  CONSTRAINT fk_picking_lists_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
  CONSTRAINT fk_picking_lists_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_picking_lists_completed_by FOREIGN KEY (completed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Mỗi dòng đơn chỉ được soạn ở một phiếu; số lượng theo đơn vị cơ sở
CREATE TABLE IF NOT EXISTS picking_list_lines (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  picking_list_id   BIGINT        NOT NULL,
  order_id          BIGINT        NOT NULL,
  order_line_id     BIGINT        NOT NULL,
  product_id        BIGINT        NOT NULL,
  required_quantity DECIMAL(14,4) NOT NULL,
  picked_quantity   DECIMAL(14,4) NULL,
  short_reason      VARCHAR(500)  NULL,
  picked_by         BIGINT        NULL,
  picked_at         DATETIME      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_picking_list_lines_order_line (order_line_id),
  KEY idx_picking_list_lines_list (picking_list_id),
  KEY idx_picking_list_lines_order (order_id),
  CONSTRAINT fk_picking_list_lines_list FOREIGN KEY (picking_list_id) REFERENCES picking_lists (id) ON DELETE CASCADE,
  CONSTRAINT fk_picking_list_lines_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_picking_list_lines_order_line FOREIGN KEY (order_line_id) REFERENCES order_lines (id),
  CONSTRAINT fk_picking_list_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_picking_list_lines_picked_by FOREIGN KEY (picked_by) REFERENCES users (id),
  CONSTRAINT chk_picking_list_lines_quantity CHECK (required_quantity > 0 AND (picked_quantity IS NULL OR (picked_quantity >= 0 AND picked_quantity <= required_quantity)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
