下面这份直接保存为：

```text
docs/29-cicd-and-online-demo.md
```

---

````markdown
# InternPilot CI/CD 自动化与在线演示部署文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的 CI/CD 自动化与在线演示部署方案，包括 GitHub Actions 工作流、后端自动测试、前端自动构建、Docker 镜像构建、服务器部署、环境变量配置、在线演示地址准备、安全注意事项和最终验收标准。

当前 InternPilot 已经具备：

```text
用户认证与 JWT 鉴权
RBAC 权限系统
简历上传与解析
简历版本管理
岗位 JD 管理
AI 匹配分析
WebSocket AI 分析进度
AI 面试题生成
岗位推荐
RAG 岗位知识库
投递记录管理
系统操作日志
管理员后台
单元测试与集成测试
````

CI/CD 和在线演示部署的目标是让项目从：

```text
本地能运行
```

升级为：

```text
GitHub 可展示
自动测试可验证
Docker 可部署
在线地址可访问
面试时可演示
```

---

# 2. 为什么要做 CI/CD 和在线演示

对于实习项目来说，代码能跑是一方面，能不能展示也很重要。

如果项目只有本地代码，面试官通常只能看 README 和截图。

如果有在线演示地址，则可以展示：

```text
真实登录页面
真实业务流程
Swagger 接口文档
AI 分析页面
管理员后台
操作日志
岗位推荐
```

CI/CD 的价值是：

```text
每次 push 自动运行测试
每次 push 自动构建前端
每次 push 自动构建后端
发现错误更早
项目更像真实工程
```

---

# 3. CI/CD 阶段目标

第一阶段目标：

1. GitHub push 后自动运行后端测试；
    
2. GitHub push 后自动运行前端 build；
    
3. 后端测试失败时 CI 失败；
    
4. 前端 build 失败时 CI 失败；
    
5. main 分支保持可构建状态；
    
6. Docker Compose 可以一键部署；
    
7. 准备在线演示环境变量；
    
8. 准备演示账号；
    
9. README 中加入在线演示说明；
    
10. 面试时可以稳定打开项目。
    

---

# 4. 推荐部署架构

## 4.1 第一阶段推荐架构

```text
用户浏览器
  ↓
Nginx
  ↓
前端静态文件 dist
  ↓
后端 Spring Boot API
  ↓
MySQL
  ↓
Redis
```

如果使用 Docker Compose：

```text
docker-compose
  ├── mysql
  ├── redis
  ├── backend
  └── nginx / frontend
