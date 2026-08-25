# InternPilot UI Product Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 InternPilot 现有 Vue 3 前端渐进重塑为“职业工作室”风格的求职产品，同时保持现有功能、接口、权限、路由、WebSocket 和打印能力兼容。

**Architecture:** 在原工程内建立 Sass 设计变量、Element Plus 主题、旅程导航配置、用户端顶栏布局和管理后台侧栏布局，再按业务领域逐页迁移。数据访问层、Pinia stores、Axios、Token 与 WebSocket 保持不变；新增组件只负责表现和交互组织。

**Tech Stack:** Vue 3.5、Vite 6、TypeScript 5.7、Vue Router 4、Pinia 2、Element Plus 2、Sass、ECharts、Vitest、Vue Test Utils、jsdom

**Spec:** `docs/superpowers/specs/2026-08-25-internpilot-ui-product-redesign-design.md`

## Global Constraints

- 所有工作只在 `codex/ui-product-redesign` 分支进行。
- 保留现有后端接口契约、数据库结构、路由 URL、权限语义、Pinia stores、Token、Axios 和 WebSocket 机制。
- 使用 Sass + Element Plus + CSS 设计变量；不引入 Tailwind、shadcn-vue 或新的 UI 框架。
- 主题固定为深色导航 + 明亮内容区；本轮不实现全局暗黑模式。
- 品牌方向固定为炭黑 + 松石绿；内容区使用细边框、中等圆角和适度留白。
- 用户端采用顶栏旅程导航并精细适配移动端；管理后台采用分组侧边栏并保证移动端基础可用。
- 不复制 Art Design Pro 商业版页面或专有资源。
- 页面迁移不得要求后端返回新的字段；派生显示数据在前端计算。
- 每个任务完成后运行相关测试、`npm run type-check` 和 `npm run build`，并使用独立提交。
- 未经用户明确要求，不推送远程、不合并 `main`、不部署线上。

---

## File Structure Map

### New files

- `frontend/intern-pilot-frontend/vitest.config.ts`：Vitest 的 Vue、jsdom 和别名配置。
- `frontend/intern-pilot-frontend/src/test/setup.ts`：Element Plus 与测试环境初始化。
- `frontend/intern-pilot-frontend/src/styles/tokens.scss`：品牌、语义、间距、圆角、阴影和动效变量。
- `frontend/intern-pilot-frontend/src/styles/element.scss`：Element Plus 全局主题与组件状态覆盖。
- `frontend/intern-pilot-frontend/src/config/navigation.ts`：用户旅程与后台分组导航的唯一配置源。
- `frontend/intern-pilot-frontend/src/types/router-meta.d.ts`：扩展 Vue Router 的旅程元数据类型。
- `frontend/intern-pilot-frontend/src/components/layout/JourneyNav.vue`：桌面用户旅程导航。
- `frontend/intern-pilot-frontend/src/components/layout/MobileBottomNav.vue`：移动端核心导航。
- `frontend/intern-pilot-frontend/src/components/layout/AdminSidebar.vue`：权限感知的后台分组侧边栏。
- `frontend/intern-pilot-frontend/src/components/common/PageHero.vue`：页面目标与主要行动区域。
- `frontend/intern-pilot-frontend/src/components/common/FilterBar.vue`：统一筛选工具栏。
- `frontend/intern-pilot-frontend/src/components/common/TableShell.vue`：统一表格容器、加载、空状态和移动降级。
- `frontend/intern-pilot-frontend/src/components/common/AiInsightPanel.vue`：AI 结论、证据、风险和下一步建议容器。
- `frontend/intern-pilot-frontend/src/**/__tests__/*.spec.ts`：导航与公共组件回归测试。

### Existing files with changed responsibilities

- `src/styles/index.scss`：只保留全局 reset、布局工具和跨页面响应式规则，并导入 tokens 与 Element Plus 主题。
- `src/router/index.ts`：保留路径和守卫，补充旅程领域元数据。
- `src/components/layout/AppLayout.vue`：从侧栏网格改为用户端顶栏应用外壳。
- `src/components/layout/AppHeader.vue`：组合品牌、旅程导航、AI 任务、反馈和账号入口。
- `src/components/layout/AdminLayout.vue`：组合后台侧栏、移动抽屉和后台头部。
- `src/components/common/*.vue`：统一页面、指标、状态和空状态接口。
- `src/views/**/*.vue`：只调整模板层级、文案、响应式和局部样式，不更改 API 调用语义。

---

### Task 1: Establish the UI Test Harness

**Files:**
- Modify: `frontend/intern-pilot-frontend/package.json`
- Create: `frontend/intern-pilot-frontend/vitest.config.ts`
- Create: `frontend/intern-pilot-frontend/src/test/setup.ts`
- Create: `frontend/intern-pilot-frontend/src/components/common/__tests__/StatusTag.spec.ts`

**Interfaces:**
- Consumes: existing `StatusTag.vue` props `{ status?, label?, size? }`.
- Produces: commands `npm run test` and `npm run test:run`; shared jsdom test environment with Element Plus installed.

- [ ] **Step 1: Install compatible test-only dependencies**

