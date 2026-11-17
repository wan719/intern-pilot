<template>
  <PageContainer title="编辑简历版本" width="wide" class="resume-editor">
    <template #hero>
      <RouterLink class="resume-editor__back" :to="returnLink">← 返回简历详情</RouterLink>
      <h1>{{ sourceSpecified ? '编辑简历版本' : '创建简历版本' }}</h1>
      <p v-if="resume" class="resume-editor__context">{{ resumeTitle(resume) }}<span v-if="source"> · 来源 {{ source.versionName }}</span></p>
    </template>

    <p v-if="loading" role="status">正在加载编辑资料…</p>
    <section v-else-if="loadError" class="resume-editor__panel">
      <p role="alert">{{ loadError }}</p>
      <el-button v-if="resumeId" @click="reload++">重试加载</el-button>
    </section>

    <form v-else class="resume-editor__form" @submit.prevent="save">
      <p class="resume-editor__hint">{{ saveHint }}</p>
      <div v-if="compact" class="resume-editor__switch" role="group" aria-label="编辑视图">
        <button type="button" :aria-pressed="mobileView === 'edit'" @click="mobileView = 'edit'">编辑</button>
        <button type="button" :aria-pressed="mobileView === 'preview'" @click="mobileView = 'preview'">预览</button>
      </div>
      <div class="resume-editor__grid" :class="{ 'resume-editor__grid--compact': compact }">
        <section v-show="!compact || mobileView === 'edit'" class="resume-editor__panel" aria-label="版本编辑">
          <label for="resume-draft-name">版本名称</label>
          <input id="resume-draft-name" v-model="form.versionName" maxlength="100" placeholder="例如：全栈开发申请版" :disabled="saving" />

          <template v-if="!sourceSpecified">
            <label for="resume-draft-type">版本类型</label>
            <select id="resume-draft-type" v-model="form.versionType" :disabled="saving">
              <option value="MANUAL">手动编辑版本</option>
              <option value="JOB_TARGETED">岗位定制版本</option>
              <option value="IMPORTED">导入版本</option>
            </select>
          </template>

          <label for="resume-draft-content">版本正文</label>
          <textarea id="resume-draft-content" v-model="form.content" maxlength="50000" rows="16" placeholder="输入简历正文" :disabled="saving" />
        </section>

        <section v-show="!compact || mobileView === 'preview'" class="resume-editor__preview" aria-label="实时预览">
          <h2>实时预览</h2>
          <ResumePaper :title="form.versionName" :content="form.content" />
        </section>
      </div>

      <p v-if="saveError" class="resume-editor__error" role="alert">{{ saveError }}</p>
      <div class="resume-editor__actions">
        <RouterLink :to="returnLink">取消</RouterLink>
        <Button type="submit" class="ip:min-h-11" :disabled="saving" :aria-busy="saving">{{ saving ? '正在保存…' : '保存新版本' }}</Button>
      </div>
    </form>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import { Button } from '@/components/ui/button'
import { getResumeDetailApi } from '@/api/resume'
import { createResumeVersionApi, getResumeVersionDetailApi, getResumeVersionListApi } from '@/api/resumeVersion'
import { useAuthStore } from '@/stores/auth'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import ResumePaper from './ResumePaper.vue'
import { positiveId, resumeTitle, type ResumeDetails, type ResumeVersionContent, type ResumeVersionSummary } from './resumePages'

const navigation = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { isMobile, isTablet } = useResponsiveSize()
const compact = computed(() => isMobile.value || isTablet.value)
const mobileView = ref<'edit' | 'preview'>('edit')
const resumeId = computed(() => positiveId(route.params.resumeId))
const sourceSpecified = computed(() => route.query.sourceVersionId !== undefined)
const sourceId = computed(() => positiveId(route.query.sourceVersionId))
const resume = ref<ResumeDetails>()
const source = ref<ResumeVersionContent>()
const firstVersion = ref(false)
const loading = ref(false)
const loadError = ref('')
const reload = ref(0)
const saving = ref(false)
const saveError = ref('')
const form = reactive({ versionName: '', versionType: 'MANUAL', content: '' })
const initialForm = ref({ ...form })
const dirty = computed(() => form.versionName !== initialForm.value.versionName
  || form.versionType !== initialForm.value.versionType || form.content !== initialForm.value.content)

const saveHint = computed(() => firstVersion.value
  ? '这是第一份版本，保存后会成为当前版本。'
  : '保存会创建一份新版本。你可以在详情页选择是否将它设为当前。')
const returnLink = computed(() => resumeId.value ? {
  name: 'resume-detail',
  params: { resumeId: resumeId.value },
  query: { tab: 'version', versionId: sourceId.value }
} : '/resumes')

