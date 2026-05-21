# InternPilot 手机号/邮箱注册登录与验证码机制设计文档

## 一、文档目的

本文档用于指导 InternPilot 项目重构用户注册与登录体系。

当前系统使用用户名 `username` + 密码完成注册和登录。随着项目逐步接近真实产品形态，用户名登录方式不够贴近实际业务。真实求职类平台通常使用手机号或邮箱作为账号标识，并通过验证码完成注册校验、账号绑定和安全验证。

本阶段目标是：

> 将 InternPilot 的认证体系从“用户名登录”升级为“手机号 / 邮箱注册登录 + 验证码校验 + JWT 鉴权”的更真实认证体系。

本阶段不是重做 Spring Security，也不是改动 RBAC 权限系统，而是在保留现有 JWT、用户角色权限、管理员后台的基础上，对注册登录入口和用户表字段进行增强。

---

## 二、当前系统现状

### 2.1 当前认证方式

当前系统大致流程：

```text
POST /api/auth/register
  username + password
  ↓
创建用户
  ↓
绑定默认 USER 角色

POST /api/auth/login
  username + password
  ↓
认证成功
  ↓
返回 JWT token
````

### 2.2 当前存在的问题

|问题|说明|
|---|---|
|登录方式不真实|实际产品更常用手机号 / 邮箱登录|
|注册缺少验证码|无法验证手机号或邮箱归属|
|username 容易产生无意义账号|用户名对求职平台价值不大|
|后续找回密码困难|没有手机号 / 邮箱基础字段，找回密码不好做|
|安全性不足|无验证码、无发送频率限制、无验证码错误次数限制|
|演示账号依赖 username|admin / demo 账号需要兼容处理|

---

## 三、本阶段目标

### 3.1 功能目标

本阶段需要实现：

1. 支持邮箱注册
    
2. 支持手机号注册
    
3. 注册前发送验证码
    
4. 注册时校验验证码
    
5. 登录时使用手机号或邮箱，不再使用 username
    
6. 保留密码登录方式
    
7. 登录成功后继续返回 JWT
    
8. 用户注册后继续绑定 USER 角色
    
9. admin / demo 演示账号仍能登录
    
10. 验证码使用 Redis 存储
    
11. 支持验证码过期时间
    
12. 支持验证码发送频率限制
    
13. 支持验证码错误次数限制
    
14. 补充后端测试和前端构建验证
    

---

### 3.2 非目标

本阶段暂不做：

1. 不做微信 / QQ / GitHub OAuth 登录
    
2. 不做扫码登录
    
3. 不做短信服务真实上线强依赖
    
4. 不做复杂风控系统
    
5. 不做图形验证码
    
6. 不做多因素认证 MFA
    
7. 不重构 RBAC
    
8. 不重构 JWT Filter
    
9. 不强制删除数据库中的 username 字段
    
10. 不做前端复杂登录动效
    

---

## 四、推荐实现策略

### 4.1 总体策略

采用渐进式改造：

```text
第一步：用户表增加 phone / email 字段
第二步：新增验证码发送与校验模块
第三步：注册接口改为 phone/email + code + password
第四步：登录接口改为 account + password
第五步：兼容旧 admin/demo 数据
第六步：前端登录注册页面改造
第七步：补充测试
```

---

### 4.2 为什么不直接删除 username

不建议直接删除 `username` 字段。

原因：

1. 历史数据可能依赖 username
    
2. Spring Security 的 UserDetails 可能依赖 username
    
3. admin / demo 演示账号可能依赖 username
    
4. 管理后台用户列表可能展示 username
    
5. 直接删除字段风险较高
    

建议改为：

```text
username 继续保留，但不再作为主要登录输入
phone / email 成为真实账号标识
```

username 可以自动生成：

```text
邮箱注册：username = 邮箱前缀
手机号注册：username = user_手机号后四位_随机数
```

---

## 五、用户账号模型设计

### 5.1 用户表字段增强

当前用户表需要增加字段：

|字段|类型|说明|
|---|---|---|
|phone|varchar(20)|手机号，可为空，唯一|
|email|varchar(100)|邮箱，可为空，唯一|
|account_type|varchar(20)|注册方式：PHONE / EMAIL / USERNAME / SYSTEM|
|phone_verified|tinyint|手机号是否已验证|
|email_verified|tinyint|邮箱是否已验证|
|last_login_time|datetime|最近登录时间，可选|

---

### 5.2 SQL 示例

需要根据当前项目真实 `user` 表名调整。如果表名是 `sys_user` 或 `user`，以实际为准。

```sql
ALTER TABLE user
    ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号',
    ADD COLUMN email VARCHAR(100) NULL COMMENT '邮箱',
    ADD COLUMN account_type VARCHAR(20) DEFAULT 'USERNAME' COMMENT '账号类型',
    ADD COLUMN phone_verified TINYINT DEFAULT 0 COMMENT '手机号是否验证',
    ADD COLUMN email_verified TINYINT DEFAULT 0 COMMENT '邮箱是否验证',
    ADD COLUMN last_login_time DATETIME NULL COMMENT '最近登录时间';

