可以，这一版建议作为 **v1.1.0 认证与线上账号整理版本** 来做。  
文档可以命名为：
# InternPilot 邮箱注册登录、单管理员初始化与用户中心设计文档

## 一、文档目的

本文档用于指导 InternPilot 在 `feature/auth-phone-email-captcha` 分支中完成认证体系的第一阶段升级。

当前系统已经完成：

1. JWT 登录认证
2. RBAC 权限系统
3. DeepSeek 真实 API 接入
4. Docker 服务器部署
5. 在线系统访问
6. admin / demo 演示数据初始化

现在项目已经进入线上展示与答辩阶段，系统不再需要公开 demo 演示账号，也暂时不接入手机短信验证码。

本阶段目标是：

> 将认证体系调整为“邮箱注册 + 邮箱验证码 + 邮箱登录”，删除默认 demo / admin 演示账号，仅保留一个真实管理员账号 `3425446714@qq.com`，并新增用户中心页面，用于查看和维护当前用户个人信息。

---

## 二、本阶段目标

### 2.1 功能目标

本阶段需要完成：

1. 仅支持邮箱注册
2. 仅支持邮箱验证码
3. 暂不支持手机号注册
4. 登录方式改为邮箱 + 密码
5. 删除默认 demo 演示账号
6. 删除旧 admin 演示账号
7. `init.sql` 中只初始化一个管理员用户：`3425446714@qq.com`
8. 该用户拥有 ADMIN 权限
9. 保留 USER 角色和 ADMIN 角色
10. 保留 RBAC 权限体系
11. 新增用户中心页面
12. 用户中心支持查看个人信息
13. 用户中心支持修改昵称等基础信息
14. 用户中心支持修改密码
15. 用户中心展示邮箱绑定状态
16. 服务器生产环境使用真实 QQ 邮箱 SMTP 发送验证码
17. 测试环境继续使用 Mock 邮箱验证码
18. 后端测试通过
19. 前端构建通过
20. Docker 部署可用

---

### 2.2 非目标

本阶段暂不做：

1. 不接入手机短信验证码
2. 不接入腾讯云短信
3. 不做微信 / GitHub / QQ 第三方登录
4. 不做找回密码完整流程
5. 不做多因素认证 MFA
6. 不做复杂账号风控
7. 不删除 username 字段
8. 不重构 RBAC 权限系统
9. 不删除 MockCaptchaSender 测试能力
10. 不在 README 公开线上管理员密码
11. 不把 QQ 邮箱授权码写入 Git 仓库

---

## 三、认证策略调整

### 3.1 当前认证策略

旧策略：

