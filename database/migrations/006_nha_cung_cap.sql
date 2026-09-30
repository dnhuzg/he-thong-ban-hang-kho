-- S2-09: danh mục nhà cung cấp (EP-05). Phiếu nhập kho (S5-04) sẽ tham chiếu bảng này bằng khoá ngoại,
-- nhờ đó nhà cung cấp đã có phiếu nhập không xoá được. Chạy lại vẫn an toàn.
USE oms;

CREATE TABLE IF NOT EXISTS suppliers (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  code           VARCHAR(20)  NOT NULL,
  name           VARCHAR(255) NOT NULL,
  tax_code       VARCHAR(14)  NULL,
  contact_name   VARCHAR(150) NULL,
  contact_phone  VARCHAR(20)  NULL,
  email          VARCHAR(150) NULL,
  address        VARCHAR(255) NULL,
  payment_terms  VARCHAR(255) NULL,
  status         ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_suppliers_code (code),
  KEY idx_suppliers_name (name),
  KEY idx_suppliers_tax_code (tax_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO suppliers (code, name, tax_code, contact_name, contact_phone, payment_terms) VALUES
  ('NCC-COCA', 'Công ty TNHH Nước giải khát Coca-Cola Việt Nam', '0300792451', 'Nguyễn Văn Hùng', '0903123456', 'Công nợ 30 ngày'),
  ('NCC-SUNTORY', 'Công ty TNHH Suntory PepsiCo Việt Nam', '0300816663', 'Trần Thị Mai', '0912345670', 'Công nợ 15 ngày'),
  ('NCC-ACECOOK', 'Công ty CP Acecook Việt Nam', '0300808687', 'Lê Minh Tuấn', '0987654320', 'Thanh toán khi nhận hàng');
