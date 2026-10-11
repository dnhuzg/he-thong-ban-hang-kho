-- S7-08: nhắc hạn thanh toán tự động cho đại lý.
-- Mỗi hoá đơn được nhắc trước hạn 3 ngày và khi quá hạn; nhật ký bảo đảm không nhắc trùng trong cùng ngày.
USE oms;

CREATE TABLE IF NOT EXISTS invoices (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  code               VARCHAR(30)   NOT NULL,
  customer_id        BIGINT        NOT NULL,
  recipient_user_id  BIGINT        NOT NULL,
  total_amount       DECIMAL(15,2) NOT NULL,
  issued_at          DATETIME      NOT NULL,
  due_date           DATE          NOT NULL,
  status             ENUM('ISSUED','PAID','CANCELLED') NOT NULL DEFAULT 'ISSUED',
  paid_at            DATETIME      NULL,
  created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_invoices_code (code),
  KEY idx_invoices_reminder (status, due_date),
  KEY idx_invoices_customer (customer_id),
  KEY idx_invoices_recipient (recipient_user_id),
  CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
  CONSTRAINT fk_invoices_recipient FOREIGN KEY (recipient_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_reminder_logs (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  invoice_id      BIGINT       NOT NULL,
  reminder_date   DATE         NOT NULL,
  reminder_type   ENUM('DUE_SOON','OVERDUE') NOT NULL,
  email_sent      TINYINT(1)   NOT NULL DEFAULT 0,
  in_app_sent     TINYINT(1)   NOT NULL DEFAULT 0,
  email_sent_at   DATETIME     NULL,
  in_app_sent_at  DATETIME     NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_invoice_reminder_day (invoice_id, reminder_date),
  KEY idx_reminder_date (reminder_date),
  CONSTRAINT fk_reminder_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  recipient_user_id  BIGINT        NOT NULL,
  invoice_id         BIGINT        NOT NULL,
  reminder_date      DATE          NOT NULL,
  notification_type  ENUM('DUE_SOON','OVERDUE') NOT NULL,
  title              VARCHAR(200)  NOT NULL,
  message            VARCHAR(1000) NOT NULL,
  read_at            DATETIME      NULL,
  created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_invoice_day (recipient_user_id, invoice_id, reminder_date),
  KEY idx_notifications_user_read (recipient_user_id, read_at, created_at),
  CONSTRAINT fk_notifications_user FOREIGN KEY (recipient_user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_notifications_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;