Run from `frontend/intern-pilot-frontend`:

```bash
npm install --save-dev vitest@^3.2.4 @vue/test-utils@^2.4.6 jsdom@^26.1.0
```

Expected: `package.json` and the existing lock file contain only test-related new dependencies.

- [ ] **Step 2: Add test scripts and Vitest configuration**

Add these scripts to `package.json`:

```json
"test": "vitest",
"test:run": "vitest run"
```

Create `vitest.config.ts`:

```ts
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts']
  }
})
```

Create `src/test/setup.ts`:

```ts
import { config } from '@vue/test-utils'
import ElementPlus from 'element-plus'

config.global.plugins = [ElementPlus]
```

- [ ] **Step 3: Write the harness regression test**

Create `StatusTag.spec.ts`:

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusTag from '@/components/common/StatusTag.vue'

describe('StatusTag', () => {
  it.each([
    ['COMPLETED', '已完成'],
    ['FAILED', '失败'],
    ['RUNNING', '进行中'],
    ['PENDING', '待处理']
  ])('renders %s as %s', (status, label) => {
    expect(mount(StatusTag, { props: { status } }).text()).toContain(label)
  })
})
```

- [ ] **Step 4: Verify the harness and existing build**

Run:

```bash
npm run test:run -- src/components/common/__tests__/StatusTag.spec.ts
npm run type-check
npm run build
```

Expected: all commands exit with code 0.

- [ ] **Step 5: Commit**

```bash
git add frontend/intern-pilot-frontend/package.json frontend/intern-pilot-frontend/package-lock.json frontend/intern-pilot-frontend/vitest.config.ts frontend/intern-pilot-frontend/src/test
git commit -m "test: add frontend component test harness"
```

### Task 2: Build the Design Token and Element Plus Theme Foundation

**Files:**
- Create: `frontend/intern-pilot-frontend/src/styles/tokens.scss`
- Create: `frontend/intern-pilot-frontend/src/styles/element.scss`
- Modify: `frontend/intern-pilot-frontend/src/styles/index.scss`
- Modify: `frontend/intern-pilot-frontend/src/main.ts`

**Interfaces:**
- Consumes: existing global class names and Element Plus CSS variables.
- Produces: stable tokens including `--color-primary`, `--color-nav`, `--color-bg`, `--color-surface`, `--color-border`, `--color-text`, `--radius-*`, `--space-*`, `--motion-fast`, and `--motion-base`.

- [ ] **Step 1: Record a build baseline**

Run:

```bash
npm run type-check
npm run build
```

Expected: both commands pass before token changes.

- [ ] **Step 2: Create the exact root token contract**

Create `tokens.scss` with at least this root contract:

```scss
:root {
  --color-primary: #0f9f8f;
  --color-primary-hover: #0f766e;
  --color-primary-soft: #ccfbf1;
  --color-primary-border: #99f6e4;
  --color-nav: #111827;
  --color-nav-soft: #1f2937;
  --color-bg: #f6f8f8;
  --color-surface: #ffffff;
  --color-surface-muted: #f8faf9;
  --color-border: #e2e8e6;
  --color-border-strong: #cbd5d1;
  --color-text: #17202a;
  --color-text-muted: #64748b;
  --color-info: #2563eb;
  --color-success: #15803d;
  --color-warning: #d97706;
  --color-danger: #dc2626;
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --radius-sm: 8px;
  --radius-md: 10px;
  --radius-lg: 12px;
  --shadow-floating: 0 18px 42px rgba(15, 23, 42, 0.12);
  --motion-fast: 150ms;
  --motion-base: 200ms;
}
```

- [ ] **Step 3: Move Element Plus overrides into `element.scss`**

Map Element Plus variables to the contract and preserve semantic colors:

```scss
:root {
  --el-color-primary: var(--color-primary);
  --el-color-success: var(--color-success);
  --el-color-warning: var(--color-warning);
  --el-color-danger: var(--color-danger);
  --el-border-radius-base: var(--radius-sm);
  --el-border-color: var(--color-border);
  --el-fill-color-light: var(--color-surface-muted);
}
```

Move current `.el-button`, `.el-table`, `.el-dialog`, `.el-drawer`, `.el-tag`, `.el-input__wrapper`, `.el-select__wrapper`, and `.el-textarea__inner` rules from `index.scss` into this file. Update table hover from blue to a neutral or teal-soft background.

- [ ] **Step 4: Simplify `index.scss` and add reduced motion**

Import tokens before global rules:

```scss
@use './tokens';
@use './element';

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

Keep the system font stack and add `font-variant-numeric: tabular-nums` to metric values. Remove obsolete blue-only literals when a token exists.

- [ ] **Step 5: Verify foundation styles**

Run:

```bash
npm run test:run
npm run type-check
npm run build
```

Expected: all commands pass; generated CSS contains `#0f9f8f` and no build warning about missing Sass imports.

- [ ] **Step 6: Commit**

```bash
git add frontend/intern-pilot-frontend/src/styles frontend/intern-pilot-frontend/src/main.ts
git commit -m "feat: establish InternPilot design tokens"
```

