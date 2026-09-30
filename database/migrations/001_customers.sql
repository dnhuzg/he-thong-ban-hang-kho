-- Áp lên database đã tạo trước ngày 2026-09-30 (không xoá dữ liệu). Cài mới thì schema.sql + seed.sql đã có sẵn.
USE oms;

CREATE TABLE IF NOT EXISTS customers (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  code          VARCHAR(20)  NOT NULL,
  name          VARCHAR(200) NOT NULL,
  phone         VARCHAR(20)  NULL,
  area_id       BIGINT       NULL,
  sales_rep_id  BIGINT       NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_customers_code (code),
  KEY idx_customers_sales_rep (sales_rep_id),
  CONSTRAINT fk_customers_area FOREIGN KEY (area_id) REFERENCES areas (id),
  CONSTRAINT fk_customers_sales_rep FOREIGN KEY (sales_rep_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO customers (code, name, phone, area_id) VALUES
  ('DL001', 'Tạp hoá Minh Anh',        '0911000001', 1),
  ('DL002', 'Cửa hàng Hoàng Long',     '0911000002', 1),
  ('DL003', 'Đại lý Phúc Thịnh',       '0911000003', 2),
  ('DL004', 'Siêu thị mini Bình An',   '0911000004', 2),
  ('DL005', 'Nhà phân phối Kinh Bắc',  '0911000005', 3);
