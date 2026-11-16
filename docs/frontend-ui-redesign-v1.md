---
type: frontend-ui-redesign-plan
status: design-baseline
version: 1.0.0
created: 2026-11-16
updated: 2026-11-16
source_repo: wan719/intern-pilot
source_branch: main
source_commit: 1aabf181f2d4707272512694a56ce968e807da14
implementation: NOT_STARTED
verification: STATIC_REVIEW_ONLY
production_authorization: NONE
---

# InternPilot 前端 UI 重设计 V1.0｜C 端 × 管理后台

> **本文件是设计与迁移准入基线，不是重构完成报告。** 本次只读取源码与公开参考，尚未执行依赖安装、前端测试、浏览器截图验证、PR 合并或生产部署。进入实现前必须刷新仓库 HEAD、检查实际工作区与锁文件。参照项目只供产品交互和设计语言借鉴，不表示 InternPilot 要迁移到 React 或复制对方代码、Logo、品牌和模板。

## 00. 决策摘要

**目标**：在保留现有用户行为、API 契约、鉴权与响应式支持的基础上，把普通用户页面改造为安静、清晰、以简历和求职行动为中心的 SaaS 工作区；把管理后台改造为信息层级清楚、操作高效、适应权限差异的运营控制台。

**分类**：这是 `UI Redesign + 渐进式组件重构`，不是“重构后外观、交互完全不变”的纯 Refactor，也不是完整重做业务系统。视觉/交互变更必须逐条写验收条件。涉及新增业务能力时另建 Feature。

**推荐但尚待兼容性验证的技术方案：**
- 继续使用 **Vue 3 + TypeScript + Vite** 的单一 `frontend/` 工程，保持现有 Vue Router、Pinia、Axios、ECharts、SockJS/STOMP。
- **C 端**：目标采用 `shadcn-vue（基于 Reka UI 的可复制组件代码）+ Tailwind CSS v4` 做新设计组件；**先以一个页面隔离试点**，不立即删除页面中仍依赖的 Element Plus。
- **管理后台**：继续采用 `Element Plus + 当前 Sass`，必要时仅使用受控的 Tailwind 布局 Utilities。借鉴 Art Design Pro 的后台布局和交互，不直接导入其整套后台工程。
- **统一 Design Tokens**：在当前 `frontend/src/styles/tokens.scss` 基础上增加语义映射，统一品牌色、前景、背景、边框、状态、间距、圆角、动效与响应式断点。C 端和后台可有不同密度与表面层级，但不能有两套相互矛盾的业务状态色。
- **暂不**：切换 React、拆成 C 端/后台两个部署应用、更换 npm 为 pnpm、统一重写所有 Sass、直接接入在线简历 WYSIWYG/PDF 排版引擎、改造后端 API 或生产部署。

技术选型是当前方案，不等于依赖兼容性已实测通过。若 Tailwind v4 + shadcn-vue 与现有 Element Plus / Vite / Node 20 / 构建流程产生无法隔离的冲突，优先采用 **Element Plus + 现有 CSS Variables/Sass 实现 C 端视觉**作为回退，不为追求新库而扩大工程风险。

## 01. 只读源码核验基线

仓库：`wan719/intern-pilot` `main` @ `1aabf181f2d4707272512694a56ce968e807da14`，2026-11-16。该 SHA 只描述本次审阅输入，不代表下一轮实施时的最新版本。