### Task 3: Create the Typed Journey Navigation Model

**Files:**
- Create: `frontend/intern-pilot-frontend/src/config/navigation.ts`
- Create: `frontend/intern-pilot-frontend/src/config/__tests__/navigation.spec.ts`
- Create: `frontend/intern-pilot-frontend/src/types/router-meta.d.ts`
- Modify: `frontend/intern-pilot-frontend/src/router/index.ts`

**Interfaces:**
- Consumes: current route paths and permission strings.
- Produces: `JourneyKey`, `JourneyItem`, `AdminNavGroup`, `journeyItems`, `adminNavGroups`, and `resolveJourneyKey(path)`.

- [ ] **Step 1: Write failing navigation tests**

Create `navigation.spec.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { journeyItems, resolveJourneyKey } from '@/config/navigation'

describe('journey navigation', () => {
  it('keeps workbench plus five journey stages in order', () => {
    expect(journeyItems.map((item) => item.key)).toEqual([
      'dashboard', 'resumes', 'jobs', 'analysis', 'interview', 'applications'
    ])
  })

  it.each([
    ['/dashboard', 'dashboard'],
    ['/resumes/7/versions', 'resumes'],
    ['/job-recommendations/3', 'jobs'],
    ['/analysis/reports', 'analysis'],
    ['/interview-questions/8', 'interview'],
    ['/applications', 'applications']
  ])('maps %s to %s', (path, key) => {
    expect(resolveJourneyKey(path)).toBe(key)
  })
})
```

- [ ] **Step 2: Run the test and confirm the missing module failure**

Run:

```bash
npm run test:run -- src/config/__tests__/navigation.spec.ts
```

Expected: FAIL because `@/config/navigation` does not exist.

- [ ] **Step 3: Implement the navigation contract**

Use this public shape in `navigation.ts`:

```ts
export type JourneyKey = 'dashboard' | 'resumes' | 'jobs' | 'analysis' | 'interview' | 'applications'

export interface JourneyItem {
  key: JourneyKey
  label: string
  shortLabel: string
  path: string
  mobile: boolean
  matches: (path: string) => boolean
}

export interface AdminNavItem {
  label: string
  path: string
  permission: string
}

export interface AdminNavGroup {
  label: string
  items: AdminNavItem[]
}
```

Set `jobs.matches` to accept both `/jobs` and `/job-recommendations`; set `analysis.matches` to accept `/analysis`. Set `mobile: true` for `dashboard`, `jobs`, `analysis`, and `applications`; set it to `false` for `resumes` and `interview`, which appear in the mobile “更多” drawer. Define admin groups exactly as “后台概览”, “用户与权限”, “AI 知识”, and “运营管理”, using the route and permission mapping in the spec.

- [ ] **Step 4: Type route metadata and annotate existing routes**

Create `router-meta.d.ts`:

```ts
import 'vue-router'
import type { JourneyKey } from '@/config/navigation'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    public?: boolean
    permission?: string
    journey?: JourneyKey
    mobilePrimary?: boolean
  }
}
```

Add the matching `journey` value to every user child route in `router/index.ts`. Do not change paths, redirects, lazy imports or guard logic.

- [ ] **Step 5: Run navigation and compile verification**

```bash
npm run test:run -- src/config/__tests__/navigation.spec.ts
npm run type-check
npm run build
```

Expected: all commands pass.

- [ ] **Step 6: Commit**

```bash
git add frontend/intern-pilot-frontend/src/config frontend/intern-pilot-frontend/src/types/router-meta.d.ts frontend/intern-pilot-frontend/src/router/index.ts
git commit -m "feat: define typed journey navigation"
```

### Task 4: Replace the User Sidebar with the Journey App Shell

**Files:**
- Create: `frontend/intern-pilot-frontend/src/components/layout/JourneyNav.vue`
- Create: `frontend/intern-pilot-frontend/src/components/layout/MobileBottomNav.vue`
- Create: `frontend/intern-pilot-frontend/src/components/layout/__tests__/JourneyNav.spec.ts`
- Modify: `frontend/intern-pilot-frontend/src/components/layout/AppLayout.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/layout/AppHeader.vue`
- Delete after successful migration: `frontend/intern-pilot-frontend/src/components/layout/AppSidebar.vue`
- Modify: `frontend/intern-pilot-frontend/src/styles/index.scss`

**Interfaces:**
- Consumes: `journeyItems`, `resolveJourneyKey`, `useAuthStore`, `AiTaskDrawer`, and `FeedbackDrawer`.
- Produces: desktop `JourneyNav` with prop `items: JourneyItem[]`; mobile `MobileBottomNav` with the same source and a “更多” drawer.

- [ ] **Step 1: Write the active-navigation component test**

```ts
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import JourneyNav from '@/components/layout/JourneyNav.vue'
import { journeyItems } from '@/config/navigation'

it('marks 岗位机会 active for a recommendation detail route', async () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/job-recommendations/:id', component: { template: '<div />' } }]
  })
  await router.push('/job-recommendations/3')
  await router.isReady()
  const wrapper = mount(JourneyNav, { props: { items: journeyItems }, global: { plugins: [router] } })
  expect(wrapper.get('[aria-current="page"]').text()).toContain('岗位机会')
})
```

