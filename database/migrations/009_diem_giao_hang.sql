-- S3-04: nhiều điểm giao hàng cho một đại lý (EP-03). Mỗi đại lý có tối đa một điểm mặc định (do ứng dụng giữ,
-- trong transaction khoá dòng đại lý). Đơn hàng (S3-09) sẽ tham chiếu điểm giao bằng khoá ngoại không xoá dây chuyền,
-- nên điểm đã dùng trong đơn không xoá được. Chạy lại vẫn an toàn.
USE oms;

CREATE TABLE IF NOT EXISTS customer_delivery_points (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  customer_id     BIGINT        NOT NULL,
  address         VARCHAR(255)  NOT NULL,
  receiver_name   VARCHAR(150)  NOT NULL,
  receiver_phone  VARCHAR(20)   NOT NULL,
  route_note      VARCHAR(500)  NULL,
  is_default      BOOLEAN       NOT NULL DEFAULT FALSE,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_delivery_points_customer (customer_id),
  CONSTRAINT fk_delivery_points_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dữ liệu mẫu: mỗi đại lý có sẵn trụ sở làm điểm giao mặc định nếu chưa có điểm nào
INSERT INTO customer_delivery_points (customer_id, address, receiver_name, receiver_phone, route_note, is_default)
SELECT c.id, CONCAT('Trụ sở ', c.name), c.name, COALESCE(c.phone, '0900000000'), NULL, TRUE
FROM customers c
WHERE NOT EXISTS (SELECT 1 FROM customer_delivery_points p WHERE p.customer_id = c.id);
