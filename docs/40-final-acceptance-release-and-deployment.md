下面这份可以直接保存为：
# InternPilot 最终验收、发布与部署设计文档

## 1. 文档背景

当前 InternPilot 已完成主要功能开发：

* RBAC 权限系统
* WebSocket AI 进度展示
* DeepSeek API 接入
* AI 简历匹配分析
* AI 面试题生成
* 岗位推荐
* RAG 岗位知识库
* 管理员后台
* 邮箱验证码注册登录
* QQ 邮箱验证码
* 单管理员账号策略
* 用户中心
* 前端界面优化
* 全局 AI 任务中心
* AI 长任务右下角通知
* 用户反馈功能
* Docker 服务器部署
* 在线系统访问

当前阶段不再建议继续新增大功能，而是进入最终收尾阶段。

本阶段目标是：

```text
本地验收
  ↓
Codex 全项目复查
  ↓
合并 dev
  ↓
服务器部署
  ↓
线上验收
  ↓
README 收尾
  ↓
合并 main
  ↓
打版本标签
  ↓
同步 Gitee
  ↓
准备答辩/提交材料
```

---

## 2. 当前项目状态

### 2.1 技术栈

```text
后端：Spring Boot + Spring Security + MyBatis + MySQL + Redis
前端：Vue 3 + Vite + Pinia + Vue Router + Element Plus
AI：DeepSeek API
部署：Docker + Docker Compose + Nginx
服务器：43.136.182.179
部署目录：~/intern-pilot
部署文件：deploy/docker-compose.yml
```

---

### 2.2 当前分支状态

当前主要开发分支：

```text
dev
```

已完成或准备合并的功能分支：

```text
feature/auth-phone-email-captcha
feature/frontend-ui-polish
feature/ai-task-center-feedback
```

后续发布主分支：

```text
main
```

---

### 2.3 线上策略

线上环境要求：

```text
1. 使用 DeepSeek 真接口
2. 不使用 Mock 作为演示
3. Mock 只保留测试用途
4. 只保留 3425446714@qq.com 管理员
5. 删除 demo / old admin 演示账号
6. 不在 README 中公开管理员密码
7. 不在 Git 中提交真实 DEEPSEEK_API_KEY
8. 不在 Git 中提交 QQ 邮箱授权码
```

---

## 3. 本阶段目标

本阶段需要完成：

```text
1. 确认 feature/ai-task-center-feedback 已经完成并通过测试
2. 合并所有功能到 dev
3. 在 dev 分支完成本地完整验收
4. 使用 Codex 做最终全项目复查
5. 部署 dev 到服务器
6. 执行线上数据库迁移
7. 完成线上核心功能验收
8. 更新 README、截图、演示说明
9. 合并 dev 到 main
10. 打正式版本 tag
11. 同步 GitHub / Gitee
12. 整理答辩提交材料
```

---

## 4. 本阶段不做什么

为了保证发布稳定，本阶段不再做：

```text
1. 不新增新的大型业务模块
2. 不重构核心认证体系
3. 不更换技术栈
4. 不更换服务器部署方式
5. 不把所有 AI 功能重新设计成完整任务调度系统
6. 不加入复杂 CI/CD 流程
7. 不公开演示账号密码
8. 不清空线上数据库，除非确认已经备份
```

---

## 5. 本地最终验收流程

### 5.1 切换到 dev 分支

```bash
git checkout dev
git pull origin dev
```

确认当前分支：

```bash
git branch
```

确认工作区干净：

```bash
git status
```

期望结果：

```text
nothing to commit, working tree clean
```

---

### 5.2 后端测试

进入后端目录：

```bash
cd backend/intern-pilot-backend
```

执行测试：

```bash
./gradlew test --no-daemon --max-workers=1
```

Windows PowerShell：

```powershell
.\gradlew.bat test --no-daemon --max-workers=1
```

验收标准：

```text
BUILD SUCCESSFUL
```

如果测试失败，优先检查：

```text
1. Mock / DeepSeek 测试环境是否混用
2. Redis / MySQL 测试配置是否正确
3. 新增 user_feedback 表相关 SQL 是否同步
4. RBAC 权限测试是否缺少 feedback:* 权限
5. AnalysisTaskStatusEnum 新增 CANCELLED 后旧测试是否需要更新
```

