# InternPilot 手机号/邮箱注册登录、验证码机制与线上真实 AI 设计文档

## 一、文档目的

本文档用于指导 InternPilot 在 `feature/auth-phone-email-captcha` 分支中完成认证体系升级，并同步调整线上系统的 AI 使用策略。

当前系统已经完成：

1. JWT 登录认证
2. RBAC 权限系统
3. 简历上传与解析
4. 岗位 JD 管理
5. AI 简历匹配分析
6. WebSocket 分析进度
7. AI 面试题生成
8. DeepSeek API 接入
9. Docker 服务器部署
10. 在线系统访问

当前登录注册方式仍然偏早期开发形态，主要依赖 `username + password`。线上系统如果用于答辩展示和真实项目展示，应更接近真实产品：

```text
注册：手机号 / 邮箱 + 验证码 + 密码
登录：手机号 / 邮箱 + 密码
线上 AI：真实 DeepSeek API
测试环境：Mock AI / Mock 验证码
````

本阶段目标是：

> 将 InternPilot 的认证体系升级为手机号 / 邮箱注册登录，并引入验证码机制；同时明确线上环境使用真实 DeepSeek API，Mock 仅用于测试，不再作为线上演示模式。

---

## 二、当前问题

### 2.1 注册登录问题

当前系统主要存在：

| 问题                            | 说明                 |
| ----------------------------- | ------------------ |
| 使用 username 登录                | 不符合真实招聘 / 求职平台习惯   |
| 注册缺少验证码                       | 无法验证手机号或邮箱归属       |
| 缺少手机号 / 邮箱账号体系                | 后续找回密码、通知、账号安全不好扩展 |
| admin / demo 账号适合开发，不适合公开线上系统 | 线上系统应使用真实账号登录      |
| 前端登录文案不真实                     | “用户名”应改为“手机号 / 邮箱” |

---

### 2.2 Mock 使用问题

当前系统支持 Mock AI，适合开发和测试，但线上答辩系统如果仍显示 Mock，会带来几个问题：

| 问题                 | 说明                  |
| ------------------ | ------------------- |
| 演示不够真实             | 答辩时 AI 结果像固定模板      |
| 难以体现 DeepSeek 接入价值 | 老师看到的是 Mock，不是真实模型  |
| 线上系统定位不清晰          | 已经有服务器，就应展示真实能力     |
| 用户体验不自然            | Mock 数据可能重复、固定、缺少变化 |

因此，本阶段调整策略：

```text
线上环境不使用 Mock AI。
Mock AI 仅用于 test profile 和自动化测试。
```

---

## 三、本阶段目标

### 3.1 功能目标

本阶段需要完成：

1. 支持邮箱注册
2. 支持手机号注册
3. 支持邮箱验证码
4. 支持手机号验证码
5. 注册时必须校验验证码
6. 登录时使用手机号或邮箱，不再面向普通用户使用 username
7. 登录成功后继续返回 JWT
8. RBAC 权限体系保持不变
9. admin 管理员账号迁移为邮箱 / 手机号登录
10. demo 开发账号可保留在本地开发数据中，但线上不公开
11. 线上环境使用 DeepSeek API
12. MockAiClient 只在 test profile 启用
13. MockCaptchaSender 只在 test profile 启用
14. Docker 线上部署使用真实配置
15. README 更新在线系统登录方式说明
16. 后端测试和前端构建通过

---

### 3.2 非目标

本阶段暂不做：

1. 不做微信 / GitHub / OAuth 第三方登录
2. 不做扫码登录
3. 不做图形验证码
4. 不做复杂风控
5. 不做密码找回完整流程
6. 不做多因素认证 MFA
7. 不重构 JWT 过滤器
8. 不重构 RBAC 权限系统
9. 不删除 username 字段
10. 不删除 MockAiClient 源码
11. 不删除 MockCaptchaSender 源码
12. 不让测试环境真实调用 DeepSeek
13. 不让测试环境真实发送短信或邮件

---

## 四、核心设计原则

### 4.1 线上真实，测试 Mock

本阶段采用明确的环境分层：

| 环境            | AI                                      | 验证码                                   |
| ------------- | --------------------------------------- | ------------------------------------- |
| test          | MockAiClient                            | MockCaptchaSender                     |
| dev           | 默认可配置，推荐真实 DeepSeek；验证码可使用 mock 或 email |                                       |
| prod / online | DeepSeekAiClient                        | EmailCaptchaSender / SmsCaptchaSender |
| CI            | MockAiClient                            | MockCaptchaSender                     |

原则：

```text
Mock 不删除，但只能作为测试和 CI 的稳定替身。
线上系统不再使用 Mock。
```

---

### 4.2 username 保留但不作为普通登录入口

不建议删除 `username` 字段。

原因：

1. 历史数据依赖
2. Spring Security 可能依赖
3. 用户展示昵称可以继续使用
4. 管理后台可能展示 username
5. 直接删除风险大

但登录逻辑改为：

```text
普通用户：只能手机号 / 邮箱登录
系统账号：admin 可通过邮箱 / 手机号登录
username 仅作为内部展示或兼容字段
```

---

### 4.3 线上 admin 不公开

线上系统可以使用管理员账号演示，但不要在 README 公开真实密码。

README 应写：

```text
如需体验管理员功能，请联系项目负责人获取临时账号。
```

答辩时你自己登录 admin 账户演示即可。

---

## 五、数据库设计

### 5.1 user 表字段增强

在用户表中增加：

| 字段              | 类型           | 说明         |
| --------------- | ------------ | ---------- |
| phone           | varchar(20)  | 手机号，唯一，可为空 |
| email           | varchar(100) | 邮箱，唯一，可为空  |
| account_type    | varchar(20)  | 账号类型       |
| phone_verified  | tinyint      | 手机号是否验证    |
| email_verified  | tinyint      | 邮箱是否验证     |
| last_login_time | datetime     | 最近登录时间     |

---

### 5.2 AccountTypeEnum

新增枚举：

```java id="xbl3dj"
public enum AccountTypeEnum {
    PHONE,
    EMAIL,
    SYSTEM
}
```

说明：

| 类型     | 说明               |
| ------ | ---------------- |
| PHONE  | 手机号注册用户          |
| EMAIL  | 邮箱注册用户           |
| SYSTEM | 系统初始化账号，例如 admin |

---

### 5.3 admin 账号设计

线上 admin 不建议继续只依赖：

```text
admin / 123456
```

建议初始化为：

```text
admin@internpilot.local / 后台设置的安全密码
或
13800000000 / 后台设置的安全密码
```

如果仍需课程演示简单登录，可以在服务器 `.env` 或初始化脚本中设置：

```text
ADMIN_EMAIL=admin@internpilot.local
ADMIN_PHONE=13800000000
ADMIN_PASSWORD=自行设置
```

不建议把线上真实 admin 密码写入 README。

---

### 5.4 demo 账号设计

demo 账号可以保留在本地开发和测试数据中：

```text
demo@internpilot.local / 123456
13900000000 / 123456
```

但线上系统不应公开 demo 密码。

---

### 5.5 SQL 修改范围

需要同步：

```text
backend/intern-pilot-backend/src/main/resources/sql/init.sql
backend/intern-pilot-backend/src/main/resources/sql/dev-init-rbac.sql
backend/intern-pilot-backend/src/main/resources/sql/dev-demo-data-reset.sql
backend/intern-pilot-backend/src/test/resources/sql/test-schema.sql
backend/intern-pilot-backend/src/test/resources/sql/test-data.sql
```

要求：

1. 不删除 username 字段
2. 不破坏 RBAC 表
3. 不破坏 admin / demo 初始化
4. test-schema 字段必须和 init.sql 对齐
5. Docker 初始化数据必须可用
6. 线上密码不要写真实强密码到公开仓库

---

## 六、验证码设计

### 6.1 验证码场景

新增枚举：

```java id="hk65jq"
public enum CaptchaSceneEnum {
    EMAIL_REGISTER,
    PHONE_REGISTER
}
```

---

### 6.2 验证码发送目标

```java id="mdazgz"
public enum CaptchaTargetTypeEnum {
    EMAIL,
    PHONE
}
```

---

### 6.3 Redis Key 设计

| Key                                    | 用途     | TTL   |
| -------------------------------------- | ------ | ----- |
| auth:captcha:{scene}:{target}          | 验证码内容  | 5 分钟  |
| auth:captcha:cooldown:{scene}:{target} | 发送冷却   | 60 秒  |
| auth:captcha:fail:{scene}:{target}     | 错误次数   | 5 分钟  |
| auth:captcha:daily:{scene}:{target}    | 每日发送次数 | 24 小时 |

---

### 6.4 验证码规则

| 项目      | 规则    |
| ------- | ----- |
| 验证码长度   | 6 位数字 |
| 有效期     | 5 分钟  |
| 发送冷却    | 60 秒  |
| 每日发送上限  | 10 次  |
| 错误次数上限  | 5 次   |
| 注册成功后   | 删除验证码 |
| 验证码返回前端 | 不允许   |

---

### 6.5 发送器设计

定义接口：

```java id="q8uxl7"
public interface CaptchaSender {
    void send(String target, String code, CaptchaSceneEnum scene);
}
```

实现类：

```text
EmailCaptchaSender
SmsCaptchaSender
MockCaptchaSender
```

---

### 6.6 发送器启用规则

| Sender             | 启用环境          |
| ------------------ | ------------- |
| EmailCaptchaSender | prod / online |
| SmsCaptchaSender   | prod / online |
| MockCaptchaSender  | test / CI     |

要求：

```text
线上环境不能启用 MockCaptchaSender。
测试环境不能真实发短信或邮件。
```

---

## 七、接口设计

### 7.1 发送注册验证码

```http id="a3t3og"
POST /api/auth/captcha/register
```

请求体：

```json id="o4rq01"
{
  "target": "student@example.com",
  "type": "EMAIL"
}
```

手机号：

```json id="m9wp72"
{
  "target": "13800000000",
  "type": "PHONE"
}
```

响应：

```json id="p285sz"
{
  "code": 200,
  "message": "验证码已发送"
}
```

---

### 7.2 注册接口

保留路径：

```http id="vim6p1"
POST /api/auth/register
```

请求体：

```json id="zr3zke"
{
  "account": "student@example.com",
  "accountType": "EMAIL",
  "password": "123456",
  "confirmPassword": "123456",
  "captchaCode": "123456"
}
```

手机号注册：

```json id="nkzoyj"
{
  "account": "13800000000",
  "accountType": "PHONE",
  "password": "123456",
  "confirmPassword": "123456",
  "captchaCode": "123456"
}
```

注册成功后：

1. 创建用户
2. BCrypt 加密密码
3. 写入 email 或 phone
4. 设置 verified = true
5. 绑定 USER 角色
6. 删除验证码
7. 返回注册成功

---

### 7.3 登录接口

保留路径：

```http id="a9hbjg"
POST /api/auth/login
```

请求体：

```json id="s5g29p"
{
  "account": "student@example.com",
  "password": "123456"
}
```

或：

```json id="mknpp9"
{
  "account": "13800000000",
  "password": "123456"
}
```

不再面向普通用户支持：

```json id="odxyfr"
{
  "username": "demo",
  "password": "123456"
}
```

---

### 7.4 当前用户接口

```http id="xq04eb"
GET /api/user/me
```

返回中补充：

```json id="2etpwc"
{
  "id": 1,
  "username": "demo",
  "phone": "13900000000",
  "email": "demo@internpilot.local",
  "accountType": "EMAIL",
  "roles": ["USER"],
  "permissions": []
}
```

---

## 八、后端模块设计

### 8.1 DTO

路径建议：

```text
dto/auth/SendRegisterCaptchaRequest.java
dto/auth/RegisterRequest.java
dto/auth/LoginRequest.java
dto/auth/LoginResponse.java
```

---

### 8.2 CaptchaService

路径：

```text
service/auth/CaptchaService.java
service/auth/impl/CaptchaServiceImpl.java
```

接口：

```java id="zsfzwu"
void sendRegisterCaptcha(SendRegisterCaptchaRequest request);

