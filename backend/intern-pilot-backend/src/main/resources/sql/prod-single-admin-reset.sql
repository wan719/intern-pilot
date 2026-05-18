-- Use only for the personal production demo environment after taking a full database backup.
-- Goal: remove old public demo/admin accounts, keep RBAC data, and ensure one administrator.

SET NAMES utf8mb4;

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
    '$2a$10$ZV3jkByDqvMmusH8GcxwPOeV2XxMmNPwW7mOkhCgdnfMHw6Osnhge',
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
