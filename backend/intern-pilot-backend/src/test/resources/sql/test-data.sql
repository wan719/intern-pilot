MERGE INTO role (id, role_code, role_name, description, enabled) KEY(role_code) VALUES
(1, 'USER', '普通用户', 'Default registered user', 1),
(2, 'ADMIN', '系统管理员', 'System administrator', 1);

MERGE INTO permission (id, permission_code, permission_name, resource_type, description, enabled) KEY(permission_code) VALUES
(1, 'resume:read', '查看简历', 'RESUME', 'Read own resumes', 1),
(2, 'resume:write', '编辑简历', 'RESUME', 'Upload and update own resumes', 1),
(3, 'resume:delete', '删除简历', 'RESUME', 'Delete own resumes', 1),
(4, 'job:read', '查看岗位', 'JOB', 'Read own job descriptions', 1),
(5, 'job:write', '编辑岗位', 'JOB', 'Create and update own job descriptions', 1),
(6, 'job:delete', '删除岗位', 'JOB', 'Delete own job descriptions', 1),
(7, 'analysis:read', '查看分析报告', 'ANALYSIS', 'Read own AI analysis reports', 1),
(8, 'analysis:write', '创建分析报告', 'ANALYSIS', 'Create AI analysis reports', 1),
(9, 'analysis:delete', '删除分析报告', 'ANALYSIS', 'Delete own AI analysis reports', 1),
(10, 'application:read', '查看投递记录', 'APPLICATION', 'Read own application records', 1),
(11, 'application:write', '编辑投递记录', 'APPLICATION', 'Create and update own application records', 1),
(12, 'application:delete', '删除投递记录', 'APPLICATION', 'Delete own application records', 1),
(13, 'user:read', '查看用户管理', 'ADMIN_USER', 'Read users', 1),
(14, 'user:update', '编辑用户管理', 'ADMIN_USER', 'Update users', 1),
(15, 'user:delete', '删除用户', 'ADMIN_USER', 'Delete users', 1),
(16, 'role:read', '查看角色管理', 'ADMIN_ROLE', 'Read roles', 1),
(17, 'role:create', '新增角色', 'ADMIN_ROLE', 'Create roles', 1),
(18, 'role:update', '编辑角色管理', 'ADMIN_ROLE', 'Update roles', 1),
(19, 'role:delete', '删除角色', 'ADMIN_ROLE', 'Delete roles', 1),
(20, 'permission:read', '查看权限管理', 'ADMIN_PERMISSION', 'Read permissions', 1),
(21, 'permission:create', '新增权限', 'ADMIN_PERMISSION', 'Create permissions', 1),
(22, 'permission:update', '编辑权限管理', 'ADMIN_PERMISSION', 'Update permissions', 1),
(23, 'permission:delete', '删除权限', 'ADMIN_PERMISSION', 'Delete permissions', 1),
(24, 'operation-log:read', '查看系统日志', 'SYSTEM_LOG', 'Read system logs', 1),
(25, 'operation-log:delete', '删除系统日志', 'SYSTEM_LOG', 'Delete system logs', 1),
(26, 'admin:dashboard', '查看管理看板', 'DASHBOARD', 'Read admin dashboard', 1),
(27, 'rag:read', '查看RAG知识库', 'RAG', '查看RAG岗位知识库文档和检索结果', 1),
(28, 'rag:manage', '管理RAG知识库', 'RAG', '创建、修改、重建、删除RAG知识库文档', 1);

MERGE INTO role_permission (role_id, permission_id) KEY(role_id, permission_id)
SELECT 1, id FROM permission WHERE permission_code IN (
    'resume:read', 'resume:write', 'resume:delete',
    'job:read', 'job:write', 'job:delete',
    'analysis:read', 'analysis:write', 'analysis:delete',
    'application:read', 'application:write', 'application:delete'
);

MERGE INTO role_permission (role_id, permission_id) KEY(role_id, permission_id)
SELECT 2, id FROM permission;

MERGE INTO `user` (id, username, password, role, enabled) KEY(id) VALUES
(1, 'wan', '$2a$10$dummyhashedpassword', 'USER', 1),
(2, 'admin', '$2a$10$dummyhashedpassword', 'ADMIN', 1);

MERGE INTO user_role (user_id, role_id) KEY(user_id, role_id) VALUES
(1, 1),
(2, 2);