void verifyRegisterCaptcha(String target, AccountTypeEnum accountType, String code);

void clearRegisterCaptcha(String target, AccountTypeEnum accountType);
```

---

### 8.3 CaptchaSender

路径建议：

```text
service/auth/captcha/CaptchaSender.java
service/auth/captcha/impl/EmailCaptchaSender.java
service/auth/captcha/impl/SmsCaptchaSender.java
service/auth/captcha/impl/MockCaptchaSender.java
```

---

### 8.4 AuthService

需要调整：

```java id="vpfke2"
void sendRegisterCaptcha(SendRegisterCaptchaRequest request);

void register(RegisterRequest request);

LoginResponse login(LoginRequest request);
```

---

### 8.5 UserService

新增：

```java id="kr6q4o"
User findByLoginAccount(String account);

boolean existsByPhone(String phone);

boolean existsByEmail(String email);
```

查询逻辑：

```text
邮箱格式 → 按 email 查询
手机号格式 → 按 phone 查询
其他 → 返回账号或密码错误
```

管理员系统账号兼容策略：

```text
可选：仅 SYSTEM 类型账号允许 username 兼容登录。
推荐：线上使用 admin email / phone 登录，不公开 username 登录。
```

---

## 九、前端设计

### 9.1 登录页

原来：

```text
用户名
密码
```

改为：

```text
手机号 / 邮箱
密码
```

请求参数：

```ts id="bxlv23"
{
  account: string
  password: string
}
```

---

### 9.2 注册页

增加：

1. 注册方式选择：邮箱 / 手机号
2. 手机号或邮箱输入框
3. 验证码输入框
4. 发送验证码按钮
5. 60 秒倒计时
6. 密码
7. 确认密码

---

### 9.3 前端 API

`auth.ts` 新增：

```ts id="jfeaq2"
export function sendRegisterCaptchaApi(data) {
  return request.post('/api/auth/captcha/register', data)
}
```

登录：

```ts id="qe20fd"
export function loginApi(data) {
  return request.post('/api/auth/login', data)
}
```

注册：

```ts id="om3z4m"
export function registerApi(data) {
  return request.post('/api/auth/register', data)
}
```

---

## 十、AI 线上真实策略

### 10.1 prod 环境必须使用 DeepSeek

线上 `.env` 推荐：

```env id="gnloka"
SPRING_PROFILES_ACTIVE=prod
AI_PROVIDER=deepseek
AI_BASE_URL=https://api.deepseek.com
DEEPSEEK_API_KEY=你的真实Key
AI_MODEL=deepseek-v4-flash
AI_PRO_MODEL=deepseek-v4-pro
AI_TIMEOUT_SECONDS=60
AUTH_CAPTCHA_MODE=real
```

---

### 10.2 test 环境使用 Mock

`application-test.yml`：

```yaml id="klvix7"
ai:
  provider: mock

