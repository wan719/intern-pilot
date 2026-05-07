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
    KEY idx_report_job_id (job_id),
    KEY idx_report_user_resume_job (user_id, resume_id, job_id),
    KEY idx_report_score (match_score)
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