- [ ] **Step 2: Confirm the test fails because the component is missing**

```bash
npm run test:run -- src/components/layout/__tests__/JourneyNav.spec.ts
```

Expected: FAIL resolving `JourneyNav.vue`.

- [ ] **Step 3: Implement semantic desktop and mobile navigation**

`JourneyNav.vue` must render a `<nav aria-label="求职旅程">` and router links. The active link must use `aria-current="page"`. `MobileBottomNav.vue` must render only items with `mobile: true` plus a “更多” button that exposes the remaining stages without duplicating route definitions.

- [ ] **Step 4: Recompose `AppHeader` and `AppLayout`**

Use this layout order:

```vue
<div class="user-app-shell">
  <AppHeader />
  <main id="main-content" class="user-app-main">
    <router-view :key="refreshKey" />
  </main>
  <MobileBottomNav :items="journeyItems" />
  <AiTaskDrawer />
  <FeedbackDrawer />
</div>
```

Move the brand, desktop `JourneyNav`, AI task entry, feedback entry, refresh action and account dropdown into `AppHeader`. The AI task button calls `useAiTaskCenterStore().openDrawer`; the feedback button calls `useFeedbackStore().openDrawer`. Keep `aiTaskCenter.initialize()` in `AppLayout` and keep current-user loading behavior. Remove `AiTaskFloat` and `FeedbackFloat` only after those header actions open their existing drawers.

- [ ] **Step 5: Remove obsolete sidebar CSS and component**

Delete `.app-shell` two-column and `.app-sidebar` rules, replace them with sticky top-header, centered content and mobile bottom padding. Delete `AppSidebar.vue` only after `rg "AppSidebar" src` returns no imports.

- [ ] **Step 6: Verify navigation behavior**

Run:

```bash
npm run test:run -- src/components/layout/__tests__/JourneyNav.spec.ts
npm run type-check
npm run build
```

Then open `/dashboard`, `/job-recommendations`, `/analysis/reports`, and `/applications` at desktop and 375px widths. Expected: correct active stage, no horizontal overflow, and account/AI/feedback actions remain reachable.

- [ ] **Step 7: Commit**

```bash
git add frontend/intern-pilot-frontend/src/components/layout frontend/intern-pilot-frontend/src/styles/index.scss
git commit -m "feat: add journey-based user app shell"
```

### Task 5: Rebuild the Admin Shell and Mobile Drawer

**Files:**
- Create: `frontend/intern-pilot-frontend/src/components/layout/AdminSidebar.vue`
- Create: `frontend/intern-pilot-frontend/src/components/layout/__tests__/AdminSidebar.spec.ts`
- Modify: `frontend/intern-pilot-frontend/src/components/layout/AdminLayout.vue`

**Interfaces:**
- Consumes: props `{ groups: AdminNavGroup[]; can: (permission: string) => boolean }`, current route and existing admin route paths. `AdminLayout` passes `adminNavGroups` and `auth.hasPermission`.
- Produces: permission-filtered grouped sidebar and mobile drawer using the same group configuration.

- [ ] **Step 1: Write the permission-filtering test**

Mount `AdminSidebar` with `adminNavGroups` and a `can` function that accepts only `user:read`:

```ts
const wrapper = mount(AdminSidebar, {
  props: {
    groups: adminNavGroups,
    can: (permission: string) => permission === 'user:read'
  },
  global: { plugins: [router] }
})
expect(wrapper.text()).toContain('用户管理')
expect(wrapper.text()).not.toContain('角色管理')
expect(wrapper.text()).not.toContain('RAG 知识库')
```

- [ ] **Step 2: Run the focused test and confirm the missing component failure**

```bash
npm run test:run -- src/components/layout/__tests__/AdminSidebar.spec.ts
```

Expected: FAIL resolving `AdminSidebar.vue`.

- [ ] **Step 3: Implement grouped, permission-aware navigation**

Render group labels only when at least one child item is visible. Use router links or `router.push` with the unchanged admin paths. Use `aria-current="page"` on the active item.

- [ ] **Step 4: Recompose `AdminLayout.vue`**

Desktop layout uses a sticky 240px dark sidebar. At widths below 900px, hide the fixed sidebar and expose the same `AdminSidebar` inside `el-drawer`. Keep refresh and “返回用户工作台” actions in the header. Preserve current-user loading and `router-view :key="refreshKey"`.

- [ ] **Step 5: Verify permissions and responsive layout**

```bash
npm run test:run -- src/components/layout/__tests__/AdminSidebar.spec.ts
npm run type-check
npm run build
```

Open `/admin/dashboard`, `/admin/users`, and `/admin/rag-knowledge` with accounts of different permissions. Expected: no inaccessible item appears, and the mobile drawer closes after route navigation.

- [ ] **Step 6: Commit**

```bash
git add frontend/intern-pilot-frontend/src/components/layout/AdminSidebar.vue frontend/intern-pilot-frontend/src/components/layout/AdminLayout.vue frontend/intern-pilot-frontend/src/components/layout/__tests__/AdminSidebar.spec.ts
git commit -m "feat: redesign the admin navigation shell"
```

