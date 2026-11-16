# P2 简历版本工作区

基础提交为 `0b9b592`，验收对象为本 PR 的 `/resumes/:resumeId/versions`。工作区采用与简历中心一致的暖色底和紧凑的当前版本摘要。历史保留当前版本置顶、其余按创建时间倒序。对比面板分别显示新增和移除的行及数量，窄屏纵向排列。重试沿用已选方向，切换版本或关闭时丢弃过期的显示结果。

## 本地验证

Windows，Node 20.20.2，npm 11.12.1，Edge 157.0.4341.60。以下命令从 `frontend/` 运行。

| 命令 | 退出码 | 结果 |
| --- | --- | --- |
| `npm exec --offline --package=node@20.20.2 -c 'node --version && npm --version && npm ci --offline --no-audit --no-fund'` | 0 | 按锁文件安装 284 个包 |
| `npm exec --offline --package=node@20.20.2 -c 'npm run test:run -- src/views/resume/__tests__/ResumeVersionList.spec.ts src/views/resume/__tests__/ResumeList.spec.ts --maxWorkers=1 --reporter=dot'` | 0 | 2 个文件，23 例通过，无跳过 |
| `npm exec --offline --package=node@20.20.2 -c 'npm run type-check && npm run build'` | 0 | 类型检查和生产构建通过，`vendor-element` 有 500 kB 分包提醒 |
| `npm exec --offline --package=node@20.20.2 -- node E:/Temp/internpilot-p2-browser.cjs` | 0 | 本机 Edge 两种视口的合成夹具检查通过 |

浏览器服务命令为 `npm exec --offline --package=node@20.20.2 -c 'node node_modules/vite/bin/vite.js preview --host 127.0.0.1 --port 15202 --strictPort'`。Edge 在 1440×900 和 375×812 验证页面及长差异行无横向溢出，旧版本 101 → 新版本 202 的请求方向、对比失败后重试、ESC 关闭与焦点返回、取消删除无 DELETE、AI 优化表单和 `?versionId=202` 详情。375px 额外检查模拟 403 读取失败后重试进入空列表。另检查无会话跳转登录。四张截图和 `results.json` 位于本机 `frontend/dist/p2-evidence/`，仅使用合成数据。

组件测试另覆盖建版、编辑、设当前、删除确认、列表加载和失败、AI 优化参数和任务结果路径。真实后端、跨用户隔离、外部 AI 服务和全量项目测试留待独立验证。
