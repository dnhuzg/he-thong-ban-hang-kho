-- S2-10: bảng giá theo nhóm khách hàng, có hiệu lực và phiên bản. Chạy lại vẫn an toàn.
-- Giá tính theo đơn vị cơ sở của sản phẩm. Bảng giá đã phát sinh đơn (locked_at khác NULL) thì không sửa,
-- chỉ tạo phiên bản mới; đơn hàng (EP-04) đặt locked_at khi lần đầu dùng bảng giá.
USE oms;

CREATE TABLE IF NOT EXISTS customer_groups (
  id    BIGINT       NOT NULL AUTO_INCREMENT,
  code  VARCHAR(20)  NOT NULL,
  name  VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_customer_groups_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO customer_groups (code, name) VALUES
  ('DL1', 'Đại lý cấp 1'), ('DL2', 'Đại lý cấp 2'), ('LE', 'Khách lẻ');

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'oms' AND table_name = 'customers' AND column_name = 'group_id') = 0,
              'ALTER TABLE customers ADD COLUMN group_id BIGINT NULL AFTER area_id, ADD CONSTRAINT fk_customers_group FOREIGN KEY (group_id) REFERENCES customer_groups (id)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS price_lists (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  code               VARCHAR(30)  NOT NULL,
  version            INT          NOT NULL DEFAULT 1,
  name               VARCHAR(150) NOT NULL,
  customer_group_id  BIGINT       NOT NULL,
  valid_from         DATE         NOT NULL,
  valid_to           DATE         NULL,
  previous_id        BIGINT       NULL,
  locked_at          DATETIME     NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_price_lists_code_version (code, version),
  KEY idx_price_lists_group_dates (customer_group_id, valid_from, valid_to),
  CONSTRAINT fk_price_lists_group FOREIGN KEY (customer_group_id) REFERENCES customer_groups (id),
  CONSTRAINT fk_price_lists_previous FOREIGN KEY (previous_id) REFERENCES price_lists (id),
  CONSTRAINT chk_price_lists_dates CHECK (valid_to IS NULL OR valid_to >= valid_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Khoá ngoại tới products không có ON DELETE: sản phẩm đã có trong bảng giá thì không xoá được
CREATE TABLE IF NOT EXISTS price_list_items (
  price_list_id  BIGINT        NOT NULL,
  product_id     BIGINT        NOT NULL,
  sale_price     DECIMAL(15,2) NOT NULL,
  floor_price    DECIMAL(15,2) NOT NULL,
  PRIMARY KEY (price_list_id, product_id),
  KEY idx_price_items_product (product_id),
  CONSTRAINT fk_price_items_list FOREIGN KEY (price_list_id) REFERENCES price_lists (id) ON DELETE CASCADE,
  CONSTRAINT fk_price_items_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_price_items CHECK (floor_price >= 0 AND floor_price <= sale_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Gán nhóm cho đại lý mẫu và 3 bảng giá mẫu đang áp dụng từ 01/09/2026
UPDATE customers SET group_id = (SELECT id FROM customer_groups WHERE code = 'DL1') WHERE code IN ('DL001', 'DL003', 'DL005') AND group_id IS NULL;
UPDATE customers SET group_id = (SELECT id FROM customer_groups WHERE code = 'DL2') WHERE code IN ('DL002', 'DL004') AND group_id IS NULL;

INSERT IGNORE INTO price_lists (code, version, name, customer_group_id, valid_from)
SELECT 'BG-DL1', 1, 'Giá đại lý cấp 1', id, '2026-09-01' FROM customer_groups WHERE code = 'DL1' UNION ALL
SELECT 'BG-DL2', 1, 'Giá đại lý cấp 2', id, '2026-09-01' FROM customer_groups WHERE code = 'DL2' UNION ALL
SELECT 'BG-LE', 1, 'Giá khách lẻ', id, '2026-09-01' FROM customer_groups WHERE code = 'LE';

-- Giá bán = giá vốn × hệ số theo nhóm, giá sàn thấp hơn giá bán một chút (làm tròn trăm đồng)
INSERT IGNORE INTO price_list_items (price_list_id, product_id, sale_price, floor_price)
SELECT pl.id, p.id,
       ROUND(p.cost_price * CASE pl.code WHEN 'BG-DL1' THEN 1.10 WHEN 'BG-DL2' THEN 1.15 ELSE 1.25 END, -2),
       ROUND(p.cost_price * CASE pl.code WHEN 'BG-DL1' THEN 1.05 WHEN 'BG-DL2' THEN 1.08 ELSE 1.15 END, -2)
FROM price_lists pl CROSS JOIN products p
WHERE pl.code IN ('BG-DL1', 'BG-DL2', 'BG-LE') AND pl.version = 1 AND p.cost_price IS NOT NULL;