auth:
  captcha:
    mode: mock
```

---

### 10.3 MockAiClient 限制

要求：

```text
MockAiClient 只允许 test profile 或明确 mock 配置时启用。
prod 环境如果 AI_PROVIDER=mock，应启动失败或记录严重错误。
```

推荐：

```text
prod 环境缺少 DEEPSEEK_API_KEY 时，启动失败。
test 环境不检查 DEEPSEEK_API_KEY。
```

---

## 十一、配置设计

### 11.1 application.yml

```yaml id="m436rh"
auth:
  captcha:
    mode: ${AUTH_CAPTCHA_MODE:mock}
    code-length: 6
    expire-minutes: 5
    cooldown-seconds: 60
    max-fail-count: 5
    daily-limit: 10
```

---

### 11.2 application-test.yml

```yaml id="136sv5"
auth:
  captcha:
    mode: mock

ai:
  provider: mock
```

---

### 11.3 application-prod.yml

```yaml id="zg8mtr"
auth:
  captcha:
    mode: ${AUTH_CAPTCHA_MODE:real}

ai:
  provider: ${AI_PROVIDER:deepseek}
```

---

### 11.4 deploy/.env.example

增加：

```env id="x9caec"
AUTH_CAPTCHA_MODE=real

MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

SMS_PROVIDER=tencent
TENCENT_SMS_SECRET_ID=
TENCENT_SMS_SECRET_KEY=
TENCENT_SMS_SDK_APP_ID=
TENCENT_SMS_SIGN_NAME=
TENCENT_SMS_TEMPLATE_ID=
```

注意：

```text
.env.example 只能放空值或占位，不能放真实密钥。
```

---

## 十二、线上环境安全规则

### 12.1 README 不公开 admin 密码

README 写：

```markdown id="jnjnnq"
## 在线演示

