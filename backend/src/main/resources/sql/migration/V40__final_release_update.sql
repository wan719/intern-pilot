-- Final release migration for existing MySQL volumes.
-- Use this script only when the database was initialized before the feedback module existed.
-- New empty Docker volumes still use sql/init.sql automatically on first MySQL startup.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Feedback ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    type VARCHAR(32) NOT NULL COMMENT 'Feedback type',
    title VARCHAR(120) NOT NULL COMMENT 'Feedback title',
    content TEXT NOT NULL COMMENT 'Feedback content',
    page_url VARCHAR(255) DEFAULT NULL COMMENT 'Page URL',
    contact VARCHAR(100) DEFAULT NULL COMMENT 'Contact',
    allow_contact TINYINT DEFAULT 0 COMMENT 'Allow contact',
    browser_info VARCHAR(500) DEFAULT NULL COMMENT 'Browser info',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Process status',
    admin_reply TEXT DEFAULT NULL COMMENT 'Admin reply',
    handled_by BIGINT DEFAULT NULL COMMENT 'Handler user ID',
    handled_at DATETIME DEFAULT NULL COMMENT 'Handled time',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag',
    KEY idx_user_feedback_user_id (user_id),
    KEY idx_user_feedback_status (status),
    KEY idx_user_feedback_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User feedback';

INSERT INTO permission (permission_code, permission_name, resource_type, description, enabled, deleted)
VALUES
('feedback:read', 'Feedback read', 'FEEDBACK', 'Read user feedback', 1, 0),
('feedback:write', 'Feedback write', 'FEEDBACK', 'Update status and reply feedback', 1, 0),
('feedback:delete', 'Feedback delete', 'FEEDBACK', 'Delete feedback records', 1, 0)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    resource_type = VALUES(resource_type),
    description = VALUES(description),
    enabled = 1,
    deleted = 0;

INSERT INTO role_permission (role_id, permission_id, deleted)
SELECT r.id, p.id, 0
FROM role r
JOIN permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('feedback:read', 'feedback:write', 'feedback:delete')
ON DUPLICATE KEY UPDATE deleted = 0;
