-- S6-05: xác nhận xuất kho từ phiếu soạn đã hoàn tất. Phiếu xuất (shipments) gắn với đơn,
-- trừ tồn đúng số thực xuất, phần thiếu ghi nợ hàng. Chạy lại an toàn.

USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='oms' AND table_name='shipments' AND column_name='order_id')=0,
 'ALTER TABLE shipments ADD order_id BIGINT NULL AFTER customer_id, ADD KEY idx_shipments_order (order_id), ADD CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE inventory_movements
  MODIFY movement_type ENUM('SHIPMENT_RETURN','GOODS_RECEIPT','SHIPMENT_OUT') NOT NULL;

-- Phần đơn còn nợ hàng sau khi xuất thiếu, để xử lý ở lượt giao sau; số lượng theo đơn vị cơ sở
CREATE TABLE IF NOT EXISTS order_backorders (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  order_id       BIGINT        NOT NULL,
  order_line_id  BIGINT        NOT NULL,
  product_id     BIGINT        NOT NULL,
  quantity       DECIMAL(14,4) NOT NULL,
  reason         VARCHAR(500)  NULL,
  shipment_id    BIGINT        NOT NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_backorders_line (order_line_id),
  KEY idx_order_backorders_order (order_id),
  CONSTRAINT fk_order_backorders_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_order_backorders_line FOREIGN KEY (order_line_id) REFERENCES order_lines (id),
  CONSTRAINT fk_order_backorders_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_order_backorders_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id),
  CONSTRAINT chk_order_backorders_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
