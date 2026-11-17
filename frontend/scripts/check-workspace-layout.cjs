const assert = require('node:assert/strict')
const { mkdir, writeFile } = require('node:fs/promises')
const path = require('node:path')
const { chromium } = require(process.env.INTERNPILOT_PLAYWRIGHT || 'playwright-core')

const origin = 'http://127.0.0.1:' + (process.env.INTERNPILOT_PREVIEW_PORT || '5184')
const output = path.resolve(process.env.INTERNPILOT_EVIDENCE || 'dist/workspace-evidence')
const viewports = [
  { width: 1440, height: 900 },
  { width: 768, height: 1024 },
  { width: 375, height: 812 }
]
const publicPaths = ['/', '/login', '/register', '/403']
const workspacePaths = [
  '/dashboard', '/resumes', '/resumes/1/versions?versionId=5', '/jobs',
  '/job-recommendations', '/job-recommendations/11', '/analysis/match', '/analysis/reports',
  '/interview-questions', '/interview-questions/41', '/applications', '/user/center'
]
const adminPaths = [
  '/admin', '/admin/dashboard', '/admin/users', '/admin/roles', '/admin/permissions',
  '/admin/rag-knowledge', '/admin/operation-logs', '/admin/feedback'
]
const printPath = '/analysis/reports/91/print'

const date = '2026-11-16T09:30:00'
const user = {
  userId: 17, username: 'sample', nickname: '合成运营账号', email: 'sample@example.invalid',
  roles: ['ADMIN'], permissions: [
    'admin:dashboard', 'user:read', 'user:update', 'role:read', 'permission:read',
    'rag:read', 'operation-log:read', 'feedback:read', 'analysis:read'
  ],
  createdAt: date, enabled: 1
}
const resume = {
  resumeId: 1, resumeName: '合成简历', originalFileName: 'sample.pdf', parseStatus: 'SUCCESS',
  isDefault: true, fileType: 'PDF', fileSize: 4096, createdAt: date
}
const version = {
  versionId: 5, resumeId: 1, versionName: '实习求职版', versionType: 'MANUAL', isCurrent: 1,
  content: '合成版本正文\n\n项目经历与专业技能', createdAt: date, updatedAt: date
}
const job = {
  jobId: 7, companyName: '合成公司', jobTitle: '前端实习生', jdContent: '开发与维护应用', createdAt: date
}
const report = {
  ...job, reportId: 91, resumeId: 1, resumeVersionId: 5, resumeName: resume.resumeName,
  matchScore: 82, matchLevel: '高匹配', strengths: ['合成优势'], weaknesses: ['合成风险'],
  missingSkills: ['合成技能'], suggestions: ['合成建议'], interviewTips: ['合成提示']
}
const interview = { ...job, reportId: 41, title: '合成题单', questionCount: 0, questions: [] }
const batch = { batchId: 11, title: '合成岗位推荐', createdAt: date, jobCount: 1, recommendedCount: 1,
  items: [{ ...job, recommendationScore: 82 }] }
const application = {
  ...job, applicationId: 17, status: 'APPLIED', priority: 'HIGH', resumeId: 1,
  applyDate: '2026-11-16', resumeName: resume.resumeName, interviewDate: new Date(Date.now() + 86400000).toISOString()
}
const counted = (...records) => ({ records, total: records.length })
const fixtures = new Map(Object.entries({
  '/api/user/me': user, '/api/user/profile': user,
  '/api/health/ai-provider': { provider: 'mock' },
  '/api/analysis/tasks/running': [], '/api/analysis/tasks/recent': [],
  '/api/resumes': counted(resume), '/api/resumes/1': resume,
  '/api/resumes/1/versions': [version], '/api/resumes/1/versions/5': version,
  '/api/jobs': counted(job), '/api/jobs/7': job,
  '/api/analysis/reports': counted(report), '/api/analysis/reports/91': report,
  '/api/interview-questions': counted(interview), '/api/interview-questions/41': interview,
  '/api/job-recommendations': counted(batch), '/api/job-recommendations/11': batch,
  '/api/applications': counted(application), '/api/applications/17': application,
  '/api/admin/dashboard/summary': { userCount: 1248, resumeCount: 2816, jobCount: 1572, analysisReportCount: 986, interviewQuestionReportCount: 348, applicationCount: 735, todayNewUserCount: 24, failedOperationCount: 3 }, '/api/admin/users': counted(user),
  '/api/admin/roles': [{ roleId: 2, roleCode: 'USER', roleName: '普通用户', enabled: 1 }],
  '/api/admin/permissions': counted(), '/api/admin/rag/knowledge': counted(),
  '/api/admin/operation-logs': counted(), '/api/admin/feedback': counted()
}))