```text
username + password 登录
admin / 123456
demo / 123456
````

问题：

1. 不适合线上系统
    
2. 演示账号容易被公开滥用
    
3. 不符合真实产品的邮箱账号体系
    
4. demo 数据不再适合正式部署后的答辩系统
    

---

### 3.2 新认证策略

新策略：

```text
注册：邮箱 + 邮箱验证码 + 密码
登录：邮箱 + 密码
管理员：3425446714@qq.com
```

说明：

1. 普通用户注册必须使用邮箱验证码
    
2. 登录页不再显示“用户名”
    
3. 登录页显示“邮箱”
    
4. 手机号注册入口暂时隐藏或禁用
    
5. 旧 demo / admin 演示账号不再初始化
    
6. 线上管理员账号不公开密码
    

---

## 四、账号初始化设计

### 4.1 线上唯一初始管理员

初始化账号：

```text
邮箱：3425446714@qq.com
角色：ADMIN
账号类型：SYSTEM / EMAIL
邮箱已验证：true
```

注意：

```text
不要在 README 公开该账号密码。
不要在 Git 仓库中写入真实密码。
不要把 QQ 邮箱授权码写入 init.sql 或 README。
```

---

### 4.2 密码处理策略

因为 `init.sql` 是公开仓库文件，不应写入明文密码。

推荐方案：

```text
init.sql 中只写 BCrypt 后的密码哈希。
真实密码只由你本人保存。
```

要求：

1. 不能在 SQL 注释中写明真实密码
    
2. 不能在 README 中写明真实密码
    
3. 不能在 docs 中写明真实密码
    
4. 如果需要重置密码，通过本地生成 BCrypt 哈希后更新数据库
    

示例：

```sql
password = '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'
```

注意：

```text
这里是示例，不要直接使用。
```

---

### 4.3 是否保留 demo 用户

本阶段不保留 demo 用户。

`init.sql` 中不再插入：

```text
demo / 123456
```

如果已有 Docker 数据卷中存在 demo，需要通过重置数据库或迁移脚本清理。

---

### 4.4 是否保留旧 admin 用户名

不再初始化旧的：

```text
admin / 123456
```

但数据库字段 `username` 可以继续存在。

对于 `3425446714@qq.com` 用户，username 可以设置为：

```text
admin
```

或：

```text
3425446714
```

推荐：

```text
username = admin
email = 3425446714@qq.com
account_type = SYSTEM
```

这样内部显示仍然简洁，但登录入口使用邮箱。

---

## 五、数据库设计

### 5.1 user 表字段

如果还没有，需要确保用户表支持以下字段：

|字段|类型|说明|
|---|---|---|
|username|varchar|内部用户名 / 昵称，不作为普通登录入口|
|password|varchar|BCrypt 密码|
|email|varchar|邮箱，唯一|
|phone|varchar|手机号，暂时可为空|
|account_type|varchar|SYSTEM / EMAIL|
|email_verified|tinyint|邮箱是否验证|
|phone_verified|tinyint|手机号是否验证，暂时默认 0|
|nickname|varchar|昵称，可选|
|avatar_url|varchar|头像地址，可选|
|last_login_time|datetime|最近登录时间|

如果当前已有 `nickname`、`avatar` 等字段，以现有字段为准，不重复新增。

---

### 5.2 init.sql 修改原则

`init.sql` 需要调整为：

1. 保留角色表初始化
    
2. 保留权限表初始化
    
3. 保留角色权限关系
    
4. 只初始化一个用户：`3425446714@qq.com`
    
5. 给该用户绑定 ADMIN 角色
    
6. 不插入 demo 用户
    
7. 不插入旧 admin 演示账号
    
8. 不插入乱码或英文演示数据
    
9. 不公开真实密码
    

---

### 5.3 Docker 首次初始化效果

新的 Docker 数据库首次启动后，应满足：

```text
用户表只有 3425446714@qq.com 这一个初始用户
该用户拥有 ADMIN 角色
该用户可以登录后台
无 demo 用户
无旧 admin / 123456 演示账号
```

---

### 5.4 服务器已有数据库清理

如果服务器 Docker 数据卷已经存在，修改 `init.sql` 不会自动重新执行。

若要让服务器数据库变成“只有 [3425446714@qq.com](mailto:3425446714@qq.com) 用户”，有两种方式。

#### 方案 A：重置 Docker 数据库

适合当前还没有重要线上数据时使用。

```bash
cd ~/intern-pilot/deploy
docker compose --env-file .env -f docker-compose.yml down -v
docker compose --env-file .env -f docker-compose.yml up -d --build
```

注意：

```text
down -v 会删除 MySQL volume，所有线上数据都会被重置。
```

#### 方案 B：执行专门清理脚本

新增脚本：

```text
backend/intern-pilot-backend/src/main/resources/sql/prod-single-admin-reset.sql
```

用途：

```text
只用于当前个人线上演示环境。
执行前必须备份数据库。
```

脚本目标：

1. 删除 demo 用户
    
2. 删除旧 admin 演示账号
    
3. 保留或创建 `3425446714@qq.com`
    
4. 给该用户绑定 ADMIN 角色
    
5. 清理被删除用户相关简历、岗位、分析报告、面试题、投递记录
    
6. 保留 RBAC 角色和权限
    
7. 不清空全库结构
    

---

## 六、邮箱验证码设计

### 6.1 验证码发送方式

本阶段只接入邮箱验证码。

支持：

```text
QQ 邮箱 SMTP
```

不接入：

```text
手机短信验证码
```

---

### 6.2 服务器环境变量

服务器 `.env` 需要配置：

```env
AUTH_EMAIL_CAPTCHA_PROVIDER=smtp
AUTH_SMS_CAPTCHA_PROVIDER=disabled

MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=3425446714@qq.com
MAIL_PASSWORD=QQ邮箱SMTP授权码
MAIL_FROM=3425446714@qq.com
MAIL_SSL_ENABLED=true
```

注意：

```text
MAIL_PASSWORD 是 QQ 邮箱授权码，不是 QQ 密码。
不能提交到 Git。
不能写入 README。
不能写入 docs。
```

---

### 6.3 测试环境

测试环境不真实发邮件：

```yaml
auth:
  captcha:
    email-provider: mock
    sms-provider: disabled
```

Mock 验证码固定为：

```text
123456
```

仅用于测试。

---

### 6.4 邮件内容

邮件标题：

```text
InternPilot 注册验证码
```

邮件正文：

```text
您好，您的 InternPilot 注册验证码为：123456。

验证码 5 分钟内有效，请勿泄露给他人。
如果不是您本人操作，请忽略本邮件。
```

---

### 6.5 验证码 Redis Key

|Key|用途|TTL|
|---|---|---|
|auth:captcha:EMAIL_REGISTER:{email}|注册验证码|5 分钟|
|auth:captcha:cooldown:EMAIL_REGISTER:{email}|发送冷却|60 秒|
|auth:captcha:fail:EMAIL_REGISTER:{email}|错误次数|5 分钟|
|auth:captcha:daily:EMAIL_REGISTER:{email}|每日次数|24 小时|

---

### 6.6 验证码限制

|项目|规则|
|---|---|
|验证码长度|6 位数字|
|有效期|5 分钟|
|发送冷却|60 秒|
|每日发送上限|10 次|
|错误次数上限|5 次|
|注册成功后|删除验证码|

---

## 七、接口设计

### 7.1 发送邮箱验证码

```http
POST /api/auth/captcha/email/register
```

或沿用通用接口：

```http
POST /api/auth/captcha/register
```

推荐保留通用接口，但前端只传 `EMAIL`。

请求：

```json
{
  "email": "user@example.com"
}
```

或：

```json
{
  "target": "user@example.com",
  "type": "EMAIL"
}
```

响应：

```json
{
  "code": 200,
  "message": "验证码已发送"
}
```

---

### 7.2 邮箱注册

```http
POST /api/auth/register
```

请求：

```json
{
  "email": "user@example.com",
  "password": "123456",
  "confirmPassword": "123456",
  "captchaCode": "123456"
}
```

如果保持通用 DTO，也可以：

```json
{
  "account": "user@example.com",
  "accountType": "EMAIL",
  "password": "123456",
  "confirmPassword": "123456",
  "captchaCode": "123456"
}
```

建议：

```text
前端使用 email 字段更直观；
后端内部可以映射到 account。
```

---

### 7.3 邮箱登录

```http
POST /api/auth/login
```

请求：

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

或保留通用：

```json
{
  "account": "user@example.com",
  "password": "123456"
}
```

推荐：

```text
前端文案显示“邮箱”；
接口字段可以使用 account，后续兼容手机号。
```

---

### 7.4 当前用户信息

```http
GET /api/user/me
```

返回：

```json
{
  "id": 1,
  "username": "admin",
  "nickname": "系统管理员",
  "email": "3425446714@qq.com",
  "emailVerified": true,
  "roles": ["ADMIN"],
  "permissions": []
}
```

---

## 八、用户中心功能设计

### 8.1 页面位置

新增页面：

```text
frontend/intern-pilot-frontend/src/views/user/UserCenter.vue
```

路由：

```text
/user/center
```

菜单入口：

1. 顶部用户头像 / 用户名下拉菜单
    
2. 侧边栏“个人中心”
    
3. 登录后右上角点击进入
    

推荐：

```text
右上角用户下拉菜单 → 个人中心
```

---

### 8.2 页面模块

用户中心包含：

|模块|功能|
|---|---|
|基础信息|昵称、邮箱、角色、账号状态|
|安全信息|邮箱验证状态、最近登录时间|
|修改资料|修改昵称、头像，可选|
|修改密码|原密码、新密码、确认新密码|
|账号权限|展示当前角色和权限数量|
|系统信息|账号创建时间、最近更新时间|

---

### 8.3 用户中心接口

#### 获取个人资料

```http
GET /api/user/profile
```

响应：

```json
{
  "id": 1,
  "username": "admin",
  "nickname": "系统管理员",
  "email": "3425446714@qq.com",
  "emailVerified": true,
  "avatarUrl": "",
  "roles": ["ADMIN"],
  "permissions": ["user:read", "role:read"],
  "lastLoginTime": "2026-05-18 12:00:00",
  "createdAt": "2026-05-18 12:00:00"
}
```

---

#### 修改基础资料

```http
PUT /api/user/profile
```

请求：

```json
{
  "nickname": "系统管理员",
  "avatarUrl": ""
}
```

说明：

```text
邮箱不在此接口直接修改。
如果后续支持修改邮箱，需要单独邮箱验证码流程。
```

---

#### 修改密码

```http
PUT /api/user/password
```

请求：

```json
{
  "oldPassword": "旧密码",
  "newPassword": "新密码",
  "confirmPassword": "新密码"
}
```

规则：

1. 必须验证旧密码
    
2. 新密码和确认密码一致
    
3. 新密码需要 BCrypt 加密
    
4. 修改成功后建议前端退出登录，重新登录
    

---

### 8.4 用户中心 DTO / VO

建议新增：

```text
dto/user/UpdateProfileRequest.java
dto/user/ChangePasswordRequest.java
vo/user/UserProfileVO.java
```

---

### 8.5 用户中心 Service

在现有结构下新增或扩展：

```text
service/user/UserProfileService.java
service/user/impl/UserProfileServiceImpl.java
```

也可以放进现有 `UserService`，但推荐独立：

```text
UserService：用户基础查询
UserProfileService：当前用户个人中心
```

---

### 8.6 用户中心权限

所有用户登录后都可以访问：

```java
@PreAuthorize("isAuthenticated()")
```

不需要额外 RBAC 权限码。

但必须保证：

```text
只能查看和修改当前登录用户自己的信息。
不能通过 userId 修改别人资料。
```

---

## 九、前端改造

### 9.1 登录页

修改：

```text
用户名 → 邮箱
```

默认提示：

```text
请输入邮箱
```

不再展示：

```text
demo / 123456
admin / 123456
```

---

### 9.2 注册页

只保留邮箱注册：

```text
邮箱
验证码
发送验证码
密码
确认密码
注册
```

隐藏或移除：

```text
手机号注册
```

---

### 9.3 用户中心页面

新增页面内容：

1. 用户头像 / 默认头像
    
2. 昵称
    
3. 邮箱
    
4. 邮箱已验证标签
    
5. 当前角色
    
6. 最近登录时间
    
7. 修改资料表单
    
8. 修改密码表单
    

---

### 9.4 前端 API

新增或扩展：

```text
src/api/user.ts
```

接口：

```ts
export function getUserProfileApi() {
  return request.get('/api/user/profile')
}

