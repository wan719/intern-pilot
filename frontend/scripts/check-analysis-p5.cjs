const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdir, readFile, writeFile } = require('node:fs/promises')
const path = require('node:path')
const { chromium } = require(process.env.INTERNPILOT_PLAYWRIGHT || 'playwright-core')

const origin = 'http://127.0.0.1:' + (process.env.INTERNPILOT_PREVIEW_PORT || '15205')
const output = path.resolve('dist/ui-p5-evidence')
const date = '2026-11-16T09:30:00'
const resume = { resumeId: 3, resumeName: '合成前端简历', originalFileName: 'sample.pdf', parseStatus: 'SUCCESS', isDefault: true }
const job = { jobId: 7, companyName: '样例公司', jobTitle: '前端实习生', jdContent: '构建可访问界面。', skillRequirements: 'Vue, TypeScript' }
const report = {
  ...job, reportId: 91, resumeId: 3, resumeVersionId: 5, resumeName: resume.resumeName,
  matchScore: 82, matchLevel: '高匹配', strengths: ['已有组件开发经验'], weaknesses: ['缺少打印样式经验'],
  missingSkills: ['打印样式'], suggestions: ['补充实战示例'], interviewTips: ['解释移动布局选择'], createdAt: date
}
const interview = {
  ...job, reportId: 41, resumeId: 3, resumeVersionId: 5, analysisReportId: 91, title: '合成前端题单',
  questionCount: 1, createdAt: date, questions: [{
    questionId: 101, questionType: 'SPRING_BOOT', difficulty: 'MEDIUM', question: '如何验证移动端的页面行为？',
    answer: '检查窄屏、输入方式和关键操作。', answerPoints: ['视口', '键盘'], followUps: ['如何覆盖失败状态？'],
    keywords: ['可访问性'], sortOrder: 1
  }]
}
const fixtures = {
  '/api/user/me': { userId: 1, nickname: '样例用户', permissions: ['analysis:read', 'analysis:write', 'interview:read', 'interview:write'] },
  '/api/health/ai-provider': { provider: 'mock' },
  '/api/resumes': { records: [resume], total: 1 },
  '/api/resumes/3/versions': [{ versionId: 5, resumeId: 3, versionName: '样例当前版本', isCurrent: 1 }],
  '/api/jobs': { records: [job], total: 1 },
  '/api/jobs/7': job,
  '/api/analysis/reports': { records: [report], total: 1 },
  '/api/analysis/reports/91': report,
  '/api/analysis/tasks/running': [],
  '/api/analysis/tasks/recent': [],
  '/api/analysis/tasks/P5-SAMPLE': { taskNo: 'P5-SAMPLE', status: 'CALLING_AI', progress: 68, message: '正在生成匹配结论', resumeId: 3, jobId: 7 },
  '/api/analysis/tasks/P5-FAILED': { taskNo: 'P5-FAILED', status: 'FAILED', progress: 42, errorMessage: '合成分析失败', resumeId: 3, jobId: 7 },
  '/api/interview-questions': { records: [interview], total: 1 },
  '/api/interview-questions/41': interview
}

async function openContext(browser, viewport) {
  const context = await browser.newContext({ viewport, locale: 'zh-CN', timezoneId: 'Asia/Shanghai', reducedMotion: 'reduce' })
  const unexpected = []
  const retryPayloads = []
  let completedSubmission = false
  await context.addInitScript(() => localStorage.setItem('internpilot_token', 'synthetic-p5'))
  await context.route('**/*', async (route) => {
    const request = route.request()
    const parsed = new URL(request.url())
    if (parsed.origin !== origin || parsed.pathname.startsWith('/ws/')) return route.abort()
    if (!parsed.pathname.startsWith('/api/')) return route.continue()
    if (request.method() === 'GET' && Object.hasOwn(fixtures, parsed.pathname)) {
      return route.fulfill({ json: { code: 200, data: fixtures[parsed.pathname] } })
    }
    if (request.method() === 'POST' && parsed.pathname === '/api/analysis/tasks') {
      if (completedSubmission) {
        return route.fulfill({ json: { code: 200, data: {
          taskNo: 'P5-IMMEDIATE', status: 'COMPLETED', progress: 100, reportId: 91, resumeId: 3, jobId: 7
        } } })
      }
      retryPayloads.push(request.postDataJSON())
      return route.fulfill({ status: 503, json: { code: 503, message: '合成重试失败' } })
    }
    unexpected.push(request.method() + ' ' + parsed.pathname)
    return route.fulfill({ status: 501, json: { code: 501 } })
  })
  return { context, unexpected, retryPayloads, completeSubmissions: () => { completedSubmission = true } }
}