async function verifyRoute(page, route, viewport) {
  const response = await page.goto(origin + route)
  assert.equal(response.status(), 200, route)
  await page.getByRole('heading', { level: 1 }).first().waitFor()
  await page.waitForLoadState('networkidle')
  const expected = route === '/admin' ? '/admin/dashboard' : route
  assert.equal(page.url(), origin + expected)

  const sizes = await page.evaluate(() => ({
    client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth
  }))
  assert(sizes.scroll <= sizes.client + 1, `${route} at ${viewport.width}px: ${sizes.scroll} > ${sizes.client}`)

  if (workspacePaths.includes(route)) {
    assert.equal(await page.getByRole('navigation', { name: '求职旅程', exact: true }).isVisible(), viewport.width >= 1024)
    assert.equal(await page.getByRole('navigation', { name: '移动端求职旅程' }).isVisible(), viewport.width < 1024)
  }
  if (route === '/') {
    await page.getByRole('heading', { level: 1, name: /^实习求职，\s*从简历到投递$/ }).waitFor()
    assert.equal(await page.locator('.workspace-sidebar, .mobile-bottom-nav').count(), 0)
  }
  if (route === '/resumes/1/versions?versionId=5') {
    const preview = viewport.width > 1024
      ? page.locator('#version-preview-panel')
      : page.getByRole('dialog', { name: '版本详情' })
    await preview.getByText('合成版本正文', { exact: false }).waitFor()
  }
  if (route === printPath) {
    assert(await page.locator('.print-page').isVisible())
    assert.equal(await page.locator('.workspace-sidebar, .mobile-bottom-nav').count(), 0)
  }
  if (['/dashboard', '/resumes/1/versions?versionId=5', '/admin/users'].includes(route)) {
    const key = new URL(origin + route).pathname.slice(1).replaceAll('/', '-')
    await page.screenshot({ path: path.join(output, `${key}-${viewport.width}.png`), animations: 'disabled' })
  }
  return { route, width: viewport.width, heading: await page.getByRole('heading', { level: 1 }).first().textContent() }
}