- 单一 Vue 3 SPA；`frontend/src/router/index.ts` 将用户页面挂在 `AppLayout.vue`，管理页面挂在 `AdminLayout.vue`，打印页 `/analysis/reports/:id/print` 独立路由。
- 实际依赖来自 `frontend/package.json`：Vue 3.5、Vite 6、TypeScript、Element Plus 2、Pinia、Vue Router、Axios、ECharts、Sass；**尚没有** Tailwind CSS / shadcn-vue。使用 npm 与 `package-lock.json`；项目 CI/Docker 的 Node 基线是 20，不能直接假设参照仓库版本适配。
- `frontend/src/styles/tokens.scss`：已存在品牌青绿色 `#0f9f8f`、背景/边框/状态语义变量与间距/圆角。`element.scss` 是 Element Plus 主题适配层，`index.scss` 还有大量共享页面与布局样式。
- C 端导航事实源：`frontend/src/config/navigation.ts`；重要入口：`/dashboard`、`/resumes`、`/jobs`、`/analysis/match`、`/analysis/reports`、`/job-recommendations`、`/interview-questions`、`/applications`、`/user/center`。`MobileBottomNav.vue` 处理窄屏导航。
- 后台入口 `/admin/dashboard`、`/admin/users`、`/admin/roles`、`/admin/permissions`、`/admin/rag-knowledge`、`/admin/operation-logs`、`/admin/feedback`，导航按 `adminNavGroups` 和权限过滤。路由守卫依据 `meta.permission`，**前端菜单隐藏不能替代服务端权限检查**。
- 现有 `ResumeList.vue` 已有“上传 PDF/DOCX、简历列表（桌面表格/移动卡片）、解析状态、默认简历、详情抽屉、版本管理、删除确认、加载/空/错误状态”。`ResumeVersionList.vue` 已支持版本操作与 AI 优化；但不是 Reactive Resume 的完整可视化所见即所得文档编辑器。
- 现有 `AdminUserList.vue` 已有关键词、角色编码/状态筛选、分页、用户详情、启禁用确认、角色分配与权限控制。其表格宽度和列密度是管理端体验的关键约束。
- 现有测试见 `frontend/src/views/resume/__tests__/ResumeList.spec.ts`、`ResumeVersionList.spec.ts`、`frontend/src/views/admin/__tests__/AdminCrudPages.spec.ts`、`AdminHandlerPermissions.spec.ts`、导航与布局组件测试。**只确认测试文件及当前断言存在，未在本次运行并获 PASS**。

事实源优先级：本次新设计计划 ＜ `AGENTS.md`、`docs/frontend.md`、`docs/test.md`、`docs/git.md` 与实施时实际源码/API/CI。历史 README 标签与生产版本需要分开判断。

## 02. 两套产品体验和参考边界

### C 端：参考 Reactive Resume 的「Calm／文档工作区」

参考固定版本：
- `reactive-resume/reactive-resume` @ `ac22d622d701bd114d97f11c99a5ce660c72a663`；`apps/web/DESIGN.md`、`PRODUCT.md`、`README.md`。
- 参考链接：https://github.com/reactive-resume/reactive-resume

应借鉴：
1. **一个清楚的下一步操作**，避免页面同时出现一排同等级主按钮。
2. **内容大于装饰**：安静背景、轻量边框、扁平列表、充分留白、清晰的主次文字、简历作为信息中心。
3. **不丢失用户工作**：上传、AI 任务、版本变化有明确进行中/失败/完成提示，不因重新排版隐藏恢复入口。
4. **渐进披露**：基础操作先露出，低频危险/高级操作收纳，但依然可被键盘访问。
5. **AI 给建议、用户确认**。当前已有的 AI 功能要保留确认、异常与重试语义。

不直接复制 React 组件、专有品牌资源、简历模板或它尚未在 InternPilot 支持的编辑能力。项目虽标示 MIT 授权，若将来确实复用任何源代码或资源，仍需分别检查具体文件许可证和署名要求。

### 管理后台：参考 Art Design Pro

参考固定版本：
- `Daymychen/art-design-pro` @ `f3aaf58eec1a0e988f162352c33862327a484f95`；`package.json`、`README.zh-CN.md` 与 `src/assets/styles/index.scss`。
- 参考链接：https://github.com/Daymychen/art-design-pro

应借鉴：分组菜单、清楚的面包屑/标题和当前定位、表格/筛选/分页的纵向节奏、批量与危险动作的区分、明暗主题一致性、状态/空态/失败态和适度的动效。不要把其演示数据、路由方案、权限实现、表格封装和全局 Store 直接覆盖 InternPilot 现有业务。

## 03. 设计系统 V1（建议值，不是已实现 Token）

### 共享规范