在线系统地址：

http://43.136.182.179

如需体验管理员功能，请联系项目负责人获取临时演示账号。
```

不要写：

```text
admin / 123456
```

---

### 12.2 服务器 `.env` 不提交

确认：

```bash id="xfh0ll"
git status --short
git ls-files deploy/.env
```

`deploy/.env` 不应被跟踪。

---

### 12.3 DeepSeek Key 不进入日志

要求：

1. 日志不能打印完整 API Key
2. 错误信息不能返回 API Key
3. README 不写真实 Key
4. docs 不写真实 Key

---

## 十三、测试设计

### 13.1 AuthControllerTest

覆盖：

| 用例                 | 预期       |
| ------------------ | -------- |
| 发送邮箱验证码成功          | 200      |
| 发送手机号验证码成功         | 200      |
| 邮箱格式错误             | 400      |
| 手机号格式错误            | 400      |
| 邮箱注册成功             | 200      |
| 手机号注册成功            | 200      |
| 验证码错误注册失败          | 400      |
| 重复邮箱注册失败           | 400      |
| 重复手机号注册失败          | 400      |
| 邮箱登录成功             | 返回 token |
| 手机号登录成功            | 返回 token |
| 密码错误登录失败           | 401      |
| 普通用户 username 登录失败 | 401      |
| admin 系统账号邮箱登录成功   | 200      |

---

### 13.2 CaptchaServiceTest

覆盖：

1. Redis 写入验证码
2. TTL 生效
3. 发送冷却
4. 错误次数递增
5. 错误次数超过限制后失效
6. 正确验证码通过
7. 注册成功后验证码清理

---

### 13.3 AiClientProfileTest

覆盖：

1. test profile 使用 MockAiClient
2. prod profile 使用 DeepSeekAiClient
3. prod profile 不允许 AI_PROVIDER=mock
4. prod profile 缺少 DeepSeek Key 时失败或抛清晰异常

---

### 13.4 前端构建

至少保证：

```bash id="ry0pse"
npm run build
```

通过。

---

## 十四、Docker 与服务器部署影响

### 14.1 服务器更新后要改 `.env`

线上服务器：

```bash id="ejbfeb"
cd ~/intern-pilot/deploy
nano .env
```

确保：

```env id="f3u6zn"
SPRING_PROFILES_ACTIVE=prod
AI_PROVIDER=deepseek
DEEPSEEK_API_KEY=真实Key
AUTH_CAPTCHA_MODE=real
```

如果暂时没有短信和邮件配置，则线上注册功能无法真实发送验证码。

因此实现时要明确：

```text
如果 AUTH_CAPTCHA_MODE=real，但邮件/短信配置缺失，应返回清晰错误：
验证码服务未配置，请联系管理员。
```

---

### 14.2 线上注册策略

如果你短期只需要自己答辩演示，可以先这样：

```text
关闭公开注册入口，或只保留登录。
由你提前在数据库中创建 admin 账号。
```

但如果你要完整展示注册功能，则必须配置：

```text
邮箱 SMTP 或短信服务
```

---

## 十五、实施步骤

### 阶段 1：设计文档与分支确认

```bash id="r90neq"
git checkout dev
git pull origin dev
git checkout -b feature/auth-phone-email-captcha
```

新增：

```text
docs/36-auth-phone-email-captcha-real-online-design.md
```

---

### 阶段 2：数据库与实体

1. User 实体增加 phone/email/accountType/verified 字段
2. init.sql 同步
3. test-schema.sql 同步
4. test-data.sql 同步
5. admin/demo 添加 email 和 phone

验证：

```bash id="vwrji8"
./gradlew compileJava
```

---

### 阶段 3：验证码模块

1. CaptchaService
2. CaptchaSender
3. EmailCaptchaSender
4. SmsCaptchaSender
5. MockCaptchaSender
6. Redis 存储与限制

验证：

```bash id="qfs2eb"
./gradlew test --tests "*Captcha*"
```

---

### 阶段 4：注册登录改造

1. 修改 RegisterRequest
2. 修改 LoginRequest
3. 修改 AuthController
4. 修改 AuthService
5. 修改 UserDetails 查询逻辑
6. 保留 JWT 返回格式

验证：

```bash id="v8g7pb"
./gradlew test --tests "*Auth*"
```

---

### 阶段 5：前端改造

1. 登录页改为手机号 / 邮箱
2. 注册页增加验证码
3. auth.ts 更新
4. 前端路由不变

验证：

```bash id="69xcc4"
npm run build
```

---

### 阶段 6：线上 AI 策略

1. prod 默认 DeepSeek
2. test 默认 Mock
3. prod 不允许 Mock AI
4. README 删除公开 mock 演示账号描述
5. `.env.example` 更新

---

## 十六、验收标准

完成后必须满足：

1. 登录页不再显示用户名
2. 邮箱注册可用
3. 手机号注册可用
4. 注册必须验证码
5. 验证码错误无法注册
6. 邮箱登录可用
7. 手机号登录可用
8. 普通用户不能 username 登录
9. admin 可以通过邮箱 / 手机号登录
10. JWT 正常返回
11. RBAC 权限正常
12. test profile 使用 Mock
13. prod profile 使用 DeepSeek
14. prod 不公开 Mock AI
15. README 不公开 admin 密码
16. 后端测试通过
17. 前端构建通过
18. Docker 可启动
19. 服务器 `.env` 可配置真实 DeepSeek
20. 无 API Key 泄露

---

## 十七、Trae + DeepSeek Pro 执行提示词

```text id="l02vsz"
你现在是我的 Java 全栈项目开发助手，请在 InternPilot 项目中实现手机号/邮箱注册登录、验证码机制，并调整线上环境为真实 DeepSeek API。