---

### 5.3 前端构建

进入前端目录：

```bash
cd frontend/intern-pilot-frontend
```

安装依赖：

```bash
npm install
```

构建：

```bash
npm run build
```

验收标准：

```text
构建成功，无 TypeScript 错误，无 Vite 构建失败
```

重点排查：

```text
1. aiTaskCenter Store 类型错误
2. Feedback 组件导入路径错误
3. Element Plus 组件未引入
4. 路由路径不存在
5. 管理员反馈页面菜单权限错误
6. resultPath 跳转路径错误
```

---

## 6. 本地手动验收清单

### 6.1 登录注册验收

必须测试：

```text
1. 邮箱验证码可以发送
2. QQ 邮箱验证码发送成功
3. 验证码过期后不能注册
4. 错误验证码不能注册
5. 新用户可以注册
6. 新用户可以登录
7. 3425446714@qq.com 管理员可以登录
8. demo 用户不能登录
9. old admin 用户不能登录
10. 退出登录正常
```

---

### 6.2 用户中心验收

必须测试：

```text
1. 可以查看当前用户信息
2. 可以修改昵称
3. 可以修改密码
4. 修改密码后旧密码不能登录
5. 新密码可以登录
6. 普通用户不能看到管理员功能
7. 管理员仍然可以访问管理员后台
```

---

### 6.3 AI 简历分析验收

必须测试：

```text
1. 上传简历正常
2. 选择岗位正常
3. 点击 AI 分析后任务开始
4. WebSocket 进度正常显示
5. 切换页面后任务中心仍显示分析中
6. 切回分析页面后进度仍存在
7. 分析完成后右下角有通知
8. 点击查看结果可以进入报告页
9. 分析失败时任务中心显示失败状态
10. 停止任务后状态变为 CANCELLED
11. CANCELLED 后不会再次变成 COMPLETED
```

---

### 6.4 岗位推荐验收

必须测试：

```text
1. 点击生成岗位推荐后任务中心出现任务
2. 页面切换后任务不丢失
3. 生成完成后右下角提示
4. 点击查看结果可以进入推荐结果页
5. 失败时显示失败状态
```

---

### 6.5 面试题生成验收

必须测试：

```text
1. 点击生成面试题后任务中心出现任务
2. 页面切换后任务不丢失
3. 生成完成后右下角提示
4. 点击查看结果可以进入面试题详情页
5. 重新生成面试题正常
6. 失败时显示失败状态
```

---

### 6.6 RAG 验收

必须测试：

```text
1. 管理员可以上传知识文档
2. 知识库列表正常
3. RAG 问答正常
4. 普通用户不能访问管理员 RAG 管理接口
5. DeepSeek 问答结果正常
```

---

### 6.7 用户反馈验收

普通用户：

```text
1. 登录后可以看到反馈入口
2. 可以打开反馈弹窗 / 抽屉
3. 不填标题不能提交
4. 不填内容不能提交
5. 可以选择反馈类型
6. 提交后提示成功
7. 我的反馈列表能看到自己提交的反馈
8. 不能看到其他人的反馈
```

管理员：

```text
1. 管理员后台可以看到用户反馈菜单
2. 可以查看反馈列表
3. 可以按类型筛选
4. 可以按状态筛选
5. 可以查看反馈详情
6. 可以修改处理状态
7. 可以填写管理员回复
8. 可以删除无效反馈
9. 普通用户访问管理员反馈接口应返回 403
```

---

## 7. Codex 最终复查要求

在合并 main 和部署前，必须让 Codex 做一次最终复查。

重点检查：

```text
1. 后端测试是否通过
2. 前端构建是否通过
3. 是否存在未提交文件
4. 是否误提交 .env、API Key、邮箱授权码
5. 是否存在 demo / old admin 账号残留
6. 是否存在 Mock 作为线上默认配置
7. 是否存在数据库迁移遗漏
8. 是否存在权限遗漏
9. 是否存在普通用户越权访问
10. 是否存在 WebSocket 重复连接或内存泄漏
11. 是否存在 AI 任务取消后状态被覆盖
12. 是否存在 README 信息过期
```

