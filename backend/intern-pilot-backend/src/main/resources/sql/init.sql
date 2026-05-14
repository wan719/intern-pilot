CREATE DATABASE IF NOT EXISTS intern_pilot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE intern_pilot;

SET NAMES utf8mb4;

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
('user:read', '查看用户管理', 'ADMIN_USER', 'Read users', 1),
('user:update', '编辑用户管理', 'ADMIN_USER', 'Update users', 1),
('user:delete', '删除用户', 'ADMIN_USER', 'Delete users', 1),
('role:read', '查看角色管理', 'ADMIN_ROLE', 'Read roles', 1),
('role:create', '新增角色', 'ADMIN_ROLE', 'Create roles', 1),
('role:update', '编辑角色管理', 'ADMIN_ROLE', 'Update roles', 1),
('role:delete', '删除角色', 'ADMIN_ROLE', 'Delete roles', 1),
('permission:read', '查看权限管理', 'ADMIN_PERMISSION', 'Read permissions', 1),
('permission:create', '新增权限', 'ADMIN_PERMISSION', 'Create permissions', 1),
('permission:update', '编辑权限管理', 'ADMIN_PERMISSION', 'Update permissions', 1),
('permission:delete', '删除权限', 'ADMIN_PERMISSION', 'Delete permissions', 1),
('operation-log:read', '查看系统日志', 'SYSTEM_LOG', 'Read system logs', 1),
('operation-log:delete', '删除系统日志', 'SYSTEM_LOG', 'Delete system logs', 1),
('admin:dashboard', '查看管理看板', 'DASHBOARD', 'Read admin dashboard', 1),
('rag:read', '查看RAG知识库', 'RAG', '查看RAG岗位知识库文档和检索结果', 1),
('rag:manage', '管理RAG知识库', 'RAG', '创建、修改、重建、删除RAG知识库文档', 1);
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

CREATE TABLE IF NOT EXISTS rag_knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识文档ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    direction VARCHAR(100) NOT NULL COMMENT '岗位方向，如Java后端',
    knowledge_type VARCHAR(50) NOT NULL COMMENT '知识类型',
    content LONGTEXT NOT NULL COMMENT '原始文档内容',
    summary VARCHAR(500) DEFAULT NULL COMMENT '文档摘要',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_rkd_direction (direction),
    KEY idx_rkd_knowledge_type (knowledge_type),
    KEY idx_rkd_enabled (enabled),
    KEY idx_rkd_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识文档表';

