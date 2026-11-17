<script setup lang="ts">
import { onMounted } from 'vue'
import { ArrowDown, ArrowRight, Briefcase, Check, Document, List, MagicStick, Position } from '@element-plus/icons-vue'
import brandLogo from '@/assets/brand-logo-optimized.png'
import { Button } from '@/components/ui/button'
import { getToken } from '@/utils/token'
import '@/styles/resume-utilities.css'

const hasSavedSession = Boolean(getToken())
const action = hasSavedSession
  ? { to: '/dashboard', label: '进入工作台' }
  : { to: '/register', label: '开始使用' }

const sections = [
  { id: 'resumes-and-jobs', label: '简历与岗位' },
  { id: 'matching-and-interviews', label: '匹配与面试' },
  { id: 'application-tracking', label: '投递进度' }
]

const job = '前端开发实习生'
const version = '前端实习版'
const applicationStatuses = [
  { label: '已投递', detail: '已记录本次投递', complete: true },
  { label: '面试中', detail: '当前状态', complete: true },
  { label: '录用', detail: '待更新', complete: false }
]

// A direct link can load before the lazily imported page has mounted.
onMounted(() => {
  const target = document.getElementById(window.location.hash.slice(1))
  target?.scrollIntoView()
})
</script>

<template>
  <div id="top" class="landing">
    <a class="landing-skip" href="#landing-main">跳到主要内容</a>
    <header class="landing-header">
      <div class="landing-container landing-header-inner">
        <a class="landing-brand" href="#top" aria-label="InternPilot 首页">
          <img :src="brandLogo" width="32" height="32" alt="" />
          <span>InternPilot</span>
        </a>
        <nav aria-label="产品介绍" class="landing-nav">
          <a v-for="section in sections" :key="section.id" :href="'#' + section.id">{{ section.label }}</a>
        </nav>
        <RouterLink class="landing-login" to="/login">登录</RouterLink>
      </div>
    </header>

    <main id="landing-main" tabindex="-1">
      <section class="landing-hero" aria-labelledby="landing-heading">
        <div class="landing-container landing-hero-inner">
          <div class="landing-hero-copy">
            <h1 id="landing-heading">实习求职，<span>从简历到投递</span></h1>
            <p>把简历版本、目标岗位和投递进度放在一个工作区。用 AI 看清匹配差距，带着具体问题准备面试。</p>
            <div class="landing-actions">
              <Button as-child class="landing-cta ip:font-semibold">
                <RouterLink :to="action.to">{{ action.label }} <ArrowRight aria-hidden="true" /></RouterLink>
              </Button>
              <a class="landing-secondary" href="#resumes-and-jobs">查看功能 <ArrowDown aria-hidden="true" /></a>
            </div>
          </div>

          <figure class="landing-preview">
            <figcaption>
              <img :src="brandLogo" width="23" height="23" alt="" />
              求职工作区
              <span>示例</span>
            </figcaption>
            <div class="landing-preview-body">
              <span class="landing-preview-label">目标岗位</span>
              <strong class="landing-preview-job">{{ job }}</strong>
              <ul class="landing-preview-list">
                <li>
                  <span class="landing-icon"><Document aria-hidden="true" /></span>
                  <span><small>简历版本</small><strong>{{ version }}</strong></span>
                  <span class="landing-chip">当前版本</span>
                </li>
                <li>
                  <span class="landing-icon"><MagicStick aria-hidden="true" /></span>
                  <span><small>匹配建议</small><strong>补充组件设计的实际成果</strong></span>
                </li>
                <li>
                  <span class="landing-icon"><Position aria-hidden="true" /></span>
                  <span><small>投递进度</small><strong>等待下一轮安排</strong></span>
                  <span class="landing-chip landing-chip-info">面试中</span>
                </li>
              </ul>
            </div>
          </figure>
        </div>
      </section>

      <div class="landing-container landing-feature-list">
        <section id="resumes-and-jobs" class="landing-feature" tabindex="-1" aria-labelledby="resume-heading">
          <div class="landing-feature-copy">
            <span class="landing-icon"><Briefcase aria-hidden="true" /></span>
            <h2 id="resume-heading">管理简历与岗位</h2>
            <p>上传简历、保留历史版本，集中整理目标岗位。面对不同岗位，随时回看简历版本和职位要求。</p>
          </div>
          <div class="landing-feature-visual landing-feature-documents">
            <div class="landing-sample-card">
              <div class="landing-sample-heading"><Document aria-hidden="true" /><strong>简历版本</strong></div>
              <ul class="landing-sample-list">
                <li><span>{{ version }}</span><span class="landing-chip">当前</span></li>
                <li><span>通用求职版</span><span class="landing-history">历史</span></li>
              </ul>
            </div>
            <div class="landing-sample-card">
              <div class="landing-sample-heading"><Briefcase aria-hidden="true" /><strong>岗位要求</strong></div>
              <strong class="landing-sample-job">{{ job }}</strong>
              <p>组件开发 · TypeScript · 协作交付</p>
            </div>
          </div>
        </section>

        <section id="matching-and-interviews" class="landing-feature" tabindex="-1" aria-labelledby="matching-heading">
          <div class="landing-feature-copy">
            <span class="landing-icon"><MagicStick aria-hidden="true" /></span>
            <h2 id="matching-heading">分析匹配与准备面试</h2>
            <p>对照职位描述，找出简历里的相关经历和待补充之处，再用生成的面试题练习表达。</p>
          </div>
          <div class="landing-feature-visual landing-feature-analysis">
            <div class="landing-sample-card">
              <div class="landing-sample-heading"><MagicStick aria-hidden="true" /><strong>AI 匹配分析</strong></div>
              <dl class="landing-analysis">
                <div><dt>相关经历</dt><dd>有 Vue 和 TypeScript 项目经验。</dd></div>
                <div><dt>待补充</dt><dd>说明组件如何复用，以及实际效果。</dd></div>
              </dl>
            </div>
            <div class="landing-sample-card">
              <div class="landing-sample-heading"><List aria-hidden="true" /><strong>面试练习</strong></div>
              <p>你如何设计一个可复用的表单组件？</p>
            </div>
          </div>
        </section>

        <section id="application-tracking" class="landing-feature" tabindex="-1" aria-labelledby="application-heading">
          <div class="landing-feature-copy">
            <span class="landing-icon"><Position aria-hidden="true" /></span>
            <h2 id="application-heading">跟踪投递进度</h2>
            <p>记录投过哪些岗位，及时更新面试和录用状态。再次打开工作区，就知道每一份投递走到了哪一步。</p>
          </div>
          <div class="landing-feature-visual">
            <div class="landing-sample-card landing-tracking">
              <div class="landing-sample-heading"><Position aria-hidden="true" /><strong>{{ job }}</strong></div>
              <ol class="landing-status-list">
                <li v-for="status in applicationStatuses" :key="status.label">
                  <span class="landing-status-marker" :class="{ 'is-complete': status.complete }" aria-hidden="true">
                    <Check v-if="status.complete" />
                  </span>
                  <strong>{{ status.label }}</strong>
                  <span>{{ status.detail }}</span>
                </li>
              </ol>
            </div>
          </div>
        </section>
      </div>

      <section class="landing-closing" aria-labelledby="closing-heading">
        <div class="landing-container">
          <h2 id="closing-heading">从下一份实习申请开始</h2>
          <Button as-child class="landing-cta ip:font-semibold">
            <RouterLink :to="action.to">{{ action.label }} <ArrowRight aria-hidden="true" /></RouterLink>
          </Button>
        </div>
      </section>
    </main>

    <footer class="landing-container landing-footer">
      <span>InternPilot</span>
      <a href="https://github.com/wan719/intern-pilot">GitHub 仓库 <ArrowRight aria-hidden="true" /></a>
    </footer>
  </div>