### Task 6: Upgrade Shared Page and Data Components

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/components/common/PageContainer.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/common/AppPageHeader.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/common/StatCard.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/common/StatusTag.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/common/AppEmpty.vue`
- Create: `frontend/intern-pilot-frontend/src/components/common/PageHero.vue`
- Create: `frontend/intern-pilot-frontend/src/components/common/FilterBar.vue`
- Create: `frontend/intern-pilot-frontend/src/components/common/TableShell.vue`
- Create: `frontend/intern-pilot-frontend/src/components/common/AiInsightPanel.vue`
- Create: `frontend/intern-pilot-frontend/src/components/common/__tests__/SharedUi.spec.ts`
- Modify: `frontend/intern-pilot-frontend/src/styles/index.scss`

**Interfaces:**
- `PageContainer`: `{ title: string; description?: string; eyebrow?: string; width?: 'default' | 'wide' }`, slots `actions`, `hero`, default.
- `StatCard`: existing props plus `{ hint?: string; trend?: string; tone?: 'primary' | 'info' | 'success' | 'warning' }`.
- `PageHero`: `{ eyebrow?: string; title: string; description?: string }`, slots `actions`, default.
- `FilterBar`: slots `filters`, `actions`; emits `reset`.
- `TableShell`: `{ loading: boolean; empty: boolean; emptyTitle: string; emptyHint?: string }`, slots `toolbar`, default, `empty-actions`.
- `AiInsightPanel`: `{ title: string; tone: 'strength' | 'risk' | 'action' }`, default slot.

- [ ] **Step 1: Write component contract tests**

Test that `AppEmpty` renders its action slot, `StatCard` renders a trend, `TableShell` shows an actionable empty state only when `empty && !loading`, and `AiInsightPanel` exposes a heading.

```ts
expect(mount(AppEmpty, { props: { title: '还没有目标岗位' }, slots: { default: '<button>添加岗位</button>' } }).text()).toContain('添加岗位')
expect(mount(StatCard, { props: { label: '准备度', value: '78%', icon: {}, trend: '+6%' } }).text()).toContain('+6%')
```

- [ ] **Step 2: Run tests and confirm new component failures**

```bash
npm run test:run -- src/components/common/__tests__/SharedUi.spec.ts
```

Expected: FAIL because the new components do not exist and existing props are incomplete.

- [ ] **Step 3: Implement components without business dependencies**

Components may import Element Plus icons and other common components, but must not import APIs or stores. Use semantic headings and slots; do not encode page-specific labels inside `FilterBar` or `TableShell`.

- [ ] **Step 4: Consolidate shared responsive styles**

Replace the long selector list for action groups with a reusable `.responsive-actions` class. Keep a temporary compatibility alias for existing page classes until Task 15, then remove it.

- [ ] **Step 5: Run component and build verification**

```bash
npm run test:run -- src/components/common/__tests__/SharedUi.spec.ts
npm run test:run
npm run type-check
npm run build
```

Expected: all commands pass.

- [ ] **Step 6: Commit**

```bash
git add frontend/intern-pilot-frontend/src/components/common frontend/intern-pilot-frontend/src/styles/index.scss
git commit -m "feat: add shared product UI primitives"
```

### Task 7: Redesign Authentication and Access-Denied Screens

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/views/auth/Login.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/auth/Register.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/error/Forbidden.vue`
- Modify: `frontend/intern-pilot-frontend/src/styles/index.scss`

**Interfaces:**
- Consumes: existing login, registration, verification-code APIs and auth store actions.
- Produces: responsive “职业工作室” auth shell with unchanged form fields, validation and submit behavior.

- [ ] **Step 1: Capture current auth behavior**

Run the app and record successful login, invalid credentials, verification-code request, registration validation, and `/403` navigation. Do not change request payloads.

- [ ] **Step 2: Recompose login and registration templates**

Keep current form models and handlers. Apply the shared brand shell and action-first copy:

```vue
<section class="auth-page auth-page--career">
  <div class="auth-panel">
    <aside class="auth-copy">...</aside>
    <main class="auth-form" aria-labelledby="auth-title">...</main>
  </div>
</section>
```

Use charcoal and teal tokens, retain visible field labels, and keep error messages adjacent to inputs.

- [ ] **Step 3: Redesign `/403` as an actionable state**

Show the missing-access explanation and buttons to return to the dashboard or previous page. Do not reveal permission internals to ordinary users.

- [ ] **Step 4: Verify auth behavior at desktop and 375px**

```bash
npm run test:run
npm run type-check
npm run build
```

Expected: forms remain keyboard-usable; no horizontal overflow; successful login still redirects to `/dashboard`.

- [ ] **Step 5: Commit**

```bash
git add frontend/intern-pilot-frontend/src/views/auth frontend/intern-pilot-frontend/src/views/error frontend/intern-pilot-frontend/src/styles/index.scss
git commit -m "feat: redesign authentication experience"
```