CREATE TABLE IF NOT EXISTS rag_knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识片段ID',
    document_id BIGINT NOT NULL COMMENT '知识文档ID',
    direction VARCHAR(100) NOT NULL COMMENT '岗位方向',
    knowledge_type VARCHAR(50) NOT NULL COMMENT '知识类型',
    chunk_index INT NOT NULL DEFAULT 0 COMMENT '片段序号',
    content TEXT NOT NULL COMMENT '片段内容',
    embedding LONGTEXT DEFAULT NULL COMMENT '向量JSON字符串',
    embedding_model VARCHAR(100) DEFAULT NULL COMMENT 'Embedding模型',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_rkc_document_id (document_id),
    KEY idx_rkc_direction (direction),
    KEY idx_rkc_knowledge_type (knowledge_type),
    KEY idx_rkc_enabled (enabled),
    KEY idx_rkc_chunk_index (chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识片段表';

INSERT INTO rag_knowledge_document (title, direction, knowledge_type, content, summary, enabled, created_by)
SELECT 'Java后端实习岗位能力模型',
       'Java后端',
       'SKILL_REQUIREMENT',
       'Java后端实习岗位通常关注 Java 基础、集合、多线程、JVM、Spring Boot、Spring Security、MyBatis、MySQL、Redis、RESTful API、接口设计、权限控制和项目工程化能力。简历中如果有登录鉴权、文件上传、缓存优化、操作日志、RBAC 权限、Swagger 文档、Docker 部署等项目经历，会更容易体现岗位匹配度。面试准备应重点复习 Spring IOC/AOP、事务传播、MyBatis 映射、索引优化、Redis 缓存穿透击穿雪崩、JWT 认证流程和接口幂等性。',
       'Java后端实习岗位技能要求、简历亮点和面试重点。',
       1,
       NULL
WHERE NOT EXISTS (
    SELECT 1 FROM rag_knowledge_document
    WHERE title = 'Java后端实习岗位能力模型'
      AND deleted = 0
);

INSERT INTO rag_knowledge_document (title, direction, knowledge_type, content, summary, enabled, created_by)
SELECT 'AI应用开发实习岗位知识',
       'AI应用',
       'JOB_DIRECTION',
       'AI应用开发实习岗位强调大模型 API 调用、Prompt Engineering、结构化输出解析、RAG、Embedding、向量检索、工具调用、前后端集成和业务场景落地能力。候选人如果能说明如何设计 Prompt、如何处理模型返回异常、如何做缓存与降级、如何将知识库检索结果拼接进上下文，会比只会调用接口更有竞争力。项目经历中可以突出 AI 简历分析、岗位推荐、面试题生成、知识库增强和可解释推荐理由。',
       'AI应用开发岗位方向、核心技能和项目包装建议。',
       1,
       NULL
WHERE NOT EXISTS (
    SELECT 1 FROM rag_knowledge_document
    WHERE title = 'AI应用开发实习岗位知识'
      AND deleted = 0
);

-- =========================
-- Demo users
-- Password: 123456
-- Only for local demo environment
-- =========================

INSERT IGNORE INTO user (
    username,
    password,
    email,
    real_name,
    school,
    major,
    grade,
    role,
    enabled,
    deleted
)
VALUES
(
    'admin',
    '$2y$10$ESsqmJNo1tZqYzKCxaSKve7VLx6xnF77vav.k/iLBQ0bCP9c5B2E2',
    'admin@internpilot.local',
    '系统管理员',
    'InternPilot',
    '软件工程',
    '管理员',
    'ADMIN',
    1,
    0
),
(
    'demo',
    '$2y$10$ESsqmJNo1tZqYzKCxaSKve7VLx6xnF77vav.k/iLBQ0bCP9c5B2E2',
    'demo@internpilot.local',
    '演示用户',
    '西南大学',
    '软件工程',
    '大二',
    'USER',
    1,
    0
);

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u
JOIN role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin'
  AND u.deleted = 0
  AND r.deleted = 0;

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u
JOIN role r ON r.role_code = 'USER'
WHERE u.username = 'demo'
  AND u.deleted = 0
  AND r.deleted = 0;

-- =========================
-- Demo resume
-- =========================

INSERT INTO resume (
    user_id,
    resume_name,
    original_file_name,
    stored_file_name,
    file_path,
    file_type,
    file_size,
    parsed_text,
    parse_status,
    is_default,
    deleted
)
SELECT
    u.id,
    'Java后端实习简历',
    'demo-java-resume.txt',
    'demo-java-resume.txt',
    '/demo/demo-java-resume.txt',
    'txt',
    1024,
    '姓名：演示用户
学校：西南大学
专业：软件工程
求职方向：Java后端开发实习

技术栈：
Java、Spring Boot、Spring Security、MyBatis、MySQL、Redis、Vue、Docker、Git。

项目经历：
InternPilot：面向大学生的 AI 实习投递与简历优化平台。
负责用户认证、JWT 鉴权、RBAC 权限系统、简历上传解析、岗位 JD 管理、AI 简历匹配分析、WebSocket 进度推送、RAG 岗位知识库、系统操作日志和 Docker 部署。

项目亮点：
1. 使用 Spring Security + JWT 实现前后端分离认证。
2. 使用 RBAC 模型实现用户、角色、权限管理。
3. 使用 Redis 缓存 AI 分析结果，提高接口响应速度。
4. 使用 WebSocket 实时展示 AI 分析进度。
5. 使用 Docker Compose 编排 MySQL、Redis、后端和前端服务。',
    'SUCCESS',
    1,
    0
FROM user u
WHERE u.username = 'demo'
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM resume r
      WHERE r.user_id = u.id
        AND r.resume_name = 'Java后端实习简历'
        AND r.deleted = 0
  );