| 项 | 候选值或规则 |
|---|---|
| 品牌主色 | 保留 `#0F9F8F`（现有品牌识别），主操作不要全页面泛滥 |
| C 端背景 | 浅暖灰候选 `#F8F7F3`；卡片/纸面 `#FFFFFF` |
| 后台导航 | 保留 `#111827` 深色侧栏；内容区可继续浅色 |
| 文本/边框 | 从当前 `--color-text`、`--color-text-muted`、`--color-border` 语义层统一，不通过固定颜色在页面重复 |
| 间距 | 以 4px 基准设置 4/8/12/16/24/32/40；密度由页面语义控制，不强制所有卡片同样 padding |
| 圆角 | 8/12/16px 为主，纸张/表格与弹窗按内容类型使用，不做处处大圆角 |
| 排版 | 中文以系统字体或合规的 Noto Sans SC 等无衬线优先，英文标题可选额外风格但不强行替换中文字体 |
| 动效 | 120–200ms 轻量反馈，遵守 `prefers-reduced-motion`；状态反馈不能只靠颜色或动画 |
| 无障碍 | 明确的 Label、键盘可达、焦点可见、弹窗焦点管理和可触达的移动端目标，目标参考 WCAG 2.2 AA（**尚未审计达标**） |

**Token 治理原则：**
- 第一阶段保留现有 `--color-*`、`--space-*` 作为兼容层，逐步新增 `--ip-*` 语义 Token（如 `--ip-bg`、`--ip-surface`、`--ip-text`、`--ip-primary`、`--ip-border`、`--ip-danger`）。不要在一次 PR 中删除旧变量。
- C 端与管理端允许分别覆盖密度、背景、侧栏颜色，避免分别维护两套品牌色。
- Element Plus 的 `--el-*` 仍由 `element.scss` 做统一映射；不要给同一个元素同时套用两套冲突的按钮/表单样式。
- 通过 Portal/Teleport 到 `body` 的 Dialog、Drawer、Popover 可能脱离 `.user-app-shell` 局部主题，必须验证主题继承、z-index、Escape、遮罩、焦点返回和滚动锁定。严禁只调整 CSS 即宣称隔离完成。
- 暗色主题先做后台必要态和组件可用性；完整 C 端暗色主题属于后续范围，不把高对比度和视觉主题混为一谈。

### Tailwind 与 shadcn-vue 的引入策略

1. **先验证构建兼容，再安装 UI 组件**：在隔离任务分支检查当前 Vite 6、Node 20、npm lockfile 和 `@/*` 路径别名；不得照抄 React 项目的 CLI。
2. Tailwind CSS v4 先在试点范围验证 Utilities；`Preflight` 全局 reset 可能影响 Element Plus、原有表单和 Sass。对比标准导入与关闭 Preflight 的实验结果，必要时采用 `theme.css` + `utilities.css` 的选择性引入，并补足新组件要求的基础样式。
3. shadcn-vue 采用 Vue 官方项目（https://next.shadcn-vue.com/），使用 Reka UI 组件。CLI 对已有全局样式和 `components.json` 的修改必须审查差异；不允许一键覆盖现有主题。
4. 只新增**第一个页面真正用到的**基础 Button/Card/Badge/Sheet 等组件；历史 Element Plus 对话框/表单按需保留。禁止因两套 Toast、Dialog、Form 同时存在而产生不可控的全局行为。
5. 若安装引起现有路由、表单、表格、Dialog 或打印页的全局样式回归，回退到**Element Plus + Sass + CSS Variables** 的保守方案，并记录试点证据。

参考文档：
- shadcn-vue / Vite Tailwind v4：https://next.shadcn-vue.com/docs/installation/vite
- shadcn-vue / Tailwind v4：https://next.shadcn-vue.com/docs/tailwind-v4
- Tailwind / Preflight：https://tailwindcss.com/docs/preflight

## 04. 组件职责与推荐边界

