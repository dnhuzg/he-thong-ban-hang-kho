-- S3-09: tạo đơn hàng cho một đại lý (EP-04, phần đơn nháp). Đơn nháp lưu đại lý, điểm giao, ngày giao mong muốn
-- và các dòng hàng (số lượng theo đơn vị đã chọn kèm hệ số quy đổi tại thời điểm ghi). Giá và chiết khấu của đơn nháp
-- luôn tính lại theo bảng giá và chính sách hiện hành; chốt đơn (Sprint 4) mới lưu cố định.
-- Khoá ngoại không CASCADE: đại lý, điểm giao, sản phẩm đã có đơn thì không xoá được. Chạy lại vẫn an toàn.
USE oms;

CREATE TABLE IF NOT EXISTS orders (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  code               VARCHAR(20)  NULL,
  customer_id        BIGINT       NOT NULL,
  delivery_point_id  BIGINT       NULL,
  delivery_date      DATE         NULL,
  note               VARCHAR(500) NULL,
  status             ENUM('DRAFT') NOT NULL DEFAULT 'DRAFT',
  created_by         BIGINT       NOT NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_orders_code (code),
  KEY idx_orders_customer (customer_id),
  KEY idx_orders_updated (updated_at),
  CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
  CONSTRAINT fk_orders_delivery_point FOREIGN KEY (delivery_point_id) REFERENCES customer_delivery_points (id),
  CONSTRAINT fk_orders_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_lines (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  order_id       BIGINT        NOT NULL,
  product_id     BIGINT        NOT NULL,
  unit_id        BIGINT        NOT NULL,
  quantity       INT           NOT NULL,
  factor         DECIMAL(12,4) NOT NULL,
  base_quantity  DECIMAL(14,4) NOT NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_lines_product_unit (order_id, product_id, unit_id),
  KEY idx_order_lines_product (product_id),
  CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
  CONSTRAINT fk_order_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_order_lines_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT chk_order_lines_qty CHECK (quantity > 0 AND factor > 0 AND base_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
