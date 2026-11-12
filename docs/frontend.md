# 前端技术框架与开发约定

## 工程与技术栈

前端是 `frontend/` 中的单个 Vue 3 + TypeScript + Vite 应用，使用 Vue Router、Pinia、Axios、Element Plus、ECharts、Sass。以 [package.json](../frontend/package.json) 和 `package-lock.json` 为依赖与脚本依据，使用 npm 管理依赖。桌面和移动布局在同一应用中实现，不是独立的 H5/Web 工程。

`src/views/` 放业务页面，`src/components/` 放可复用组件，`src/router/` 管理路由，`src/stores/` 管理跨页面状态，`src/utils/request.ts` 封装 API 请求。页面与组件应呈现加载、空数据、失败及权限受限状态；权限与业务规则仍由后端复核。本地另存有完整的 UI 设计基线。

## 界面基线摘要

项目的 v1.4.0 UI 设计基线是一份独立的本地设计资料；它记录设计意图与历史验收条件，当前实现仍以源码为准。公开开发约定保留以下需要日常遵守的要点：

- 用户端围绕工作台和简历、岗位、匹配分析、面试准备、投递追踪组织导航；管理员使用独立的分组侧栏。修改路由时同步核对导航配置和权限标记。
- 使用现有 Sass、Element Plus 与 CSS 设计变量维护颜色、间距和状态样式；不要在单页重复写一套独立主题。
- 移动端用户流程需保持可操作；管理表格至少保证基础可用和信息可达。检查导航抽屉、表单、对话框、长内容和触控目标。
- 保留明确的加载、空状态、错误与成功反馈，并检查键盘焦点、表单标签和对比度。打印报告使用独立布局。

## 接口与配置

开发服务器在 5173 端口运行；[Vite 配置](../frontend/vite.config.ts) 将 `/api` 和 `/uploads` 代理到本地 8080 后端。请求模块可从 `VITE_API_BASE_URL` 读取基础地址，并携带现有 token；修改响应处理时核对业务 `code`、401 跳转及用户可见错误。浏览器环境变量和构建产物不得包含后端密钥。

当前请求封装从 `src/utils/token.ts` 读取浏览器本地 token，使用 `Authorization: Bearer <token>`；响应体 `code === 200` 时返回 `data`，业务失败会展示友好消息，401 会清除 token 并跳转登录。修改接口响应时，要检查现有调用方是否依赖这一解包方式。AI 进度使用 WebSocket 相关工具和任务中心状态；变更长任务页面时同时检查断线、刷新及完成通知。生产环境的 API/静态文件代理以 Nginx 和部署配置为准，本地 Vite 代理不能代表线上配置。

## 本地运行与验证

```powershell
cd frontend
npm ci
npm run dev
npm run test:run
npm run build
```

`npm run build` 包含 `vue-tsc -b` 类型检查和 Vite 构建。测试使用 Vitest 与 Vue Test Utils，通常放在相关源码旁的 `__tests__/`。具体测试选择和结果报告见 [测试规范](test.md)；运行命令前先确认依赖已安装。

## 目录与页面职责

| 位置（相对于前端工程） | 当前用途 |
| --- | --- |
| `src/views/` | 登录注册、工作台、简历、岗位、分析报告、面试题、推荐、投递、用户中心及管理员页面 |
| `src/api/` | 按业务域封装认证、简历、岗位、分析、投递、反馈和管理接口 |
| `src/components/layout/` | 用户端与管理员端布局、导航及移动端入口 |
| `src/config/navigation.ts` | 导航结构与页面入口配置 |
| `src/router/index.ts` | 路由、登录检查及页面权限标记 |
| `src/stores/` | 登录用户、AI 任务中心、反馈等跨页面状态 |
| `src/utils/` | 请求拦截器、token、WebSocket、格式化等共享逻辑 |
| `src/styles/` | 全局样式、Element Plus 覆盖和设计 token |

页面通常调用 `src/api/`，由 API 层复用 `src/utils/request.ts`；避免在页面里重复写请求地址、鉴权头和全局错误处理。页面级状态留在组件内，跨页面且需要持续共享的状态再放入 Pinia。

## 路由、权限与交互

路由使用 history 模式。`/login`、`/register` 为公开页；普通用户页面从 `/dashboard`、`/resumes`、`/jobs`、`/analysis/...` 等入口进入；管理员页面位于 `/admin/...`。路由守卫会检查 token、读取当前用户，并根据路由的 `meta.permission` 决定是否跳到 `/403`。后端仍是授权的最终判断者；新增按钮或接口时应同时核对后端权限与前端可见性。

修改页面时至少检查加载、空数据、请求失败、表单校验及成功反馈。涉及删除、状态转换或 AI 长任务时，应在后端确认后更新界面，并检查重复点击、刷新恢复与失败重试。桌面与手机视图共用一套应用；对表格、导航、对话框及长文本作响应式检查。打印报告页 `/analysis/reports/:id/print` 是独立呈现路径，修改报告时也要核对打印布局。

## 开发与检查命令

| 命令 | 作用 |
| --- | --- |
| `npm ci` | 根据锁文件安装依赖；CI 使用此命令 |
| `npm run dev` | 启动本地 Vite 服务，默认端口 5173 |
| `npm run test:run` | 运行一次 Vitest 测试并退出 |
| `npm run test` | 开发期间以 Vitest 交互/监听方式运行 |
| `npm run type-check` | 单独执行 Vue/TypeScript 类型检查 |
| `npm run build` | 类型检查后生成生产构建 |

新增页面或 API 封装时，更新相关 Vitest 测试；路由、导航与权限变化需覆盖允许和拒绝路径。构建通过只证明类型与打包可完成，不能替代浏览器交互和真实后端联调。首次安装示例见 [README 部署说明](../README.md#部署说明)；生产代理以实际配置为准。