async function capture(browser, viewport) {
  const { context, unexpected, retryPayloads, completeSubmissions } = await openContext(browser, viewport)
  const page = await context.newPage()
  const screen = async (name) => {
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), viewport.width)
    await page.screenshot({ path: path.join(output, name + '-' + viewport.width + '.png'), fullPage: true, animations: 'disabled' })
  }
  try {
    await page.goto(origin + '/analysis/match')
    await page.getByRole('heading', { name: '确认这次 AI 匹配', exact: true }).waitFor()
    await page.locator('[data-options-loading]').waitFor({ state: 'hidden' })
    assert.equal(await page.locator('.progress-panel').count(), 0)
    await page.goto(origin + '/analysis/match?taskNo=P5-SAMPLE')
    await page.locator('[data-progress-percentage]').getByText('68%', { exact: true }).waitFor()
    assert(await page.locator('.progress-panel').evaluate((panel) =>
      Boolean(panel.compareDocumentPosition(document.querySelector('.setup-grid')) & Node.DOCUMENT_POSITION_FOLLOWING)))
    await screen('analysis-progress')

    await page.goto(origin + '/analysis/match?taskNo=P5-FAILED')
    const retry = page.locator('.progress-panel [data-task-retry]')
    await retry.click()
    const error = page.locator('.progress-panel [data-submission-error]')
    await error.getByText('合成重试失败').waitFor()
    const errorBox = await error.boundingBox()
    assert(errorBox.y >= 0 && errorBox.y + errorBox.height <= viewport.height)
    await retry.click()
    await error.getByText('合成重试失败').waitFor()
    assert.equal(retryPayloads.length, 2)
    assert.deepEqual(retryPayloads[0], retryPayloads[1])

    completeSubmissions()
    await page.evaluate(() => localStorage.removeItem('internpilot:analysis:lastTaskNo'))
    await page.goto(origin + '/analysis/match?resumeId=3&jobId=7')
    await page.locator('[data-analysis-submit]').click()
    const result = page.getByRole('button', { name: '查看分析报告', exact: true })
    await result.waitFor()
    await page.waitForFunction(() => document.activeElement.id === 'analysis-progress-title')
    const progressBox = await page.locator('#analysis-progress-title').boundingBox()
    assert(progressBox.y >= 0 && progressBox.y < viewport.height)

    await page.goto(origin + '/analysis/reports')
    await page.locator('[data-report-card]').getByRole('heading', { name: job.jobTitle, exact: true }).waitFor()
    assert.equal(await page.locator('dl[aria-label="当前报告概览"] dt').count(), 4)
    await screen('analysis-reports')

    await page.goto(origin + '/interview-questions')
    await page.getByRole('heading', { name: interview.title, exact: true }).waitFor()
    assert.equal(await page.locator('dl[aria-label="当前题单概览"] dt').count(), 4)
    await screen('interview-queue')

    await page.goto(origin + '/interview-questions/41')
    const question = page.getByRole('region', { name: '题目与参考答案' })
    await question.getByRole('heading', { name: interview.questions[0].question, exact: true }).waitFor()
    const answer = question.getByRole('button', { name: '查看答案', exact: true })
    await answer.focus()
    await page.keyboard.press('Enter')
    await question.getByText(interview.questions[0].answer, { exact: true }).waitFor()
    await screen('interview-practice')

    const target = '/analysis/reports/91/print'
    await page.evaluate(async (route) => {
      await document.querySelector('#app').__vue_app__.config.globalProperties.$router.push(route)
    }, target)
    assert.equal(new URL(page.url()).pathname, target)
    const print = page.locator('.print-page').getByText('样例公司 - 前端实习生', { exact: true })
    await print.waitFor()
    assert.equal(await page.locator('.analysis-workspace, .user-app-shell').count(), 0)
    const printImage = async (targetPage, label) => {
      await targetPage.emulateMedia({ media: 'print' })
      await targetPage.waitForFunction(() => matchMedia('print').matches &&
        getComputedStyle(document.querySelector('.print-page')).color === 'rgb(0, 0, 0)')
      assert.equal(await targetPage.locator('.print-toolbar').isVisible(), false)
      assert.equal(await targetPage.locator('.report-section ul').first().evaluate((el) => getComputedStyle(el).listStyleType), 'disc')
      const file = path.join(output, label + '-' + viewport.width)
      await targetPage.pdf({ path: file + '.pdf', printBackground: true, preferCSSPageSize: true })
      execFileSync('pdftoppm', ['-png', '-r', '96', file + '.pdf', file])
      const meta = execFileSync('pdfinfo', [file + '.pdf'], { encoding: 'utf8' })
      const pages = Number(meta.match(/^Pages:\s+(\d+)/m)?.[1])
      assert(pages > 0)
      return Promise.all(Array.from({ length: pages }, (_, index) => readFile(file + '-' + (index + 1) + '.png')))
    }
    const after = await printImage(page, 'print-after-navigation')
    const directContext = await openContext(browser, viewport)
    try {
      const direct = await directContext.context.newPage()
      await direct.goto(origin + target)
      await direct.locator('.print-page').getByText('样例公司 - 前端实习生', { exact: true }).waitFor()
      const before = await printImage(direct, 'print-direct')
      assert.equal(after.length, before.length)
      assert(after.every((image, index) => image.equals(before[index])))
      assert.deepEqual(directContext.unexpected, [])
    } finally {
      await directContext.context.close()
    }
    assert.deepEqual(unexpected, [])
    return { viewport, printPages: after.length, routes: 5, retryFailuresInView: true, result: 'PASS' }
  } finally {
    await context.close()
  }
}

async function main() {
  await mkdir(output, { recursive: true })
  const browser = await chromium.launch({ channel: 'msedge' })
  try {
    const checks = []
    for (const viewport of [{ width: 1440, height: 960 }, { width: 375, height: 812 }]) {
      const check = await capture(browser, viewport)
      checks.push(check)
      console.log(viewport.width + '×' + viewport.height + ': P5 pages, restored task, keyboard answer and print raster PASS')
    }
    await writeFile(path.join(output, 'results.json'), JSON.stringify({ edge: browser.version(), synthetic: true, checks }, null, 2) + '\n')
  } finally {
    await browser.close()
  }
}
main().catch((error) => { console.error(error); process.exitCode = 1 })
