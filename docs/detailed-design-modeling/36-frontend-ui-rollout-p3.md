# P3｜用户工作台

基于 `0b9b592` 的 `codex/frontend-ui-p3`。工作台页首显示当前阶段和一个主操作，五个阶段入口各自显示操作提示与启动状态。375px 视口使用单列阶段入口和两列统计卡。后续内容保留近期报告、投递记录、投递状态和匹配分图表。

面试数据失败后，如果该阶段决定主建议，页首操作用于重试。更早的阶段仍需启动时，面试数据在旅程内独立重试。四类基础请求失败时仍可重新加载工作台。阶段优先级、路由、接口及共享 AI 任务中心保持原有契约。

## 本地验证

Windows。离线依赖安装由系统 npm 启动器以 Node 24.15.0、npm 11.12.1 执行。测试、类型检查、构建和浏览器脚本使用 Node 20.20.2、npm 11.12.1。以下命令均在 `frontend/` 执行。

| 命令 | 结果 |
| --- | --- |
| `npm ci --offline --no-audit --no-fund` | 退出 0，安装 284 个包 |
| `node $npmCli run test:run -- src/views/dashboard/__tests__/Dashboard.spec.ts --maxWorkers=1` | 改造前退出 1，新增测试发现旅程缺少可访问名称，其他 14 项通过。改造后退出 0，15 项通过 |
| `node $npmCli run test:run -- src/views/dashboard/__tests__/Dashboard.spec.ts src/views/admin/__tests__/AdminCrudPages.spec.ts --maxWorkers=1` | 退出 0，24 项通过 |
| `node $npmCli run test:run -- --maxWorkers=1` | 首次退出 1，316 项通过，`AdminCrudPages.spec.ts` 中列表/角色/详情读取、启禁用/角色确认两项达 5 秒超时。两项专项复测通过。随后原命令复跑退出 0，36 个文件、318 项通过，无跳过 |
| `node $npmCli run type-check` | 退出 0 |
| `node $npmCli run build` | 退出 0，Vite 提示 Element Plus vendor chunk 大于 500 kB |
| `node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 15203 --strictPort` | 开发服务就绪 |
| `node E:/Temp/internpilot-dashboard-p3-edge.cjs` | 退出 0，Edge 157.0.4341.60，Playwright Core 合成接口 |

测试命令中的 `$npmCli` 为 `E:/node_modules/npm/bin/npm-cli.js`。测试前将 Node 20 的目录放到 PATH 首位。

Edge 在 1440×1000、375×812 检查一个主操作、五个阶段、两张图表、无横向溢出、键盘焦点与启动、AI 任务抽屉打开和 Escape 关闭。375px 视口另检查第五阶段在底部导航上方，以及全空、面试确认中、面试错误单独恢复、基础请求失败后重新加载、缺权限访问推荐页转至 `/403`。本机截图位于 `frontend/dist/p3-dashboard/dashboard-1440.png` 和 `frontend/dist/p3-dashboard/dashboard-375.png`。

本轮浏览器使用合成数据。真实后端、WebSocket、其他浏览器与生产环境未验证。

[工作台页面](../../frontend/src/views/dashboard/Dashboard.vue) · [组件测试](../../frontend/src/views/dashboard/__tests__/Dashboard.spec.ts)
