-- Schema tối thiểu cho Sprint 1 (EP-01: tài khoản, phân quyền). MySQL 8.
-- Chạy lại được nhiều lần: xoá và tạo lại database `oms`.

DROP DATABASE IF EXISTS oms;
CREATE DATABASE oms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oms;

CREATE TABLE users (
  id                   BIGINT       NOT NULL AUTO_INCREMENT,
  username             VARCHAR(50)  NOT NULL,
  email                VARCHAR(150) NOT NULL,
  full_name            VARCHAR(150) NOT NULL,
  phone                VARCHAR(20)  NULL,
  -- Đường dẫn tương đối trong thư mục upload.dir; bản thu nhỏ cùng tên, đuôi -64.png
  avatar_path          VARCHAR(255) NULL,
  password_hash        VARCHAR(100) NOT NULL,
  status               ENUM('PENDING','ACTIVE','LOCKED') NOT NULL DEFAULT 'PENDING',
  must_change_password TINYINT(1)   NOT NULL DEFAULT 1,
  failed_login_count   INT          NOT NULL DEFAULT 0,
  login_locked_until   DATETIME     NULL,
  -- Tăng lên mỗi khi cần thu hồi mọi phiên đang mở (đổi mật khẩu, khoá tài khoản)
  session_version      INT          NOT NULL DEFAULT 0,
  lock_reason          VARCHAR(500) NULL,
  locked_at            DATETIME     NULL,
  locked_by            BIGINT       NULL,
  created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_status (status),
  KEY idx_users_phone (phone),
  CONSTRAINT fk_users_locked_by FOREIGN KEY (locked_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
  code        VARCHAR(30)  NOT NULL,
  name        VARCHAR(100) NOT NULL,
  sort_order  INT          NOT NULL,
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
  user_id    BIGINT      NOT NULL,
  role_code  VARCHAR(30) NOT NULL,
  PRIMARY KEY (user_id, role_code),
  KEY idx_user_roles_role (role_code),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code) REFERENCES roles (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE modules (
  code        VARCHAR(30)  NOT NULL,
  name        VARCHAR(100) NOT NULL,
  sort_order  INT          NOT NULL,
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Mức quyền: F toàn quyền, W ghi, W_OWN ghi dữ liệu của mình, R xem, R_OWN xem dữ liệu của mình.
-- Không có dòng = không truy cập (mặc định từ chối). ADMIN không lưu ở đây: luôn toàn quyền.
CREATE TABLE role_permissions (
  role_code    VARCHAR(30) NOT NULL,
  module_code  VARCHAR(30) NOT NULL,
  level        ENUM('F','W','W_OWN','R','R_OWN') NOT NULL,
  PRIMARY KEY (role_code, module_code),
  CONSTRAINT fk_role_perm_role FOREIGN KEY (role_code) REFERENCES roles (code),
  CONSTRAINT fk_role_perm_module FOREIGN KEY (module_code) REFERENCES modules (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE warehouses (
  id    BIGINT       NOT NULL AUTO_INCREMENT,
  code  VARCHAR(20)  NOT NULL,
  name  VARCHAR(150) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_warehouses_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE areas (
  id    BIGINT       NOT NULL AUTO_INCREMENT,
  code  VARCHAR(20)  NOT NULL,
  name  VARCHAR(150) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_areas_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Đại lý: bản tối thiểu cho S1-10 (cảnh báo bàn giao khi khoá nhân viên). EP-03 sẽ mở rộng.
-- S2-10: nhóm khách hàng quyết định bảng giá áp dụng
CREATE TABLE customer_groups (
  id    BIGINT       NOT NULL AUTO_INCREMENT,
  code  VARCHAR(20)  NOT NULL,
  name  VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_customer_groups_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customers (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  code          VARCHAR(20)  NOT NULL,
  name          VARCHAR(200) NOT NULL,
  phone         VARCHAR(20)  NULL,
  tax_code      VARCHAR(14)  NULL,
  area_id       BIGINT       NULL,
  group_id      BIGINT       NULL,
  sales_rep_id  BIGINT       NULL,
  status        ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
  credit_limit  DECIMAL(15,2) NOT NULL DEFAULT 0,
  credit_days   INT          NOT NULL DEFAULT 0,
  lock_reason   VARCHAR(500) NULL,
  locked_at     DATETIME     NULL,
  locked_by     BIGINT       NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_customers_code (code),
  UNIQUE KEY uk_customers_tax_code (tax_code),
  KEY idx_customers_name (name),
  KEY idx_customers_sales_rep (sales_rep_id),
  CONSTRAINT chk_customers_credit CHECK (credit_limit >= 0 AND credit_days >= 0),
  CONSTRAINT fk_customers_area FOREIGN KEY (area_id) REFERENCES areas (id),
  CONSTRAINT fk_customers_group FOREIGN KEY (group_id) REFERENCES customer_groups (id),
  CONSTRAINT fk_customers_sales_rep FOREIGN KEY (sales_rep_id) REFERENCES users (id),
  CONSTRAINT fk_customers_locked_by FOREIGN KEY (locked_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- S3-04: điểm giao hàng của đại lý; mỗi đại lý tối đa một điểm mặc định (ứng dụng giữ)
CREATE TABLE customer_delivery_points (
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

-- S3-05: lịch sử thay đổi hạn mức công nợ (chỉ thêm, không sửa / xoá); mọi lần đổi bắt buộc có lý do
CREATE TABLE customer_credit_history (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  customer_id   BIGINT        NOT NULL,
  old_limit     DECIMAL(15,2) NOT NULL,
  new_limit     DECIMAL(15,2) NOT NULL,
  old_days      INT           NOT NULL,
  new_days      INT           NOT NULL,
  reason        VARCHAR(500)  NOT NULL,
  changed_by    BIGINT        NOT NULL,
  changed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_credit_history_customer (customer_id, changed_at),
  CONSTRAINT fk_credit_history_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
  CONSTRAINT fk_credit_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- S3-06: lịch sử phân công người phụ trách đại lý (chỉ thêm, không sửa / xoá); null là chưa có người phụ trách
CREATE TABLE customer_assignments (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  customer_id   BIGINT        NOT NULL,
  from_user_id  BIGINT        NULL,
  to_user_id    BIGINT        NULL,
  reason        VARCHAR(500)  NULL,
  changed_by    BIGINT        NOT NULL,
  changed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_assignments_customer (customer_id, changed_at),
  KEY idx_assignments_changed_at (changed_at),
  CONSTRAINT fk_assignments_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
  CONSTRAINT fk_assignments_from FOREIGN KEY (from_user_id) REFERENCES users (id),
  CONSTRAINT fk_assignments_to FOREIGN KEY (to_user_id) REFERENCES users (id),
  CONSTRAINT fk_assignments_by FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- S3-07: lịch sử khoá / mở giao dịch với đại lý (chỉ thêm, không sửa / xoá)
CREATE TABLE customer_lock_history (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  customer_id   BIGINT        NOT NULL,
  action        ENUM('LOCK','UNLOCK') NOT NULL,
  reason        VARCHAR(500)  NULL,
  changed_by    BIGINT        NOT NULL,
  changed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_lock_history_customer (customer_id, changed_at),
  CONSTRAINT fk_lock_history_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
  CONSTRAINT fk_lock_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- S2-05 / S2-06: nhóm hàng (cây nhiều cấp), đơn vị tính, sản phẩm. Giá vốn chỉ Quản lý kinh doanh xem / sửa.
CREATE TABLE product_categories (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  parent_id   BIGINT       NULL,
  name        VARCHAR(150) NOT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_categories_parent (parent_id),
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES product_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE units (
  id    BIGINT      NOT NULL AUTO_INCREMENT,
  code  VARCHAR(20) NOT NULL,
  name  VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_units_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE products (
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

-- S2-07: đơn vị quy đổi; dòng giao dịch sau này lưu kèm hệ số tại thời điểm ghi
CREATE TABLE product_units (
  product_id  BIGINT        NOT NULL,
  unit_id     BIGINT        NOT NULL,
  factor      DECIMAL(12,4) NOT NULL,
  PRIMARY KEY (product_id, unit_id),
  CONSTRAINT fk_product_units_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
  CONSTRAINT fk_product_units_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT chk_product_units_factor CHECK (factor > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- S2-09: nhà cung cấp; phiếu nhập kho sau này tham chiếu bằng khoá ngoại để chặn xoá
CREATE TABLE suppliers (
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

-- S2-10: bảng giá có hiệu lực, phiên bản; đã phát sinh đơn (locked_at) thì chỉ tạo phiên bản mới
CREATE TABLE price_lists (
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

CREATE TABLE price_list_items (
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

-- S3-02: lịch sử đổi giá của sản phẩm; chỉ thêm, không sửa / xoá (khoá ngoại không CASCADE)
CREATE TABLE price_history (
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

-- S3-01: chính sách chiết khấu theo sản lượng (một sản phẩm hoặc một nhóm hàng, nhiều bậc theo số lượng)
CREATE TABLE discount_policies (
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

CREATE TABLE discount_tiers (
  policy_id       BIGINT        NOT NULL,
  min_qty         INT           NOT NULL,
  discount_value  DECIMAL(15,2) NOT NULL,
  PRIMARY KEY (policy_id, min_qty),
  CONSTRAINT fk_discount_tiers_policy FOREIGN KEY (policy_id) REFERENCES discount_policies (id) ON DELETE CASCADE,
  CONSTRAINT chk_discount_tiers CHECK (min_qty > 0 AND discount_value > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- S3-09: đơn hàng nháp và các dòng hàng (EP-04 mở rộng trạng thái); khoá ngoại không CASCADE trừ dòng theo đơn
CREATE TABLE orders (
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

CREATE TABLE order_lines (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  order_id       BIGINT        NOT NULL,
  product_id     BIGINT        NOT NULL,
  unit_id        BIGINT        NOT NULL,
  quantity       INT           NOT NULL,
  factor         DECIMAL(12,4) NOT NULL,
  base_quantity  DECIMAL(14,4) NOT NULL,
  manual_base_price DECIMAL(15,2) NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_lines_product_unit (order_id, product_id, unit_id),
  KEY idx_order_lines_product (product_id),
  CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
  CONSTRAINT fk_order_lines_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_order_lines_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT chk_order_lines_qty CHECK (quantity > 0 AND factor > 0 AND base_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_warehouses (
  user_id       BIGINT NOT NULL,
  warehouse_id  BIGINT NOT NULL,
  PRIMARY KEY (user_id, warehouse_id),
  CONSTRAINT fk_user_wh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_wh_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_areas (
  user_id  BIGINT NOT NULL,
  area_id  BIGINT NOT NULL,
  PRIMARY KEY (user_id, area_id),
  CONSTRAINT fk_user_area_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_area_area FOREIGN KEY (area_id) REFERENCES areas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chỉ lưu hash SHA-256 của token, không lưu token gốc
CREATE TABLE password_reset_tokens (
  id          BIGINT    NOT NULL AUTO_INCREMENT,
  user_id     BIGINT    NOT NULL,
  token_hash  CHAR(64)  NOT NULL,
  expires_at  DATETIME  NOT NULL,
  used_at     DATETIME  NULL,
  created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reset_token_hash (token_hash),
  KEY idx_reset_user (user_id),
  CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  actor_id    BIGINT       NULL,
  action      VARCHAR(50)  NOT NULL,
  target_type VARCHAR(50)  NOT NULL,
  target_id   BIGINT       NULL,
  detail      VARCHAR(1000) NULL,
  -- Giá trị trước / sau khi thay đổi, dạng chữ đọc được (S2-04)
  old_value   TEXT         NULL,
  new_value   TEXT         NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_target (target_type, target_id),
  KEY idx_audit_created (created_at),
  KEY idx_audit_actor_created (actor_id, created_at),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Backend Sprint 4

CREATE TABLE IF NOT EXISTS customer_receivables (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  document_code VARCHAR(50) NOT NULL UNIQUE,
  issued_on DATE NOT NULL,
  outstanding DECIMAL(15,2) NOT NULL,
  CONSTRAINT fk_receivable_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT chk_receivable_amount CHECK (outstanding >= 0),
  KEY idx_receivable_customer (customer_id, issued_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS order_credit_reservations (
  order_id BIGINT NOT NULL PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  CONSTRAINT fk_credit_reservation_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_credit_reservation_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT chk_credit_reservation_amount CHECK (amount >= 0),
  KEY idx_credit_reservation_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Backend Sprint 4

CREATE TABLE IF NOT EXISTS warehouse_inventory (
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  on_hand DECIMAL(14,4) NOT NULL DEFAULT 0,
  reserved DECIMAL(14,4) NOT NULL DEFAULT 0,
  PRIMARY KEY (warehouse_id, product_id),
  CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT chk_inventory_nonnegative CHECK (on_hand >= 0 AND reserved >= 0 AND reserved <= on_hand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS order_inventory_reservations (
  order_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity DECIMAL(14,4) NOT NULL,
  PRIMARY KEY (order_id, warehouse_id, product_id),
  CONSTRAINT fk_stock_reservation_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_stock_reservation_inventory FOREIGN KEY (warehouse_id,product_id) REFERENCES warehouse_inventory(warehouse_id,product_id),
  CONSTRAINT chk_stock_reservation_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


ALTER TABLE customers ADD serving_warehouse_id BIGINT NULL, ADD CONSTRAINT fk_customer_serving_warehouse FOREIGN KEY (serving_warehouse_id) REFERENCES warehouses(id);

-- Backend Sprint 4

CREATE TABLE IF NOT EXISTS order_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(32) NOT NULL,
  to_status VARCHAR(32) NOT NULL,
  action VARCHAR(30) NOT NULL,
  opinion VARCHAR(500) NULL,
  actor_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_history_order FOREIGN KEY(order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_history_actor FOREIGN KEY(actor_id) REFERENCES users(id),
  KEY idx_order_history_order(order_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


ALTER TABLE orders MODIFY status VARCHAR(32) NOT NULL DEFAULT 'DRAFT', ADD approval_reason VARCHAR(1000) NULL, ADD warehouse_id BIGINT NULL, ADD CONSTRAINT fk_order_warehouse FOREIGN KEY(warehouse_id) REFERENCES warehouses(id);
ALTER TABLE order_lines ADD unit_price DECIMAL(15,2) NULL, ADD gross_amount DECIMAL(18,2) NULL, ADD discount_amount DECIMAL(18,2) NULL, ADD discount_note VARCHAR(500) NULL;

-- Backend Sprint 4

CREATE TABLE IF NOT EXISTS customer_accounts (
  customer_id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  CONSTRAINT fk_customer_account_customer FOREIGN KEY(customer_id) REFERENCES customers(id),
  CONSTRAINT fk_customer_account_user FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


ALTER TABLE orders ADD customer_submitted BOOLEAN NOT NULL DEFAULT FALSE;