CREATE UNIQUE INDEX uk_user_phone ON user(phone);
CREATE UNIQUE INDEX uk_user_email ON user(email);
```

如果 MySQL 不允许多个 NULL 唯一值异常，需要确认实际行为。MySQL 的 UNIQUE 索引允许多个 NULL，一般可接受。

---

### 5.3 admin / demo 数据兼容

初始化数据建议：

|username|phone|email|account_type|说明|
|---|---|---|---|---|
|admin|13800000000|[admin@internpilot.local](mailto:admin@internpilot.local)|SYSTEM|管理员|
|demo|13900000000|[demo@internpilot.local](mailto:demo@internpilot.local)|SYSTEM|演示用户|

这样登录时可以支持：

```text
admin@internpilot.local / 123456
demo@internpilot.local / 123456
13800000000 / 123456
13900000000 / 123456
```

是否继续支持 `admin / 123456`、`demo / 123456` 有两种方案。

### 方案 A：完全不再支持 username 登录

优点：

```text
符合“登录时不要使用用户名”的目标
```

缺点：

```text
README 默认账号要改成邮箱或手机号
```

### 方案 B：仅对 SYSTEM 账号兼容 username 登录

优点：

```text
不破坏 admin / demo 演示习惯
```

缺点：

```text
登录逻辑略复杂
```

推荐方案：

```text
普通用户不支持 username 登录；
admin/demo 这类系统演示账号可临时兼容 username 登录；
README 推荐使用邮箱/手机号登录。
```

---

## 六、验证码机制设计

### 6.1 验证码类型

支持两类验证码：

|类型|场景|发送方式|
|---|---|---|
|EMAIL_REGISTER|邮箱注册|邮件|
|PHONE_REGISTER|手机号注册|短信或 Mock|
|EMAIL_RESET_PASSWORD|找回密码，后续扩展|邮件|
|PHONE_RESET_PASSWORD|找回密码，后续扩展|短信或 Mock|

本阶段优先实现：

```text
EMAIL_REGISTER
PHONE_REGISTER
```

---

### 6.2 验证码存储

使用 Redis 存储验证码。

Key 设计：

```text
auth:captcha:{scene}:{target}
```

示例：

```text
auth:captcha:EMAIL_REGISTER:demo@example.com
auth:captcha:PHONE_REGISTER:13800000000
```

Value 建议存 JSON：

```json
{
  "code": "123456",
  "scene": "EMAIL_REGISTER",
  "target": "demo@example.com",
  "expireAt": "2026-05-17T20:00:00",
  "failCount": 0
}
```

也可以简化为只存验证码字符串，错误次数单独 key。

---

### 6.3 Redis Key 设计

|Key|用途|TTL|
|---|---|---|
|auth:captcha:{scene}:{target}|验证码内容|5 分钟|
|auth:captcha:cooldown:{scene}:{target}|发送冷却|60 秒|
|auth:captcha:fail:{scene}:{target}|错误次数|5 分钟|
|auth:captcha:daily:{scene}:{target}|每日发送次数|24 小时|

---

### 6.4 发送限制

|限制|建议|
|---|---|
|验证码长度|6 位数字|
|有效期|5 分钟|
|同一账号发送间隔|60 秒|
|同一账号每日发送次数|10 次|
|验证错误次数|5 次|
|错误超限处理|验证码失效，需要重新发送|

---

### 6.5 验证码发送器设计

定义统一接口：

```java
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

推荐配置：

|环境|发送器|
|---|---|
|dev|MockCaptchaSender|
|test|MockCaptchaSender|
|prod|EmailCaptchaSender + SmsCaptchaSender|
|docker demo|MockCaptchaSender|

