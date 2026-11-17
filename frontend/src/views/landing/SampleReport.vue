<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getToken } from '@/utils/token'
import PublicLandingHeader from './PublicLandingHeader.vue'
import { example } from './landing-example'
import './public-landing.scss'

const heading = ref<HTMLHeadingElement>()
const hasSession = Boolean(getToken())
const startPath = hasSession ? '/dashboard' : '/register'

onMounted(() => heading.value?.focus({ preventScroll: true }))
</script>

<template>
  <div class="landing-public sample-page">
    <PublicLandingHeader :has-session="hasSession" report />
    <main id="sample-main" class="sample-frame" tabindex="-1">
      <header class="sample-heading">
        <p class="sheet-label">合成数据 · 公开样例</p>
        <h1 ref="heading" tabindex="-1">样例报告</h1>
        <p>{{ example.candidate }} · <span>{{ example.job }}</span></p>
      </header>
      <section class="chapter-paper report-paper" aria-labelledby="report-match">
        <h2 id="report-match">岗位匹配</h2>
        <dl class="paper-rows">
          <div><dt>岗位要求</dt><dd>{{ example.requirement }}</dd></div>
          <div><dt>简历已有</dt><dd><mark>{{ example.original }}</mark>。</dd></div>
          <div><dt>还可以补充</dt><dd>{{ example.gap }}</dd></div>
        </dl>
      </section>
      <section class="chapter-paper report-paper" aria-labelledby="report-revision">
        <h2 id="report-revision">简历改写</h2>
        <p class="original-line">{{ example.original }}</p>
        <div class="revision-line"><p class="sheet-label">改写后 · <span>{{ example.version }}</span></p><p>{{ example.revision }}</p></div>
        <p class="paper-note">{{ example.revisionNote }}</p>
      </section>
      <section class="chapter-paper report-paper" aria-labelledby="report-interview">
        <h2 id="report-interview">面试准备</h2>
        <p class="sample-question">{{ example.question }}</p>
        <dl class="paper-rows">
          <div v-for="cue in example.interviewCues" :key="cue.title"><dt>{{ cue.title }}</dt><dd>{{ cue.detail }}</dd></div>
        </dl>
      </section>
      <div class="actions sample-actions"><RouterLink class="primary" :to="startPath">用我的简历开始 <span aria-hidden="true">↗</span></RouterLink></div>
    </main>
  </div>
</template>
