-- GHTK-72 / S7-06: chụp cố định dòng giao dịch thuộc biên bản tại thời điểm lập.
USE oms;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema='oms' AND table_name='debt_reconciliations'
                 AND column_name='details_snapshotted_at')=0,
  'ALTER TABLE debt_reconciliations ADD COLUMN details_snapshotted_at DATETIME NULL AFTER created_at',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS debt_reconciliation_lines (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reconciliation_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  entry_date DATE NOT NULL,
  reference VARCHAR(50) NOT NULL,
  entry_type VARCHAR(80) NOT NULL,
  note VARCHAR(500) NULL,
  increase_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  decrease_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reconciliation_line (reconciliation_id, line_no),
  CONSTRAINT fk_reconciliation_lines_document FOREIGN KEY (reconciliation_id)
    REFERENCES debt_reconciliations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