当前分支必须是：
feature/auth-phone-email-captcha

请严格参考：
docs/36-auth-phone-email-captcha-real-online-design.md

项目背景：
InternPilot 是 Spring Boot + Spring Security + JWT + MyBatis-Plus + MySQL + Redis + Vue3 的 AI 实习投递与简历优化平台。
当前系统已部署到服务器，有在线访问地址。
现在需要把登录注册升级为手机号/邮箱注册登录，并引入验证码机制。
同时，线上环境不再使用 Mock AI。Mock 只保留给 test / CI。

执行要求：
1. 先检查当前分支。
2. 不要自动提交 Git。
3. 不要 push。
4. 不要删除 username 字段。
5. 不要破坏 JWT。
6. 不要破坏 RBAC。
7. 不要删除 MockAiClient 源码。
8. 但 MockAiClient 只能用于 test / CI，不能作为线上演示模式。
9. 不要真实调用 DeepSeek。
10. 不要真实发送短信或邮件。
11. 不要泄露 API Key。
12. 不要修改无关业务模块。

任务一：数据库增强
1. 用户表增加 phone、email、account_type、phone_verified、email_verified、last_login_time。
2. User 实体同步。
3. init.sql、dev-init-rbac.sql、test-schema.sql、test-data.sql 同步。
4. admin/demo 补充 email 和 phone。
5. 不删除 username 字段。
6. 不破坏 RBAC 初始化。