let pageActive = true
watch([resumeId, () => route.query.sourceVersionId, reload], async ([id, rawSource], _previous, onCleanup) => {
  let active = true
  onCleanup(() => { active = false })
  loading.value = true
  loadError.value = ''
  saveError.value = ''
  resume.value = undefined
  source.value = undefined
  firstVersion.value = false
  mobileView.value = 'edit'
  Object.assign(form, { versionName: '', versionType: 'MANUAL', content: '' })
  initialForm.value = { ...form }

  if (!auth.hasPermission('resume:read') || !auth.hasPermission('resume:write')) {
    void navigation.replace('/403')
    return
  }

  const selected = positiveId(rawSource)
  try {
    if (!id || (rawSource !== undefined && !selected)) {
      loadError.value = '简历或版本地址无效。'
      return
    }
    const detail = await getResumeDetailApi(id, { silentError: true }) as unknown as ResumeDetails
    if (!active) return
    resume.value = detail
    const list = await getResumeVersionListApi(id, { silentError: true }) as unknown as ResumeVersionSummary[]
    if (!active) return
    firstVersion.value = !list.length
    if (selected) {
      if (!list.some(item => item.versionId === selected)) {
        loadError.value = '来源版本已不可用。请返回简历详情选择另一个版本。'
        return
      }
      const version = await getResumeVersionDetailApi(id, selected, { silentError: true }) as unknown as ResumeVersionContent
      if (!active) return
      source.value = version
      form.versionName = (version.versionName || '简历版本').slice(0, 97) + ' 副本'
      form.content = version.content || ''
      initialForm.value = { ...form }
    }
  } catch {
    if (active) loadError.value = '无法读取编辑资料。请检查访问权限或重试。'
  } finally {
    if (active) loading.value = false
  }
}, { immediate: true })

async function save() {
  const id = resumeId.value
  if (!id || !resume.value || loading.value || loadError.value || saving.value || !auth.hasPermission('resume:write')) return
  const versionName = form.versionName.trim()
  if (!versionName || !form.content.trim()) {
    saveError.value = '请填写版本名称和正文。'
    return
  }
  const from = route.fullPath
  const payload = {
    versionName,
    content: form.content,
    versionType: sourceSpecified.value ? 'MANUAL' : form.versionType,
    ...(source.value ? { sourceVersionId: source.value.versionId } : {})
  }
  saving.value = true
  saveError.value = ''
  let versionId: number | undefined
  try {
    const result = await createResumeVersionApi(id, payload) as unknown as { versionId: number }
    if (!Number.isSafeInteger(result.versionId) || result.versionId < 1) throw new Error('Invalid version id')
    versionId = result.versionId
  } catch {
    if (pageActive && route.fullPath === from) saveError.value = '保存失败。已保留你的输入，请重试。'
  } finally {
    saving.value = false
  }
  if (versionId && pageActive && route.fullPath === from) {
    initialForm.value = { ...form }
    await navigation.replace({ name: 'resume-detail', params: { resumeId: id }, query: { tab: 'version', versionId } })
  }
}

async function confirmLeave() {
  if (!auth.user) return true
  if (saving.value) return false
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('离开后会丢失未保存的改动。', '放弃本次编辑？', {
      type: 'warning', confirmButtonText: '放弃改动', cancelButtonText: '继续编辑',
      distinguishCancelAndClose: true, closeOnClickModal: false
    })
    return true
  } catch {
    return false
  }
}
onBeforeRouteLeave(confirmLeave)
onBeforeRouteUpdate(confirmLeave)

function warnBeforeUnload(event: BeforeUnloadEvent) {
  event.preventDefault()
  event.returnValue = ''
}
watch(() => dirty.value || saving.value, (pending) => {
  if (pending) window.addEventListener('beforeunload', warnBeforeUnload)
  else window.removeEventListener('beforeunload', warnBeforeUnload)
})
onBeforeUnmount(() => {
  pageActive = false
  window.removeEventListener('beforeunload', warnBeforeUnload)
})
</script>

<style scoped>
.resume-editor__back { display: inline-block; margin-bottom: 12px; }
.resume-editor h1 { margin: 0; }
.resume-editor__context, .resume-editor__hint { color: var(--color-text-muted); overflow-wrap: anywhere; }
.resume-editor__hint { margin: 0 0 14px; }
.resume-editor__grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 20px; align-items: start; }
.resume-editor__grid--compact { grid-template-columns: minmax(0, 1fr); }
.resume-editor__panel {
  min-width: 0;
  padding: clamp(14px, 3vw, 24px);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.resume-editor__panel label { display: block; margin-bottom: 6px; font-weight: 600; }
.resume-editor__panel input, .resume-editor__panel select, .resume-editor__panel textarea {
  display: block; box-sizing: border-box; width: 100%; min-width: 0; margin: 0 0 16px; padding: 9px 12px;
  border: 1px solid var(--color-border); border-radius: var(--radius-md);
  background: var(--color-surface); color: var(--color-text); font: inherit;
}
.resume-editor__panel textarea { margin-bottom: 0; min-height: 240px; line-height: 1.7; resize: vertical; }
.resume-editor__preview { min-width: 0; }
.resume-editor__preview > h2 { margin: 0 0 10px; font-size: 16px; }
.resume-editor__actions, .resume-editor__switch { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.resume-editor__actions { justify-content: flex-end; margin-top: 16px; }
.resume-editor__switch { margin-bottom: 14px; }
.resume-editor__switch button {
  padding: 8px 16px; border: 1px solid var(--color-border); border-radius: var(--radius-md);
  background: var(--color-surface); color: var(--color-text); cursor: pointer;
}
.resume-editor__switch button[aria-pressed='true'] { color: var(--color-primary); border-color: currentColor; font-weight: 700; }
.resume-editor__error { color: var(--color-danger); }
.resume-editor :is(input, select, textarea, a, button):focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }
</style>