---

### 6.6 为什么保留 MockCaptchaSender

必须保留 Mock。

原因：

1. 本地开发不应该依赖真实短信费用
    
2. 自动化测试不能真实发短信或邮件
    
3. Docker 演示环境可以不用配置 SMTP / SMS
    
4. 老师检查时可以直接看到验证码
    
5. 避免第三方服务不可用导致注册无法演示
    

Mock 模式下可以：

```text
后端日志打印验证码
或固定验证码为 123456
```

推荐开发环境：

```text
固定验证码 123456
```

生产环境：

```text
随机 6 位验证码
```

---

## 七、接口设计

### 7.1 发送注册验证码

```http
POST /api/auth/captcha/register
```

请求体：

```json
{
  "target": "demo@example.com",
  "type": "EMAIL"
}
```

或：

```json
{
  "target": "13800000000",
  "type": "PHONE"
}
```

字段说明：

|字段|说明|
|---|---|
|target|手机号或邮箱|
|type|PHONE / EMAIL|

响应：

```json
{
  "code": 200,
  "message": "验证码已发送"
}
```

异常：

|场景|响应|
|---|---|
|邮箱格式错误|400|
|手机号格式错误|400|
|账号已注册|400|
|发送过于频繁|429|
|今日次数超限|429|
|短信/邮件发送失败|500 或业务错误码|

---

### 7.2 手机号/邮箱注册

```http
POST /api/auth/register
```

请求体：

```json
{
  "account": "demo@example.com",
  "accountType": "EMAIL",
  "password": "123456",
  "confirmPassword": "123456",
  "captchaCode": "123456"
}
```

手机号注册：

```json
{
  "account": "13800000000",
  "accountType": "PHONE",
  "password": "123456",
  "confirmPassword": "123456",
  "captchaCode": "123456"
}
```

响应：

```json
{
  "code": 200,
  "message": "注册成功"
}
```

注册成功后：

```text
创建用户
加密密码
设置 phone/email
设置 verified = true
绑定 USER 角色
清理验证码
```

---

### 7.3 手机号/邮箱登录

```http
POST /api/auth/login
```

请求体：

```json
{
  "account": "demo@example.com",
  "password": "123456"
}
```

或：

```json
{
  "account": "13800000000",
  "password": "123456"
}
```

响应：

```json
{
  "token": "Bearer xxx",
  "user": {
    "id": 1,
    "username": "demo",
    "phone": "13900000000",
    "email": "demo@internpilot.local",
    "roles": ["USER"],
    "permissions": []
  }
}
```

---

### 7.4 当前用户信息

```http
GET /api/user/me
```

需要补充返回：

```json
{
  "id": 1,
  "username": "demo",
  "phone": "13900000000",
  "email": "demo@internpilot.local",
  "roles": ["USER"],
  "permissions": []
}
```

---

## 八、DTO / VO 设计

### 8.1 SendRegisterCaptchaRequest

路径建议：

```text
dto/auth/SendRegisterCaptchaRequest.java
```

字段：

```java
private String target;
private AccountTypeEnum type;
```

校验：

```text
target 不能为空
type 不能为空
EMAIL 时校验邮箱格式
PHONE 时校验手机号格式
```

---

### 8.2 RegisterRequest

原来的 `UserRegisterRequest` 或 `RegisterRequest` 改造为：

```java
private String account;
private AccountTypeEnum accountType;
private String password;
private String confirmPassword;
private String captchaCode;
```

---

### 8.3 LoginRequest

原来的：

```java
private String username;
private String password;
```

改为：

```java
private String account;
private String password;
```

前端登录页不再显示“用户名”，改成：

```text
手机号 / 邮箱
```

---

### 8.4 LoginResponse

补充：

```java
private String phone;
private String email;
private String accountType;
```

---

## 九、枚举设计

### 9.1 AccountTypeEnum

```java
public enum AccountTypeEnum {
    PHONE,
    EMAIL,
    USERNAME,
    SYSTEM
}
```

---

### 9.2 CaptchaSceneEnum

```java
public enum CaptchaSceneEnum {
    PHONE_REGISTER,
    EMAIL_REGISTER
}
```

---

### 9.3 CaptchaTargetTypeEnum

```java
public enum CaptchaTargetTypeEnum {
    PHONE,
    EMAIL
}
```

---

## 十、Service 设计

### 10.1 AuthService

