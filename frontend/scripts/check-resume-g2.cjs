const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdir, readFile, writeFile } = require('node:fs/promises')
const path = require('node:path')
const { chromium } = require(process.env.INTERNPILOT_PLAYWRIGHT || 'playwright-core')

const origin = 'http://127.0.0.1:5184'
const output = path.resolve(process.env.INTERNPILOT_RESUME_EVIDENCE || '../.local-evidence/frontend-rebuild/resume-g2')
const sizes = [{ width: 1440, height: 960 }, { width: 375, height: 812 }]
const longName = '模拟样例长名称'.repeat(12) + '.pdf'
const resumes = ['SUCCESS', 'PROCESSING', 'PENDING', 'FAILED'].map((parseStatus, index) => ({
  resumeId: index + 1, resumeName: ['后端实习简历', '平台工程简历', '数据分析简历', longName][index],
  originalFileName: index === 3 ? longName : 'sample-' + (index + 1) + '.pdf',
  fileType: 'PDF', fileSize: 4096, isDefault: index === 0, parseStatus,
  createdAt: '2026-11-16T09:00:00'
}))
const user = { userId: 1, nickname: '样例用户', username: 'demo', roles: ['USER'], enabled: 1, createdAt: resumes[0].createdAt }
const report = {
  reportId: 91, companyName: '合成公司', jobTitle: '开发实习生', resumeName: resumes[0].resumeName,
  resumeId: 1, jobId: 7, matchScore: 82, matchLevel: '高匹配', strengths: ['模拟优势'],
  weaknesses: ['模拟风险'], missingSkills: ['模拟技能'], suggestions: ['模拟建议'],
  interviewTips: ['模拟面试提示'], createdAt: resumes[0].createdAt
}
const fixtures = {
  '/api/user/me': { ...user, roles: ['ADMIN'], permissions: ['user:read', 'user:update', 'role:read'] },
  '/api/health/ai-provider': { provider: 'deepseek' },
  '/api/analysis/tasks/running': [],
  '/api/analysis/tasks/recent': [],
  '/api/resumes/1': { ...resumes[0], parsedText: '合成简历内容' },
  '/api/admin/users': { records: [user], total: 1 },
  '/api/admin/roles': [{ roleId: 1, roleCode: 'USER', roleName: '普通用户' }],
  '/api/analysis/reports/91': report
}

async function session(browser, viewport, state, authenticated = true) {
  const context = await browser.newContext({
    viewport, locale: 'zh-CN', timezoneId: 'Asia/Shanghai', deviceScaleFactor: 1,
    colorScheme: 'light', reducedMotion: 'reduce', hasTouch: viewport.width === 375
  })
  context.setDefaultTimeout(10000)
  const unexpected = []
  let listReads = 0
  let deletes = 0
  let uploads = 0
  if (authenticated) await context.addInitScript(() => localStorage.setItem('internpilot_token', 'synthetic-g2-session'))
  await context.route('**/*', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (url.origin !== origin || url.pathname.startsWith('/ws/')) return route.abort()
    if (!url.pathname.startsWith('/api/')) return route.continue()
    const ok = (data) => route.fulfill({ json: { code: 200, message: 'OK', data } })
    const fail = (status) => route.fulfill({ status, json: { code: status } })
    if (request.method() === 'GET') {
      if (url.pathname === '/api/resumes') {
        listReads += 1
        assert.equal(url.searchParams.get('pageNum'), '1')
        assert.equal(url.searchParams.get('pageSize'), '100')
        if (state.scenario === 'loading') await state.releaseList
        if (state.status) return fail(state.status)
        const records = state.scenario === 'empty' ? [] : resumes
        return ok({ records, total: records.length })
      }
      if (Object.hasOwn(fixtures, url.pathname)) return ok(fixtures[url.pathname])
    }
    if (request.method() === 'POST' && url.pathname === '/api/resumes/upload') {
      uploads += 1
      assert.match(request.postData() || '', /name="file"; filename="g2-fixture.pdf"/)
      assert.match(request.postData() || '', /name="resumeName"\r\n\r\n合成上传/)
      if (uploads === 1) {
        await new Promise((resolve) => setTimeout(resolve, 350))
        return fail(503)
      }
      return ok(null)
    }
    if (request.method() === 'DELETE' && url.pathname.startsWith('/api/resumes/')) {
      deletes += 1
    }
    unexpected.push(request.method() + ' ' + url.pathname)
    return fail(501)
  })
  return { context, unexpected, listReads: () => listReads, deletes: () => deletes, uploads: () => uploads }
}