| 组件/功能 | C 端目标方案 | 后台方案 | 注意事项 |
|---|---|---|---|
| 基础 Button、Badge、Card、Tabs | 新业务区域优先 shadcn-vue / 自有包装组件 | Element Plus | 不在同一区域无序混用样式语义 |
| 上传和复杂表单 | 初期保留现有 Element Plus 行为，后续按收益逐项迁移 | Element Plus | 文件校验、异常、重复提交语义不变 |
| Dialog / Drawer / Popover | 新区域可试点 Reka/shadcn-vue；原有弹窗先保留 | Element Plus | Portal、焦点、Escape、层级、遮罩与滚动锁定 |
| 列表/数据表格/分页 | C 端简历库可改为内容卡片/扁平列表 | Element Plus 表格 | 后台分页、角色权限、操作确认必须保留 |
| 图表、AI 进度 | 复用 ECharts、Pinia、WebSocket | 复用 ECharts | 进度与任务状态以服务端事实为准 |
| 图标 | 选定一套图标语言，渐进统一 | 保留现有图标并逐步适配 | 不一次删除旧图标依赖 |
| 通用业务组件 | 现有 `PageContainer`、`PageHero`、`StatCard`、`AppEmpty` 按需适配 | 复用 `FilterBar`、`TableShell` 等 | 共享 API 保持稳定，避免一次重构全部调用方 |

建议目录是**将来有实际组件才创建**的目标，不是当前文件事实：

```text
frontend/src/
├── components/
│   ├── ui/                    # C 端引入的少量基础组件（按需）
│   ├── common/                # 已有业务共享组件，保持兼容
│   └── layout/                # 已有 C 端/后台 Layout，逐步改造
├── styles/
│   ├── tokens.scss           # 保留既有变量的兼容层
│   ├── element.scss          # Element Plus 映射
│   └── [Tailwind 入口文件]    # 兼容性评估通过后再加入
├── views/
│   ├── resume/               # 第一批 C 端试点
│   └── admin/                # 后台后续试点
└── ...
```

## 05. 页面范围与改造优先级

| 批次 | 路由/模块 | 主要体验目标 | 保留的关键行为 |
|---|---|---|---|
| P1（首个试点） | `/resumes` 简历中心 | 内容优先的“简历文档库”，清楚的默认简历、解析状态、上传主操作、轻量详情 | 上传 FormData、列表请求、解析状态、默认简历设置、详情、版本入口、删除确认、失败恢复 |
| P2 | `/resumes/:resumeId/versions` | 同一视觉语言的版本时间线/差异信息 | 原有版本创建/修改/AI 优化与回退语义，不能捏造文档实时预览 |
| P3 | `/dashboard` | 突出当前求职阶段与一个明确下一步行动 | 现有五阶段数据来源、AI 任务中心、加载、失败和导航 |
| P4 | `/jobs`、`/job-recommendations`、`/applications` | 清晰的岗位卡片、筛选、申请状态反馈 | 推荐、分页、权限、投递状态流转、失败重试 |
| P5 | `/analysis/*`、`/interview-questions*` | 减少信息噪音、突出分析结论与下一步操作 | WebSocket 进度恢复、AI 报告和打印隔离、题单筛选及权限 |
| P6 | `/admin/users`（后台首个试点） | Art Design Pro 风格分组侧栏、紧凑筛选/表格、统一操作反馈 | 关键词/角色/状态筛选、分页、详情、角色分配、启禁用确认、RBAC |
| P7 | `/admin/*` 其他页面 | 统一后台控件和表格布局 | 原有菜单、权限、RAG、操作日志、反馈处理 |
| 独立核对 | `/login`、`/register`、`/403`、`/user/center` 和打印页 | 品牌/响应式/可用性一致 | 会话、注册、403 状态、个人资料、打印样式不受全局 CSS 影响 |

执行顺序可以依据风险和验收证据调整；**不要在同一次大 PR 中重做所有页面**。其中 `/analysis/reports/:id/print` 需要独立无导航打印检查，即便本轮不直接修改，也必须纳入全局 CSS 的回归范围。

## 06. P1 详细试点：`/resumes`

**首屏结构（候选）**

