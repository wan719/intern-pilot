# 部署与回滚指南

本指南面向现有 Docker Compose 部署的维护者。首次安装依赖和配置说明见[README](../README.md#部署说明)；证书文件约定见[证书说明](../deploy/ssl/README.md)。命令按仓库现有 Compose / Nginx 配置编写，不声明任何生产环境已验收。

## 部署前

- 确认服务器身份、当前部署目录和原 Compose 项目名；访问凭据从安全存储获取。
- 记录当前运行 Commit、容器镜像 ID、环境配置版本和可用的回滚镜像。
- 备份数据库、上传文件、生产配置和证书，验证恢复步骤。不要直接打包正在写入的数据库卷充当一致性备份。
- 检查目标版本变更、数据库结构、证书有效期、磁盘空间和维护窗口。
- 保留服务器现有 `.env` 与持久卷。不用 `.env.example` 覆盖生产配置，不输出配置中的密钥。
- 命令示例沿用默认部署目录与域名。自定义目录、域名或 Compose 项目名时，使用现有部署的实际值；不要因此创建新的空数据卷。

## 固定源码版本

以下命令在 Ubuntu 服务器的 Bash 中逐项执行，失败立即停止。工作区存在改动时先核对归属，不强制覆盖：

~~~bash
cd ~/intern-pilot
git status --short
git fetch origin --tags
git switch --detach v1.4.0
git rev-parse HEAD
git describe --tags --exact-match
~~~

`v1.4.0` 对应 `e278add294b8c4b06e719d6567a12a17900ad88e`。其他版本应核对其发布记录。detached HEAD 是按标签部署的正常状态，不需要重新创建标签。

## 配置检查与更新

先确认下面每项检查成功，再开始构建。Compose 配置检查不能验证真实 AI / SMTP 凭据是否有效。

~~~bash
cd deploy
test -f .env
test -f ssl/internpilot.com.cn_bundle.crt
test -f ssl/internpilot.com.cn.key
docker compose --env-file .env -f docker-compose.yml config --quiet
~~~

配置和备份确认后，先构建成功再更新运行服务；更新可能产生短暂重启：

~~~bash
docker compose --env-file .env -f docker-compose.yml build backend frontend
docker compose --env-file .env -f docker-compose.yml up -d
docker compose --env-file .env -f docker-compose.yml ps
~~~

现有编排将后端 8080 映射到主机；生产环境应结合防火墙限制直接访问，避免绕过公网代理暴露管理端口。MySQL / Redis 使用内部容器网络。

## 健康与业务验收

~~~bash
curl -fsS http://localhost:8080/actuator/health
curl -fsS https://internpilot.com.cn/actuator/health
docker compose --env-file .env -f docker-compose.yml logs --tail=200 backend
docker compose --env-file .env -f docker-compose.yml logs --tail=200 frontend
~~~

健康接口应返回 JSON 且包含 `"status":"UP"`。首页 HTTP 200 或前端 `/healthz` 成功不能代替后端健康检查。

进一步验证登录、简历、岗位、投递、AI 分析进度与报告、面试题、管理员及受限账号权限。需要单独核验真实 AI、验证码邮件、证书和数据持久化。日志分享前脱敏。

健康响应不能证明运行版本；把本轮源码 Commit 与实际启动的镜像 ID 关联记录。部署时间、备份位置、检查结果与回滚演练属于环境执行记录，不包含在公开技术文档中。

## 数据库

`v1.3.1 → v1.4.0` 的 SQL 目录与 `deploy/.env.example` 无源码差异，不代表数据库或服务器配置没有漂移。

MySQL 初始化 SQL 只在首次创建数据目录时执行。已有 `V40__final_release_update.sql` 是条件性人工迁移脚本，不在每次更新时无条件重放。执行迁移前必须确认适用版本、实际结构、数据影响与恢复办法。

## 回滚

优先恢复部署前保存的已验证旧镜像及兼容配置。只能从源码重建时，切换已验证的旧标签，并重新构建、启动、检查健康与业务流程；不要把任意旧 Tag 自动当成可用回滚版本。

- 使用原部署目录、Compose 项目名和数据卷。
- 普通更新或回滚不执行 `docker compose down -v`。
- 应用回滚不自动撤销数据库变化；恢复备份可能覆盖新数据，需要单独评估。
- 源码重建可能受到依赖与构建环境变化影响，不能替代保存旧镜像。