### Task 8: Turn the Dashboard into the Career Action Workspace

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/views/dashboard/Dashboard.vue`

**Interfaces:**
- Consumes: all existing dashboard API calls and the existing `getInterviewQuestionReportsApi` list endpoint.
- Produces: journey progress, “today actions”, summary metrics, recent results and shortcut sections without new backend fields.

- [ ] **Step 1: Identify existing metrics and derive journey progress locally**

Add `interviewReportCount = ref(0)`, load `getInterviewQuestionReportsApi({ page: 1, size: 1 })` alongside the existing dashboard requests, and derive the stages from those counts:

```ts
const journeyProgress = computed(() => ({
  resume: summary.resumes > 0,
  job: summary.jobs > 0,
  analysis: summary.reports > 0,
  interview: interviewReportCount.value > 0,
  application: summary.applications > 0
}))
```

Normalize the interview list response with `interviewReportCount.value = result.total ?? result.records?.length ?? (Array.isArray(result) ? result.length : 0)`.

- [ ] **Step 2: Replace the dashboard hierarchy**

Order content as `PageHero` → journey progress → three or four `StatCard`s → today actions → recent results. The primary CTA must point to the earliest incomplete stage.

- [ ] **Step 3: Replace generic quick links with action copy**

Examples: “上传第一份简历”, “添加目标岗位”, “开始 AI 匹配”, “准备下一场面试”. Preserve existing router targets.

- [ ] **Step 4: Verify all metric states**

Check zero-data, partially complete, fully complete, loading and API-error states at desktop and mobile widths.

```bash
npm run test:run
npm run type-check
npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/intern-pilot-frontend/src/views/dashboard/Dashboard.vue
git commit -m "feat: turn dashboard into career action workspace"
```

### Task 9: Migrate Resume Center Pages

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/views/resume/ResumeList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/resume/ResumeVersionList.vue`

**Interfaces:**
- Consumes: existing resume and resume-version APIs, upload behavior, default-resume action and confirmation flows.
- Produces: mobile-friendly resume cards, version timeline, actionable empty state and shared page components.

- [ ] **Step 1: Preserve all current resume actions in a checklist**

Confirm the templates still expose upload, view/download where present, set-default, version navigation, optimization and delete actions. Use this list as the manual regression checklist.

- [ ] **Step 2: Rebuild `ResumeList.vue` with shared primitives**

Use `PageContainer`, `PageHero`, `FilterBar` where needed, and an actionable `AppEmpty`. Desktop may retain a structured list or table; under 768px render each resume as a card with name, default status, upload time and a grouped action menu.

- [ ] **Step 3: Rebuild `ResumeVersionList.vue` as a version timeline**

Show the current/default version first, keep version identifiers and dates visible, and place destructive actions behind confirmation. Do not change route params or API payloads.

- [ ] **Step 4: Verify resume flows**

Test upload success/failure, set default, open versions, empty state, long filename and delete confirmation.

```bash
npm run test:run
npm run type-check
npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/intern-pilot-frontend/src/views/resume
git commit -m "feat: redesign resume center"
```

### Task 10: Migrate Job and Recommendation Pages

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/views/job/JobList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/recommendation/JobRecommendationList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/recommendation/JobRecommendationDetail.vue`

**Interfaces:**
- Consumes: existing job CRUD and recommendation batch APIs and `analysis:read` permission.
- Produces: unified “岗位机会” experience with target jobs and AI recommendations sharing one visual vocabulary.

- [ ] **Step 1: Preserve job and recommendation action coverage**

List and retain create, edit, delete, view recommendation batch, open recommendation detail, create application and start analysis actions that already exist.

- [ ] **Step 2: Redesign `JobList.vue`**

Use action-oriented title “锁定目标岗位”. Place filters in `FilterBar`; use `TableShell` or responsive job cards. Keep long JD text collapsed with an explicit expand action.

- [ ] **Step 3: Redesign recommendation list and detail**

Use `AiInsightPanel` to separate recommendation reason, fit evidence and risk. Keep score prominent but not as the only indicator. Put the primary next action—analyze, prepare interview or track application—next to each recommendation.

- [ ] **Step 4: Verify permission and responsive behavior**

Check with and without `analysis:read`; check long company names, no recommendations, failed recommendation batch and 375px layout.

```bash
npm run test:run
npm run type-check
npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/intern-pilot-frontend/src/views/job frontend/intern-pilot-frontend/src/views/recommendation
git commit -m "feat: redesign job opportunity experience"
```

### Task 11: Migrate AI Match and Analysis Report Pages

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/views/analysis/AnalysisMatch.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/analysis/AnalysisReportList.vue`

**Interfaces:**
- Consumes: existing analysis creation, task-status, report and WebSocket flows.
- Produces: guided match setup, visible progress/reconnect state, report cards and AI insight hierarchy.

- [ ] **Step 1: Trace current analysis states before editing**

Record exact template states for idle, validating, submitted, processing, completed, failed and reconnected tasks. Keep the same store and API transitions.

- [ ] **Step 2: Redesign `AnalysisMatch.vue` as a guided action**

Present resume selection, job selection and confirmation as a clear sequence. The primary button must state the action and loading status. Progress must include percentage, textual stage and reconnect/retry affordance.