---

## 8. 服务器部署前准备

服务器信息：

```text
服务器 IP：43.136.182.179
部署目录：~/intern-pilot
部署方式：Docker Compose
Compose 文件：deploy/docker-compose.yml
```

登录服务器：

```bash
ssh ubuntu@43.136.182.179
```

进入项目目录：

```bash
cd ~/intern-pilot
```

查看当前分支：

```bash
git branch
```

切换到 dev：

```bash
git checkout dev
git pull origin dev
```

---

## 9. 服务器本地配置保护

服务器上可能有本地配置文件，例如：

```text
frontend/intern-pilot-frontend/nginx.conf
backend/intern-pilot-backend/gradle/wrapper/gradle-wrapper.properties
.env
deploy/.env
```

部署前检查：

```bash
git status
```

如果有服务器本地配置改动，不要直接覆盖。

可以先 stash：

```bash
git stash push -m "server local config before final deployment" -- \
  frontend/intern-pilot-frontend/nginx.conf \
  backend/intern-pilot-backend/gradle/wrapper/gradle-wrapper.properties
```

然后再 pull：

```bash
git pull origin dev
```

必要时恢复：

```bash
git stash list
git stash apply stash@{0}
```

---

## 10. 线上数据库迁移注意事项

### 10.1 重要提醒

Docker MySQL 使用 volume 后：

```text
init.sql 只会在 MySQL volume 第一次初始化时执行。
```

因此，后续新增表、新增字段、新增权限，不能只改 `init.sql`。

必须手动执行迁移 SQL。

---

### 10.2 本阶段需要确认的数据库内容

需要确认线上数据库已有：

```text
1. user_feedback 表
2. feedback:read 权限
3. feedback:write 权限
4. feedback:delete 权限
5. 管理员角色绑定 feedback:* 权限
6. AnalysisTask 支持 CANCELLED 状态
7. 只保留 3425446714@qq.com 管理员
8. demo / old admin 账号已删除或禁用
```

---

### 10.3 建议新增迁移脚本

建议新增：

```text
backend/intern-pilot-backend/src/main/resources/sql/migration/V40__final_release_update.sql
```

内容包括：

```sql
-- 1. 用户反馈表
CREATE TABLE IF NOT EXISTS user_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '反馈ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(32) NOT NULL COMMENT '反馈类型',
    title VARCHAR(120) NOT NULL COMMENT '反馈标题',
    content TEXT NOT NULL COMMENT '反馈内容',
    page_url VARCHAR(255) DEFAULT NULL COMMENT '反馈发生页面',
    contact VARCHAR(100) DEFAULT NULL COMMENT '联系方式',
    allow_contact TINYINT DEFAULT 0 COMMENT '是否允许联系',
    browser_info VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    admin_reply TEXT DEFAULT NULL COMMENT '管理员回复',
    handled_by BIGINT DEFAULT NULL COMMENT '处理人ID',
    handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
) COMMENT='用户反馈表';

-- 2. 新增反馈权限
-- 具体 SQL 需要根据当前 permission 表结构调整。
-- 示例：
-- INSERT IGNORE INTO permission(code, name, description, created_at, updated_at)
-- VALUES
-- ('feedback:read', '反馈查看', '查看用户反馈', NOW(), NOW()),
-- ('feedback:write', '反馈处理', '处理和回复用户反馈', NOW(), NOW()),
-- ('feedback:delete', '反馈删除', '删除用户反馈', NOW(), NOW());

-- 3. 管理员角色绑定反馈权限
-- 具体 SQL 需要根据 role_permission 表结构调整。
```

注意：

```text
不要直接复制示例 SQL 到线上执行。
必须根据你项目真实表结构调整字段名。
```

---

### 10.4 线上数据库备份

部署前建议备份数据库。

进入服务器：

```bash
cd ~/intern-pilot
```

查看 MySQL 容器名：

```bash
docker compose -f deploy/docker-compose.yml ps
```

备份示例：

