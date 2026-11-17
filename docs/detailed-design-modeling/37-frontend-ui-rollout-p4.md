# P4 岗位机会与投递追踪

本批基于 `0b9b592`。岗位库页把搜索放在列表前，区分筛选无结果、首次空库和读取失败。推荐页将生成表单与历史分区，结果页把推荐分与岗位标题放在一起，同时保留理由、匹配证据、风险和后续操作。投递追踪页在筛选后显示匹配记录数和本页摘要，每条记录显示精确状态、优先级和下一步。来自报告的待投递确认位于筛选前。四页沿用 G2 的语义 token 和现有组件，375px 下主要列表的操作区为两列。

## 本地验证

Windows，Node `20.20.2`，npm `11.12.1`，Edge `157.0.4341.60`。命令在 `frontend/` 运行，`git diff --check` 在仓库根目录运行。

| 命令 | 结果 |
| --- | --- |
| `npm exec --yes --package=node@20.20.2 -c 'node -v && npm ci --offline --no-audit --no-fund'` | 退出 0，安装 284 个包 |
| `npm exec --yes --package=node@20.20.2 -c 'npm run test:run -- --maxWorkers=1 --hookTimeout=20000 src/views/job/__tests__/JobList.spec.ts src/views/recommendation/__tests__ src/views/application/__tests__/ApplicationList.spec.ts'` | 退出 0，5 个文件 68 个测试通过，无跳过 |
| `npm exec --yes --package=node@20.20.2 -c 'npm run type-check && npm run build'` | 退出 0，Vite 处理 2873 个模块，提示大于 500 kB 的依赖分块 |
| `git diff --check` | 退出 0 |

新增两个筛选无结果回归测试。改动前相关两份测试为 2 失败、21 通过，失败原因为筛选后显示了首次空库文案。首次并发验证时权限测试的 10 秒启动钩子超时，单工作进程以 20 秒钩子上限重跑通过。

## Edge 浏览器证据

启动 `npm exec --yes --package=node@20.20.2 -c 'npm run dev -- --host 127.0.0.1 --port 15204 --strictPort'`，设置 `$env:P4_SCREENSHOT_DIR='E:/Temp/internpilot-p4-screenshots'` 后运行 `npm exec --yes --package=node@20.20.2 -c 'node E:/Temp/internpilot-p4-browser.cjs'`（退出 0）。Playwright Core 拦截 API 并使用合成数据。1440×900 和 375×812 各通过四页布局及操作核验。岗位验证包括键盘搜索、清除筛选、JD 展开、新建对话框与模拟 403 后重试加载。推荐验证包括等级筛选、按简历和版本生成、加入投递失败后重试、成功后撤下同岗位的创建操作。投递验证包括分页、改状态、筛选无结果恢复和报告确认入口。另以缺少权限与无会话检查 403、登录跳转。八张截图位于本工作树 `frontend/dist/p4-screenshots/`。

组件测试还覆盖岗位写入、推荐任务记录与并发提交、投递写入与报告来源状态。后端服务、真实 AI、跨用户数据隔离、生产配置、其他浏览器和完整前端测试套件未在本批执行。
