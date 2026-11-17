# P7 后台看板与管理列表

基础提交为 `0b9b592`。本批覆盖后台看板、角色、权限、RAG 知识库、操作日志和反馈。看板把八项指标归入系统规模、今日动态两组。五个列表共用紧凑指标、读失败重试区和可用键盘横向滚动的表格。移动端筛选纵向排列，日志分页保留每页条数并独立排布页码和前后页操作。表格原有字段、列表查询、分页、权限判断与写操作确认沿用原契约。

## 本地验证

Windows，Node 20.20.2，npm 11.12.1，Edge 157.0.4341.60。以下命令从 `frontend/` 运行。

| 命令 | 退出码 | 结果 |
| --- | --- | --- |
| `npm exec --offline --package=node@20.20.2 -c 'npm run test:run -- src/views/admin/__tests__ src/components/layout/__tests__/AdminSidebar.spec.ts --maxWorkers=1 --reporter=dot'` | 0 | 7 个文件，66 例通过，无跳过 |
| `npm exec --offline --package=node@20.20.2 -c 'npm run type-check && npm run build'` | 0 | 类型检查和生产构建通过，`vendor-element` 有 500 kB 分包提醒 |
| `npm exec --offline --package=node@20.20.2 -- node E:/Temp/internpilot-p7-browser.cjs` | 0 | 两种 Edge 视口的合成夹具检查通过 |

浏览器服务命令为 `npm exec --offline --package=node@20.20.2 -c 'node node_modules/vite/bin/vite.js preview --host 127.0.0.1 --port 15207 --strictPort'`。Edge 在 1440×900 和 375×812 打开六个页面，均无页面级横向溢出。375px 进一步验证五个表格可聚焦并通过方向键横向滚动，日志下一页可见且发出 `pageNum=2`，取消角色权限分配不发写请求，模拟权限列表读取 403 后重试恢复，只读账号隐藏 RAG 和反馈写入口且访问角色管理进入 403。看板、RAG、日志在两种视口的截图和 `results.json` 位于本机 `frontend/dist/p7-evidence/`。

测试使用合成数据和模拟响应，真实后台服务、生产账号、外部 RAG 和全量项目测试待独立验证。