新增方法：

```java
void sendRegisterCaptcha(SendRegisterCaptchaRequest request);

void register(RegisterRequest request);

LoginResponse login(LoginRequest request);
```

---

### 10.2 CaptchaService

新增：

```text
service/auth/CaptchaService.java
service/auth/impl/CaptchaServiceImpl.java
```

接口：

```java
void sendRegisterCaptcha(String target, AccountTypeEnum type);

void verifyRegisterCaptcha(String target, AccountTypeEnum type, String code);

void clearRegisterCaptcha(String target, AccountTypeEnum type);
```

---

### 10.3 CaptchaSender

路径：

```text
service/auth/CaptchaSender.java
service/auth/impl/MockCaptchaSender.java
service/auth/impl/EmailCaptchaSender.java
service/auth/impl/SmsCaptchaSender.java
```

也可以单独放到：

```text
auth/captcha/
```

如果后端包结构已经按业务模块整理，则推荐：

```text
service/auth/captcha/CaptchaSender.java
service/auth/captcha/impl/MockCaptchaSender.java
```

---

## 十一、登录认证逻辑设计

### 11.1 账号识别

登录时只传：

```text
account + password
```

后端自动判断：

```text
如果 account 是邮箱格式 → 按 email 查询
如果 account 是手机号格式 → 按 phone 查询
如果 account 是 admin/demo 且为系统账号 → 可选兼容 username 查询
否则返回账号或密码错误
```

---

### 11.2 查询逻辑

UserService 增加：

```java
User findByLoginAccount(String account);
```

伪代码：

```java
if (isEmail(account)) {
    return userMapper.selectByEmail(account);
}

if (isPhone(account)) {
    return userMapper.selectByPhone(account);
}

if (isSystemUsername(account)) {
    return userMapper.selectByUsername(account);
}

throw new BadCredentialsException("账号或密码错误");
```

---

### 11.3 安全提示

登录失败统一返回：

```text
账号或密码错误
```

不要分别提示：

```text
手机号不存在
邮箱不存在
密码错误
```

避免账号枚举风险。

---

## 十二、数据库初始化设计

### 12.1 init.sql

需要同步：

1. user 表字段
    
2. admin/demo 的 phone/email
    
3. 默认账号说明
    
4. 保留 RBAC 初始化
    

示例：

```sql
UPDATE user
SET
    phone = '13800000000',
    email = 'admin@internpilot.local',
    account_type = 'SYSTEM',
    phone_verified = 1,
    email_verified = 1
WHERE username = 'admin';

UPDATE user
SET
    phone = '13900000000',
    email = 'demo@internpilot.local',
    account_type = 'SYSTEM',
    phone_verified = 1,
    email_verified = 1
WHERE username = 'demo';
```

---

### 12.2 dev-init-rbac.sql

同步字段和演示账号。

---

### 12.3 test-schema.sql

测试表结构必须增加同样字段。

---

### 12.4 test-data.sql

测试用户需要补充 phone/email。

---

## 十三、前端页面改造

### 13.1 登录页

原字段：

```text
用户名
密码
```

改为：

```text
手机号 / 邮箱
密码
```

示例：

```text
手机号或邮箱：demo@internpilot.local
密码：123456
```

登录接口请求：

```ts
{
  account: form.account,
  password: form.password
}
```

---

### 13.2 注册页

注册页改为：

```text
注册方式：邮箱 / 手机号
手机号或邮箱输入框
验证码输入框
发送验证码按钮
密码
确认密码
注册按钮
```

按钮交互：

1. 点击发送验证码
    
2. 校验手机号/邮箱格式
    
3. 请求后端发送验证码
    
4. 开始 60 秒倒计时
    
5. 输入验证码和密码后注册
    

---

### 13.3 默认账号提示

登录页可以显示：

```text
演示账号：
demo@internpilot.local / 123456
admin@internpilot.local / 123456
```

或者 README 显示即可，登录页不一定要显示。

---

## 十四、前端 API 改造

### 14.1 auth.ts

新增：

```ts
export function sendRegisterCaptchaApi(data) {
  return request.post('/api/auth/captcha/register', data)
}
```

修改登录：

```ts
export function loginApi(data) {
  return request.post('/api/auth/login', data)
}
```

data 改为：

```ts
{
  account: string
  password: string
}
```

修改注册：

