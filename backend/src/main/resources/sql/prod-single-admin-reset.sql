-- Use only for the personal production demo environment after taking a full database backup.
-- Goal: remove old public demo/admin accounts, keep RBAC data, and ensure one administrator.

SET NAMES utf8mb4;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE user ADD COLUMN avatar_url VARCHAR(255) DEFAULT NULL AFTER real_name',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'avatar_url'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE user ADD COLUMN preferred_job_title VARCHAR(100) DEFAULT NULL AFTER grade',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'preferred_job_title'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE user ADD COLUMN preferred_city VARCHAR(100) DEFAULT NULL AFTER preferred_job_title',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'preferred_city'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE user ADD COLUMN expected_salary VARCHAR(100) DEFAULT NULL AFTER preferred_city',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'expected_salary'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE user ADD COLUMN employment_type VARCHAR(50) DEFAULT NULL AFTER expected_salary',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'employment_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TEMPORARY TABLE tmp_removed_users AS
SELECT id
FROM user
WHERE deleted = 0
  AND email <> '3425446714@qq.com'
  AND (
      username IN ('admin', 'demo')
      OR email IN ('admin@internpilot.local', 'demo@internpilot.local')
      OR phone IN ('13800000000', '13900000000')
  );

UPDATE interview_question iq
JOIN interview_question_report iqr ON iq.report_id = iqr.id
JOIN tmp_removed_users ru ON iqr.user_id = ru.id
SET iq.deleted = 1;

UPDATE interview_question_report iqr
JOIN tmp_removed_users ru ON iqr.user_id = ru.id
SET iqr.deleted = 1;

UPDATE job_recommendation_item item
JOIN job_recommendation_batch batch ON item.batch_id = batch.id
JOIN tmp_removed_users ru ON batch.user_id = ru.id
SET item.deleted = 1;

UPDATE job_recommendation_batch batch
JOIN tmp_removed_users ru ON batch.user_id = ru.id
SET batch.deleted = 1;

UPDATE analysis_report ar
JOIN tmp_removed_users ru ON ar.user_id = ru.id
SET ar.deleted = 1;

UPDATE analysis_task task
JOIN tmp_removed_users ru ON task.user_id = ru.id
SET task.deleted = 1;

UPDATE application_record app
JOIN tmp_removed_users ru ON app.user_id = ru.id
SET app.deleted = 1;

UPDATE resume_version rv
JOIN tmp_removed_users ru ON rv.user_id = ru.id
SET rv.deleted = 1;

UPDATE resume r
JOIN tmp_removed_users ru ON r.user_id = ru.id
SET r.deleted = 1;

UPDATE job_description jd
JOIN tmp_removed_users ru ON jd.user_id = ru.id
SET jd.deleted = 1;

UPDATE user_role ur
JOIN tmp_removed_users ru ON ur.user_id = ru.id
SET ur.deleted = 1;

UPDATE user u
JOIN tmp_removed_users ru ON u.id = ru.id
SET u.deleted = 1,
    u.enabled = 0,
    u.email = CONCAT('deleted_', u.id, '_', COALESCE(u.email, 'no_email')),
    u.phone = NULL,
    u.username = CONCAT('deleted_', u.id, '_', u.username);

INSERT INTO user (
    username,
    password,
    email,
    phone,
    real_name,
    school,
    major,
    grade,
    role,
    account_type,
    phone_verified,
    email_verified,
    enabled,
    deleted
)
VALUES (
    'admin',
    '$2a$10$T6XhB6hsozua.k9fJCwQSeqUEzsPLDUbFh23TDoareYatLp09S286',
    '3425446714@qq.com',
    NULL,
    '系统管理员',
    NULL,
    NULL,
    NULL,
    'ADMIN',
    'SYSTEM',
    0,
    1,
    1,
    0
)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    role = VALUES(role),
    account_type = VALUES(account_type),
    phone_verified = VALUES(phone_verified),
    email_verified = VALUES(email_verified),
    enabled = 1,
    deleted = 0;

INSERT INTO user_role (user_id, role_id, deleted)
SELECT u.id, r.id, 0
FROM user u
JOIN role r ON r.role_code = 'ADMIN'
WHERE u.email = '3425446714@qq.com'
  AND u.deleted = 0
  AND r.deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

DROP TEMPORARY TABLE tmp_removed_users;