任务二：验证码机制
1. 新增 AccountTypeEnum。
2. 新增 CaptchaSceneEnum。
3. 新增 CaptchaTargetTypeEnum。
4. 新增 CaptchaService。
5. 新增 CaptchaSender。
6. 新增 EmailCaptchaSender。
7. 新增 SmsCaptchaSender。
8. 新增 MockCaptchaSender。
9. Redis 存储验证码。
10. TTL 5 分钟。
11. 发送冷却 60 秒。
12. 错误次数最多 5 次。
13. 每日发送次数最多 10 次。
14. 注册成功后清理验证码。
15. test 环境使用 MockCaptchaSender。
16. prod 环境不使用 MockCaptchaSender。

任务三：认证接口
1. 新增 POST /api/auth/captcha/register。
2. 修改 POST /api/auth/register，请求体为 account、accountType、password、confirmPassword、captchaCode。
3. 修改 POST /api/auth/login，请求体为 account、password。
4. 登录支持 email。
5. 登录支持 phone。
6. 普通用户不再支持 username 登录。
7. admin 系统账号可以通过 email / phone 登录。
8. 登录成功继续返回 JWT、用户角色和权限。

任务四：前端改造
1. 登录页“用户名”改为“手机号 / 邮箱”。
2. 登录请求字段改为 account。
3. 注册页增加手机号 / 邮箱选择。
4. 注册页增加验证码输入框。
5. 注册页增加发送验证码按钮和倒计时。
6. auth.ts 增加 sendRegisterCaptchaApi。
7. README 默认账号说明改为不公开 admin 密码。

