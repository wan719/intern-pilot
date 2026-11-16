# P6 · 后台导航与用户管理

基线为 `0b9b592`。后台保留 Element Plus、深色分组导航及原有权限判断。用户页将查询放在列表上方，列表内集中展示筛选结果和本页指标。窄屏在表格内横向滚动，保留关键词、角色编码、状态筛选及每页条数。读取失败时可以使用原查询重试。详情、分配角色及启禁用确认使用现有交互。

## 自动化验收

在 `frontend/` 使用 Node 20.20.2。以下命令退出码均为 0。

| 命令 | 结果 |
| --- | --- |
| `npm ci --offline --no-audit --no-fund` | 依据现有锁文件安装 284 个包 |
| `npm run test:run -- --maxWorkers=1 --testTimeout=20000 --hookTimeout=30000 src/views/admin/__tests__ src/components/layout/__tests__/AdminSidebar.spec.ts` | 7 个文件，62 项通过。覆盖读取重试、原查询、角色权限、写操作前确认与取消、竞态及导航守卫 |
| `npm run type-check` | Vue 类型检查通过 |
| `npm run build` | 生产构建通过，仍有既有依赖分块大于 500 kB 的提示 |

一次与其他批次并行执行的测试触发了两项 5 秒超时，最终验证显式放宽了此次命令的超时，仓库测试配置保持原值。

## 浏览器验收

Windows，Edge 157.0.4341.60，1440×960 和 375×812。合成账户、用户与角色数据通过 HTTP API 拦截。浏览器验证了初始读取失败与原参数重试、回车查询、桌面分组侧栏、移动抽屉的权限过滤与 Esc 焦点返回、表格内可用方向键横滚、用户详情及角色和禁用取消。两个视口均无页面级横向溢出，取消后均无写请求。真正的后台写入、生产权限验收和其他浏览器在本次覆盖之外。

在 `frontend/` 执行 `npm run preview -- --host 127.0.0.1 --port 15206 --strictPort`，再运行 `node scripts/check-admin-users-p6.cjs`。脚本使用 Edge 和 Playwright Core，可用环境变量 `INTERNPILOT_PLAYWRIGHT` 指定 Playwright Core 安装路径。截图和 JSON 结果保存在本地 `frontend/dist/ui-p6-evidence/`。
