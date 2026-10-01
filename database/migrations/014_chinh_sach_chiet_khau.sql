-- S3-01: chính sách chiết khấu theo sản lượng (EP-02). Một chính sách áp cho một sản phẩm HOẶC một nhóm hàng
-- (gồm cả nhóm con), có nhiều bậc theo số lượng mua tính bằng đơn vị cơ sở; mỗi chính sách chiết khấu theo
-- phần trăm hoặc theo số tiền trên một đơn vị. Nhiều chính sách cùng áp dụng thì lấy chính sách có lợi nhất cho
-- khách (xem docs/chinh-sach-chiet-khau.md). Chạy lại vẫn an toàn.
USE oms;

CREATE TABLE IF NOT EXISTS discount_policies (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  name           VARCHAR(150) NOT NULL,
  product_id     BIGINT       NULL,
  category_id    BIGINT       NULL,
  discount_type  ENUM('PERCENT','AMOUNT') NOT NULL,
  valid_from     DATE         NULL,
  valid_to       DATE         NULL,
  status         ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_discount_policies_product (product_id),
  KEY idx_discount_policies_category (category_id),
  CONSTRAINT fk_discount_policies_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
  CONSTRAINT fk_discount_policies_category FOREIGN KEY (category_id) REFERENCES product_categories (id) ON DELETE CASCADE,
  CONSTRAINT chk_discount_policies_scope CHECK ((product_id IS NULL) <> (category_id IS NULL)),
  CONSTRAINT chk_discount_policies_dates CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS discount_tiers (
  policy_id       BIGINT        NOT NULL,
  min_qty         INT           NOT NULL,
  discount_value  DECIMAL(15,2) NOT NULL,
  PRIMARY KEY (policy_id, min_qty),
  CONSTRAINT fk_discount_tiers_policy FOREIGN KEY (policy_id) REFERENCES discount_policies (id) ON DELETE CASCADE,
  CONSTRAINT chk_discount_tiers CHECK (min_qty > 0 AND discount_value > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
