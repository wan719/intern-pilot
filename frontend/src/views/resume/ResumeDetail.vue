<template>
  <PageContainer title="简历详情" width="wide" class="resume-detail-page">
    <template #hero>
      <RouterLink class="resume-page-back" to="/resumes">← 返回简历中心</RouterLink>
      <div class="resume-detail-heading">
        <div>
          <h1>{{ resume ? resumeTitle(resume) : '简历详情' }}</h1>
          <p v-if="resume" class="resume-file-info">
            {{ resume.originalFileName || '未记录原文件名' }} · {{ resume.fileType }}
            <span v-if="resume.isDefault"> · 默认简历</span>
          </p>
        </div>
        <RouterLink v-if="resume" :to="`/resumes/${resume.resumeId}/versions`">版本管理</RouterLink>
      </div>
    </template>

    <p v-if="loading" role="status">正在加载简历…</p>
    <section v-else-if="!resume" class="resume-state">
      <p role="alert">{{ detailError }}</p>
      <el-button v-if="resumeId" @click="resumeReload++">重试加载简历</el-button>
    </section>

    <el-tabs v-else v-model="tab" class="resume-detail-tabs">
      <el-tab-pane label="原件" name="original">
        <ResumeOriginal v-if="tab === 'original'" :key="resume.resumeId" :resume="resume" />
      </el-tab-pane>
      <el-tab-pane label="AI 读取文本" name="text">
        <section class="resume-state">
          <h2>上传时保存的解析结果</h2>
          <p>匹配和面试使用所选简历版本。这里可以核对上传原件时解析出的文字。</p>
          <div class="text-preview">{{ resume.parsedText || '暂无解析文本' }}</div>
        </section>
      </el-tab-pane>
      <el-tab-pane label="版本内容" name="version">
        <p v-if="versionLoading" role="status">正在加载版本内容…</p>
        <section v-else-if="!version" class="resume-state">
          <p v-if="versionError" role="alert">{{ versionError }}</p>
          <p v-else>这份简历还没有可展示的当前版本。</p>
          <div class="resume-version-actions">
            <el-button v-if="versionError && !missingVersion" @click="versionReload++">重试加载版本</el-button>
            <RouterLink v-if="requestedVersion !== undefined" :to="currentVersionLink">查看当前版本</RouterLink>
            <RouterLink v-else-if="canWrite && !versionError" :to="{ name: 'resume-edit', params: { resumeId } }">新建版本</RouterLink>
          </div>
        </section>
        <template v-else>
          <div class="resume-version-toolbar">
            <div>
              <label for="resume-version-select">查看版本</label>
              <select id="resume-version-select" :value="version.versionId" @change="selectVersion">
                <option v-for="item in versions" :key="item.versionId" :value="item.versionId">
                  {{ item.versionName }}{{ item.isCurrent === 1 ? '（当前版本）' : '' }}
                </option>
              </select>
              <span class="resume-version-status">{{ version.isCurrent === 1 ? '当前版本' : '未设为当前' }}</span>
              <p v-if="version.sourceVersionId" class="resume-version-source">
                来源版本
                <RouterLink v-if="sourceVersion" :to="versionLink(sourceVersion.versionId)">{{ sourceVersion.versionName }}</RouterLink>
                <span v-else>#{{ version.sourceVersionId }}（已不可用）</span>
              </p>
            </div>
            <div class="resume-version-actions">
              <RouterLink v-if="canWrite" :to="{ name: 'resume-edit', params: { resumeId }, query: { sourceVersionId: version.versionId } }">编辑版本</RouterLink>
              <el-button v-if="canWrite && version.isCurrent !== 1" size="small" :loading="settingCurrent" @click="setCurrent">设为当前</el-button>
              <RouterLink :to="{ path: '/analysis/match', query: { resumeId, resumeVersionId: version.versionId } }">用此版本做匹配</RouterLink>
            </div>
          </div>
          <p v-if="currentError" role="alert">{{ currentError }}</p>
          <p v-if="currentNotice" role="status">{{ currentNotice }}</p>
          <ResumePaper :title="version.versionName" :content="version.content" />
        </template>
      </el-tab-pane>
    </el-tabs>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import { getResumeDetailApi } from '@/api/resume'
import { getResumeVersionDetailApi, getResumeVersionListApi, setCurrentResumeVersionApi } from '@/api/resumeVersion'
import { useAuthStore } from '@/stores/auth'
import '@/styles/resume-utilities.css'
import ResumeOriginal from './ResumeOriginal.vue'
import ResumePaper from './ResumePaper.vue'
import { positiveId, resumeTitle, type ResumeDetails, type ResumeVersionContent, type ResumeVersionSummary } from './resumePages'

