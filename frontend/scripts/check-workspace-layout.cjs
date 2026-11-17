const assert = require('node:assert/strict')
const { mkdir, writeFile } = require('node:fs/promises')
const path = require('node:path')
const { chromium } = require(process.env.INTERNPILOT_PLAYWRIGHT || 'playwright-core')

const origin = 'http://127.0.0.1:' + (process.env.INTERNPILOT_PREVIEW_PORT || '5184')
const output = path.resolve(process.env.INTERNPILOT_EVIDENCE || 'dist/workspace-evidence')
const viewports = [
  { width: 1440, height: 960 },
  { width: 768, height: 1024 },
  { width: 375, height: 812 }
]
const publicPaths = ['/login', '/register', '/403']
const workspacePaths = [
  '/', '/dashboard', '/resumes', '/resumes/1/versions?versionId=5', '/jobs',
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
  applyDate: '2026-11-16', resumeName: resume.resumeName
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
  '/api/admin/dashboard/summary': {}, '/api/admin/users': counted(user),
  '/api/admin/roles': [{ roleId: 2, roleCode: 'USER', roleName: '普通用户', enabled: 1 }],
  '/api/admin/permissions': counted(), '/api/admin/rag/knowledge': counted(),
  '/api/admin/operation-logs': counted(), '/api/admin/feedback': counted()
}))

async function verifyRoute(page, route, viewport) {
  const response = await page.goto(origin + route)
  assert.equal(response.status(), 200, route)
  await page.getByRole('heading', { level: 1 }).first().waitFor()
  await page.waitForLoadState('networkidle')
  const expected = { '/': '/dashboard', '/admin': '/admin/dashboard' }[route] || route
  assert.equal(page.url(), origin + expected)

  const sizes = await page.evaluate(() => ({
    client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth
  }))
  assert(sizes.scroll <= sizes.client + 1, `${route} at ${viewport.width}px: ${sizes.scroll} > ${sizes.client}`)

  if (workspacePaths.includes(route)) {
    assert.equal(await page.locator('.workspace-sidebar').isVisible(), viewport.width >= 1024)
    assert.equal(await page.getByRole('navigation', { name: '移动端求职旅程' }).isVisible(), viewport.width < 1024)
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
    if (viewport.width === 768) {
      for (const width of [1023, 1024]) {
        await page.setViewportSize({ width, height: viewport.height })
        results.push(await verifyRoute(page, '/dashboard', { width }))
      }
    }
    return { results, pageErrors, missing: [...missing] }
  } finally {
    await context.close()
  }
}

async function main() {
  await mkdir(output, { recursive: true })
  const browser = await chromium.launch({ channel: 'msedge' })
  try {
    const reports = []
    for (const viewport of viewports) {
      reports.push(await verifyViewport(browser, viewport))
    }
    await writeFile(path.join(output, 'layout-results.json'), JSON.stringify({ browser: browser.version(), reports }, null, 2))
    const failures = reports.flatMap(({ results, pageErrors, missing }) => [
      ...results.filter((result) => result.error), ...pageErrors, ...missing
    ])
    assert.deepEqual(failures, [], 'Workspace layout checks')
    console.log(`Verified ${reports.reduce((count, report) => count + report.results.length, 0)} route and viewport combinations.`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