async function verifyAppearance(page) {
  await page.goto(origin + '/dashboard')
  await page.getByRole('heading', { name: '即将到来' }).waitFor()
  await page.evaluate(() => sessionStorage.removeItem('internpilot:admin-work-tabs:v1:17'))
  const root = page.locator('html')
  const shell = page.locator('.user-app-shell')
  const drawer = page.locator('.workspace-settings')
  const account = page.getByRole('button', { name: '打开账号菜单' })
  const option = (group, label) => drawer.getByRole('group', { name: group, exact: true }).getByRole('button', { name: label, exact: true })
  const visible = async (selector, expected) => assert.equal(await page.locator(selector).isVisible(), expected, selector)
  const shot = name => page.screenshot({ path: path.join(output, 'appearance-' + name + '.png'), animations: 'disabled' })

  await page.getByText('合成公司 · 前端实习生', { exact: true }).first().waitFor()
  assert.equal(await root.getAttribute('data-workspace-preset'), 'clario')
  await account.press('Enter')
  await page.getByRole('button', { name: '外观设置', exact: true }).waitFor()
  await shot('consumer-account-menu')
  await page.getByRole('button', { name: '外观设置', exact: true }).click()
  await drawer.waitFor()
  await shot('consumer-clario')
  await option('主题风格', 'LiquidGlass').click()
  assert.equal(await root.getAttribute('data-workspace-preset'), 'liquid')
  assert.notEqual(await page.locator('.dashboard-schedule').evaluate(node => getComputedStyle(node).backdropFilter), 'none')
  await shot('consumer-liquidglass')
  await option('系统主题色', '紫色').click()
  await drawer.getByText('自定义', { exact: true }).waitFor()
  await option('菜单布局', '紧凑侧栏').click()
  assert.equal(await shell.getAttribute('data-layout'), 'compact')
  assert.equal(await page.getByRole('navigation', { name: '求职旅程', exact: true }).getByRole('link').count(), 6)
  await option('菜单布局', '顶部').click()
  assert.equal(await shell.getAttribute('data-layout'), 'top')
  await page.keyboard.press('Escape')
  await drawer.waitFor({ state: 'hidden' })
  assert.equal(await shell.getAttribute('data-layout'), 'left')
  assert.equal(await root.getAttribute('data-workspace-preset'), 'clario')
  await page.waitForFunction(() => document.activeElement?.getAttribute('aria-label') === '打开账号菜单')

  async function openConsumerSettings() {
    await account.click()
    await page.getByRole('button', { name: '外观设置', exact: true }).click()
    await drawer.waitFor()
  }
  await openConsumerSettings()
  await option('主题风格', 'LiquidGlass').click()
  await page.mouse.click(600, 130)
  await drawer.waitFor({ state: 'hidden' })
  assert.equal(await root.getAttribute('data-workspace-preset'), 'clario')
  await openConsumerSettings()
  await option('主题风格', 'LiquidGlass').click()
  await option('系统外观', '跟随系统').click()
  await drawer.getByRole('button', { name: '保存设置', exact: true }).click()
  await drawer.waitFor({ state: 'hidden' })
  await page.reload()
  await account.waitFor()
  assert.equal(await root.getAttribute('data-workspace-preset'), 'liquid')
  await page.emulateMedia({ colorScheme: 'dark' })
  await page.waitForFunction(() => document.documentElement.dataset.workspaceTone === 'dark')
  await page.emulateMedia({ colorScheme: 'light' })
  await page.waitForFunction(() => document.documentElement.dataset.workspaceTone === 'light')

  await account.click()
  await page.getByRole('button', { name: '管理后台', exact: true }).click()
  await page.waitForURL(origin + '/admin/dashboard')
  await page.getByRole('heading', { name: '后台看板', exact: true, level: 1 }).waitFor()
  assert.equal(await root.getAttribute('data-workspace'), 'admin')
  assert.equal(await page.locator('.admin-sidebar').getAttribute('data-menu-tone'), 'light')
  const adminNav = page.locator('.admin-sidebar')
  const tabs = page.getByRole('navigation', { name: '工作页签' })
  await adminNav.getByRole('link', { name: '用户管理', exact: true }).click()
  await page.waitForURL(origin + '/admin/users')
  await adminNav.getByRole('link', { name: '操作日志', exact: true }).click()
  await page.waitForURL(origin + '/admin/operation-logs')
  await tabs.getByRole('link', { name: '后台看板', exact: true }).click()
  await page.waitForURL(origin + '/admin/dashboard')
  await page.getByRole('button', { name: '外观设置', exact: true }).click()
  await drawer.waitFor()
  await shot('admin-light')
  for (const [label, value] of [['顶部', 'top'], ['混合', 'mixed'], ['双列', 'dual']]) {
    await option('菜单布局', label).click()
    assert.equal(await page.locator('.admin-shell').getAttribute('data-layout'), value)
  }
  await option('菜单风格', '深色').click()
  assert.equal(await adminNav.getAttribute('data-menu-tone'), 'dark')
  await drawer.locator('.el-drawer__close-btn').click()
  await drawer.waitFor({ state: 'hidden' })
  assert.equal(await adminNav.getAttribute('data-menu-tone'), 'light')
  await page.getByRole('button', { name: '切换到深色', exact: true }).click()
  assert.equal(await root.getAttribute('data-workspace-tone'), 'dark')
  assert.equal(await adminNav.getAttribute('data-menu-tone'), 'dark')
  await shot('admin-dark')
  await page.reload()
  await tabs.getByRole('link', { name: '操作日志', exact: true }).waitFor()
  assert.equal(await root.getAttribute('data-workspace-tone'), 'dark')
  await tabs.getByRole('link', { name: '用户管理', exact: true }).click()
  await page.waitForURL(origin + '/admin/users')
  await tabs.getByRole('button', { name: '关闭用户管理', exact: true }).press('Enter')
  await page.waitForURL(origin + '/admin/operation-logs')
  await tabs.getByRole('button', { name: '关闭用户管理', exact: true }).waitFor({ state: 'hidden' })
  await page.waitForFunction(() => document.activeElement?.textContent === '操作日志')
  await page.getByRole('button', { name: '返回用户工作台', exact: true }).click()
  await page.waitForURL(origin + '/dashboard')
  await account.waitFor()
  assert.equal(await root.getAttribute('data-workspace-preset'), 'liquid')
  await page.evaluate(url => document.querySelector('#app').__vue_app__.config.globalProperties.$router.push(url), printPath)
  await page.locator('.print-page').waitFor()
  assert.equal(await root.getAttribute('data-workspace'), null)

  const original = page.viewportSize()
  for (const width of [1024, 1023, 375]) {
    await page.setViewportSize({ width, height: 850 })
    await page.goto(origin + '/dashboard')
    await account.waitFor()
    await visible('nav[aria-label="求职旅程"]', width >= 1024)
    await visible('nav[aria-label="移动端求职旅程"]', width < 1024)
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
  }
  await openConsumerSettings()
  assert.equal(await option('主题风格', 'LiquidGlass').getAttribute('aria-pressed'), 'true')
  await page.keyboard.press('Escape')
  await drawer.waitFor({ state: 'hidden' })
  for (const width of [901, 900, 375]) {
    await page.setViewportSize({ width, height: 850 })
    await page.goto(origin + '/admin/operation-logs')
    await tabs.getByRole('link', { name: '操作日志' }).waitFor()
    await visible('.admin-sidebar', width > 900)
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
  }
  await page.getByRole('button', { name: '打开后台导航', exact: true }).click()
  await page.getByRole('dialog', { name: '后台导航' }).getByRole('link', { name: '后台看板' }).waitFor()
  await page.setViewportSize(original)
}