任务五：线上真实 AI 策略
1. prod 默认 AI_PROVIDER=deepseek。
2. test 默认 AI_PROVIDER=mock。
3. prod 环境如果 AI_PROVIDER=mock，应启动失败或输出明确错误。
4. prod 环境缺少 DEEPSEEK_API_KEY，应启动失败或输出明确错误。
5. MockAiClient 保留，但仅用于 test / CI。
6. deploy/.env.example 更新 AI_PROVIDER=deepseek，不要写真实 Key。

任务六：配置
1. application.yml 增加 auth.captcha 配置。
2. application-test.yml 使用 mock captcha 和 mock ai。
3. application-prod.yml 使用 real captcha 和 deepseek。
4. deploy/.env.example 增加 AUTH_CAPTCHA_MODE=real、邮件和短信配置占位。
5. 不把真实 Key 写入任何文件。

任务七：测试
至少覆盖：
1. 发送邮箱验证码成功。
2. 发送手机号验证码成功。
3. 验证码错误注册失败。
4. 邮箱注册成功。
5. 手机号注册成功。
6. 邮箱登录成功。
7. 手机号登录成功。
8. 普通用户 username 登录失败。
9. admin 邮箱登录成功。
10. JWT/RBAC 不受影响。
11. test profile 使用 MockAiClient。
12. prod profile 不允许 MockAiClient。
13. prod 缺少 DeepSeek Key 有清晰错误。

验证命令：
后端：
cd backend/intern-pilot-backend
.\gradlew.bat test --no-daemon --max-workers=1

前端：
cd frontend/intern-pilot-frontend
npm run build

输出：
1. 当前分支
2. 做了什么
3. 修改了哪些文件
4. 数据库字段如何变化
5. 新增了哪些接口
6. 验证码机制如何实现
7. 线上 AI 策略如何调整
8. Mock 现在只在哪些环境使用
9. admin/demo 如何处理
10. 前端页面如何变化
11. 新增或修改了哪些测试
12. 后端测试结果
13. 前端构建结果
14. Docker/服务器需要如何更新 .env
15. 是否建议提交 Git
16. 需要我提供什么

注意：
不要自动提交 Git。
不要 push。
不要真实发送短信。
不要真实调用 DeepSeek。
不要泄露 API Key。
```

---

## 十八、Codex 复查提示词

```text id="xnes0e"
你现在是我的 Java 全栈项目代码复查与收尾助手，请检查手机号/邮箱注册登录、验证码机制和线上真实 AI 策略的实现结果。

当前分支：
feature/auth-phone-email-captcha

请严格参考：
docs/36-auth-phone-email-captcha-real-online-design.md