const route = useRoute()
const navigation = useRouter()
const auth = useAuthStore()
const canWrite = computed(() => auth.hasPermission('resume:write'))
const resumeId = computed(() => positiveId(route.params.resumeId))
const requestedVersion = computed(() => route.query.versionId)
const resume = ref<ResumeDetails>()
const loading = ref(false)
const detailError = ref('')
const resumeReload = ref(0)
const version = ref<ResumeVersionContent>()
const versions = ref<ResumeVersionSummary[]>([])
const versionLoading = ref(false)
const versionError = ref('')
const missingVersion = ref(false)
const versionReload = ref(0)
const settingCurrent = ref(false)
const currentError = ref('')
const currentNotice = ref('')
const sourceVersion = computed(() => versions.value.find(item => item.versionId === version.value?.sourceVersionId))

function versionLink(versionId?: number) {
  return { name: 'resume-detail', params: { resumeId: resumeId.value }, query: { tab: 'version', versionId } }
}
const currentVersionLink = computed(() => versionLink())

const tab = computed({
  get: () => route.query.tab === 'text' || route.query.tab === 'version' ? route.query.tab : 'original',
  set: (next: string) => {
    void navigation.push({
      name: 'resume-detail',
      params: { resumeId: resumeId.value },
      query: next === 'original' ? {} : { tab: next }
    })
  }
})

watch([resumeId, resumeReload], async ([id], _previous, onCleanup) => {
  let active = true
  onCleanup(() => { active = false })
  resume.value = undefined
  detailError.value = ''
  loading.value = Boolean(id)
  if (!id) {
    detailError.value = '简历地址无效。'
    return
  }
  try {
    const detail = await getResumeDetailApi(id, { silentError: true }) as unknown as ResumeDetails
    if (active) resume.value = detail
  } catch {
    if (active) detailError.value = '无法读取这份简历，记录可能已删除或当前账号无权访问。'
  } finally {
    if (active) loading.value = false
  }
}, { immediate: true })

watch([resumeId, resume, tab, requestedVersion, versionReload], async ([id, detail, currentTab, requested], _previous, onCleanup) => {
  let active = true
  onCleanup(() => { active = false })
  version.value = undefined
  versions.value = []
  versionError.value = ''
  missingVersion.value = false
  versionLoading.value = Boolean(id && detail?.resumeId === id && currentTab === 'version')
  if (!id || detail?.resumeId !== id || currentTab !== 'version') return
  try {
    const list = await getResumeVersionListApi(id, { silentError: true }) as unknown as ResumeVersionSummary[]
    if (!active) return
    versions.value = list ?? []
    const selected = requested === undefined
      ? versions.value.find(item => item.isCurrent === 1)
      : versions.value.find(item => item.versionId === positiveId(requested))
    if (!selected) {
      if (requested !== undefined) {
        missingVersion.value = true
        versionError.value = '指定版本已不可用。'
      }
      return
    }
    const result = await getResumeVersionDetailApi(id, selected.versionId, { silentError: true }) as unknown as ResumeVersionContent
    if (active) version.value = result
  } catch {
    if (active) versionError.value = '无法读取版本，请重试。'
  } finally {
    if (active) versionLoading.value = false
  }
}, { immediate: true })

watch(() => route.fullPath, () => {
  currentError.value = ''
  currentNotice.value = ''
})

function selectVersion(event: Event) {
  const id = positiveId((event.target as HTMLSelectElement).value)
  if (id) void navigation.push(versionLink(id))
}

async function setCurrent() {
  const selected = version.value
  const id = resumeId.value
  if (!selected || !id || !canWrite.value || settingCurrent.value || selected.isCurrent === 1) return
  const path = route.fullPath
  settingCurrent.value = true
  currentError.value = ''
  currentNotice.value = ''
  try {
    await setCurrentResumeVersionApi(id, selected.versionId)
    if (route.fullPath === path) {
      currentNotice.value = '已设为当前版本。'
      versionReload.value++
    }
  } catch {
    if (route.fullPath === path) currentError.value = '设置失败，原有当前版本仍可查看，请重试。'
  } finally {
    settingCurrent.value = false
  }
}
</script>

<style scoped>
.resume-page-back { display: inline-block; margin-bottom: 12px; }
.resume-detail-heading { display: flex; flex-wrap: wrap; align-items: end; justify-content: space-between; gap: 12px; }
.resume-detail-heading > div { min-width: 0; }
.resume-detail-heading h1 { margin: 0; overflow-wrap: anywhere; }
.resume-file-info, .resume-version-source { margin: 6px 0 0; color: var(--color-text-muted); overflow-wrap: anywhere; }
.resume-detail-tabs { margin-top: 16px; min-width: 0; }
.resume-state, .resume-version-toolbar {
  padding: clamp(14px, 3vw, 24px);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.resume-version-toolbar { display: flex; justify-content: space-between; align-items: start; gap: 14px; flex-wrap: wrap; margin-bottom: 18px; }
.resume-version-toolbar > div { min-width: 0; }
.resume-version-toolbar label { display: block; margin-bottom: 6px; }
.resume-version-toolbar select {
  max-width: 100%; padding: 6px 10px; background: var(--color-surface); color: var(--color-text);
  border: 1px solid var(--color-border); border-radius: var(--radius-md);
}
.resume-version-status { display: inline-block; margin-left: 8px; color: var(--color-text-muted); }
.resume-version-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.resume-detail-page a:focus-visible, .resume-detail-page select:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 3px; }
</style>
