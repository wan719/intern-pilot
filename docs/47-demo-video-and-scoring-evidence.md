# InternPilot 演示视频与评分证据清单

本文档用于答辩前录制 1-3 分钟功能演示视频，并汇总评分表中容易被老师检查的证据。这里不伪造视频文件；录制完成后，将视频放到 `docs/assets/demo/` 或上传到可访问平台，再把链接补到 README。

## 1. 建议视频信息

- 视频长度：1-3 分钟
- 建议文件名：`docs/assets/demo/internpilot-v1.3.1-demo.mp4`
- 建议分辨率：1920x1080 或 1280x720
- 建议提交方式：若文件较大，优先上传到 Gitee / GitHub Release / 课程平台，README 中放链接
- 录制前检查：
  - 线上地址可访问：`https://internpilot.com.cn`
  - 后端健康检查：`https://internpilot.com.cn/api/health`
  - Actuator 健康检查：`https://internpilot.com.cn/actuator/health`
  - 管理员账号和密码不要出现在视频画面中
  - 不展示 `.env`、API Key、邮箱授权码、JWT token

## 2. 1-3 分钟演示路线

| 时间 | 画面 | 讲解重点 |
| --- | --- | --- |
| 0:00-0:15 | 首页 / 登录页 | InternPilot 是面向大学生实习求职的 AI 简历优化与岗位匹配平台 |
| 0:15-0:30 | 注册或登录、用户工作台 | 邮箱验证码、JWT 登录、用户工作台 |
| 0:30-0:50 | 简历管理 | 上传简历、解析文本、版本管理、AI 优化入口 |
| 0:50-1:15 | 岗位管理与 AI 分析 | 选择简历和岗位，发起 AI 简历岗位匹配分析 |
| 1:15-1:35 | AI 任务中心 | WebSocket 进度、全局任务中心、结果入口 |
| 1:35-1:55 | 分析报告与 PDF | 展示匹配分数、优势、不足、优化建议，打开打印页并说明可保存 PDF |
| 1:55-2:15 | 岗位推荐和面试题 | 展示推荐批次、推荐理由、面试题分类和参考答案 |
| 2:15-2:35 | RAG 与管理员后台 | 展示 RAG 知识库、用户/角色/权限/反馈/日志 |
| 2:35-3:00 | 工程能力 | 展示 `/api/health`、`/actuator/health`、GitHub Release、测试和 Docker 部署说明 |

## 3. 30 秒口播稿

InternPilot 是一个面向大学生实习求职的 AI 工作台，基于 Spring Boot、Vue3、MySQL、Redis、Docker 和 DeepSeek API 构建。系统支持简历上传解析、岗位管理、AI 简历岗位匹配分析、岗位推荐、AI 面试题生成、RAG 岗位知识库、PDF 报告导出、用户反馈和 RBAC 管理后台。项目还实现了 WebSocket 进度推送、AI 任务中心、模型路由、Prompt 版本管理、Actuator 健康检查和 Docker Compose 线上部署，目前发布到 v1.3.1。

## 4. 评分证据

| 评分项 | 可展示证据 |
| --- | --- |
| 功能完整性 | README 截图、线上演示、AI 分析、推荐、面试题、RAG、管理员后台、PDF 导出 |
| 技术实现 | Gradle、Spring Boot、Swagger/Knife4j、MyBatis-Plus、Redis、WebSocket、Actuator、JaCoCo 报告 |
| Git 提交历史 | `git log --oneline --decorate --all`、`main/dev/feature` 分支、`v1.3.1` Release |
| README 文档 | 中文 README、英文 README、架构图、截图、快速开始、Docker 部署、测试说明 |
| 创新与实用性 | DeepSeek 接入、Prompt 版本管理、AI 模型路由、RAG、全局 AI 任务中心、PDF 导出 |
| 加分项 | 在线部署、演示视频、英文 README、额外 AI 工程化能力 |

## 5. 录制后 README 更新位置

录制完成后，在 README 顶部徽章区域下方补充：

```markdown
演示视频：[InternPilot v1.3.1 功能演示](docs/assets/demo/internpilot-v1.3.1-demo.mp4)
```

如果视频放在 Release 或课程平台，则使用外部链接：

```markdown
演示视频：[InternPilot v1.3.1 功能演示](https://example.com/internpilot-demo)
```