</template>

<style scoped lang="scss">
.landing {
  --landing-mint: color-mix(in srgb, var(--color-primary-soft) 33%, var(--color-surface));
  --landing-gutter: clamp(20px, 5vw, 64px);
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  background: var(--color-surface);
  color: var(--color-text);
}

.landing-container {
  width: min(100% - 2 * var(--landing-gutter), 1160px);
  margin-inline: auto;
}

.landing a:focus-visible,
.landing main:focus-visible,
.landing section:focus-visible {
  outline: var(--focus-ring);
  outline-offset: 4px;
}

.landing-skip {
  position: absolute;
  z-index: 3;
  inset: 10px auto auto 10px;
  padding: 10px 14px;
  transform: translateY(-150%);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-primary-hover);

  &:focus { transform: none; }
}

.landing-header {
  border-bottom: 1px solid var(--color-border);
}

.landing-header-inner {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  min-height: 72px;
  gap: 16px;
}

.landing-brand,
.landing-nav,
.landing-actions,
.landing-footer {
  display: flex;
  align-items: center;
}

.landing-brand {
  width: fit-content;
  gap: 9px;
  color: var(--color-text);
  font-size: 19px;
  font-weight: 750;
}

.landing-brand img,
.landing-preview img {
  object-fit: contain;
}