async function verifyViewport(browser, viewport) {
  const context = await browser.newContext({ viewport, locale: 'zh-CN', reducedMotion: 'reduce' })
  const missing = new Set()
  const pageErrors = []
  try {
    await context.addInitScript(() => window.localStorage.setItem('internpilot_token', 'synthetic-workspace-session'))
    await context.route(origin + '/api/**', (route) => {
      const request = route.request()
      const key = new URL(request.url()).pathname
      if (request.method() !== 'GET' || !fixtures.has(key)) {
        missing.add(request.method() + ' ' + key)
        return route.fulfill({ status: 404, json: { code: 404, message: 'No fixture' } })
      }
      return route.fulfill({ json: { code: 200, data: fixtures.get(key) } })
    })
    const page = await context.newPage()
    page.on('pageerror', (error) => pageErrors.push(error.message))
    const results = []
    for (const route of [...publicPaths, ...workspacePaths, ...adminPaths, printPath]) {
      try {
        results.push(await verifyRoute(page, route, viewport))
      } catch (error) {
        results.push({ route, width: viewport.width, error: String(error) })
      }
    }
    await page.goto(origin + '/')
    await page.getByRole('link', { name: '进入工作台' }).first().click()
    await page.waitForURL(origin + '/dashboard')
    await page.getByRole('heading', { level: 1 }).first().waitFor()
    if (viewport.width === 768) {
      for (const width of [1023, 1024]) {
        await page.setViewportSize({ width, height: viewport.height })
        results.push(await verifyRoute(page, '/dashboard', { width }))
      }
    }
    if (viewport.width === 1440) await verifyAppearance(page)
    return { results, pageErrors, missing: [...missing] }
  } finally {
    await context.close()
  }
}


