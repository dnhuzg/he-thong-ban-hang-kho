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
CREATE TABLE customers (
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
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_target (target_type, target_id),
  KEY idx_audit_created (created_at),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