async function openResumeList(page) {
  await page.goto(origin + '/resumes')
  await page.getByRole('list', { name: '我的简历' }).waitFor()
}

async function closeWithEscape(page, dialog, trigger) {
  await page.keyboard.press('Escape')
  await dialog.waitFor({ state: 'hidden' })
  await page.waitForFunction(() => getComputedStyle(document.body).overflowY !== 'hidden')
  if (trigger) {
    await page.waitForFunction((button) => document.activeElement === button, await trigger.elementHandle())
  }
}

async function layoutAndActions(browser, viewport) {
  let release
  const state = { scenario: 'loading', releaseList: new Promise((resolve) => { release = resolve }) }
  const run = await session(browser, viewport, state)
  const errors = []
  try {
    const page = await run.context.newPage()
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto(origin + '/resumes')
    await page.locator('.resume-loading').waitFor()
    state.scenario = 'populated'
    release()
    const list = page.getByRole('list', { name: '我的简历' })
    await list.waitFor()
    assert.equal(await list.locator('.resume-row').count(), 4)
    for (const text of ['已解析', '解析中', '待解析', '解析失败']) {
      await list.getByText(text, { exact: true }).waitFor()
    }
    assert(await list.getByRole('heading', { name: longName, exact: true }).isVisible())
    assert(await page.locator('[data-resume-default]').getByText(resumes[0].resumeName, { exact: true }).isVisible())
    const upload = page.getByRole('button', { name: '上传简历', exact: true })
    const uploadBounds = await upload.boundingBox()
    assert(uploadBounds.y >= 0 && uploadBounds.y + uploadBounds.height <= viewport.height)
    assert.equal(await upload.evaluate((el) => getComputedStyle(el).backgroundColor), 'rgb(15, 118, 110)', 'prefixed shadcn Button CSS')
    assert(await upload.evaluate((el) => getComputedStyle(el).minHeight === '44px'))
    const firstRow = list.locator('.resume-row').first()
    const columns = await firstRow.evaluate((el) => getComputedStyle(el).gridTemplateColumns.split(' ').length)
    assert.equal(columns, viewport.width <= 940 ? 2 : 6)
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), viewport.width)
    assert.equal(await page.getByRole('navigation', { name: '移动端求职旅程' }).isVisible(), viewport.width === 375)
    await page.screenshot({ path: path.join(output, 'resumes-' + viewport.width + '.png'), fullPage: viewport.width === 375, animations: 'disabled' })

    if (viewport.width === 1440) {
      for (const [width, expectedColumns] of [[940, 2], [941, 6]]) {
        await page.setViewportSize({ width, height: viewport.height })
        assert.equal(await firstRow.evaluate((el) => getComputedStyle(el).gridTemplateColumns.split(' ').length), expectedColumns)
        assert.equal(await list.locator('.resume-row').count(), 4)
      }
      await page.setViewportSize(viewport)
    }

    await upload.focus()
    await page.keyboard.press('Enter')
    const uploadDialog = page.getByRole('dialog', { name: '上传简历' })
    await uploadDialog.waitFor()
    assert.equal(await page.evaluate(() => getComputedStyle(document.body).overflowY), 'hidden')
    await page.keyboard.press('Tab')
    assert(await uploadDialog.evaluate((el) => el.contains(document.activeElement)))
    await closeWithEscape(page, uploadDialog, upload)

    await upload.click()
    await uploadDialog.getByPlaceholder('例如：Java 后端实习简历.pdf').fill('合成上传')
    const fileInput = uploadDialog.locator('input[type=file]')
    await fileInput.setInputFiles({ name: 'g2-fixture.pdf', mimeType: 'application/pdf', buffer: Buffer.from('synthetic file') })
    const submit = uploadDialog.getByRole('button', { name: '上传', exact: true })
    await submit.click()
    await uploadDialog.locator('[data-resume-upload-error]').waitFor()
    assert.equal(await uploadDialog.getByText('g2-fixture.pdf', { exact: true }).count(), 1)
    await submit.click()
    await uploadDialog.waitFor({ state: 'hidden' })
    await page.waitForFunction(() => getComputedStyle(document.body).overflowY !== 'hidden')
    await list.waitFor()
    assert.equal(run.uploads(), 2)
    await upload.click()
    await uploadDialog.waitFor()
    assert.equal(await uploadDialog.getByText('g2-fixture.pdf', { exact: true }).count(), 0, 'successful upload clears file queue')
    await closeWithEscape(page, uploadDialog, upload)

    const detail = firstRow.getByRole('button', { name: '详情', exact: true })
    await detail.click()
    const drawer = page.getByRole('dialog', { name: '简历详情' })
    await drawer.getByText('合成简历内容', { exact: true }).waitFor()
    assert.equal(await page.evaluate(() => getComputedStyle(document.body).overflowY), 'hidden')
    await closeWithEscape(page, drawer, detail)

    const more = firstRow.getByRole('button', { name: '更多', exact: true })
    await more.focus()
    await page.keyboard.press('Enter')
    const remove = page.getByRole('menuitem', { name: '删除', exact: true })
    await remove.waitFor()
    await remove.focus()
    await page.keyboard.press('Escape')
    await remove.waitFor({ state: 'hidden' })
    assert(await more.evaluate((el) => el === document.activeElement))
    await page.keyboard.press('Enter')
    await remove.waitFor()
    await remove.focus()
    await page.keyboard.press('Enter')
    const confirmation = page.getByRole('dialog', { name: '删除简历' })
    await confirmation.getByRole('button', { name: /^(取消|Cancel)$/ }).click()
    await confirmation.waitFor({ state: 'hidden' })
    assert.equal(run.deletes(), 0)

    state.scenario = 'empty'
    await page.reload()
    await page.getByText('还没有上传简历', { exact: true }).waitFor()
    assert(await page.locator('[data-resume-default]').getByText('尚未设置', { exact: true }).isVisible())
    state.scenario = 'populated'
    state.status = 503
    await page.reload()
    await page.getByText('简历列表暂时无法加载', { exact: true }).waitFor()
    state.status = undefined
    await page.getByRole('button', { name: '重新加载', exact: true }).click()
    await list.waitFor()
    assert.deepEqual(run.unexpected, [])
    assert.deepEqual(errors, [])
  } finally {
    release()
    await run.context.close()
  }
}

