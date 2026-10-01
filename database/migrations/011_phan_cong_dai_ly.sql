-- S3-06: lịch sử phân công người phụ trách đại lý (EP-03). Mỗi lần đổi người phụ trách (sửa hồ sơ hoặc chuyển giao
-- hàng loạt khi nhân viên nghỉ) ghi một dòng: từ ai, sang ai, lý do, người thực hiện, thời điểm. Chỉ thêm, không sửa / xoá.
-- from_user_id / to_user_id null nghĩa là chưa có người phụ trách. Chạy lại vẫn an toàn.
USE oms;

CREATE TABLE IF NOT EXISTS customer_assignments (
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
