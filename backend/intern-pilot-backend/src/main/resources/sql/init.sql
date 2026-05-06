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