```

---

## 4.2 端口规划

|服务|容器端口|宿主机端口|
|---|---|---|
|MySQL|3306|不建议公网暴露|
|Redis|6379|不建议公网暴露|
|Backend|8080|8080 或仅内网|
|Frontend / Nginx|80|80|
|HTTPS|443|443|

---

# 5. 分支策略

## 5.1 推荐分支

```text
main        稳定分支
dev         开发分支
feature/*   功能分支
fix/*       修复分支
```

---

## 5.2 提交流程

```text
feature 分支开发
  ↓
本地测试
  ↓
提交 Pull Request
  ↓
GitHub Actions 自动测试
  ↓
测试通过
  ↓
合并到 main
```

个人项目可以简化为：

```text
本地开发
  ↓
git push
  ↓
GitHub Actions 自动测试
  ↓
main 保持可运行
```

---

# 6. GitHub Actions 目录结构

在项目根目录创建：

```text
.github
└── workflows
    ├── backend-ci.yml
    ├── frontend-ci.yml
    └── docker-build.yml
```

如果想简单一些，也可以只创建一个：

```text
.github/workflows/ci.yml
```

第一阶段推荐一个文件即可。

---

# 7. GitHub Actions：完整 CI

## 7.1 ci.yml

路径：

```text
.github/workflows/ci.yml
```

内容：

```yaml
name: InternPilot CI

on:
  push:
    branches:
      - main
      - dev
  pull_request:
    branches:
      - main
      - dev

jobs:
  backend-test:
    name: Backend Test
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: backend/intern-pilot-backend

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run backend tests
        run: ./gradlew clean test --no-daemon

  frontend-build:
    name: Frontend Build
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: frontend/intern-pilot-frontend

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: frontend/intern-pilot-frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Build frontend
        run: npm run build
```

---

## 7.2 说明

这个 CI 会做两件事：

```text
后端：
./gradlew clean test

前端：
npm ci
npm run build
```

如果任意一步失败，GitHub Actions 会显示失败。

这能证明：

```text
后端测试能过
前端能构建
项目没有明显编译错误
```

---

# 8. 后端测试环境注意事项

CI 中不能依赖你本机的：

```text
MySQL
Redis
AI API Key
本地 application-dev.yml
```

所以测试必须满足：

```text
使用 application-test.yml
使用 H2 或 Mock 数据库
使用 MockAiClient
不要调用真实 AI
不要依赖真实 Redis
```

推荐在测试类上使用：

```java
@ActiveProfiles("test")
```

---

# 9. 前端 CI 注意事项

## 9.1 package-lock.json 必须提交

前端使用 `npm ci` 时，需要有：

```text
package-lock.json
```

所以这个文件应该提交到 GitHub。

不应该提交：

```text
node_modules
dist
```

---

## 9.2 前端环境变量

CI build 时，如果前端代码引用：

```text
VITE_API_BASE_URL
```

最好提供默认值，或者在 GitHub Actions 中设置：

```yaml
env:
  VITE_API_BASE_URL: http://localhost:8080
```

也可以在前端根目录提供：

```text
.env.production.example
```

---

# 10. Docker 构建工作流

## 10.1 docker-build.yml

如果你希望 GitHub Actions 测试 Docker 是否能构建，可以新增：

```text
.github/workflows/docker-build.yml
```

内容：

```yaml
name: Docker Build Check

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main

jobs:
  docker-build:
    name: Docker Compose Build
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Build Docker Compose services
        run: docker compose -f deploy/docker-compose.yml build
```

---

## 10.2 说明

这个工作流只检查：

```text
Dockerfile 是否能构建
docker-compose.yml 是否有明显错误
```

不一定启动完整服务。

如果要启动并健康检查，可以后续扩展。

---

# 11. Dockerfile 设计建议

## 11.1 后端 Dockerfile

路径：

```text
backend/intern-pilot-backend/Dockerfile
```

推荐：

```dockerfile
FROM gradle:8.14.4-jdk17 AS builder

WORKDIR /app

COPY . .

RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 11.2 前端 Dockerfile

路径：

```text
frontend/intern-pilot-frontend/Dockerfile
```

推荐：

```dockerfile
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./

RUN npm ci

COPY . .

RUN npm run build

FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
```

---

# 12. 前端 Nginx 配置

路径：

```text
frontend/intern-pilot-frontend/nginx.conf
```

内容：

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /ws/ {
        proxy_pass http://backend:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

---

# 13. docker-compose.yml 设计

路径：

```text
deploy/docker-compose.yml
```

示例：

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: internpilot-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - internpilot-mysql-data:/var/lib/mysql
      - ../backend/intern-pilot-backend/src/main/resources/sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    command:
      --default-authentication-plugin=mysql_native_password
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
    networks:
      - internpilot-network

  redis:
    image: redis:7-alpine
    container_name: internpilot-redis
    restart: always
    command: redis-server --requirepass ${REDIS_PASSWORD}
    ports:
      - "6379:6379"
    volumes:
      - internpilot-redis-data:/data
    networks:
      - internpilot-network

  backend:
    build:
      context: ../backend/intern-pilot-backend
      dockerfile: Dockerfile
    container_name: internpilot-backend
    restart: always
    depends_on:
      - mysql
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USERNAME: root
      MYSQL_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      AI_PROVIDER: ${AI_PROVIDER}
      AI_API_KEY: ${AI_API_KEY}
      AI_BASE_URL: ${AI_BASE_URL}
      AI_MODEL: ${AI_MODEL}
    ports:
      - "8080:8080"
    volumes:
      - internpilot-uploads:/app/uploads
    networks:
      - internpilot-network

  frontend:
    build:
      context: ../frontend/intern-pilot-frontend
      dockerfile: Dockerfile
    container_name: internpilot-frontend
    restart: always
    depends_on:
      - backend
    ports:
      - "80:80"
    networks:
      - internpilot-network

volumes:
  internpilot-mysql-data:
  internpilot-redis-data:
  internpilot-uploads:

networks:
  internpilot-network:
    driver: bridge
```

---

# 14. 生产配置文件

## 14.1 application-prod.yml

路径：

```text
backend/intern-pilot-backend/src/main/resources/application-prod.yml
```

推荐：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:mysql}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:intern_pilot}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      database: ${REDIS_DATABASE:0}

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}

file:
  upload-dir: uploads/resumes
  max-size: 10485760
  allowed-types:
    - pdf
    - docx

ai:
  provider: ${AI_PROVIDER:deepseek}
  api-key: ${AI_API_KEY:}
  base-url: ${AI_BASE_URL:https://api.deepseek.com}
  model: ${AI_MODEL:deepseek-chat}
  timeout: ${AI_TIMEOUT:60000}

rag:
  enabled: ${RAG_ENABLED:true}
  top-k: ${RAG_TOP_K:5}
  embedding:
    provider: ${RAG_EMBEDDING_PROVIDER:mock}
    model: ${RAG_EMBEDDING_MODEL:mock-embedding-64}
```

---

## 14.2 注意

`application-prod.yml` 可以提交，但里面不能写真实密钥。

真实值放到：

```text
.env
```

或者服务器环境变量中。

---

# 15. .env.example

路径：

```text
deploy/.env.example
```

内容：

```env
MYSQL_ROOT_PASSWORD=change_me_mysql_password
MYSQL_DATABASE=intern_pilot

REDIS_PASSWORD=change_me_redis_password

JWT_SECRET=change_me_to_a_very_long_random_secret_at_least_32_chars

AI_PROVIDER=deepseek
AI_API_KEY=change_me_ai_api_key
AI_BASE_URL=https://api.deepseek.com
AI_MODEL=deepseek-chat

RAG_ENABLED=true
RAG_TOP_K=5
RAG_EMBEDDING_PROVIDER=mock
RAG_EMBEDDING_MODEL=mock-embedding-64
```

---

# 16. .gitignore 检查

必须忽略：

```gitignore
# env
.env
*.env.local

# backend
backend/intern-pilot-backend/build/
backend/intern-pilot-backend/.gradle/
backend/intern-pilot-backend/uploads/
backend/intern-pilot-backend/*.log

# frontend
frontend/intern-pilot-frontend/node_modules/
frontend/intern-pilot-frontend/dist/
frontend/intern-pilot-frontend/.env.local

# system
.DS_Store
Thumbs.db
```

可以提交：

```text
.env.example
application-dev.yml 如果没有真实密钥
application-prod.yml 如果没有真实密钥
```

不要提交：

```text
真实 .env
真实 API Key
真实数据库密码
真实 JWT_SECRET
```

---

# 17. 服务器部署流程

## 17.1 服务器最低配置

推荐最低：

```text
2 核 CPU
2GB 内存
40GB 磁盘
Ubuntu 22.04
Docker
Docker Compose
```

如果 AI 请求走外部 API，服务器压力不大。

---

## 17.2 安装 Docker

Ubuntu：

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
```

检查：

```bash
docker --version
docker compose version
```

---

## 17.3 上传项目

方式一：Git clone

```bash
git clone https://github.com/你的用户名/InternPilot.git
cd InternPilot
```

方式二：上传压缩包

```bash
unzip InternPilot.zip
cd InternPilot
```

---

## 17.4 配置环境变量

```bash
cd deploy
cp .env.example .env
nano .env
```

修改：

```env
MYSQL_ROOT_PASSWORD=强密码
REDIS_PASSWORD=强密码
JWT_SECRET=至少32位随机字符串
AI_API_KEY=你的AI Key
```

---

## 17.5 启动服务

```bash
docker compose up -d --build
```

查看：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f backend
```

---

## 17.6 健康检查

```bash
curl http://localhost:8080/api/health
```

浏览器访问：

```text
http://服务器IP
```

Swagger：

```text
http://服务器IP/doc.html
```

---

# 18. HTTPS 与域名

## 18.1 第一阶段

第一阶段可以先使用：

```text
http://服务器IP
```

用于演示已经足够。

---

## 18.2 后续正式演示

建议准备域名：

```text
internpilot.example.com
```

然后使用：

```text
Nginx
Let's Encrypt
Certbot
```

配置 HTTPS。

---

# 19. 在线演示安全策略

在线演示环境必须注意安全。

## 19.1 演示账号

准备普通用户：

```text
username: demo
password: Demo@123456
```

准备管理员用户：

```text
username: admin_demo
password: AdminDemo@123456
```

---

## 19.2 管理员账号风险

如果给面试官演示管理员账号，建议：

```text
只展示页面
不要公开写在 README
不要给陌生人管理员密码
管理员账号权限可以降低
```

README 中可以写：

```text
如需管理员演示账号，请联系作者。
```

---

## 19.3 AI Key 保护

后端调用 AI，前端不能出现：

```text
AI_API_KEY
```

AI Key 只能存在：

```text
服务器 .env
服务器环境变量
```

---

## 19.4 数据库和 Redis 不要公网暴露

生产环境不建议暴露：

```text
3306
6379
```

如果只是 Docker 内部通信，可以去掉端口映射：

```yaml
# ports:
#   - "3306:3306"
```

---

# 20. README 在线演示部分

README 中可以加入：

````markdown
## 在线演示

- 前端演示地址：http://你的服务器IP
- 接口文档地址：http://你的服务器IP/doc.html

### 演示账号

普通用户：

```text
username: demo
password: Demo@123456
````

管理员账号请联系作者获取。

### 演示功能

- 用户注册 / 登录
    
- 简历上传与解析
    
- 简历版本管理
    
- 岗位 JD 管理
    
- AI 匹配分析
    
- AI 面试题生成
    
- 岗位推荐
    
- 投递记录管理
    
- 管理员后台
    
- 操作日志
    

````

---

# 21. GitHub Secrets 配置

如果后续要自动部署到服务器，可以在 GitHub 仓库中配置 Secrets：

```text
Settings
  ↓
Secrets and variables
  ↓
Actions
````

添加：

```text
SERVER_HOST
SERVER_USER
SERVER_SSH_KEY
DEPLOY_PATH
```

如果要构建并推送 Docker 镜像：

```text
DOCKER_USERNAME
DOCKER_PASSWORD
```

---

# 22. 自动部署工作流

## 22.1 deploy.yml 示例

路径：

```text
.github/workflows/deploy.yml
```

示例：

```yaml
name: Deploy InternPilot

on:
  push:
    branches:
      - main

jobs:
  deploy:
    name: Deploy to Server
    runs-on: ubuntu-latest

    steps:
      - name: Deploy with SSH
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SERVER_SSH_KEY }}
          script: |
            cd ${{ secrets.DEPLOY_PATH }}
            git pull
            cd deploy
            docker compose down
            docker compose up -d --build
            docker image prune -f
```

---

## 22.2 自动部署风险

自动部署虽然方便，但有风险：

```text
main 分支一推就上线
错误代码可能直接影响演示环境
数据库迁移失败可能导致服务异常
```

第一阶段建议：

```text
先只做 CI
手动部署
稳定后再做自动部署
```

---

# 23. 数据初始化

## 23.1 init.sql 应包含

```text
基础表结构
基础角色
基础权限
ADMIN 角色权限
演示管理员账号
可选演示岗位数据
可选 RAG 知识样例
```

---

## 23.2 演示数据建议

准备：

```text
demo 用户
admin_demo 管理员
1 份示例简历
3 个示例岗位
3 条 RAG 岗位知识
1 条分析报告
1 条投递记录
```

这样演示时不用从零开始。

---

# 24. 数据库迁移建议

第一阶段可以使用：

```text
init.sql
```

后续更规范可以使用：

```text
Flyway
Liquibase
```

推荐后续添加：

```groovy
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
```

迁移文件：

```text
src/main/resources/db/migration
├── V1__init_schema.sql
├── V2__add_rbac.sql
├── V3__add_resume_version.sql
├── V4__add_rag.sql
└── V5__add_operation_log.sql
```

---

# 25. CI/CD 与测试关系

CI/CD 不应该只 build。

推荐 CI 执行：

```text
后端 test
前端 build
Docker build
```

最小合格标准：

```text
./gradlew clean test
npm run build
docker compose build
```

---

# 26. 常见部署问题

## 26.1 后端连不上 MySQL

检查：

```bash
docker compose logs backend
docker compose logs mysql
```

常见原因：

```text
MYSQL_HOST 写错
MySQL 还没启动完成
密码错误
数据库不存在
```

解决：

```text
depends_on 只能保证启动顺序，不保证 MySQL 已就绪
后端可以配置重试
或者手动重启 backend
```

命令：

```bash
docker compose restart backend
```

---

## 26.2 Redis 连接失败

检查：

```bash
docker compose logs redis
docker compose logs backend
```

常见原因：

```text
REDIS_PASSWORD 不一致
REDIS_HOST 写错
```

---

## 26.3 前端刷新 404

Vue Router history 模式需要 Nginx：

```nginx
try_files $uri $uri/ /index.html;
```

---

## 26.4 WebSocket 连接失败

Nginx 必须配置：

```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

---

## 26.5 AI 分析失败

检查：

```text
AI_API_KEY 是否配置
AI_BASE_URL 是否正确
服务器能否访问外网
AI_MODEL 是否正确
```

如果只是演示，可以切换：

```env
AI_PROVIDER=mock
```

---

## 26.6 文件上传后丢失

原因：

```text
容器重启后文件没有挂载 volume
```

解决：

```yaml
volumes:
  - internpilot-uploads:/app/uploads
```

---

# 27. 在线演示验收流程

## 27.1 普通用户流程

```text
打开前端地址
  ↓
登录 demo 用户
  ↓
查看 Dashboard
  ↓
上传简历
  ↓
创建岗位 JD
  ↓
发起 AI 分析
  ↓
查看分析报告
  ↓
生成面试题
  ↓
生成岗位推荐
  ↓
创建投递记录
  ↓
修改投递状态
```

---

## 27.2 管理员流程

```text
登录 admin_demo
  ↓
进入管理员后台
  ↓
查看用户列表
  ↓
查看角色权限
  ↓
查看操作日志
  ↓
查看 RAG 知识库
  ↓
查看后台数据看板
```

---

# 28. 面试演示顺序

建议面试时这样演示：

```text
1. 打开 GitHub README
2. 展示项目架构图
3. 展示在线演示地址
4. 登录普通用户
5. 演示简历上传
6. 演示 AI 匹配分析进度
7. 演示分析报告
8. 演示 AI 面试题
9. 演示岗位推荐
10. 演示投递记录
11. 登录管理员
12. 展示 RBAC 管理后台
13. 展示操作日志
14. 展示 Swagger 接口文档
15. 展示 GitHub Actions 自动测试
```

---

# 29. GitHub Actions 徽章

README 顶部可以加：

```markdown
![CI](https://github.com/你的用户名/InternPilot/actions/workflows/ci.yml/badge.svg)
```

如果 workflow 名称不同，需要改成实际文件名。

---

# 30. 项目展示最终清单

## 30.1 GitHub 仓库应包含

```text
README.md
docs/
backend/
frontend/
deploy/
.github/workflows/
.env.example
docker-compose.yml
```

---

## 30.2 GitHub 仓库不应包含

```text
node_modules
dist
build
.gradle
uploads
.env
真实 API Key
真实数据库密码
日志文件
hs_err_pid 文件
```

---

# 31. 简历写法

完成 CI/CD 和在线演示后，简历可以写：

```text
- 使用 Docker Compose 编排 Spring Boot 后端、Vue 前端、MySQL 和 Redis，完成项目容器化部署，并通过 GitHub Actions 实现后端测试和前端构建自动化校验。
```

更完整写法：

```text
- 为项目搭建 GitHub Actions CI 流程，自动执行 Gradle 单元测试与前端 Vite 构建；基于 Docker Compose 完成 MySQL、Redis、Spring Boot 后端和 Vue 前端的一键部署，并提供在线演示环境。
```

---

# 32. 开发顺序建议

推荐按以下顺序完成：

```text
1. 清理仓库无关文件；
2. 确认 .gitignore；
3. 补充 .env.example；
4. 补充 application-prod.yml；
5. 完善后端 Dockerfile；
6. 完善前端 Dockerfile；
7. 完善 nginx.conf；
8. 完善 deploy/docker-compose.yml；
9. 本地 docker compose up -d --build；
10. 本地验证前端、后端、Swagger；
11. 创建 .github/workflows/ci.yml；
12. push 到 GitHub；
13. 查看 Actions 是否通过；
14. 准备云服务器；
15. 服务器安装 Docker；
16. git clone 项目；
17. 配置 deploy/.env；
18. docker compose up -d --build；
19. 验证在线地址；
20. README 加入在线演示说明和 CI 徽章。
```

---

# 33. 最终验收标准

## 33.1 CI 验收

-  push 后 GitHub Actions 自动触发；
    
-  后端测试通过；
    
-  前端 build 通过；
    
-  CI 徽章显示 passing；
    
-  测试失败时 CI 显示 failed。
    

---

## 33.2 Docker 验收

-  docker compose build 成功；
    
-  MySQL 容器运行；
    
-  Redis 容器运行；
    
-  Backend 容器运行；
    
-  Frontend 容器运行；
    
-  `/api/health` 可访问；
    
-  `/doc.html` 可访问；
    
-  前端页面可访问；
    
-  WebSocket AI 进度可用。
    

---

## 33.3 在线演示验收

-  在线前端地址可访问；
    
-  普通用户可以登录；
    
-  管理员可以登录；
    
-  AI 分析可用或 Mock 可用；
    
-  简历上传可用；
    
-  岗位推荐可用；
    
-  操作日志可查看；
    
-  README 中有演示说明；
    
-  不暴露敏感信息。
    

---

# 34. 模块设计结论

CI/CD 和在线演示部署是 InternPilot 工程化展示的最后一环。

它将项目从：

```text
本地开发项目
```

升级为：

```text
GitHub 可验证
Docker 可部署
在线可演示
面试可展示
```

完成后，InternPilot 的最终项目能力可以总结为：

```text
Spring Boot 后端工程
Vue 前端工程
JWT + RBAC 权限系统
AI 简历岗位分析
WebSocket 实时进度
AI 面试题生成
RAG 岗位知识库
岗位推荐
管理员后台
操作日志
测试体系
CI/CD
Docker 部署
在线演示
```

这已经是一份比较完整、适合暑期实习投递和面试展示的 Java + AI 应用项目。