INSERT INTO resume_version (
    user_id,
    resume_id,
    version_name,
    version_type,
    content,
    content_summary,
    is_current,
    deleted
)
SELECT
    r.user_id,
    r.id,
    '原始版本',
    'ORIGINAL',
    r.parsed_text,
    LEFT(REPLACE(REPLACE(r.parsed_text, '\r', ' '), '\n', ' '), 160),
    1,
    0
FROM resume r
JOIN user u ON u.id = r.user_id
WHERE u.username = 'demo'
  AND r.resume_name = 'Java后端实习简历'
  AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM resume_version rv
      WHERE rv.resume_id = r.id
        AND rv.version_name = '原始版本'
        AND rv.deleted = 0
  );

-- =========================
-- Demo jobs
-- =========================

INSERT INTO job_description (
    user_id,
    company_name,
    job_title,
    job_type,
    location,
    source_platform,
    jd_content,
    skill_requirements,
    salary_range,
    work_days_per_week,
    internship_duration,
    deleted
)
SELECT
    u.id,
    '星云科技',
    'Java后端开发实习生',
    'Java后端',
    '重庆',
    'BOSS直聘',
    '岗位职责：
1. 参与公司业务系统后端接口开发；
2. 参与用户认证、权限控制、数据管理等模块开发；
3. 配合前端完成接口联调；
4. 编写接口文档和单元测试。

任职要求：
1. 熟悉 Java 基础、集合、多线程；
2. 熟悉 Spring Boot、MyBatis、MySQL；
3. 了解 Redis、JWT、Spring Security；
4. 有完整后端项目经验优先；
5. 每周至少实习 3 天，实习 3 个月以上。',
    'Java,Spring Boot,MyBatis,MySQL,Redis,Spring Security,JWT,RESTful API',
    '200-400元/天',
    '3天/周',
    '3个月',
    0
FROM user u
WHERE u.username = 'demo'
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM job_description j
      WHERE j.user_id = u.id
        AND j.company_name = '星云科技'
        AND j.job_title = 'Java后端开发实习生'
        AND j.deleted = 0
  );

INSERT INTO job_description (
    user_id,
    company_name,
    job_title,
    job_type,
    location,
    source_platform,
    jd_content,
    skill_requirements,
    salary_range,
    work_days_per_week,
    internship_duration,
    deleted
)
SELECT
    u.id,
    '智启AI',
    'AI应用开发实习生',
    'AI应用',
    '远程',
    '牛客实习',
    '岗位职责：
1. 参与 AI 应用平台后端开发；
2. 负责大模型 API 调用、Prompt 构造和结构化结果解析；
3. 参与 RAG 知识库、Embedding 检索和问答功能开发；
4. 与前端配合完成 AI 功能页面联调。

任职要求：
1. 熟悉 Java 或 Python；
2. 熟悉 Spring Boot 基础开发；
3. 了解大模型 API、Prompt Engineering、RAG；
4. 有 AI 应用项目经验优先。',
    'Java,Spring Boot,大模型API,Prompt,RAG,Embedding,向量检索,Vue',
    '150-250元/天',
    '4天/周',
    '2个月',
    0
FROM user u
WHERE u.username = 'demo'
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM job_description j
      WHERE j.user_id = u.id
        AND j.company_name = '智启AI'
        AND j.job_title = 'AI应用开发实习生'
        AND j.deleted = 0
  );
