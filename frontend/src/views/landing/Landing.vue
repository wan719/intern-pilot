<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import brandLogo from '@/assets/brand-logo-optimized.png'
import { getToken } from '@/utils/token'
import PublicLandingHeader from './PublicLandingHeader.vue'
import { example } from './landing-example'
import './public-landing.scss'

const hasSession = Boolean(getToken())
const startPath = hasSession ? '/dashboard' : '/register'
const startLabel = hasSession ? '进入工作台' : '开始使用'
const sections = [
  { id: 'match', label: '匹配分析' },
  { id: 'revision', label: '简历优化' },
  { id: 'interview', label: '面试准备' },
  { id: 'application', label: '投递追踪' }
]

const previousAnchors = new Map([
  ['resumes-and-jobs', 'match'],
  ['matching-and-interviews', 'match'],
  ['application-tracking', 'application']
])

function currentSection() {
  const anchor = window.location.hash.slice(1)
  const id = previousAnchors.get(anchor) ?? anchor
  return document.getElementById(id)
}

function restoreSection() {
  const section = currentSection()
  section?.scrollIntoView?.()
  if (!document.activeElement?.classList.contains('chapter')) return
  const target = section ?? document.getElementById('landing-main')
  target?.focus({ preventScroll: true })
}

onMounted(() => {
  currentSection()?.scrollIntoView?.()
  window.addEventListener('hashchange', restoreSection)
})
onUnmounted(() => window.removeEventListener('hashchange', restoreSection))
</script>