- 页面标题：“简历中心”，副标题概括可完成的求职操作；**唯一突出主操作：“上传简历”**。
- 内容区优先展示默认简历状态及简历库。条目结构：简历名称、文件类型、解析状态、默认标记、上传时间，常用操作“查看详情 / 版本管理”可见。
- 删除为危险动作，需保持确认流程；设置默认需清晰反馈，失败不应误报成功。
- 桌面可以采用扁平列表或卡片列表，窄屏改为完整可触达的卡片布局；不为复制参考产品而强制引入文档实时预览画布。
- 上传弹窗/抽屉**首轮保留既有 Element Plus 交互**，优先验证视觉一致与键盘/焦点行为；新 shadcn-vue Dialog 只在后续明确获益时替换。
- 必须定义并展示：loading、空数据、列表加载错误及重试、解析成功/进行中/失败、上传中/失败、无默认简历、权限/会话失效、删除取消等状态。

**功能保持清单（以代码和现有测试为依据）：**
- `getResumeListApi({ pageNum: 1, pageSize: 100 })`：首批视觉改造不暗中改动查询与分页语义；未来如需分页另设独立 Feature。
- `uploadResumeApi(FormData)` 中 `file` 与 `resumeName` 两个字段、PDF/DOCX 限制和成功后刷新。
- `getResumeDetailApi(id)`：详情仍能获取解析文本；不要虚构原生 PDF 预览功能。
- `setDefaultResumeApi(id)`：默认简历仍能更新，原有正确行为不变。
- `deleteResumeApi(id)`：仅在确认后删除指定记录；取消/失败保持正确状态。
- 版本管理跳转 `/resumes/:resumeId/versions`；AI 分析入口及其他 C 端导航语义保持。

**原型交付（开发前）**：至少一份桌面布局说明、一份 375px 移动布局说明、一份“加载/空/错误/解析状态”的状态表、一份 Token/组件映射表；需评审后再把原型实现接入真实业务数据。设计稿不等于已经通过交互测试。

## 07. 后台首个试点：`/admin/users`

- 保留当前 `AdminLayout.vue`、`AdminSidebar.vue` 的权限过滤与进入/离开导航行为，以 Art Design Pro 为视觉参考改进侧栏分组、标题/筛选/表格/分页的层级。
- `AdminUserList.vue` 的筛选（关键词、角色编码、启用状态）、分页（10/20/50/100）、详情/角色分配及禁用/启用确认必须保持。
- 管理表格是高信息密度场景，优先 Element Plus；不能为组件统一而硬把复杂表格迁到 C 端组件库。
- 断点较窄时允许横向滚动或把筛选收纳，但不能隐藏关键管理动作而没有替代入口；权限不足时前端交互需对应后端 401/403 的权威处理。
- 表格/详情/角色选项的 loading、空、失败、权限不足与操作 pending 需分别呈现；不要用样例数据冒充真实权限行为。

## 08. 逐批实施与 Gate

**G0｜只读准入（待执行）**
- 核对新 HEAD、工作区是否 clean、相关测试是否存在、当前 npm 依赖锁、Node 和目标浏览器范围。
- 收集现有 `/resumes` 和 `/admin/users` 的桌面/窄屏界面基线，使用**合成、脱敏数据**；截图需要有可追溯的版本与环境。
- 检查 C 端、后台共享样式的选择器和 `ElMessage` / `ElDrawer` / `ElDialog` 等 Teleport 行为。

**G1｜设计准入（本文件是初稿）**
- 确认 C 端/后台各自的导航、字体、色彩、布局、动效、状态表达规范；完成 `/resumes` 的桌面和移动原型、关键状态列表。
- 评估 Tailwind v4/shadcn-vue 的真实依赖兼容、构建增量、暗色/焦点/Portal 问题；失败可选纯 Element Plus + Sass 方案。
- 明确何者是纯视觉改动，何者改变了交互或需求；后者添加明确的验收条件。

