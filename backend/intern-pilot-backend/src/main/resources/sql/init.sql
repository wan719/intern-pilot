CREATE DATABASE IF NOT EXISTS intern_pilot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE intern_pilot;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    real_name VARCHAR(50) DEFAULT NULL,
    school VARCHAR(100) DEFAULT NULL,
    major VARCHAR(100) DEFAULT NULL,
    grade VARCHAR(30) DEFAULT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    enabled TINYINT NOT NULL DEFAULT 1,
    last_login_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_email (email),
    KEY idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_role_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) DEFAULT NULL,
    description VARCHAR(255) DEFAULT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_permission_resource_type (resource_type),
    KEY idx_permission_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_role_user_id (user_id),
    KEY idx_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_permission_role_id (role_id),
    KEY idx_role_permission_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO role (role_code, role_name, description, enabled)
VALUES
('USER', '普通用户', 'Default registered user', 1),
('ADMIN', '系统管理员', 'System administrator', 1);

INSERT IGNORE INTO permission (permission_code, permission_name, resource_type, description, enabled)
VALUES
('resume:read', '查看简历', 'RESUME', 'Read own resumes', 1),
('resume:write', '编辑简历', 'RESUME', 'Upload and update own resumes', 1),
('resume:delete', '删除简历', 'RESUME', 'Delete own resumes', 1),
('job:read', '查看岗位', 'JOB', 'Read own job descriptions', 1),
('job:write', '编辑岗位', 'JOB', 'Create and update own job descriptions', 1),
('job:delete', '删除岗位', 'JOB', 'Delete own job descriptions', 1),
('analysis:read', '查看分析报告', 'ANALYSIS', 'Read own AI analysis reports', 1),
('analysis:write', '创建分析报告', 'ANALYSIS', 'Create AI analysis reports', 1),
('analysis:delete', '删除分析报告', 'ANALYSIS', 'Delete own AI analysis reports', 1),
('application:read', '查看投递记录', 'APPLICATION', 'Read own application records', 1),
('application:write', '编辑投递记录', 'APPLICATION', 'Create and update own application records', 1),
('application:delete', '删除投递记录', 'APPLICATION', 'Delete own application records', 1),
('admin:user:read', '查看用户管理', 'ADMIN_USER', 'Read users', 1),
('admin:user:write', '编辑用户管理', 'ADMIN_USER', 'Update users', 1),
('admin:user:disable', '禁用用户', 'ADMIN_USER', 'Enable or disable users', 1),
('admin:role:read', '查看角色管理', 'ADMIN_ROLE', 'Read roles', 1),
('admin:role:write', '编辑角色管理', 'ADMIN_ROLE', 'Update roles', 1),
('admin:permission:read', '查看权限管理', 'ADMIN_PERMISSION', 'Read permissions', 1),
('admin:permission:write', '编辑权限管理', 'ADMIN_PERMISSION', 'Update permissions', 1),
('system:log:read', '查看系统日志', 'SYSTEM_LOG', 'Read system logs', 1),
('system:log:delete', '删除系统日志', 'SYSTEM_LOG', 'Delete system logs', 1),
('dashboard:admin:read', '查看管理看板', 'DASHBOARD', 'Read admin dashboard', 1);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p
WHERE r.role_code = 'USER'
  AND p.permission_code IN (
      'resume:read',
      'resume:write',
      'resume:delete',
      'job:read',
      'job:write',
      'job:delete',
      'analysis:read',
      'analysis:write',
      'analysis:delete',
      'application:read',
      'application:write',
      'application:delete'
  );

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p
WHERE r.role_code = 'ADMIN';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u
JOIN role r ON r.role_code = 'USER'
WHERE u.role = 'USER'
  AND u.deleted = 0;

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u
JOIN role r ON r.role_code = 'ADMIN'
WHERE u.role = 'ADMIN'
  AND u.deleted = 0;