```bash
docker exec internpilot-mysql mysqldump -uroot -p intern_pilot > ~/intern_pilot_backup_before_v1_1_0.sql
```

如果数据库用户名、库名不同，以 `deploy/docker-compose.yml` 为准。

---

## 11. 服务器部署流程

进入项目目录：

```bash
cd ~/intern-pilot
```

拉取最新 dev：

```bash
git checkout dev
git pull origin dev
```

停止旧服务：

```bash
docker compose -f deploy/docker-compose.yml down
```

重新构建并启动：

```bash
docker compose -f deploy/docker-compose.yml up -d --build
```

查看服务状态：

```bash
docker compose -f deploy/docker-compose.yml ps
```

查看后端日志：

```bash
docker logs -f internpilot-backend
```

查看前端容器日志：

```bash
docker logs -f internpilot-frontend
```

查看 MySQL：

```bash
docker logs -f internpilot-mysql
```

查看 Redis：

```bash
docker logs -f internpilot-redis
```

---

## 12. 线上环境变量检查

必须确认：

```text
DEEPSEEK_API_KEY 已配置
AI_PROVIDER 不是 mock
QQ 邮箱 SMTP 授权码已配置
MySQL 连接正常
Redis 连接正常
JWT_SECRET 已配置
前端 API 地址正确
Nginx /api 反向代理正常
WebSocket 代理正常
```

重点检查：

```bash
docker exec internpilot-backend env | grep AI
docker exec internpilot-backend env | grep DEEPSEEK
docker exec internpilot-backend env | grep MAIL
```

注意：

```text
不要把这些输出截图发到公开 README。
不要把真实 key 提交到 Git。
```

---

## 13. 线上验收清单

部署完成后，浏览器访问线上系统。

### 13.1 基础访问

```text
1. 首页可以打开
2. 刷新页面不会 404
3. /api 接口正常
4. 静态资源正常加载
5. 控制台无明显红色报错
```

---

### 13.2 登录注册

```text
1. 邮箱验证码发送成功
2. 新用户注册成功
3. 新用户登录成功
4. 管理员登录成功
5. demo / old admin 登录失败
```

---

### 13.3 AI 功能

```text
1. AI 分析调用 DeepSeek 真接口
2. WebSocket 进度正常
3. 任务中心显示正常
4. 切换页面后任务不丢
5. AI 完成后右下角提示
6. 岗位推荐正常
7. 面试题生成正常
8. RAG 问答正常
```

---

### 13.4 权限

```text
1. 普通用户不能进入管理员后台
2. 普通用户不能访问 /api/admin/**
3. 管理员可以访问用户管理
4. 管理员可以访问角色权限
5. 管理员可以访问反馈管理
6. 管理员可以访问 RAG 管理
```

---

### 13.5 反馈功能

```text
1. 普通用户可以提交反馈
2. 管理员可以查看反馈
3. 管理员可以修改反馈状态
4. 管理员可以回复反馈
5. 普通用户不能查看其他用户反馈
```

---

## 14. 线上回滚方案

如果部署后出现严重问题，可以回滚。

### 14.1 Git 回滚到上一个稳定提交

查看提交记录：

```bash
git log --oneline -10
```

切换到上一个稳定提交：

```bash
git checkout <stable_commit_id>
```

重新部署：

```bash
docker compose -f deploy/docker-compose.yml down
docker compose -f deploy/docker-compose.yml up -d --build
```

---

### 14.2 数据库回滚

如果数据库变更造成严重问题，使用部署前备份恢复。

恢复前必须确认：

```text
1. 是否可以覆盖当前线上数据
2. 是否有新用户数据需要保留
3. 是否有新反馈数据需要保留
```

恢复示例：

```bash
cat ~/intern_pilot_backup_before_v1_1_0.sql | docker exec -i internpilot-mysql mysql -uroot -p intern_pilot
```

---

## 15. README 收尾要求

README 需要最终确认以下内容：

```text
1. 项目简介准确
2. 技术栈准确
3. 功能清单包含最新功能
4. 在线系统地址正确
5. 部署说明正确
6. 环境变量说明完整
7. 不包含真实 API Key
8. 不包含 QQ 邮箱授权码
9. 不公开管理员密码
10. 不再写 demo / 123456 演示账号
11. 截图是最新前端界面
12. 架构图与当前系统一致
13. 更新日志包含最新版本
```

