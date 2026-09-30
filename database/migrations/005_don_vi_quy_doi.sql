-- S2-07: đơn vị quy đổi của từng sản phẩm (lốc, thùng...) kèm hệ số về đơn vị cơ sở. Chạy lại vẫn an toàn.
-- Đơn vị cơ sở của sản phẩm (products.base_unit_id) luôn có hệ số 1, không lưu ở đây.
-- Dòng đơn hàng / phiếu kho sau này phải lưu kèm hệ số tại thời điểm ghi, để đổi hệ số không làm sai số đã ghi.
USE oms;

CREATE TABLE IF NOT EXISTS product_units (
  product_id  BIGINT        NOT NULL,
  unit_id     BIGINT        NOT NULL,
  factor      DECIMAL(12,4) NOT NULL,
  PRIMARY KEY (product_id, unit_id),
  CONSTRAINT fk_product_units_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
  CONSTRAINT fk_product_units_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT chk_product_units_factor CHECK (factor > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Quy đổi mẫu: 1 lốc = 6 lon, 1 thùng = 24 lon cho các sản phẩm lon; 1 thùng = 30 gói cho mì
INSERT IGNORE INTO product_units (product_id, unit_id, factor)
SELECT p.id, u.id, 6 FROM products p JOIN units b ON b.id = p.base_unit_id AND b.code = 'LON' JOIN units u ON u.code = 'LOC'
UNION ALL
SELECT p.id, u.id, 24 FROM products p JOIN units b ON b.id = p.base_unit_id AND b.code = 'LON' JOIN units u ON u.code = 'THUNG'
UNION ALL
SELECT p.id, u.id, 30 FROM products p JOIN units b ON b.id = p.base_unit_id AND b.code = 'GOI' JOIN units u ON u.code = 'THUNG';