.landing-nav { gap: clamp(14px, 3vw, 36px); }

.landing-nav a,
.landing-login,
.landing-footer a {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  color: var(--color-text);

  &:hover { color: var(--color-primary-hover); }
}

.landing-login {
  justify-self: end;
  padding-inline: 18px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  font-weight: 650;
}

.landing-hero {
  border-bottom: 1px solid var(--color-border);
  background:
    radial-gradient(ellipse at 77% 40%, var(--landing-mint), transparent 65%),
    var(--color-surface-muted);
}

.landing-hero-inner {
  display: grid;
  min-height: 560px;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1fr);
  align-items: center;
  gap: clamp(28px, 5vw, 76px);
  padding-block: clamp(52px, 7vw, 100px);
}

.landing-hero h1 {
  margin: 0;
  font-size: clamp(34px, 4.4vw, 66px);
  line-height: 1.18;
  letter-spacing: -0.045em;
  font-weight: 780;

  span { display: block; color: var(--color-primary-hover); }
}

.landing-hero-copy > p,
.landing-feature-copy > p {
  max-width: 35ch;
  margin: 24px 0 0;
  color: var(--color-text-muted);
  font-size: clamp(15px, 1.55vw, 19px);
  line-height: 1.8;
}

.landing-actions {
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 32px;
}

.landing a.landing-cta,
.landing-secondary {
  display: inline-flex;
  height: 46px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding-inline: 17px;
  border-radius: var(--radius-sm);
}

.landing a.landing-cta { color: var(--color-surface); }

.landing-secondary {
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text);
  font-weight: 650;

  &:hover { color: var(--color-primary-hover); }
}

.landing svg {
  width: 17px;
  height: 17px;
  flex: none;
}

.landing-preview {
  min-width: 0;
  margin: 0;
  border: 1px solid var(--color-border-strong);
  border-radius: 18px;
  background: var(--color-surface);
  box-shadow: var(--shadow-floating);
}

.landing-preview figcaption {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 18px;
  border-bottom: 1px solid var(--color-border);
  font-weight: 700;

  span {
    margin-inline-start: auto;
    color: var(--color-text-muted);
    font-size: 12px;
    font-weight: 500;
  }
}

.landing-preview-body { padding: clamp(14px, 3vw, 28px); }

.landing-preview-label,
.landing-preview-list small {
  color: var(--color-text-muted);
  font-size: 12px;
}

.landing-preview-job {
  display: block;
  margin-block: 5px 17px;
  font-size: clamp(19px, 2vw, 26px);
}

.landing-preview-list,
.landing-sample-list,
.landing-status-list {
  padding: 0;
  margin: 0;
  list-style: none;
}

