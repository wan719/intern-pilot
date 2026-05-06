# InternPilot 技术选型文档

## 1. 技术选型原则

本项目技术选型遵循以下原则：

1. 优先选择企业常用技术；
2. 优先选择适合 Java 后端实习的技术栈；
3. 保证项目能在个人开发周期内完成；
4. 兼顾工程化能力和 AI 应用亮点；
5. 避免第一阶段引入过多复杂组件。

## 2. 后端技术栈

| 技术 | 版本建议 | 用途 |
|---|---|---|
| Java | 17 | 后端开发语言 |
| Spring Boot | 3.x | Web 应用开发框架 |
| Spring Security | 6.x | 认证与授权 |
| JWT | jjwt | 无状态登录认证 |
| MyBatis Plus | 3.x | 数据持久层 |
| MySQL | 8.x | 关系型数据库 |
| Redis | 7.x | 缓存 AI 分析结果 |
| Swagger/Knife4j | 最新稳定版 | 接口文档 |
| Lombok | 最新稳定版 | 简化实体类代码 |
| Validator | Spring Validation | 参数校验 |

## 3. 前端技术栈

| 技术 | 用途 |
|---|---|
| Vue 3 | 前端框架 |
| Vite | 项目构建 |
| Element Plus | UI 组件库 |
| Axios | 请求后端接口 |
| Pinia | 状态管理 |
| Vue Router | 路由管理 |

## 4. AI 技术栈

| 技术 | 用途 |
|---|---|
| DeepSeek API / OpenAI API / Qwen API | 大模型分析能力 |
| Prompt Template | 规范输入 |
| JSON Output | 规范输出 |
| Redis Cache | 缓存分析结果 |

## 5. 文件处理技术栈

| 技术 | 用途 |
|---|---|
| Apache PDFBox | PDF 文本解析 |
| Apache POI | DOCX 文本解析 |
| 本地文件存储 | 第一阶段文件存储 |
| MinIO | 后续对象存储扩展 |

## 6. 部署技术栈

| 技术 | 用途 |
|---|---|
| Docker | 容器化 |
| Docker Compose | 编排 MySQL、Redis、后端服务 |
| Nginx | 前端部署和反向代理 |
| GitHub Actions | 后续 CI/CD 扩展 |

## 7. 为什么暂时不引入复杂技术

第一阶段暂时不引入以下技术：

1. 微服务；
2. Spring Cloud；
3. Elasticsearch；
4. 向量数据库；
5. Kubernetes；
6. 完整 RAG 系统。

原因：

1. 项目由个人开发，周期有限；
2. 第一目标是完成核心业务闭环；
3. 过早引入复杂技术会增加开发风险；
4. 简历项目更看重是否完整、是否能讲清楚，而不是堆技术名词。

## 8. 后续可扩展技术

项目稳定后，可以逐步引入：

1. WebSocket：实时返回 AI 分析进度；
2. MinIO：管理简历文件；
3. Elasticsearch：岗位搜索；
4. 向量数据库：实现岗位知识库和 RAG；
5. RabbitMQ：处理异步 AI 分析任务；
6. GitHub Actions：自动化测试和部署。