重点复查：
1. 登录页是否不再显示用户名。
2. 登录接口是否使用 account + password。
3. 注册接口是否使用 account + accountType + captchaCode。
4. 是否支持邮箱注册。
5. 是否支持手机号注册。
6. 是否支持邮箱登录。
7. 是否支持手机号登录。
8. 注册是否必须验证码。
9. 验证码是否存 Redis。
10. 验证码是否有 TTL。
11. 验证码是否有发送冷却。
12. 验证码是否有错误次数限制。
13. test 环境是否使用 MockCaptchaSender。
14. prod 环境是否不使用 MockCaptchaSender。
15. MockAiClient 是否只用于 test / CI。
16. prod 是否默认 DeepSeek。
17. prod 缺少 DeepSeek Key 是否有清晰错误。
18. 是否没有真实调用 DeepSeek。
19. 是否没有真实发送短信或邮件。
20. 是否没有泄露 API Key。
21. admin 是否可通过 email / phone 登录。
22. 普通用户 username 登录是否禁用。
23. JWT 是否正常。
24. RBAC 是否正常。
25. init.sql、test-schema.sql、test-data.sql 是否同步。
26. deploy/.env.example 是否更新。
27. README 是否不公开线上 admin 密码。
28. 后端测试是否通过。
29. 前端构建是否通过。

如果发现问题，只做最小修复：
1. import / package 错误
2. SQL 字段遗漏
3. 测试失败
4. 前端类型错误
5. 配置不一致
6. 登录注册参数不一致
7. Mock 在 prod 被误启用

不要做：
1. 不要重构 RBAC。
2. 不要删除 username 字段。
3. 不要删除 MockAiClient。
4. 不要删除 MockCaptchaSender。
5. 不要真实发短信。
6. 不要真实调用 DeepSeek。
7. 不要修改无关业务模块。
8. 不要自动提交 Git。

运行：
后端：
cd backend/intern-pilot-backend
.\gradlew.bat test --no-daemon --max-workers=1

前端：
cd frontend/intern-pilot-frontend
npm run build

输出：
1. 当前分支
2. 复查了哪些文件
3. 发现的问题
4. 修复的问题
5. 是否确认手机号/邮箱注册可用
6. 是否确认手机号/邮箱登录可用
7. 是否确认验证码机制可用
8. 是否确认线上环境不再使用 Mock AI
9. 是否确认 Mock 仅用于测试
10. 是否确认 admin 登录方式可用
11. 是否确认 JWT/RBAC 未破坏
12. 后端测试结果
13. 前端构建结果
14. 是否建议提交 Git
15. 建议 commit message
16. 服务器 .env 需要怎么改
```

---

## 十九、服务器 `.env` 最终目标

这个功能上线后，你服务器 `.env` 应该类似：

```env id="x5fksv"
SPRING_PROFILES_ACTIVE=prod

AI_PROVIDER=deepseek
AI_BASE_URL=https://api.deepseek.com
DEEPSEEK_API_KEY=你的真实Key
AI_MODEL=deepseek-v4-flash
AI_PRO_MODEL=deepseek-v4-pro
AI_TIMEOUT_SECONDS=60

AUTH_CAPTCHA_MODE=real

MAIL_HOST=smtp.xxx.com
MAIL_PORT=587
MAIL_USERNAME=你的邮箱
MAIL_PASSWORD=你的邮箱授权码

SMS_PROVIDER=tencent
TENCENT_SMS_SECRET_ID=你的SecretId
TENCENT_SMS_SECRET_KEY=你的SecretKey
TENCENT_SMS_SDK_APP_ID=你的短信AppId
TENCENT_SMS_SIGN_NAME=你的短信签名
TENCENT_SMS_TEMPLATE_ID=你的短信模板
```

如果暂时不配置短信/邮箱，线上注册功能应提示：

```text
验证码服务未配置，请联系管理员。
```

但登录和已有账号使用不应受影响。

---

## 二十、总结

本阶段完成后，系统从：

```text
开发演示型账号体系：
username + password
mock AI 可演示
```

升级为：

```text
线上真实产品型账号体系：
手机号 / 邮箱注册
验证码校验
手机号 / 邮箱登录
真实 DeepSeek AI
Mock 仅用于自动化测试
```

这会更适合答辩、线上展示和后续继续迭代。

```
```