```ts
{
  account: string
  accountType: 'PHONE' | 'EMAIL'
  password: string
  confirmPassword: string
  captchaCode: string
}
```

---

## 十五、配置设计

### 15.1 application.yml

```yaml
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

### 15.2 application-dev.yml

```yaml
auth:
  captcha:
    mode: mock
```

---

### 15.3 application-test.yml

```yaml
auth:
  captcha:
    mode: mock
```

---

### 15.4 application-prod.yml

```yaml
auth:
  captcha:
    mode: ${AUTH_CAPTCHA_MODE:mock}
```

生产部署时，如果暂时没有短信/邮件服务，也可以保持：

```env
AUTH_CAPTCHA_MODE=mock
```

用于课程演示。

---

## 十六、邮件与短信服务设计

### 16.1 邮箱验证码

可选实现：

```text
JavaMailSender
SMTP
```

配置：

```yaml
spring:
  mail:
    host: ${MAIL_HOST:}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
```

---

### 16.2 手机验证码

真实短信服务可以后续接：

```text
腾讯云 SMS
阿里云 SMS
短信宝
其他短信服务商
```

本阶段为了演示，不强制接真实短信服务。

推荐：

```text
mock 模式固定验证码 123456
真实短信作为后续增强
```

---

## 十七、后端测试设计

### 17.1 CaptchaServiceTest

测试：

|用例|预期|
|---|---|
|发送邮箱验证码成功|Redis 写入|
|发送手机号验证码成功|Redis 写入|
|60 秒内重复发送|抛出发送频繁|
|验证码错误|failCount 增加|
|验证码错误超过次数|验证码失效|
|验证码过期|校验失败|
|验证码正确|校验成功并可清理|

---

### 17.2 AuthControllerTest

测试：

|用例|预期|
|---|---|
|发送邮箱验证码|200|
|发送手机号验证码|200|
|邮箱格式错误|400|
|手机号格式错误|400|
|邮箱注册成功|200|
|手机号注册成功|200|
|验证码错误注册失败|400|
|邮箱重复注册失败|400|
|手机号重复注册失败|400|
|邮箱登录成功|返回 token|
|手机号登录成功|返回 token|
|密码错误登录失败|401|
|username 普通用户登录失败|401|
|admin/demo 系统账号兼容登录|视方案而定|

---

### 17.3 CustomUserDetailsServiceTest

测试：

1. 根据 email 加载用户
    
2. 根据 phone 加载用户
    
3. 系统账号根据 username 加载
    
4. 不存在账号抛异常
    
5. 禁用用户不能登录
    

---

## 十八、前端测试与构建

本阶段至少保证：

```bash
npm run build
```

通过。

如果项目已有前端测试，可补充：

1. 登录表单字段校验
    
2. 注册发送验证码按钮倒计时
    
3. 注册表单校验
    
4. auth.ts API 参数正确
    

---

## 十九、兼容性设计

### 19.1 与 JWT 兼容

JWT 中 subject 可以继续使用 userId 或 username。

推荐：

```text
JWT subject 使用 userId
```

如果当前项目已经使用 username，也可以暂时不改，避免扩大影响。

---

### 19.2 与 RBAC 兼容

RBAC 不需要改。

用户注册成功后仍然绑定 USER 角色。

---

### 19.3 与管理员后台兼容

用户列表新增显示：

```text
手机号
邮箱
账号类型
是否验证
```

管理员可以查看，但本阶段不一定要支持修改手机号/邮箱。

---

### 19.4 与演示数据兼容

README 默认账号改为：

```text
管理员：
admin@internpilot.local / 123456
或 13800000000 / 123456