export function updateUserProfileApi(data) {
  return request.put('/api/user/profile', data)
}

export function changePasswordApi(data) {
  return request.put('/api/user/password', data)
}
```

认证 API：

```text
src/api/auth.ts
```

新增：

```ts
export function sendEmailRegisterCaptchaApi(data) {
  return request.post('/api/auth/captcha/register', data)
}
```

---

## 十、配置设计

### 10.1 application.yml

```yaml
auth:
  captcha:
    email-provider: ${AUTH_EMAIL_CAPTCHA_PROVIDER:mock}
    sms-provider: ${AUTH_SMS_CAPTCHA_PROVIDER:disabled}
    code-length: 6
    expire-minutes: 5
    cooldown-seconds: 60
    max-fail-count: 5
    daily-limit: 10

spring:
  mail:
    host: ${MAIL_HOST:}
    port: ${MAIL_PORT:465}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: ${MAIL_SSL_ENABLED:true}
          starttls:
            enable: ${MAIL_STARTTLS_ENABLED:false}
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
```

---

### 10.2 application-test.yml

```yaml
auth:
  captcha:
    email-provider: mock
    sms-provider: disabled
```

---

### 10.3 application-prod.yml

```yaml
auth:
  captcha:
    email-provider: ${AUTH_EMAIL_CAPTCHA_PROVIDER:smtp}
    sms-provider: ${AUTH_SMS_CAPTCHA_PROVIDER:disabled}