- [ ] **Step 3: Redesign `AnalysisReportList.vue`**

Use summary metrics, `FilterBar`, and responsive report cards. Each report must expose score, company, role, date, strengths, risks and next actions without relying only on score color.

- [ ] **Step 4: Verify async states and refresh recovery**

Start an analysis, refresh during processing, simulate WebSocket interruption, reopen the task center and open the completed report. Confirm no request payload or route changed.

```bash
npm run test:run
npm run type-check
npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/intern-pilot-frontend/src/views/analysis/AnalysisMatch.vue frontend/intern-pilot-frontend/src/views/analysis/AnalysisReportList.vue
git commit -m "feat: redesign AI match and report experience"
```

### Task 12: Migrate Interview Preparation Pages

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/views/interview/InterviewQuestionList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/interview/InterviewQuestionDetail.vue`

**Interfaces:**
- Consumes: existing question generation, list, detail, answer, follow-up and keyword data.
- Produces: focused study workflow with readable mobile content and preserved generation behavior.

- [ ] **Step 1: Preserve question taxonomy and generation states**

Record current category, difficulty, status, generation, delete and detail interactions. Do not rename values sent to the API.

- [ ] **Step 2: Redesign the list as a preparation queue**

Use `FilterBar` for category/difficulty/status. Each item shows role context, category, difficulty, question count/status and one primary study action. Mobile items become stacked cards.

- [ ] **Step 3: Redesign the detail page for reading**

Order content as question → answer → key points → follow-ups. Use `AiInsightPanel` or equivalent semantic sections, readable line length and sticky desktop actions that become normal flow on mobile.

- [ ] **Step 4: Verify long-form and generation states**

Check long answers, many keywords, no follow-up, generation loading/failure, delete confirmation and keyboard focus.

```bash
npm run test:run
npm run type-check
npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/intern-pilot-frontend/src/views/interview
git commit -m "feat: redesign interview preparation experience"
```

### Task 13: Migrate Applications, User Center, AI Tasks, and Feedback

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/views/application/ApplicationList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/user/UserCenter.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/ai/AiTaskItem.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/ai/AiTaskDrawer.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/feedback/FeedbackDrawer.vue`
- Remove after verified unused: `frontend/intern-pilot-frontend/src/components/ai/AiTaskFloat.vue`
- Remove after verified unused: `frontend/intern-pilot-frontend/src/components/feedback/FeedbackFloat.vue`

**Interfaces:**
- Consumes: existing application APIs, user profile/avatar APIs, task-center store and feedback store/API.
- Produces: action-centered application tracking, account settings, header-driven task/feedback drawers and no duplicate floating controls.

- [ ] **Step 1: Verify header entries open existing drawers**

Before deleting floats, add or confirm explicit buttons in `AppHeader` whose click handlers call `useAiTaskCenterStore().openDrawer()` and `useFeedbackStore().openDrawer()`. Run `rg "AiTaskFloat|FeedbackFloat" src` and remove components only when imports are gone.

- [ ] **Step 2: Redesign application tracking**

Use `FilterBar` for status and search; present application status as text plus icon/color. Desktop uses a structured table or timeline; mobile uses cards with the next available action visible.

- [ ] **Step 3: Redesign user center**

Separate account identity, education profile and security/session actions into clear sections. Keep existing avatar upload and profile payloads unchanged. Ensure form labels remain visible.

- [ ] **Step 4: Restyle AI task and feedback drawers**

AI tasks show status, stage, progress, result link and retry/recovery action. Feedback uses a clear form, success state and retained error state. Both drawers must fit above mobile bottom navigation.

- [ ] **Step 5: Verify global interactions**

Check running/completed/failed task states, unread indicator, feedback submission success/failure, profile save, avatar display and application status updates.

```bash
npm run test:run
npm run type-check
npm run build
```

- [ ] **Step 6: Commit**

```bash
git add frontend/intern-pilot-frontend/src/views/application frontend/intern-pilot-frontend/src/views/user frontend/intern-pilot-frontend/src/components/ai frontend/intern-pilot-frontend/src/components/feedback frontend/intern-pilot-frontend/src/components/layout/AppHeader.vue
git commit -m "feat: redesign tracking and global user actions"
```