<template>
  <div class="landing-public">
    <a href="#landing-main" class="skip">跳到主要内容</a>
    <PublicLandingHeader :has-session="hasSession">
      <nav class="overview" aria-label="首页导航">
        <a v-for="section in sections" :key="section.id" :href="'#' + section.id">{{ section.label }}</a>
      </nav>
    </PublicLandingHeader>
    <main id="landing-main" tabindex="-1">
      <div id="top"></div>
      <section class="hero hero-a frame" aria-labelledby="headline">
        <div class="display-a">
          <div class="resume-slot">
            <article class="paper" :aria-label="`${example.candidate}的合成简历`">
              <header class="paper-header"><div><p class="person-name">{{ example.candidate }}</p><p>{{ example.direction }} · 2027 届</p></div><span>合成简历</span></header>
              <section>
                <h2>教育背景</h2>
                <div class="resume-meta"><strong>软件工程 · 本科</strong><span>2023 — 2027</span></div>
                <p>相关课程：Web 开发、数据库、软件工程</p>
              </section>
              <section>
                <h2>项目经历</h2>
                <div class="resume-meta"><strong>{{ example.project }}</strong><span>2026.02 — 05</span></div>
                <ul><li class="source-line"><mark>{{ example.original }}</mark>。</li><li>{{ example.projectSummary }}</li></ul>
              </section>
              <section><h2>技能</h2><p>Vue、Java、Spring Boot</p></section>
            </article>
          </div>
          <div class="requirement-note"><span>岗位要求</span><strong>{{ example.requirement }}</strong></div>
          <div class="evidence-note"><span>简历证据</span><strong>{{ example.original }}</strong></div>
          <div class="revision-note"><span>改写后</span><strong>{{ example.shortRevision }}</strong></div>
        </div>
        <div class="intro">
          <h1 id="headline">投递前，先看你和岗位差在哪</h1>
          <p>上传简历，填好目标岗位。看清哪些经历对得上、哪里值得补，再准备面试和投递。</p>
          <div class="actions">
            <RouterLink class="primary" :to="startPath">{{ startLabel }} <span aria-hidden="true">↗</span></RouterLink>
            <RouterLink class="secondary" to="/sample-report">查看样例报告 <span aria-hidden="true">↗</span></RouterLink>
          </div>
        </div>
      </section>

      <div class="casebook frame">
        <div class="casebook-heading"><p>{{ example.job }}案例</p><span>简历、岗位和进度均为合成数据</span></div>
        <section class="chapter" id="match" aria-labelledby="match-title" tabindex="-1">
          <header class="chapter-copy"><p>匹配分析</p><h2 id="match-title">这份简历，<br />和岗位差在哪？</h2><p>上传简历、填写目标岗位后，匹配报告会列出已有证据和待补充的信息。</p></header>
          <article class="chapter-paper" aria-label="匹配案例">
            <div class="input-pair">
              <div><p>上传的简历</p><strong>{{ example.candidate }}的简历.pdf</strong><small>原始版 · {{ example.project }}</small></div>
              <div><p>填写的岗位</p><strong>{{ example.job }}</strong><small>{{ example.jobSummary }}</small></div>
            </div>
            <dl class="paper-rows">
              <div><dt>岗位要求</dt><dd>{{ example.requirement }}</dd></div>
              <div><dt>简历已有</dt><dd><mark>{{ example.original }}</mark>。</dd></div>
              <div><dt>还可以补充</dt><dd>{{ example.gap }}</dd></div>
            </dl>
          </article>
        </section>

        <section class="chapter" id="revision" aria-labelledby="revision-title" tabindex="-1">
          <header class="chapter-copy"><p>简历优化与版本</p><h2 id="revision-title">把做过的事，<br />写得更具体</h2><p>可以让 AI 针对目标岗位生成新版本。核对经历和数字后，再决定用哪个版本投递。</p></header>
          <article class="chapter-paper" aria-label="改写和版本对照">
            <p class="sheet-label">改写前</p><p class="original-line">{{ example.original }}</p>
            <div class="revision-line"><p class="sheet-label">改写后</p><p>{{ example.revision }}</p></div>
            <dl class="paper-rows versions" aria-label="简历版本">
              <div><dt>原始版</dt><dd>保留初始内容</dd></div>
              <div><dt>{{ example.version }}</dt><dd><span class="status-pill">新版本</span></dd></div>
            </dl>
            <p class="paper-note">{{ example.revisionNote }}</p>
          </article>
        </section>

        <section class="chapter" id="interview" aria-labelledby="interview-title" tabindex="-1">
          <header class="chapter-copy"><p>面试准备</p><h2 id="interview-title">从简历经历里，<br />提前想一遍怎么答</h2><p>用简历和岗位生成题目。有匹配报告时，也可以附上作为额外依据。</p></header>
          <article class="chapter-paper" aria-label="面试样题">
            <p class="sheet-label"><span>{{ example.job }}</span> · 项目追问</p>
            <h3 class="sample-question">{{ example.question }}</h3>
            <dl class="paper-rows">
              <div v-for="cue in example.interviewCues" :key="cue.title"><dt>{{ cue.title }}</dt><dd>{{ cue.detail }}</dd></div>
            </dl>
          </article>
        </section>

        <section class="chapter" id="application" aria-labelledby="application-title" tabindex="-1">
          <header class="chapter-copy"><p>投递追踪</p><h2 id="application-title">投了哪个岗位，<br />现在到哪一步？</h2><p>用投递记录查看关联简历和当前进度，把已投递、面试和录用结果记在一处。</p></header>
          <article class="chapter-paper application-sheet" aria-label="合成投递记录">
            <header class="paper-title"><div><p class="sheet-label">投递记录</p><h3>{{ example.job }}</h3></div><span class="status-pill">面试中</span></header>
            <dl class="paper-rows"><div><dt>关联简历</dt><dd>{{ example.resumeName }}</dd></div></dl>
            <ol class="timeline" aria-label="投递进度">
              <li class="complete"><span>已投递</span><small>已关联简历</small></li>
              <li aria-current="step"><span>面试中</span><small>当前状态</small></li>
              <li><span>录用结果</span><small>待更新</small></li>
            </ol>
          </article>
        </section>
      </div>

      <section class="end frame" aria-labelledby="end-title">
        <div><h2 id="end-title">看看这份简历的样例报告</h2></div>
        <div class="actions">
          <RouterLink class="primary" to="/sample-report">查看样例报告 <span aria-hidden="true">↗</span></RouterLink>
          <RouterLink class="secondary" :to="startPath">{{ startLabel }}</RouterLink>
        </div>
      </section>
    </main>

    <footer class="frame site-footer">
      <a class="public-brand" href="#top"><img :src="brandLogo" alt="" width="24" height="24" />InternPilot</a>
      <span>本页展示合成样例</span>
    </footer>
  </div>
</template>