async function sessionFailures(browser, viewport) {
  const missing = await session(browser, viewport, {}, false)
  try {
    const page = await missing.context.newPage()
    await page.goto(origin + '/resumes')
    await page.waitForURL('**/login')
    assert.equal(missing.listReads(), 0)
  } finally {
    await missing.context.close()
  }
  for (const status of [403, 401]) {
    const denied = await session(browser, viewport, { status })
    try {
      const page = await denied.context.newPage()
      await page.goto(origin + '/resumes')
      if (status === 401) {
        await page.waitForURL('**/login')
        assert.equal(await page.evaluate(() => localStorage.getItem('internpilot_token')), null)
      } else {
        await page.getByText('当前账号没有权限执行该操作', { exact: true }).waitFor()
        await page.getByText('简历列表暂时无法加载', { exact: true }).waitFor()
        assert.equal(await page.getByRole('list', { name: '我的简历' }).count(), 0)
        assert.equal(new URL(page.url()).pathname, '/resumes')
      }
      assert.equal(denied.listReads(), 1)
      assert.deepEqual(denied.unexpected, [])
    } finally {
      await denied.context.close()
    }
  }
}

async function sampledStyles(page, selectors) {
  return page.evaluate(async (targets) => {
    await document.fonts.ready
    await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))
    return Object.fromEntries(targets.map((selector) => {
      const node = document.querySelector(selector)
      if (!node) throw new Error('Missing sample element: ' + selector)
      const css = getComputedStyle(node)
      const rect = node.getBoundingClientRect()
      return [selector, {
        display: css.display, color: css.color, background: css.backgroundColor, lineHeight: css.lineHeight,
        fontWeight: css.fontWeight, listStyleType: css.listStyleType, overflowX: css.overflowX,
        width: Math.round(rect.width * 100) / 100, height: Math.round(rect.height * 100) / 100
      }]
    }))
  }, selectors)
}