### Task 14: Migrate All Admin Pages to the Shared System

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/views/admin/AdminDashboard.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/admin/AdminUserList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/admin/AdminRoleList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/admin/AdminPermissionList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/admin/AdminRagKnowledgeList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/admin/OperationLogList.vue`
- Modify: `frontend/intern-pilot-frontend/src/views/admin/AdminFeedbackList.vue`

**Interfaces:**
- Consumes: existing admin APIs and permission strings.
- Produces: dense, consistent admin pages using `PageContainer`, `StatCard`, `FilterBar`, `TableShell`, `StatusTag` and `AppEmpty`.

- [ ] **Step 1: Create an admin action inventory**

For each page list current read, create, edit, enable/disable, assign, delete, reply and detail operations. Preserve permission directives/components and confirmation behavior.

- [ ] **Step 2: Migrate dashboard and read-heavy pages**

Apply shared metrics and chart panels to `AdminDashboard.vue`; apply `FilterBar` and `TableShell` to operation logs and feedback. Keep chart initialization and API calls unchanged.

- [ ] **Step 3: Migrate permission-sensitive CRUD pages**

Update users, roles, permissions and RAG knowledge pages. Keep every `PermissionButton`, permission check and payload intact. Use compact desktop tables; on mobile, allow horizontal scrolling rather than hiding critical columns.

- [ ] **Step 4: Normalize dialog and drawer forms**

Use shared spacing, visible labels, consistent footer actions and inline validation. Ensure cancel never submits and destructive actions remain visually distinct from primary teal actions.

- [ ] **Step 5: Verify with multiple permission sets**

Check full admin, user-only admin and restricted accounts. Confirm hidden actions cannot be reached from the UI and router guards still send unauthorized routes to `/403`.

```bash
npm run test:run
npm run type-check
npm run build
```

- [ ] **Step 6: Commit**

```bash
git add frontend/intern-pilot-frontend/src/views/admin
git commit -m "feat: migrate admin pages to shared design system"
```

### Task 15: Protect Print Layout, Accessibility, Responsive Behavior, and Final Consistency

**Files:**
- Modify: `frontend/intern-pilot-frontend/src/views/analysis/AnalysisReportPrint.vue`
- Modify: `frontend/intern-pilot-frontend/src/styles/index.scss`
- Modify: `frontend/intern-pilot-frontend/src/components/layout/AppHeader.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/layout/JourneyNav.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/layout/MobileBottomNav.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/layout/AdminSidebar.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/common/AppPageHeader.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/common/FilterBar.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/common/TableShell.vue`
- Modify: `frontend/intern-pilot-frontend/src/components/common/AiInsightPanel.vue`
- Modify: `README.md`
- Modify: `README_EN.md`

**Interfaces:**
- Consumes: completed redesigned pages and existing print route.
- Produces: verified desktop/mobile/print behavior, accessible interaction states, removed compatibility CSS and updated screenshots/documentation references.

- [ ] **Step 1: Audit obsolete selectors and components**

Run:

```bash
rg "#2563eb|#1d4ed8|#eff6ff|AppSidebar|AiTaskFloat|FeedbackFloat" src
rg "application-actions|job-actions|resume-actions|report-actions|interview-actions|batch-actions" src
```

Replace remaining obsolete blue literals with semantic tokens unless the usage is intentionally informational. Replace page-specific action classes with `.responsive-actions`; remove compatibility aliases only when searches return no consumers.

- [ ] **Step 2: Verify and protect print behavior**

Ensure `AnalysisReportPrint.vue` does not render screen navigation, mobile bottom navigation, task center or feedback. Add print CSS that forces white background, black readable text, no clipped content and stable page breaks.

- [ ] **Step 3: Run keyboard and accessibility checks**

For login, desktop journey nav, mobile nav, one dialog, one drawer, analysis flow and admin sidebar: tab through controls, verify visible focus, correct accessible names, no color-only status and Escape behavior for overlays.

- [ ] **Step 4: Run the responsive visual matrix**

Check these routes at 375px, 768px, 1280px and 1440px:

```text
/login
/dashboard
/resumes
/jobs
/analysis/match
/analysis/reports
/interview-questions
/applications
/user/center
/admin/dashboard
/admin/users
/analysis/reports/:id/print
```

Expected: no unintended horizontal page overflow; fixed controls do not overlap; all primary actions remain reachable; management tables either fit or scroll within their container.

- [ ] **Step 5: Run the full automated gate**

```bash
npm run test:run
npm run type-check
npm run build
```

Expected: all commands exit with code 0 and production assets are generated in `dist/`.

- [ ] **Step 6: Update project documentation**

Update README technology/experience descriptions and replace screenshots only after the corresponding pages are visually verified. Do not alter release version numbers unless the user explicitly requests a release.

- [ ] **Step 7: Review the complete diff and commit**

```bash
git diff --check
git status --short
git diff --stat main...HEAD
git add frontend/intern-pilot-frontend README.md README_EN.md
git commit -m "chore: complete UI redesign verification"
```

Expected: `.superpowers/` and generated `dist/` are absent from the commit.

---

## Final Acceptance Checklist

- [ ] All existing user and admin routes still resolve at their original URLs.
- [ ] Authentication, permission checks and `/403` behavior are unchanged.
- [ ] AI task progress survives refresh and WebSocket reconnect behavior remains available.
- [ ] Report printing contains no application chrome and remains readable on paper/PDF.
- [ ] User desktop navigation follows workbench plus five journey stages.
- [ ] User mobile navigation exposes all stages through primary items plus “更多”.
- [ ] Admin navigation is grouped, permission-filtered and available through a mobile drawer.
- [ ] Teal is used for brand/action; info, warning, danger and success remain semantically distinct.
- [ ] Loading, empty, error, long-text and destructive-action states are checked.
- [ ] `npm run test:run`, `npm run type-check` and `npm run build` pass.
- [ ] Every migration task has its own commit and the branch has not been pushed, merged or deployed without user instruction.