CREATE TABLE IF NOT EXISTS resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resume_name VARCHAR(100) DEFAULT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    parsed_text LONGTEXT DEFAULT NULL,
    parse_status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    is_default TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_resume_user_id (user_id),
    KEY idx_resume_user_default (user_id, is_default),
    KEY idx_resume_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resume_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    version_name VARCHAR(200) NOT NULL,
    version_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    content LONGTEXT NOT NULL,
    content_summary VARCHAR(500) DEFAULT NULL,
    target_job_id BIGINT DEFAULT NULL,
    source_version_id BIGINT DEFAULT NULL,
    ai_report_id BIGINT DEFAULT NULL,
    optimize_prompt LONGTEXT DEFAULT NULL,
    ai_raw_response LONGTEXT DEFAULT NULL,
    is_current TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_rv_user_id (user_id),
    KEY idx_rv_resume_id (resume_id),
    KEY idx_rv_target_job_id (target_job_id),
    KEY idx_rv_source_version_id (source_version_id),
    KEY idx_rv_ai_report_id (ai_report_id),
    KEY idx_rv_version_type (version_type),
    KEY idx_rv_is_current (is_current),
    KEY idx_rv_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO resume_version (user_id, resume_id, version_name, version_type, content, content_summary, is_current)
SELECT r.user_id,
       r.id,
       '原始版本',
       'ORIGINAL',
       COALESCE(r.parsed_text, ''),
       LEFT(REPLACE(REPLACE(COALESCE(r.parsed_text, ''), '\r', ' '), '\n', ' '), 160),
       1
FROM resume r
WHERE r.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM resume_version rv
      WHERE rv.resume_id = r.id
        AND rv.user_id = r.user_id
        AND rv.deleted = 0
  );

