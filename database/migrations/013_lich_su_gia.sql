-- S3-02: lịch sử thay đổi giá của một sản phẩm (EP-02). Mỗi lần thêm / sửa / bỏ một dòng giá trong bảng giá
-- thêm một dòng vào price_history (giá cũ, giá mới, người sửa, ngày bắt đầu áp dụng, thời điểm sửa).
-- Bảng chỉ thêm, không sửa / xoá: ứng dụng không có câu lệnh UPDATE hay DELETE nào trên bảng này, và khoá ngoại
-- không CASCADE nên sản phẩm hay bảng giá đã có lịch sử không xoá được. Chạy lại vẫn an toàn.
USE oms;

CREATE TABLE IF NOT EXISTS price_history (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  product_id       BIGINT        NOT NULL,
  price_list_id    BIGINT        NOT NULL,
  change_type      ENUM('ADD','UPDATE','REMOVE') NOT NULL,
  old_sale_price   DECIMAL(15,2) NULL,
  old_floor_price  DECIMAL(15,2) NULL,
  new_sale_price   DECIMAL(15,2) NULL,
  new_floor_price  DECIMAL(15,2) NULL,
  effective_from   DATE          NOT NULL,
  changed_by       BIGINT        NOT NULL,
  changed_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_price_history_product (product_id, changed_at),
  KEY idx_price_history_list (price_list_id),
  CONSTRAINT fk_price_history_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_price_history_list FOREIGN KEY (price_list_id) REFERENCES price_lists (id),
  CONSTRAINT fk_price_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