普通用户：
demo@internpilot.local / 123456
或 13900000000 / 123456
```

---

## 二十、安全设计

### 20.1 密码安全

保持：

```text
BCryptPasswordEncoder
```

---

### 20.2 验证码安全

必须有：

1. TTL
    
2. 发送冷却
    
3. 错误次数限制
    
4. 每日发送限制
    
5. 注册成功后删除验证码
    
6. 验证码不要返回给前端
    

Mock 模式除外，Mock 可以通过日志提示或固定验证码。

---

### 20.3 错误提示

登录失败统一：

```text
账号或密码错误
```

注册失败可以明确提示：

```text
验证码错误
验证码已过期
该邮箱已注册
该手机号已注册
```

---

## 二十一、API 兼容策略

### 21.1 是否保留旧字段 username

接口层不建议继续接收 username。

旧：

```json
{
  "username": "demo",
  "password": "123456"
}
```

新：

```json
{
  "account": "demo@internpilot.local",
  "password": "123456"
}
```

前端必须同步修改。

---

### 21.2 是否保留旧接口路径

保留路径：

```text
POST /api/auth/login
POST /api/auth/register
```

只改请求体字段，不改路径。

这样前端改动较少，API 路径也更稳定。

---

## 二十二、实施步骤

### 阶段 1：数据库字段增强

任务：

1. 修改 user entity
    
2. 修改 init.sql
    
3. 修改 dev-init-rbac.sql
    
4. 修改 test-schema.sql
    
5. 修改 test-data.sql
    
6. 确认 admin/demo 有 phone/email
    

验证：

```bash
./gradlew compileJava
```

---

### 阶段 2：验证码模块

任务：

1. 新增 CaptchaSceneEnum
    
2. 新增 AccountTypeEnum
    
3. 新增 CaptchaService
    
4. 新增 CaptchaSender
    
5. 新增 MockCaptchaSender
    
6. Redis 存储验证码
    
7. 发送频率和错误次数限制
    

验证：

```bash
./gradlew test --tests "*Captcha*"
```

---

### 阶段 3：注册接口改造

任务：

1. 修改 RegisterRequest
    
2. 修改 AuthService.register
    
3. 注册时校验验证码
    
4. 注册时写入 phone/email
    
5. 注册后绑定 USER 角色
    
6. 注册成功后清理验证码
    

验证：

```bash
./gradlew test --tests "*AuthControllerTest*"
```

---

### 阶段 4：登录接口改造

任务：

1. 修改 LoginRequest
    
2. 修改 AuthService.login
    
3. 修改 CustomUserDetailsService 或 UserService 查询逻辑
    
4. 支持 email 登录
    
5. 支持 phone 登录
    
6. 可选支持 SYSTEM 账号 username 兼容
    

验证：

```bash
./gradlew test --tests "*Auth*"
```

---

### 阶段 5：前端登录注册页面

任务：

1. 登录页字段改为手机号/邮箱
    
2. 注册页增加注册方式选择
    
3. 注册页增加验证码发送
    
4. 注册页增加倒计时
    
5. auth.ts API 参数同步
    
6. 登录成功后的权限恢复逻辑保持不变
    

验证：

```bash
npm run build
```

---

### 阶段 6：README 和演示数据

任务：

1. README 默认账号更新
    
2. Docker `.env.example` 增加 AUTH_CAPTCHA_MODE
    
3. 演示说明增加 mock 验证码
    
4. Docker 初始化数据同步
    

---

## 二十三、验收标准

本阶段完成后，需要满足：

1. 可以发送邮箱注册验证码
    
2. 可以发送手机号注册验证码
    
3. 邮箱验证码注册成功
    
4. 手机号验证码注册成功
    
5. 验证码错误无法注册
    
6. 验证码过期无法注册
    
7. 重复邮箱不能注册
    
8. 重复手机号不能注册
    
9. 邮箱 + 密码可以登录
    
10. 手机号 + 密码可以登录
    
11. 登录页不再显示用户名
    
12. 注册页有验证码发送按钮
    
13. demo 演示账号仍可登录
    
14. admin 管理员账号仍可登录
    
15. JWT 鉴权正常
    
16. RBAC 权限正常
    
17. 后端测试通过
    
18. 前端构建通过
    
19. Docker 启动不报错
    
20. README 默认账号说明已更新
    

---

## 二十四、推荐 Git 分支

```bash
git checkout dev
git pull origin dev
git checkout -b feature/auth-phone-email-captcha
```

提交建议：

```bash
git commit -m "feat: support phone and email registration with captcha"
```

---


## 二十七、总结

本阶段完成后，InternPilot 的认证体系将从：

```text
用户名 + 密码
```

升级为：

```text
手机号 / 邮箱 + 验证码注册
手机号 / 邮箱 + 密码登录
JWT 鉴权
RBAC 权限控制
```

这会让项目更接近真实招聘平台、实习投递平台和企业级前后端分离系统。

```

---

我建议这个功能排到 `v1.1.0`，因为它会影响登录、注册、测试数据、Docker 初始化数据和 README 默认账号。不要直接在 `main` 上改，单独开 `feature/auth-phone-email-captcha` 最稳。
```