---
type: frontend-ui-design-plan
status: design-proposal
version: 1.0.0
created: 2026-11-16
updated: 2026-11-16
source_repo: wan719/intern-pilot
source_branch: main
source_commit: 1aabf181f2d4707272512694a56ce968e807da14
implementation: RESUMES_G2_IMPLEMENTED
verification: RESUMES_G2_LOCAL_AND_REAL_BACKEND
production_authorization: NONE
---

# 05 InternPilot 前端 UI 重设计概要设计

> 需求、业务保持边界及验收编号见[需求与验收基线](../requirements/06-frontend-ui-redesign-requirements.md)。2026-11-16 用户通过简历原型并批准 G2 接入。当前试点的实现和本地验证见[第 11 节](#11-resumes-g2-接入与验收)。

## 当前架构与组件分工

改版基于 `7b0a47fc7df8c85d1048312afce4e660c2a73609`。C 端与后台共用一个 Vue 3、Vite 和 TypeScript 应用。`AppLayout` 在 1024px 起采用浅色侧栏，较窄视口保留底部导航。`AdminLayout` 使用深色权限侧栏，在 900px 及以下通过抽屉显示。报告打印使用独立路由。C 端参考 Reactive Resume 的文档工作区，后台参考 Art Design Pro 的导航分组和表格层级。

C 端的主要操作复用 shadcn-vue Button，上传表单、抽屉和确认操作沿用 Element Plus。后台继续由 Element Plus、局部 SCSS 和公共表格组件承载。两端共享 `tokens.scss` 中的语义变量。C 端布局按路由加载 Tailwind CSS v4，由 `@theme inline` 对接，使用 `ip:` 前缀和显式 `@source`，保持 Preflight 关闭。Element Plus 通过 `--el-*` 变量对接。

页面与状态流见[详细设计与建模](../detailed-design-modeling/34-frontend-ui-redesign-modeling.md)，开发约定见[前端指南](../frontend.md)。下方编号章节保留 V1.0 的历史方案、回退条件和对应提交的验收证据。

## 本次整体验收

2026-11-17，Windows，Node `20.20.2`、npm `11.12.1`、Edge `157.0.4341.60`。验证对象为基于上述提交的本地改版工作树。浏览器使用生产构建的本地预览和合成 API 数据，后端授权验证使用隔离测试配置。

| 工作目录与命令 | 结果与覆盖范围 |
| --- | --- |
| `frontend/` · `npm ci --offline --no-audit --no-fund` | 退出 0，按锁文件安装 284 个包。 |
| `frontend/` · `npm run test:run -- --maxWorkers=1 --reporter=dot` | 退出 0，36 个文件、333 项通过，0 跳过。覆盖账号流程、投递状态更新、AI 任务状态与轮询、权限入口和页面行为。 |
| `frontend/` · `npm run type-check`、`npm run build` | 均退出 0。生产构建保留一个 HTML 入口。Vite 仍提示 `vendor-element` 分包超过 500 kB。 |
| `frontend/` · `node scripts/check-workspace-layout.cjs` | 退出 0，77 个检查通过。25 个入口在 1440、768、375px 各检查一次，另检查 C 端导航的 1023/1024px 边界。检查标题、跳转、文档宽度、版本深链接预览和未预期的 API 或脚本错误。 |
| `frontend/` · `node scripts/check-resume-g2.cjs`、`node scripts/check-analysis-p5.cjs`、`node scripts/check-admin-users-p6.cjs` | 均退出 0。覆盖简历加载、空列表、长名称、上传失败重试、删除取消、会话和样式隔离。分析脚本覆盖任务恢复、失败重试和键盘查看答案。后台覆盖筛选、分页、受限导航、抽屉焦点和取消操作。直接访问和经 C 端切换后的打印产物逐页对比一致，简历脚本同时检查两页 A4。 |
| `backend/` · `./gradlew.bat test --tests '*AdminSecurityTest' --tests '*ResumeControllerSecurityTest' --tests '*FeedbackSecurityTest' --no-daemon --max-workers=1 --offline` | 退出 0，18 项通过，0 跳过。Spring Security 与控制器在 test profile 下执行，业务服务使用 mock。 |

浏览器脚本从 `frontend/` 运行。先启动 `npm run preview -- --host 127.0.0.1 --port 5184 --strictPort`，将 `INTERNPILOT_PREVIEW_PORT` 设为 `5184`，`INTERNPILOT_PLAYWRIGHT` 指向本机的 `playwright-core` 模块。打印核验需要 PATH 中有 `pdfinfo` 与 `pdftoppm`。这次将 `INTERNPILOT_EVIDENCE` 和 `INTERNPILOT_ADMIN_EVIDENCE` 分别指向 `.local-evidence/frontend-rebuild/` 下的 `layout` 与 `admin-final`。其余浏览器回执也保存在该本地证据目录。

首次并行浏览器检查使开发服务器内存不足退出。改为逐项检查生产构建后复验。真实 AI、邮件、浏览器与真实业务数据全链路以及生产部署未纳入本次验证。

## 00. 决策摘要

**设计输入**：以[需求文档](../requirements/06-frontend-ui-redesign-requirements.md)的 REQ-C01～C05、REQ-A01～A02、REQ-X01、REQ-N01～N05 为交付边界。本文件负责技术方案、具体页面布局与实施顺序；新增需求必须先更新需求文档。

**技术方案**
- 继续使用 **Vue 3 + TypeScript + Vite** 的单一 `frontend/` 工程，保持现有 Vue Router、Pinia、Axios、ECharts、SockJS/STOMP。
- **C 端**：目标采用 `shadcn-vue（基于 Reka UI 的可复制组件代码）+ Tailwind CSS v4` 做新设计组件；**先以一个页面隔离试点**，不立即删除页面中仍依赖的 Element Plus。
- **管理后台**：继续采用 `Element Plus + 当前 Sass`，必要时仅使用受控的 Tailwind 布局 Utilities。借鉴 Art Design Pro 的后台布局和交互，不直接导入其整套后台工程。
- **统一 Design Tokens**：在当前 `frontend/src/styles/tokens.scss` 基础上增加语义映射，统一品牌色、前景、背景、边框、状态、间距、圆角、动效与响应式断点。C 端和后台可有不同密度与表面层级，但不能有两套相互矛盾的业务状态色。
- **暂不**：切换 React、拆成 C 端/后台两个部署应用、更换 npm 为 pnpm、统一重写所有 Sass、直接接入在线简历 WYSIWYG/PDF 排版引擎、改造后端 API 或生产部署。

组件试验支持为简历首批接入选择关闭 Preflight 的 Tailwind CSS v4 和 shadcn-vue Button。业务弹窗仍用 Element Plus。若后续出现无法隔离的跨页样式回归，使用 Element Plus 和局部 Sass 回退。

## 01. 试点前源码核验基线

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

## 05. 页面改造顺序与需求追踪

此处是技术实施的批次顺序，产品范围与验收状态以[需求文档](../requirements/06-frontend-ui-redesign-requirements.md)为准。

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

**G0｜现有页面基线已采集**
- 核对新 HEAD、工作区是否 clean、相关测试是否存在、当前 npm 依赖锁、Node 和目标浏览器范围。
- 收集现有 `/resumes` 和 `/admin/users` 的桌面/窄屏界面基线，使用**合成、脱敏数据**；截图需要有可追溯的版本与环境。
- 检查 C 端、后台共享样式的选择器和 `ElMessage` / `ElDrawer` / `ElDialog` 等 Teleport 行为。

**G1｜原型评审及隔离兼容性试验已完成**
- 确认 C 端/后台各自的导航、字体、色彩、布局、动效、状态表达规范；完成 `/resumes` 的桌面和移动原型、关键状态列表。
- 评估 Tailwind v4/shadcn-vue 的真实依赖兼容、构建增量、暗色/焦点/Portal 问题；失败可选纯 Element Plus + Sass 方案。
- 明确何者是纯视觉改动，何者改变了交互或需求；后者添加明确的验收条件。

**G2｜简历列表已实现并完成本地验证**
- 以独立分支从新 HEAD 出发；先最小化引入样式/Tokens，再只改 `/resumes`，保留上传、详情、版本、默认、删除和异常恢复。
- 从 `frontend/` 运行 `npm ci`、受影响 Vitest、`npm run type-check`、`npm run build`；评估全套 `npm run test:run` 的回归覆盖。失败、跳过和未执行分别记录。
- 桌面（例如 1440/1280）与移动（例如 375/768）在真实浏览器检查键盘、Tab 顺序、焦点返回、空/加载/错误、上传和取消。
- 对受影响范围做视觉对比；检查前端 bundle 变化、主要交互响应与页面横向溢出（保留检查方法和版本，**不设无证据的收益数字**）。

**G3｜PR 评审及 CI 检查**
- 重点 Review：REST/FormData 未变、登录/角色权限没回归、移动导航仍可用、AI 任务与 PDF 打印未被全局 CSS 破坏、敏感用户简历内容未流入样例或截图。
- 逐批 PR、检查 CI 与当前提交 SHA，按 `docs/git.md` 完成 Review；只有验证和决策都充分才可合并。
- 生产镜像、部署与回滚 **不在本计划的自动授权范围**；发布需要另行制定变更与回滚方案。

**停止/回退**：任何构建/类型/业务回归失败，先停止扩大组件替换；优先 revert 本批 UI 提交或切换保守的现有组件方案。禁止在生产实例上以热修方式验证 Tailwind 基础样式影响，不强推共享分支。

## 09. 验收映射与测试计划

**唯一的需求与验收状态表**在[需求与验收基线](../requirements/06-frontend-ui-redesign-requirements.md#四验收矩阵保留旧设计稿中的编号)。原有 `UI-01`、`UI-02`、`FUN-01`、`FUN-02`、`UX-01`、`UX-02`、`SEC-01`、`ENG-01`、`ADM-01` 编号保持不变，不应在此处复制另一份 PASS/FAIL 状态表。

- `frontend/src/views/resume/__tests__/ResumeList.spec.ts`、`ResumeVersionList.spec.ts`：针对 REQ-C02/C03/C04，验收 FUN-01、FUN-02。
- `frontend/src/views/admin/__tests__/AdminCrudPages.spec.ts`、`AdminHandlerPermissions.spec.ts` 及后台守卫测试：针对 REQ-A01/A02，验收 SEC-01、ADM-01。
- `frontend/src/components/layout/__tests__/`、`frontend/src/config/__tests__/navigation.spec.ts`、`frontend/src/views/analysis/__tests__/AnalysisReportPrint.spec.ts`：针对 REQ-X01/N01/N04，验收 UX-02。
- 在 `frontend/` 内运行 `npm run test:run`、`npm run type-check`、`npm run build` 并留存 SHA、命令、环境及退出码，对应 ENG-01。
- 浏览器验证桌面/375px 手机、键盘与焦点、Dialog、加载/空/错误、RBAC、删除确认和打印隔离，对应 UI-01/UI-02/UX-01/SEC-01。

本轮已执行项及剩余范围见[第 11 节](#11-resumes-g2-接入与验收)。

## 10. 待验证的技术方案与下一合法动作

产品范围、简历实时编辑和信息架构的决策以[需求文档](../requirements/06-frontend-ui-redesign-requirements.md#五变更管理与待确认问题)为准；此处只列尚未验证的技术事项。

- **组件方案**：shadcn-vue + Tailwind v4 是**候选架构**，只有隔离试点证明兼容后才升级为全站规范；失败优先回退已有 Element Plus/Sass。
- **生产边界**：现有生产服务版本、备份和上线策略不由本设计文档声明；此处完全没有发布/生产执行授权。

简历 G2 的真实后端联调和 Firefox 冒烟已完成。进入 G3 时用第 12 节的证据评审试点改动及剩余验收范围。

## 11. /resumes G2 接入与验收

### 实现范围

- 在 `codex/frontend-ui-resumes` 从 `b2d17660de6a93eefad3be2715a51937fcdcd626` 接入已评审的布局。验证对象为本分支的 G2 提交 `b6617ea8487a9f981b5a5d41f828e90cd25e7978`。工作区保留单一简历列表 DOM，在 940px 及以下呈现卡片。页首保留上传与默认简历摘要，列表行保留名称、原文件名、类型、大小、解析状态、默认标记和上传时间。
- G1 对比试验证实完整 Preflight 会改变旧页面标题字重和打印项目符号。G2 仅引入 Tailwind v4.3.5 的 theme 与 utilities，扫描范围为简历页和 Button。样式随路由加载，utility 使用 `ip:` 前缀。生成的主题变量和 Tailwind 属性声明仍可能随 CSS 留在文档内，跨页 SPA 回归以浏览器验证为准。
- shadcn-vue Button 用于上传入口，包含源代码许可。初批采用 Element Plus 的「更多」菜单、上传、详情抽屉和删除确认。上传失败时保留文件和名称并显示重试提示，成功后清理文件队列。列表、版本路由、上传字段、会话及权限沿用既有契约。

### 证据与复现

- [1440×960 桌面截图](https://github.com/wan719/intern-pilot/blob/7b0a47fc7df8c85d1048312afce4e660c2a73609/docs/design/frontend-ui-resume-g2/resumes-1440.png) · [375×812 移动截图](https://github.com/wan719/intern-pilot/blob/7b0a47fc7df8c85d1048312afce4e660c2a73609/docs/design/frontend-ui-resume-g2/resumes-375.png)。使用合成 API 数据，移动截图包含整页长名称示例，底栏固定在首个视口底部。
- Windows / Node `20.20.2` / npm `11.12.1` / Edge `157.0.4341.60`。在本分支的 `frontend/` 使用 `npm ci --offline --no-audit --no-fund`、`npm run test:run -- --reporter=dot`、`npm run type-check`、`npm run build`。命令均退出 `0`。Vitest 共 36 个文件、318 例通过，无跳过。生产构建只有 `dist/index.html` 一个 HTML 入口。Element Plus 分包仍有 Vite 的 500 kB 告警。
- 在 `frontend/` 执行 `npm run preview -- --host 127.0.0.1 --port 5184 --strictPort`。另开 PowerShell，将 `INTERNPILOT_PLAYWRIGHT` 设为临时安装的 `playwright-core@1.65.1` 模块绝对路径，并保证 `pdfinfo`、`pdftoppm` 在 PATH 中。用 Node 20 执行 [浏览器脚本](../../frontend/scripts/check-resume-g2.cjs)，退出 `0`，结果写入 `frontend/dist/resume-g2-evidence/`。
- 两种视口覆盖加载、空列表、读取失败重试、四种解析状态、长名称、上传失败保留文件并成功重试、菜单键盘操作、删除取消、弹窗焦点与滚动锁、无会话及 401/403。额外核对 940px/941px 的卡片和列表切换。后台和打印各对比直接访问与加载简历 CSS 后的同会话 SPA 导航。后台抽样样式一致，打印项目符号保留且两页 A4 的逐页像素一致。
- 第 11 节的浏览器证据来自模拟接口。真实服务端联调和 Firefox 结果见第 12 节。若本批需要回退，仅回退 G2 接入改动和试点依赖，数据及部署配置无迁移步骤。

## 12. G2 真实后端联调与 Firefox 冒烟

2026-11-16 在 `codex/frontend-ui-resumes` 的 `94ecffbb6a6dc5813ecd2c06fc1a84bcad389ee6` 上完成。本轮为本地 Windows 联调，使用独立的 MySQL 8.0.43、Redis 5.0.14.1、Temurin JDK 17.0.18、Node 20.20.2、Playwright Core 1.65.1。后端用真实 Spring Boot、权限校验、解析器、数据库和文件存储。验证码与 AI 使用 test profile 的 mock，实现浏览器测试所需的合成注册与报告数据。

| 场景 | 实际证据 |
| --- | --- |
| Edge 157.0.4341.60，1440×960 | PDF 和 DOCX 的上传均返回 HTTP 200 / 业务 200，详情展示合成解析文本。首份简历自动默认，第二份设为默认后刷新仍生效，并能从页面进入原始版本。 |
| 解析失败与恢复 | 不可解析 PDF 返回 HTTP 200 / 业务 700，页面保留文件并显示重试提示，列表未产生失败记录。移除错误文件并选择合成 DOCX 后上传成功。 |
| 删除 | 取消后浏览器未发 DELETE，刷新后保留记录。确认返回 HTTP 200 / 业务 200，删除后详情返回 HTTP 200 / 业务 400，另一份简历仍保留并为默认。 |
| 会话与角色权限 | 无会话访问简历页面跳转登录，直接读取 API 返回 HTTP 401 / 业务 401。普通账号访问管理列表 API 返回 HTTP 403 / 业务 403。隔离测试库的合成管理员读取该列表成功。 |
| 账号归属 | B 的列表没有 A 的两份简历；B 请求 A 的简历详情、设默认和删除均返回 HTTP 200 / 业务 400，响应没有业务数据。A 的列表和默认状态与操作前一致。 |
| Firefox 159.0，375×812 | 简历卡片、底部导航、更多菜单键盘操作、上传弹窗焦点返回及滚动释放通过。加载简历 CSS 后经同一 SPA 会话进入后台，抽样表格布局与直接访问一致。用 mock AI 产出的真实持久化报告检查打印媒体，保留列表圆点，隐藏操作栏，内容为黑色。 |

### 执行记录

- 在新临时目录运行 `mysqld --defaults-file=<临时目录>/mysql.ini --initialize-insecure`，随后用同一配置启动 MySQL；Redis 用 `redis-server <临时目录>/redis.conf` 启动。MySQL 客户端通过 `--protocol=TCP --host=127.0.0.1 --port=13316` 执行 `source backend/src/main/resources/sql/init.sql`，初始化退出 `0`，确认初始简历数为零。
- 在 `backend/` 执行 `.gradlew.bat bootJar --no-daemon`，退出 `0`。以 `java -jar <bootJar> --spring.profiles.active=dev,test --spring.config.additional-location=file:<临时目录>/application-real.properties` 启动，显式配置本轮 MySQL、Redis、独立上传目录及临时 JWT 密钥。MySQL、Redis、后端、前端监听 `127.0.0.1:13316/16389/18080/15173`。后端 `/actuator/health` 返回 `UP`，数据库与 Redis 组件均为 `UP`。
- 在 `frontend/` 设置 `VITE_API_BASE_URL=http://127.0.0.1:15173` 后使用 Node 20 执行 `npm run build`，退出 `0`。以 `vite preview --config <临时目录>/preview.config.mjs` 启动，`/api`、`/uploads`、`/ws` 代理到本轮后端。
- Playwright Core CLI 的 `install firefox` 退出 `0`。在本轮临时目录执行 `npm exec --yes --package=node@20.20.2 -- node check-real.cjs`，退出 `0`。本机 `$env:TEMP/internpilot-g2-real-94ecffb/evidence/` 保留脱敏的 `real-results.json`，以及 `edge-resumes-real.png`、`firefox-resumes-real-375.png`、`firefox-print-media-375.png` 和 `firefox-admin-after-resumes-375.png`。回执仅包含测试名称、HTTP／业务码及浏览器版本；截图使用合成数据。
- 已停止四个本轮监听。后台分页和写操作、Firefox 实体打印或 PDF 输出、Redis 7 兼容及生产验收仍待验证。前端全量测试、CI 和 PR 状态沿用各自的独立证据，本次执行范围是构建与联调。