**G2｜隔离实现与验证（未执行）**
- 以独立分支从新 HEAD 出发；先最小化引入样式/Tokens，再只改 `/resumes`，保留上传、详情、版本、默认、删除和异常恢复。
- 从 `frontend/` 运行 `npm ci`、受影响 Vitest、`npm run type-check`、`npm run build`；评估全套 `npm run test:run` 的回归覆盖。失败、跳过和未执行分别记录。
- 桌面（例如 1440/1280）与移动（例如 375/768）在真实浏览器检查键盘、Tab 顺序、焦点返回、空/加载/错误、上传和取消。
- 对受影响范围做视觉对比；检查前端 bundle 变化、主要交互响应与页面横向溢出（保留检查方法和版本，**不设无证据的收益数字**）。

**G3｜独立 Review 与逐批交付（未执行）**
- 重点 Review：REST/FormData 未变、登录/角色权限没回归、移动导航仍可用、AI 任务与 PDF 打印未被全局 CSS 破坏、敏感用户简历内容未流入样例或截图。
- 逐批 PR、检查 CI 与当前提交 SHA，按 `docs/git.md` 完成 Review；只有验证和决策都充分才可合并。
- 生产镜像、部署与回滚 **不在本计划的自动授权范围**；发布需要另行制定变更与回滚方案。

**停止/回退**：任何构建/类型/业务回归失败，先停止扩大组件替换；优先 revert 本批 UI 提交或切换保守的现有组件方案。禁止在生产实例上以热修方式验证 Tailwind 基础样式影响，不强推共享分支。

## 09. 验收矩阵（P1 初始）

| 编号 | 需保持或实现的可观察行为 | 验证方式 | 状态 |
|---|---|---|---|
| UI-01 | C 端简历中心可在桌面/375px 移动端找到“上传简历”，无功能性溢出 | 浏览器响应式检查 | NOT_RUN |
| UI-02 | 页面包含成功/失败/加载/空数据/解析状态且易于区分，错误可重试 | 模拟状态 + 手工检查 | NOT_RUN |
| FUN-01 | 简历列表、详情、默认操作与版本跳转行为不变 | `ResumeList.spec.ts` 及必要新测试 | NOT_RUN |
| FUN-02 | 上传 FormData 字段正确，失败可重试，删除仅在确认后发生 | 现有回归测试 + 边界用例 | NOT_RUN |
| UX-01 | 键盘可完成主要操作；弹窗焦点/ESC/返回焦点正确 | 浏览器键盘验收 | NOT_RUN |
| UX-02 | C 端/后台的 Token 与全局样式不使打印页、后台表格、移动导航失效 | 交叉页面检查 | NOT_RUN |
| SEC-01 | 路由守卫、RBAC、401/403 语义和后台按钮授权保持 | 现有路由与后台权限测试 | NOT_RUN |
| ENG-01 | `npm run test:run`、`npm run type-check`、`npm run build` 在目标 SHA 通过（如不运行则明确） | 终端/CI 记录 | NOT_RUN |
| ADM-01 | `/admin/users` 的筛选、分页、详情、角色、禁启操作无回归（后台批次） | `AdminCrudPages.spec.ts` + 手工检查 | NOT_RUN |

## 10. 未裁决项与下一合法动作

- **产品边界**：未来是否需要从“上传/解析/版本”升级到所见即所得简历编辑 + PDF 实时预览？当前答案为 **NOT_IN_SCOPE**。该项需要独立的编辑数据模型、保存冲突、导出/排版、预览与数据兼容设计，不能借 UI 改造私自加入。
- **组件方案**：shadcn-vue + Tailwind v4 是**候选架构**，只有隔离试点证明兼容后才升级为全站规范；失败优先回退已有 Element Plus/Sass。
- **用户体验**：是否将现有“求职旅程”六项导航按参考改为文档/岗位/投递的更精简入口？首批不变更路由语义，可先在原型比较导航信息架构。
- **生产边界**：现有生产服务版本、备份和上线策略不由本设计文档声明；此处完全没有发布/生产执行授权。

**下一步**：先执行 `G0` 的基线采集和 `G1` 的 `/resumes` 桌面 + 375px 移动设计原型，再小范围试验 Tailwind v4 与 shadcn-vue。不要先批量安装 UI 库或重做所有页面。