async function verifyRestrictedAdminTabs(browser) {
  const context = await browser.newContext({ viewport: viewports[0] })
  try {
    await context.addInitScript(() => {
      localStorage.setItem('internpilot_token', 'synthetic-restricted-session')
      sessionStorage.setItem('internpilot:admin-work-tabs:v1:17', JSON.stringify(['/admin/dashboard', '/admin/users']))
    })
    await context.route(origin + '/api/**', route => {
      const key = new URL(route.request().url()).pathname
      const data = key === '/api/user/me' ? { ...user, permissions: ['operation-log:read'] } : fixtures.get(key)
      return route.fulfill({ json: { code: 200, data } })
    })
    const page = await context.newPage()
    await page.goto(origin + '/admin/operation-logs')
    const tabs = page.getByRole('navigation', { name: '工作页签' })
    await tabs.getByRole('link', { name: '操作日志', exact: true }).waitFor()
    assert.deepEqual(await tabs.getByRole('link').allTextContents(), ['操作日志'])
    await page.goto(origin + '/admin/users')
    await page.waitForURL(origin + '/403')
    assert.equal(await page.locator('html').getAttribute('data-workspace'), null)
  } finally {
    await context.close()
  }
}

async function verifyAnonymousLanding(browser, viewport) {
  const context = await browser.newContext({ viewport, locale: 'zh-CN', reducedMotion: 'reduce' })
  const requestedApiPaths = []
  try {
    await context.route(origin + '/api/**', (route) => {
      requestedApiPaths.push(new URL(route.request().url()).pathname)
      return route.abort()
    })
    const page = await context.newPage()
    await verifyRoute(page, '/', viewport)
    const primaryAction = page.getByRole('link', { name: '开始使用' }).first()
    assert.equal(await primaryAction.evaluate((link) => getComputedStyle(link).color), 'rgb(255, 255, 255)')
    await page.screenshot({ path: path.join(output, 'landing-' + viewport.width + '.png'), animations: 'disabled' })
    await page.keyboard.press('Tab')
    const skipLink = page.getByRole('link', { name: '跳到主要内容' })
    assert.equal(await skipLink.evaluate((link) => link === document.activeElement), true)
    await page.keyboard.press('Enter')
    assert.equal(await page.locator('#landing-main').evaluate((main) => main === document.activeElement), true)

    const sections = [
      ['简历与岗位', 'resumes-and-jobs'],
      ['匹配与面试', 'matching-and-interviews'],
      ['投递进度', 'application-tracking']
    ]
    for (const [name, fragment] of sections) {
      await page.getByRole('link', { name, exact: true }).click()
      await page.waitForURL(origin + '/#' + fragment)
      await page.locator('#' + fragment + ' h2').waitFor()
      await expectInViewport(page.locator('#' + fragment + ' h2'))
    }

    await page.reload()
    await expectInViewport(page.locator('#application-tracking h2'))
    assert.deepEqual(requestedApiPaths, [], 'Public landing must not call the API')
    await page.getByRole('link', { name: '开始使用' }).last().click()
    await page.waitForURL(origin + '/register')
    await page.waitForFunction(() => window.scrollY === 0)
    await expectInViewport(page.getByRole('heading', { level: 1 }))
    await page.goto(origin + '/')
    await page.getByRole('link', { name: '登录', exact: true }).click()
    await page.waitForURL(origin + '/login')
    await page.goto(origin + printPath)
    await page.waitForURL(origin + '/login')
  } finally {
    await context.close()
  }
}

async function expectInViewport(heading) {
  assert(await heading.evaluate((element) => {
    const { top, bottom } = element.getBoundingClientRect()
    return top >= 0 && bottom <= window.innerHeight
  }))
}

async function main() {
  await mkdir(output, { recursive: true })
  const browser = await chromium.launch({ channel: 'msedge' })
  try {
    const reports = []
    for (const viewport of viewports) {
      reports.push(await verifyViewport(browser, viewport))
      await verifyAnonymousLanding(browser, viewport)
    }
    await verifyRestrictedAdminTabs(browser)
    await writeFile(path.join(output, 'layout-results.json'), JSON.stringify({ browser: browser.version(), reports }, null, 2))
    const failures = reports.flatMap(({ results, pageErrors, missing }) => [
      ...results.filter((result) => result.error), ...pageErrors, ...missing
    ])
    assert.deepEqual(failures, [], 'Workspace layout checks')
    console.log('Verified anonymous landing, keyboard skip link, section links, authentication links and protected print at all three widths.')
    console.log(`Verified ${reports.reduce((count, report) => count + report.results.length, 0)} route and viewport combinations.`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