async function comparePage(browser, viewport, route, afterResume) {
  const run = await session(browser, viewport, {})
  const width = viewport.width
  const name = afterResume ? 'after-resumes' : 'direct'
  try {
    const page = await run.context.newPage()
    if (afterResume) {
      await openResumeList(page)
      await page.evaluate(async (target) => {
        const app = document.querySelector('#app').__vue_app__
        await app.config.globalProperties.$router.push(target)
      }, route)
      assert(await page.evaluate(() => Array.from(document.styleSheets).some((sheet) => sheet.href?.includes('/assets/ResumeList-'))))
    } else {
      await page.goto(origin + route)
      assert.equal(await page.evaluate(() => Array.from(document.styleSheets).some((sheet) => sheet.href?.includes('/assets/ResumeList-'))), false)
    }
    assert.equal(new URL(page.url()).pathname, route)
    if (route === '/admin/users') {
      await page.getByText('样例用户', { exact: true }).waitFor()
      const styles = await sampledStyles(page, ['h1', '.el-table', '.el-table th', '.el-table td', '.el-pagination'])
      await page.screenshot({ path: path.join(output, name + '-admin-' + width + '.png'), fullPage: width === 375, animations: 'disabled' })
      assert.deepEqual(run.unexpected, [])
      return styles
    }

    await page.locator('.print-page').getByText('合成公司 - 开发实习生', { exact: true }).waitFor()
    assert.equal(await page.locator('.user-app-shell, .mobile-bottom-nav').count(), 0)
    assert(await page.locator('.print-toolbar').isVisible())
    await page.emulateMedia({ media: 'print' })
    await page.waitForFunction(() => matchMedia('print').matches &&
      getComputedStyle(document.querySelector('.print-page')).color === 'rgb(0, 0, 0)')
    assert.equal(await page.locator('.print-toolbar').isVisible(), false)
    const styles = await sampledStyles(page, ['.print-page', '.report-header h1', '.report-section h2', '.report-section ul', '.report-section li'])
    assert.equal(styles['.report-section ul'].listStyleType, 'disc')
    const stem = path.join(output, name + '-report-' + width)
    await page.pdf({ path: stem + '.pdf', printBackground: true, preferCSSPageSize: true })
    const info = execFileSync('pdfinfo', [stem + '.pdf'], { encoding: 'utf8' })
    assert.match(info, /^Pages:[ \t]+2[ \t]*$/m)
    assert.match(info, /^Page size:.*\(A4\)/m)
    execFileSync('pdftoppm', ['-png', '-r', '96', stem + '.pdf', stem])
    assert.deepEqual(run.unexpected, [])
    return { styles, pages: await Promise.all([readFile(stem + '-1.png'), readFile(stem + '-2.png')]) }
  } finally {
    await run.context.close()
  }
}

async function main() {
  await mkdir(output, { recursive: true })
  const browser = await chromium.launch({ channel: 'msedge' })
  try {
    const results = []
    for (const viewport of sizes) {
      await layoutAndActions(browser, viewport)
      await sessionFailures(browser, viewport)
      const admin = await comparePage(browser, viewport, '/admin/users', false)
      const spaAdmin = await comparePage(browser, viewport, '/admin/users', true)
      assert.deepEqual(spaAdmin, admin)
      const print = await comparePage(browser, viewport, '/analysis/reports/91/print', false)
      const spaPrint = await comparePage(browser, viewport, '/analysis/reports/91/print', true)
      assert.deepEqual(spaPrint.styles, print.styles)
      assert(spaPrint.pages.every((page, index) => page.equals(print.pages[index])), 'printed pixels changed after loading resume CSS')
      results.push({ viewport, layout: 'PASS', sessionAndPermission: 'PASS', adminStyleComparison: 'PASS', printRasterComparison: 'PASS (2 A4 pages)' })
      console.log(viewport.width + '×' + viewport.height + ' PASS: resume flows, auth, admin, print')
    }
    await writeFile(path.join(output, 'results.json'), JSON.stringify({ edge: browser.version(), data: 'synthetic API fixtures', responsiveBoundary: '940px cards / 941px list', results }, null, 2) + '\n')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
