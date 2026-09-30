-- S2-05: danh mục sản phẩm, nhóm hàng (cây nhiều cấp, dùng cho S2-06) và đơn vị tính. Chạy lại vẫn an toàn.
USE oms;

CREATE TABLE IF NOT EXISTS product_categories (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  parent_id   BIGINT       NULL,
  name        VARCHAR(150) NOT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_categories_parent (parent_id),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES product_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS units (
  id    BIGINT      NOT NULL AUTO_INCREMENT,
  code  VARCHAR(20) NOT NULL,
  name  VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_units_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Giá vốn chỉ Quản lý kinh doanh xem / sửa (kiểm ở tầng service, không chỉ ẩn trên giao diện)
CREATE TABLE IF NOT EXISTS products (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  sku           VARCHAR(50)   NOT NULL,
  name          VARCHAR(255)  NOT NULL,
  category_id   BIGINT        NOT NULL,
  base_unit_id  BIGINT        NOT NULL,
  packing_spec  VARCHAR(255)  NULL,
  cost_price    DECIMAL(15,2) NULL,
  image_path    VARCHAR(255)  NULL,
  status        ENUM('ACTIVE','DISCONTINUED') NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_products_sku (sku),
  KEY idx_products_category (category_id),
  KEY idx_products_name (name),
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES product_categories (id),
  CONSTRAINT fk_products_base_unit FOREIGN KEY (base_unit_id) REFERENCES units (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO units (code, name) VALUES
  ('LON', 'Lon'), ('LOC', 'Lốc'), ('THUNG', 'Thùng'), ('CHAI', 'Chai'),
  ('GOI', 'Gói'), ('HOP', 'Hộp'), ('KG', 'Kg'), ('CAI', 'Cái');

-- Nhóm hàng mẫu 3 cấp; chỉ thêm khi bảng còn trống để chạy lại không nhân đôi
INSERT INTO product_categories (id, parent_id, name)
SELECT * FROM (SELECT 1 AS id, NULL AS parent_id, 'Đồ uống' AS name UNION ALL
               SELECT 2, 1, 'Nước giải khát' UNION ALL
               SELECT 3, 2, 'Nước ngọt có ga' UNION ALL
               SELECT 4, 2, 'Nước tăng lực' UNION ALL
               SELECT 5, 1, 'Bia' UNION ALL
               SELECT 6, NULL, 'Thực phẩm khô' UNION ALL
               SELECT 7, 6, 'Mì, phở ăn liền') seed
WHERE NOT EXISTS (SELECT 1 FROM product_categories);

INSERT IGNORE INTO products (sku, name, category_id, base_unit_id, packing_spec, cost_price)
SELECT 'COCA-330', 'Coca-Cola lon 330ml', 3, (SELECT id FROM units WHERE code='LON'), '24 lon/thùng', 7200.00 UNION ALL
SELECT 'PEPSI-330', 'Pepsi lon 330ml', 3, (SELECT id FROM units WHERE code='LON'), '24 lon/thùng', 7000.00 UNION ALL
SELECT 'REDBULL-250', 'Red Bull lon 250ml', 4, (SELECT id FROM units WHERE code='LON'), '24 lon/thùng', 9500.00 UNION ALL
SELECT 'TIGER-330', 'Bia Tiger lon 330ml', 5, (SELECT id FROM units WHERE code='LON'), '24 lon/thùng', 14500.00 UNION ALL
SELECT 'HAOHAO-TOM', 'Mì Hảo Hảo tôm chua cay', 7, (SELECT id FROM units WHERE code='GOI'), '30 gói/thùng', 3300.00;