.landing-preview-list {
  display: grid;
  gap: 8px;

  li {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: 10px;
    padding: 9px 10px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface-muted);
  }

  strong {
    display: block;
    margin-top: 3px;
    font-size: 13px;
    line-height: 1.4;
  }
}

.landing-icon {
  display: inline-grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--color-primary-subtle);
  color: var(--color-primary-hover);
}

.landing-chip,
.landing-history {
  width: fit-content;
  padding: 4px 7px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-subtle);
  color: var(--color-primary-hover);
  font-size: 12px;
  white-space: nowrap;
}

.landing-chip-info {
  background: color-mix(in srgb, var(--color-info) 8%, var(--color-surface));
  color: var(--color-info);
}

.landing-history {
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.landing-feature {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1fr);
  align-items: center;
  gap: clamp(26px, 7vw, 106px);
  padding-block: clamp(48px, 7vw, 92px);
  scroll-margin-top: 18px;

  & + & { border-top: 1px solid var(--color-border); }
}

.landing-feature h2,
.landing-closing h2 {
  margin: 16px 0 0;
  font-size: clamp(26px, 3vw, 39px);
  line-height: 1.25;
  letter-spacing: -0.03em;
}

.landing-feature-copy > p { margin-top: 12px; }

.landing-feature-visual {
  display: grid;
  min-width: 0;
  gap: 10px;
  padding: clamp(14px, 2.8vw, 28px);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: var(--landing-mint);
}

.landing-feature-documents {
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 165px), 1fr));
  align-items: stretch;
}

.landing-feature-analysis { background: var(--color-surface-muted); }

.landing-sample-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);

  p {
    margin: 10px 0 0;
    color: var(--color-text-muted);
    line-height: 1.55;
  }
}

.landing-sample-heading,
.landing-sample-list li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.landing-sample-heading svg { color: var(--color-primary-hover); }

.landing-sample-list li {
  justify-content: space-between;
  padding-block: 8px 0;
}

.landing-sample-job {
  display: block;
  margin-top: 12px;
}

.landing-analysis {
  display: grid;
  gap: 9px;
  margin: 12px 0 0;

  div { display: flex; flex-wrap: wrap; gap: 4px 9px; }
  dt { color: var(--color-primary-hover); font-weight: 650; }
  dd { margin: 0; color: var(--color-text-muted); }
}

.landing-status-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 18px;

  li {
    display: grid;
    align-content: start;
    gap: 5px;
    border-top: 1px solid var(--color-border);
    padding-top: 12px;
  }

  li > span:last-child { color: var(--color-text-muted); font-size: 12px; }
}

.landing-status-marker {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border: 1px solid var(--color-border-strong);
  border-radius: 50%;

  &.is-complete {
    border-color: var(--color-primary-border);
    background: var(--color-primary-subtle);
    color: var(--color-primary-hover);
  }

  svg { width: 13px; height: 13px; }
}

.landing-closing {
  padding-block: clamp(48px, 7vw, 90px);
  border-block: 1px solid var(--color-border);
  background: var(--landing-mint);
  text-align: center;

  h2 { margin: 0 0 22px; }
}

.landing-footer {
  min-height: 78px;
  justify-content: space-between;
  gap: 16px;
  color: var(--color-text-muted);
  font-weight: 600;

  a { gap: 6px; font-weight: 500; }
}

@media (max-width: 680px) {
  .landing-header-inner { grid-template-columns: 1fr auto; gap: 2px 10px; padding-top: 10px; }
  .landing-nav { grid-row: 2; grid-column: 1 / -1; justify-content: space-between; gap: 8px; }
  .landing-nav a { font-size: 13px; }
  .landing-hero-inner,
  .landing-feature { grid-template-columns: minmax(0, 1fr); }
  .landing-hero-inner { min-height: 0; }
  .landing-hero-copy > p { margin-top: 16px; }
  .landing-actions { margin-top: 22px; }
}

@media (prefers-reduced-motion: reduce) {
  .landing a { transition: none; }
}
</style>