CREATE TABLE IF NOT EXISTS job_description (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    job_type VARCHAR(50) DEFAULT NULL,
    location VARCHAR(100) DEFAULT NULL,
    source_platform VARCHAR(50) DEFAULT NULL,
    job_url VARCHAR(500) DEFAULT NULL,
    jd_content LONGTEXT NOT NULL,
    skill_requirements TEXT DEFAULT NULL,
    salary_range VARCHAR(50) DEFAULT NULL,
    work_days_per_week VARCHAR(30) DEFAULT NULL,
    internship_duration VARCHAR(50) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_job_user_id (user_id),
    KEY idx_job_company (company_name),
    KEY idx_job_title (job_title),
    KEY idx_job_type (job_type),
    KEY idx_job_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS analysis_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    resume_version_id BIGINT DEFAULT NULL,
    job_id BIGINT NOT NULL,
    match_score INT DEFAULT NULL,
    match_level VARCHAR(30) DEFAULT NULL,
    strengths TEXT DEFAULT NULL,
    weaknesses TEXT DEFAULT NULL,
    missing_skills TEXT DEFAULT NULL,
    suggestions TEXT DEFAULT NULL,
    interview_tips TEXT DEFAULT NULL,
    raw_ai_response LONGTEXT DEFAULT NULL,
    ai_provider VARCHAR(50) DEFAULT NULL,
    ai_model VARCHAR(100) DEFAULT NULL,
    cache_hit TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_report_user_id (user_id),
    KEY idx_report_resume_id (resume_id),
    KEY idx_report_resume_version_id (resume_version_id),
    KEY idx_report_job_id (job_id),
    KEY idx_report_user_resume_job (user_id, resume_id, job_id),
    KEY idx_report_score (match_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS analysis_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    resume_version_id BIGINT DEFAULT NULL,
    job_id BIGINT NOT NULL,
    report_id BIGINT DEFAULT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    progress INT NOT NULL DEFAULT 0,
    message VARCHAR(500) DEFAULT NULL,
    force_refresh TINYINT NOT NULL DEFAULT 0,
    error_message TEXT DEFAULT NULL,
    started_at DATETIME DEFAULT NULL,
    finished_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_analysis_task_no (task_no),
    KEY idx_analysis_task_user_id (user_id),
    KEY idx_analysis_task_resume_id (resume_id),
    KEY idx_analysis_task_resume_version_id (resume_version_id),
    KEY idx_analysis_task_job_id (job_id),
    KEY idx_analysis_task_status (status),
    KEY idx_analysis_task_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_question_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    resume_version_id BIGINT DEFAULT NULL,
    job_id BIGINT NOT NULL,
    analysis_report_id BIGINT DEFAULT NULL,
    title VARCHAR(200) DEFAULT NULL,
    question_count INT NOT NULL DEFAULT 0,
    ai_provider VARCHAR(50) DEFAULT NULL,
    ai_model VARCHAR(100) DEFAULT NULL,
    raw_ai_response LONGTEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_iqr_user_id (user_id),
    KEY idx_iqr_resume_id (resume_id),
    KEY idx_iqr_resume_version_id (resume_version_id),
    KEY idx_iqr_job_id (job_id),
    KEY idx_iqr_analysis_report_id (analysis_report_id),
    KEY idx_iqr_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    difficulty VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    question TEXT NOT NULL,
    answer TEXT DEFAULT NULL,
    answer_points TEXT DEFAULT NULL,
    related_skills VARCHAR(500) DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_iq_report_id (report_id),
    KEY idx_iq_user_id (user_id),
    KEY idx_iq_question_type (question_type),
    KEY idx_iq_difficulty (difficulty),
    KEY idx_iq_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS application_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    resume_id BIGINT DEFAULT NULL,
    report_id BIGINT DEFAULT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'TO_APPLY',
    apply_date DATE DEFAULT NULL,
    interview_date DATETIME DEFAULT NULL,
    note TEXT DEFAULT NULL,
    review TEXT DEFAULT NULL,
    priority VARCHAR(30) DEFAULT 'MEDIUM',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_app_user_id (user_id),
    KEY idx_app_job_id (job_id),
    KEY idx_app_resume_id (resume_id),
    KEY idx_app_status (status),
    KEY idx_app_apply_date (apply_date),
    KEY idx_app_user_job (user_id, job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT DEFAULT NULL,
    operator_username VARCHAR(100) DEFAULT NULL,
    module VARCHAR(100) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    request_uri VARCHAR(255) DEFAULT NULL,
    request_method VARCHAR(20) DEFAULT NULL,
    request_params TEXT DEFAULT NULL,
    ip_address VARCHAR(100) DEFAULT NULL,
    user_agent VARCHAR(500) DEFAULT NULL,
    success TINYINT NOT NULL DEFAULT 1,
    error_message TEXT DEFAULT NULL,
    cost_time BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_sol_operator_id (operator_id),
    KEY idx_sol_operator_username (operator_username),
    KEY idx_sol_module (module),
    KEY idx_sol_operation_type (operation_type),
    KEY idx_sol_success (success),
    KEY idx_sol_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_recommendation_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐批次ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_id BIGINT NOT NULL COMMENT '简历ID',
    resume_version_id BIGINT DEFAULT NULL COMMENT '简历版本ID',
    title VARCHAR(200) NOT NULL COMMENT '推荐批次标题',
    job_count INT NOT NULL DEFAULT 0 COMMENT '参与推荐的岗位数量',
    recommended_count INT NOT NULL DEFAULT 0 COMMENT '推荐结果数量',
    strategy VARCHAR(100) NOT NULL DEFAULT 'RULE_BASED' COMMENT '推荐策略',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_jrb_user_id (user_id),
    KEY idx_jrb_resume_id (resume_id),
    KEY idx_jrb_resume_version_id (resume_version_id),
    KEY idx_jrb_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位推荐批次表';

CREATE TABLE IF NOT EXISTS job_recommendation_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐项ID',
    batch_id BIGINT NOT NULL COMMENT '推荐批次ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    job_id BIGINT NOT NULL COMMENT '岗位ID',
    analysis_report_id BIGINT DEFAULT NULL COMMENT '关联AI分析报告ID',
    recommendation_score INT NOT NULL DEFAULT 0 COMMENT '推荐分数',
    recommendation_level VARCHAR(50) NOT NULL COMMENT '推荐等级',
    skill_match_score INT DEFAULT NULL COMMENT '技能匹配分',
    ai_match_score INT DEFAULT NULL COMMENT 'AI匹配分',
    job_type_score INT DEFAULT NULL COMMENT '岗位类型匹配分',
    matched_skills TEXT DEFAULT NULL COMMENT '匹配技能，JSON数组字符串',
    missing_skills TEXT DEFAULT NULL COMMENT '缺失技能，JSON数组字符串',
    reasons TEXT DEFAULT NULL COMMENT '推荐理由，JSON数组字符串',
    is_applied TINYINT NOT NULL DEFAULT 0 COMMENT '是否已投递：0否，1是',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_jri_batch_id (batch_id),
    KEY idx_jri_user_id (user_id),
    KEY idx_jri_job_id (job_id),
    KEY idx_jri_score (recommendation_score),
    KEY idx_jri_level (recommendation_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位推荐结果表';
