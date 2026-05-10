-- Migration v3: Add notifications table
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '接收通知的用户',
  type VARCHAR(50) NOT NULL COMMENT 'LIKE/COMMENT/REPORT_HANDLED',
  title VARCHAR(200) NOT NULL,
  content TEXT,
  related_type VARCHAR(50) COMMENT 'POST/COMMENT/REPORT',
  related_id BIGINT,
  sender_id BIGINT COMMENT '触发通知的用户',
  is_read TINYINT(1) DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add action_taken column to reports
ALTER TABLE reports ADD COLUMN IF NOT EXISTS action_taken VARCHAR(50) COMMENT 'HIDE/DELETE/IGNORE';