---

## 16. 建议版本号

建议使用：

```text
v1.1.0
```

版本说明：

```text
v1.1.0：邮箱注册登录、单管理员、用户中心、前端 UI 优化、全局 AI 任务中心、用户反馈功能、最终部署优化
```

如果你想把它作为答辩最终版，也可以使用：

```text
v1.0.0
```

但推荐：

```text
v1.1.0
```

因为项目之前已经经历过多个阶段，v1.1.0 更符合迭代过程。

---

## 17. 合并 main 流程

确认 dev 已经通过本地和线上验收后：

```bash
git checkout main
git pull origin main
git merge dev
```

如果没有冲突：

```bash
git push origin main
```

如果有冲突：

```text
1. 优先保留 dev 中的新功能
2. 保留服务器部署相关配置
3. 保留 README 最新版本
4. 保留最新 SQL
5. 冲突解决后重新测试
```

---

## 18. 打版本标签

确认 main 稳定后：

```bash
git tag -a v1.1.0 -m "Release v1.1.0: final acceptance, AI task center, feedback and deployment"
```

推送标签：

```bash
git push origin v1.1.0
```

查看标签：

```bash
git tag
```

---

## 19. 同步 Gitee

如果 GitHub 是主仓库，Gitee 是镜像仓库：

```bash
git push gitee main
git push gitee dev
git push gitee v1.1.0
```

如果远程名不是 `gitee`，先查看：

```bash
git remote -v
```

---

## 20. 答辩提交材料检查

课程要求可能包括：

```text
1. 在线系统地址
2. README
3. PPT 介绍文档
4. 功能演示视频
```

你当前已经有在线系统地址，因此至少满足一个演示材料要求。

建议最终提交材料包含：

```text
1. GitHub 仓库地址
2. Gitee 仓库地址，如果老师要求
3. 在线系统地址
4. README 项目说明
5. 系统截图
6. 数据库设计说明
7. 核心功能说明
8. 部署说明
9. 测试说明
10. 后续迭代计划
```

---

## 21. 最终功能清单

README 或答辩中可以总结为：

```text
1. 用户注册登录
2. QQ 邮箱验证码
3. JWT 鉴权
4. RBAC 权限控制
5. 用户中心
6. 简历管理
7. 岗位管理
8. AI 简历岗位匹配分析
9. WebSocket AI 分析进度
10. 全局 AI 任务中心
11. 岗位推荐
12. AI 面试题生成
13. RAG 岗位知识库问答
14. 管理员后台
15. 用户反馈
16. Docker 一键部署
17. 线上系统访问
```

---

## 22. 最终验收通过标准

本阶段完成后，必须满足：

```text
1. dev 分支本地后端测试通过
2. dev 分支前端构建通过
3. Codex 复查没有严重问题
4. 服务器部署成功
5. 线上系统可以访问
6. 登录注册正常
7. 邮箱验证码正常
8. AI 功能使用 DeepSeek 真接口
9. 全局 AI 任务中心正常
10. 用户反馈正常
11. 管理员后台正常
12. 普通用户无越权访问
13. README 信息准确
14. main 分支合并完成
15. v1.1.0 标签已推送
16. Gitee 同步完成
```

---

# 给 Trae + DeepSeek Pro 的提示词

我正在开发 InternPilot 智能实习领航员项目，技术栈是 Spring Boot + Vue3 + MySQL + Redis + Docker + DeepSeek API。当前已经完成到 `docs/40-final-acceptance-release-and-deployment.md`，本阶段目标是做最终验收、发布、服务器部署、README 收尾、main 合并、打标签和 Gitee 同步。

请严格按照 `docs/40-final-acceptance-release-and-deployment.md` 执行最终发布前检查与收尾工作。

请重点完成：

1. 检查当前 dev 分支状态

* 确认所有功能分支已经合并到 dev
* 确认工作区干净
* 确认没有未提交文件
* 确认没有误提交 `.env`、API Key、QQ 邮箱授权码、真实密码

