CREATE DATABASE IF NOT EXISTS intern_pilot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE intern_pilot;

SET NAMES utf8mb4;

-- Local development RBAC seed data.
-- Password for both accounts: 123456
-- Run this after the base schema/init script has created the RBAC tables.
-- This script is intended for local MySQL only, not for production.

INSERT IGNORE INTO role (role_code, role_name, description, enabled, deleted)
VALUES
('USER', 'User', 'Default registered user', 1, 0),
('ADMIN', 'Administrator', 'System administrator', 1, 0);

INSERT IGNORE INTO permission (permission_code, permission_name, resource_type, description, enabled, deleted)
VALUES
('resume:read', 'Resume read', 'RESUME', 'Read own resumes', 1, 0),
('resume:write', 'Resume write', 'RESUME', 'Upload and update own resumes', 1, 0),
('resume:delete', 'Resume delete', 'RESUME', 'Delete own resumes', 1, 0),
('job:read', 'Job read', 'JOB', 'Read own job descriptions', 1, 0),
('job:write', 'Job write', 'JOB', 'Create and update own job descriptions', 1, 0),
('job:delete', 'Job delete', 'JOB', 'Delete own job descriptions', 1, 0),
('analysis:read', 'Analysis read', 'ANALYSIS', 'Read own AI analysis reports', 1, 0),
('analysis:write', 'Analysis write', 'ANALYSIS', 'Create AI analysis reports', 1, 0),
('analysis:delete', 'Analysis delete', 'ANALYSIS', 'Delete own AI analysis reports', 1, 0),
('application:read', 'Application read', 'APPLICATION', 'Read own application records', 1, 0),
('application:write', 'Application write', 'APPLICATION', 'Create and update own application records', 1, 0),
('application:delete', 'Application delete', 'APPLICATION', 'Delete own application records', 1, 0),
('user:read', 'Admin user read', 'ADMIN_USER', 'Read users', 1, 0),
('user:update', 'Admin user update', 'ADMIN_USER', 'Update users and roles', 1, 0),
('user:delete', 'Admin user delete', 'ADMIN_USER', 'Delete users', 1, 0),
('role:read', 'Admin role read', 'ADMIN_ROLE', 'Read roles', 1, 0),
('role:create', 'Admin role create', 'ADMIN_ROLE', 'Create roles', 1, 0),
('role:update', 'Admin role update', 'ADMIN_ROLE', 'Update role permissions', 1, 0),
('role:delete', 'Admin role delete', 'ADMIN_ROLE', 'Delete roles', 1, 0),
('permission:read', 'Admin permission read', 'ADMIN_PERMISSION', 'Read permissions', 1, 0),
('permission:create', 'Admin permission create', 'ADMIN_PERMISSION', 'Create permissions', 1, 0),
('permission:update', 'Admin permission update', 'ADMIN_PERMISSION', 'Update permissions', 1, 0),
('permission:delete', 'Admin permission delete', 'ADMIN_PERMISSION', 'Delete permissions', 1, 0),
('operation-log:read', 'Operation log read', 'SYSTEM_LOG', 'Read system logs', 1, 0),
('operation-log:delete', 'Operation log delete', 'SYSTEM_LOG', 'Delete system logs', 1, 0),
('admin:dashboard', 'Admin dashboard read', 'DASHBOARD', 'Read admin dashboard', 1, 0),
('rag:read', 'RAG knowledge read', 'RAG', 'Read RAG knowledge documents', 1, 0),
('rag:manage', 'RAG knowledge manage', 'RAG', 'Create, update, rebuild, and delete RAG knowledge documents', 1, 0);

INSERT INTO role_permission (role_id, permission_id, deleted)
SELECT r.id, p.id, 0
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
  )
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO role_permission (role_id, permission_id, deleted)
SELECT r.id, p.id, 0
FROM role r
JOIN permission p
WHERE r.role_code = 'ADMIN'
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO user (username, password, email, real_name, school, major, grade, role, enabled, deleted)
VALUES
(
    'admin',
    '$2a$10$vT/TFZxrkbTqFSyWqheFM.mKDMBH0MH78znE4y72rW65jiuBB0j0u',
    'admin@internpilot.local',
    'System Administrator',
    'InternPilot',
    'Software Engineering',
    'Admin',
    'ADMIN',
    1,
    0
),
(
    'demo',
    '$2a$10$vT/TFZxrkbTqFSyWqheFM.mKDMBH0MH78znE4y72rW65jiuBB0j0u',
    'demo@internpilot.local',
    'Demo User',
    'Southwest University',
    'Software Engineering',
    'Sophomore',
    'USER',
    1,
    0
)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    email = VALUES(email),
    real_name = VALUES(real_name),
    school = VALUES(school),
    major = VALUES(major),
    grade = VALUES(grade),
    role = VALUES(role),
    enabled = 1,
    deleted = 0;

INSERT INTO user_role (user_id, role_id, deleted)
SELECT u.id, r.id, 0
FROM user u
JOIN role r ON r.role_code = u.role
WHERE u.username IN ('admin', 'demo')
  AND u.deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;
