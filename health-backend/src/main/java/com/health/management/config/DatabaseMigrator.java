package com.health.management.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrator implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DatabaseMigrator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Create notifications table
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT NOT NULL,
                  type VARCHAR(50) NOT NULL,
                  title VARCHAR(200) NOT NULL,
                  content TEXT,
                  related_type VARCHAR(50),
                  related_id BIGINT,
                  sender_id BIGINT,
                  is_read TINYINT(1) DEFAULT 0,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        // Add action_taken column to reports if not exists
        try {
            jdbc.execute("ALTER TABLE reports ADD COLUMN action_taken VARCHAR(50) NULL");
        } catch (Exception ignored) {
            // column already exists
        }
    }
}