```

---

### 10.4 deploy/.env.example

新增：

```env
AUTH_EMAIL_CAPTCHA_PROVIDER=smtp
AUTH_SMS_CAPTCHA_PROVIDER=disabled

MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=
MAIL_SSL_ENABLED=true
MAIL_STARTTLS_ENABLED=false
```

---

## 十一、README 更新

README 需要调整：

### 11.1 在线系统说明

```markdown
## 在线系统

系统已部署至云服务器，可通过以下地址访问：

- 在线地址：https://internpilot.com.cn

管理员功能不公开账号密码，如需体验请联系项目负责人。
```

---

### 11.2 默认账号说明

删除：

```text
demo / 123456
admin / 123456
```

改为：

```text
线上系统不公开默认账号。
本地开发可根据 init.sql 初始化管理员账号。
```

---

### 11.3 邮箱验证码说明

新增：

```markdown
## 邮箱验证码配置

生产环境使用 SMTP 邮箱验证码。以 QQ 邮箱为例，需要在服务器 `.env` 中配置：

MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=你的邮箱
MAIL_PASSWORD=邮箱SMTP授权码
MAIL_FROM=你的邮箱

注意：MAIL_PASSWORD 是邮箱授权码，不是邮箱登录密码。
```

---

## 十二、Docker 与服务器更新

### 12.1 服务器 `.env`

服务器最终建议：

```env
SPRING_PROFILES_ACTIVE=prod

AI_PROVIDER=deepseek
AI_BASE_URL=https://api.deepseek.com
DEEPSEEK_API_KEY=真实Key
AI_MODEL=deepseek-v4-flash
AI_PRO_MODEL=deepseek-v4-pro
AI_TIMEOUT_SECONDS=60

AUTH_EMAIL_CAPTCHA_PROVIDER=smtp
AUTH_SMS_CAPTCHA_PROVIDER=disabled

MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=3425446714@qq.com
MAIL_PASSWORD=QQ邮箱SMTP授权码
MAIL_FROM=3425446714@qq.com
MAIL_SSL_ENABLED=true
MAIL_STARTTLS_ENABLED=false
```

---

### 12.2 服务器数据库重置

如果要让服务器只保留 `3425446714@qq.com` 用户，部署后执行：

```bash
cd ~/intern-pilot/deploy
docker compose --env-file .env -f docker-compose.yml down -v
docker compose --env-file .env -f docker-compose.yml up -d --build
```

注意：

```text
这会删除服务器 Docker MySQL 数据卷。
如果线上已有重要数据，必须先备份。
```

---

## 十三、后端测试设计

### 13.1 AuthControllerTest

覆盖：

1. 发送邮箱验证码成功
    
2. 邮箱格式错误发送失败
    
3. 邮箱验证码错误注册失败
    
4. 邮箱验证码正确注册成功
    
5. 重复邮箱注册失败
    
6. 邮箱登录成功
    
7. 密码错误登录失败
    
8. username 登录普通用户失败
    
9. [3425446714@qq.com](mailto:3425446714@qq.com) 管理员登录成功
    

---

### 13.2 CaptchaServiceTest

覆盖：

1. Redis 写入验证码
    
2. 验证码 TTL
    
3. 发送冷却
    
4. 错误次数限制
    
5. 正确验证码校验成功
    
6. 注册成功后验证码清理
    
7. mock provider 测试环境可用
    
8. smtp provider 配置缺失时返回清晰错误
    

---

### 13.3 UserProfileServiceTest

覆盖：

1. 获取当前用户个人信息
    
2. 修改昵称成功
    
3. 修改密码成功
    
4. 原密码错误修改失败
    
5. 新密码与确认密码不一致失败
    
6. 不能修改其他用户信息
    

---

### 13.4 前端构建

必须通过：

```bash
npm run build
```

---

## 十四、验收标准

完成后需要满足：

1. 登录页只显示邮箱登录
    
2. 注册页只支持邮箱注册
    
3. 发送邮箱验证码成功
    
4. 邮箱验证码错误不能注册
    
5. 邮箱验证码正确可以注册
    
6. 邮箱登录成功
    
7. username 普通登录不可用
    
8. 线上初始化不再有 demo 用户
    
9. 线上初始化不再有 admin / 123456
    
10. 只初始化 `3425446714@qq.com` 管理员
    
11. 该管理员拥有 ADMIN 权限
    
12. 用户中心可以访问
    
13. 用户中心可以修改昵称
    
14. 用户中心可以修改密码
    
15. README 不公开管理员密码
    
16. 服务器 `.env` 不进入 Git
    
17. 后端测试通过
    
18. 前端构建通过
    
19. Docker 启动成功
    
20. 在线系统可登录
    

---

## 十五、实施步骤

### 阶段 1：SQL 与账号初始化

1. 修改 init.sql
    
2. 删除 demo 初始化
    
3. 删除旧 admin / 123456 初始化
    
4. 只初始化 `3425446714@qq.com`
    
5. 同步 test-schema.sql / test-data.sql
    
6. 新增 prod-single-admin-reset.sql，可选
    

验证：

```bash
./gradlew test --tests "*Auth*"
```

---

### 阶段 2：邮箱验证码

1. 增加 spring-boot-starter-mail
    
2. 实现 EmailCaptchaSender
    
3. 配置 QQ SMTP
    
4. 测试环境保留 MockCaptchaSender
    
5. 关闭手机号验证码
    

验证：

```bash
./gradlew test --tests "*Captcha*"
```

---

### 阶段 3：登录注册前后端改造

1. 登录改邮箱
    
2. 注册改邮箱验证码
    
3. 前端去掉手机号注册
    
4. 前端去掉默认 demo/admin 提示
    

验证：

```bash
npm run build
```

---

### 阶段 4：用户中心

1. 新增后端接口
    
2. 新增前端页面
    
3. 接入右上角菜单
    
4. 支持修改基础信息
    
5. 支持修改密码
    

验证：

```bash
./gradlew test --tests "*UserProfile*"
npm run build
```

---

### 阶段 5：Docker 与服务器

1. 更新 `.env.example`
    
2. 更新服务器 `.env`
    
3. 重建容器
    
4. 必要时 `down -v` 重置数据库
    
5. 验证在线系统登录
    

---


## 十八、服务器上线后的命令

功能完成并合并 main 后，服务器更新：

```bash
cd ~/intern-pilot
git checkout main
git pull origin main

cd deploy
nano .env
```

确认：

```env
AUTH_EMAIL_CAPTCHA_PROVIDER=smtp
AUTH_SMS_CAPTCHA_PROVIDER=disabled

MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=3425446714@qq.com
MAIL_PASSWORD=你的QQ邮箱授权码
MAIL_FROM=3425446714@qq.com
MAIL_SSL_ENABLED=true
MAIL_STARTTLS_ENABLED=false
```

如果要重置服务器数据库，只保留新管理员：

```bash
docker compose --env-file .env -f docker-compose.yml down -v
docker compose --env-file .env -f docker-compose.yml up -d --build
```

如果不重置数据库，只更新代码：

```bash
docker compose --env-file .env -f docker-compose.yml up -d --build
```

---

## 十九、最终效果

本阶段完成后，系统将变成：

```text
登录：邮箱 + 密码
注册：邮箱 + 邮箱验证码 + 密码
管理员：3425446714@qq.com
演示账号：不再公开
手机验证码：暂不开放
用户中心：支持查看资料、修改昵称、修改密码
线上系统：更接近真实产品
```