2. 执行本地测试

* 后端执行：`./gradlew test --no-daemon --max-workers=1`
* 前端执行：`npm install` 和 `npm run build`
* 修复测试失败、构建失败、TypeScript 错误、路径错误

3. 检查数据库迁移

* 确认 `user_feedback` 表 SQL 已存在
* 确认 `feedback:read`、`feedback:write`、`feedback:delete` 权限初始化逻辑存在
* 确认管理员角色绑定这些权限
* 确认 init.sql 和迁移 SQL 不冲突
* 确认线上 MySQL volume 不会自动重新执行 init.sql 的注意事项已写入文档或 README

4. 检查线上部署配置

* 检查 `deploy/docker-compose.yml`
* 确认后端、前端、MySQL、Redis 服务配置正常
* 确认线上使用 DeepSeek 真接口，不使用 Mock
* 确认 Nginx `/api` 和 WebSocket 代理配置正确
* 确认前端构建产物可以被 Nginx 正确访问

5. 检查 README

* 更新功能清单
* 更新部署说明
* 更新环境变量说明
* 更新版本记录
* 确认不出现 demo / 123456、admin / 123456 等公开演示账号密码
* 确认不公开 [3425446714@qq.com](mailto:3425446714@qq.com) 的管理员密码
* 确认截图和描述与当前系统一致

6. 输出最终结果
   请输出：

* 检查了什么
* 修改了什么
* 执行了哪些测试
* 测试结果
* 是否可以合并 main
* 是否可以部署服务器
* 还需要我手动完成什么

---

# 给 Codex 的最终复查提示词

请你作为代码审查与发布前验收助手，对 InternPilot 项目进行最终发布前复查。本轮对应文档是 `docs/40-final-acceptance-release-and-deployment.md`。

请不要新增大功能，重点是检查稳定性、安全性、部署可用性和提交质量。

请重点检查：

1. 分支与提交状态

* 当前是否在 dev 分支
* 是否有未提交文件
* 功能分支是否都已经合并
* 是否存在冲突残留
* 是否存在调试代码、console.log、临时注释、TODO 未处理

2. 安全检查

* 是否误提交 `.env`
* 是否误提交 DeepSeek API Key
* 是否误提交 QQ 邮箱授权码
* 是否公开管理员密码
* README 中是否存在 demo / 123456、admin / 123456 等不该公开的演示账号密码
* 线上是否默认使用 DeepSeek，不使用 Mock
* Mock 是否只用于测试

3. 后端检查

* `./gradlew test --no-daemon --max-workers=1` 是否通过
* Spring Security 权限是否正确
* 普通用户是否不能访问 `/api/admin/**`
* 反馈接口是否有越权问题
* AI 任务取消后是否可能被覆盖成 COMPLETED
* user_feedback 表和权限初始化是否完整
* 单管理员策略是否和 README 描述一致

4. 前端检查

* `npm run build` 是否通过
* 路由是否正常
* 登录注册页面是否正常
* 用户中心是否正常
* 全局 AI 任务中心是否正常
* 用户反馈入口是否正常
* 管理员反馈页面是否正常
* 页面刷新是否不会 404
* Nginx 部署是否兼容 Vue Router history 模式

5. 部署检查

* `deploy/docker-compose.yml` 是否可用
* 后端、前端、MySQL、Redis 服务名是否和文档一致
* 端口映射是否正确
* Nginx `/api` 代理是否正确
* WebSocket 代理是否正确
* MySQL volume 下 init.sql 不会重复执行的问题是否已说明
* 是否提供线上数据库迁移 SQL 或明确迁移步骤

6. README 和发布检查

* README 是否反映当前功能
* 是否有在线系统地址
* 是否有环境变量说明
* 是否有部署步骤
* 是否有版本记录
* 是否适合作为答辩提交材料
* 是否可以合并 dev 到 main
* 是否可以打 `v1.1.0` 标签

请直接检查、修复能修复的问题，并运行必要测试。完成后输出：

* 检查了什么
* 修复了什么
* 测试命令和结果
* 仍然存在的风险
* 是否建议合并 main
* 是否建议部署服务器
* 需要我手动验证的地方

---
