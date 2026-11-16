const assert = require('node:assert/strict')
const { mkdir, writeFile } = require('node:fs/promises')
const path = require('node:path')
const { chromium } = require(process.env.INTERNPILOT_PLAYWRIGHT || 'playwright-core')

const origin = 'http://127.0.0.1:' + (process.env.INTERNPILOT_PREVIEW_PORT || '15206')
const output = path.resolve('dist/ui-p6-evidence')
const account = {
  userId: 1, nickname: '合成运营账号',
  permissions: ['user:read', 'user:update', 'role:read'],
}
const user = {
  userId: 17, nickname: '陈同学', username: 'sample-user', email: 'student@example.invalid',
  school: '合成大学', major: '计算机科学', grade: '大三', roles: ['USER'],
  enabled: 1, createdAt: '2026-11-16T09:00:00', permissions: ['sample:read'],
  resumeCount: 1, jobCount: 2, analysisReportCount: 3, applicationCount: 4,
}
const roles = [
  { roleId: 2, roleCode: 'USER', roleName: '普通用户', enabled: 1 },
  { roleId: 3, roleCode: 'ADMIN', roleName: '管理员', enabled: 1 },
]

async function exercise(browser, viewport) {
  const context = await browser.newContext({ viewport, locale: 'zh-CN', timezoneId: 'Asia/Shanghai', reducedMotion: 'reduce' })
  const requests = []
  const unexpected = []
  let failList = true
  await context.addInitScript(() => localStorage.setItem('internpilot_token', 'synthetic-p6'))
  await context.route('**/*', (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (url.origin !== origin || url.pathname.startsWith('/ws/')) return route.abort()
    if (!url.pathname.startsWith('/api/')) return route.continue()
    const result = (data) => route.fulfill({ json: { code: 200, data } })
    if (request.method() === 'GET') {
      if (url.pathname === '/api/user/me') return result(account)
      if (url.pathname === '/api/admin/roles') return result(roles)
      if (url.pathname === '/api/admin/users/17') return result(user)
      if (url.pathname === '/api/admin/users') {
        requests.push(Object.fromEntries(url.searchParams.entries()))
        if (failList) {
          failList = false
          return route.fulfill({ status: 503, json: { code: 503, message: '合成读取失败' } })
        }
        return result({ records: [user], total: 32 })
      }
    }
    unexpected.push(request.method() + ' ' + url.pathname)
    return route.fulfill({ status: 501, json: { code: 501, message: '未配置的合成请求' } })
  })
  const page = await context.newPage()
  try {
    await page.goto(origin + '/admin/users')
    await page.getByRole('heading', { level: 1, name: '用户管理' }).waitFor()
    await page.getByText('用户列表加载失败', { exact: true }).waitFor()
    await page.getByRole('button', { name: '重试用户列表' }).click()
    const table = page.locator('.admin-data-table')
    await table.getByText(user.nickname, { exact: true }).waitFor()
    assert.deepEqual(requests[0], requests[1])
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), viewport.width)
    await page.locator('.el-message').waitFor({ state: 'hidden' })
    await page.screenshot({ path: path.join(output, 'users-' + viewport.width + '.png'), fullPage: true })

    const keyword = page.getByRole('textbox', { name: '用户关键词' })
    await keyword.fill(user.nickname)
    await keyword.press('Enter')
    await table.getByText(user.nickname, { exact: true }).waitFor()
    assert.equal(requests.at(-1).keyword, user.nickname)
    assert.equal(requests.at(-1).pageNum, '1')

    const scroll = table.locator('.el-scrollbar__wrap[tabindex="0"]')
    assert(await scroll.evaluate((element) => element.scrollWidth > element.clientWidth))
    await scroll.focus()
    await page.keyboard.press('ArrowRight')
    await page.waitForFunction(() => document.activeElement.scrollLeft > 0)
    await table.getByRole('button', { name: '详情', exact: true }).focus()
    await page.keyboard.press('Enter')
    await page.getByRole('dialog', { name: '用户详情' }).getByText('登录用户名', { exact: true }).waitFor()
    await page.keyboard.press('Escape')
    await page.getByRole('dialog', { name: '用户详情' }).waitFor({ state: 'hidden' })

    await table.getByRole('button', { name: '分配角色' }).click()
    const roleDialog = page.getByRole('dialog', { name: '分配角色' })
    await roleDialog.getByText('ADMIN - 管理员').waitFor()
    await roleDialog.getByRole('button', { name: '取消', exact: true }).click()
    await roleDialog.waitFor({ state: 'hidden' })

    await table.getByRole('button', { name: '禁用', exact: true }).click()
    const confirmation = page.getByRole('dialog', { name: '确认操作' })
    await confirmation.getByText('确认禁用用户 陈同学？').waitFor()
    await page.keyboard.press('Escape')
    await confirmation.waitFor({ state: 'hidden' })

    if (viewport.width <= 900) {
      const trigger = page.getByRole('button', { name: '打开后台导航' })
      await trigger.focus()
      await page.keyboard.press('Enter')
      const drawer = page.getByRole('dialog', { name: '后台导航' })
      await drawer.getByRole('link', { name: '用户管理' }).waitFor()
      assert.equal(await drawer.getByRole('link', { name: '用户管理' }).getAttribute('aria-current'), 'page')
      assert.equal(await drawer.getByRole('link', { name: '权限管理' }).count(), 0)
      await page.screenshot({ path: path.join(output, 'navigation-' + viewport.width + '.png') })
      await page.keyboard.press('Escape')
      await drawer.waitFor({ state: 'hidden' })
      await page.waitForFunction(() => document.activeElement.getAttribute('aria-label') === '打开后台导航')
    } else {
      const sidebar = page.getByRole('navigation', { name: '后台导航' })
      assert.equal(await sidebar.getByRole('link', { name: '用户管理' }).getAttribute('aria-current'), 'page')
      assert.equal(await sidebar.getByRole('link', { name: '权限管理' }).count(), 0)
    }
    assert.deepEqual(unexpected, [])
    return { viewport, listQueries: requests, writesAfterCancellation: 0 }
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
      checks.push(await exercise(browser, viewport))
      console.log(viewport.width + '×' + viewport.height + ': admin user workflow PASS')
    }
    await writeFile(path.join(output, 'results.json'), JSON.stringify({ edge: browser.version(), synthetic: true, checks }, null, 2) + '\n')
  } finally {
    await browser.close()
  }
}

main().catch((error) => { console.error(error); process.exitCode = 1 })
