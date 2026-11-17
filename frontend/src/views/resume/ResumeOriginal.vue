<template>
  <section class="panel flat resume-original" aria-label="上传原件">
    <div class="panel-header">
      <div>
        <h2>上传原件</h2>
        <p v-if="resume.fileType === 'DOCX'">下载后用文档应用查看原始排版。</p>
        <p v-else-if="!canPreviewPdf">下载后用 PDF 查看器查看原始排版。</p>
      </div>
      <Button type="button" class="ip:min-h-11" :disabled="originalLoading" @click="downloadOriginal">下载原件</Button>
    </div>
    <p v-if="originalLoading" role="status">正在读取原件…</p>
    <div v-else-if="originalError" class="resume-original-error">
      <p role="alert">{{ originalError }}</p>
      <el-button size="small" @click="retryOriginal">重试</el-button>
    </div>
    <iframe v-else-if="canPreviewPdf && originalUrl" :src="originalUrl"
      class="resume-pdf-preview" title="PDF 简历原件" />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { getResumeFileApi } from '@/api/resume'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import type { ResumeDetails } from './resumePages'

const props = defineProps<{ resume: ResumeDetails }>()
const { isMobile } = useResponsiveSize()
const originalUrl = ref('')
const originalLoading = ref(false)
const originalError = ref('')
let originalRequest: AbortController | undefined
const canPreviewPdf = computed(() => props.resume.fileType === 'PDF' && !isMobile.value && navigator.pdfViewerEnabled === true)

function releaseOriginal() {
  originalRequest?.abort()
  originalRequest = undefined
  if (originalUrl.value) URL.revokeObjectURL(originalUrl.value)
  originalUrl.value = ''
  originalError.value = ''
  originalLoading.value = false
}

async function loadOriginal() {
  if (originalRequest) return
  if (originalUrl.value) return originalUrl.value
  const { resumeId, fileType } = props.resume
  const controller = new AbortController()
  originalRequest = controller
  originalLoading.value = true
  originalError.value = ''
  try {
    const file = await getResumeFileApi(resumeId, controller.signal)
    if (originalRequest !== controller) return
    const expectedType = fileType === 'PDF' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    if (!file.size || file.type !== expectedType) throw new Error('Invalid original file response')
    originalUrl.value = URL.createObjectURL(file)
    return originalUrl.value
  } catch (error) {
    if (originalRequest !== controller) return
    const status = (error as { response?: { status?: number } })?.response?.status
    originalError.value = status === 404 ? '原件不存在或已不可用。'
      : status === 403 ? '当前账号没有读取原件的权限。'
        : '暂时无法读取原件，请重试。'
  } finally {
    if (originalRequest === controller) {
      originalRequest = undefined
      originalLoading.value = false
    }
  }
}

async function downloadOriginal() {
  const current = props.resume
  if (!current) return
  const url = await loadOriginal()
  if (!url || originalUrl.value !== url) return
  const link = document.createElement('a')
  link.href = url
  const name = String(current.originalFileName || '').split(/[\\/]/).pop()?.replace(/[\x00-\x1f\x7f]/g, '').trim()
  link.download = name || (current.fileType === 'PDF' ? '简历.pdf' : '简历.docx')
  document.body.append(link)
  link.click()
  link.remove()
}

function retryOriginal() {
  return canPreviewPdf.value ? loadOriginal() : downloadOriginal()
}

watch(canPreviewPdf, (preview) => {
  if (preview && !originalError.value) void loadOriginal()
}, { immediate: true })

onBeforeUnmount(releaseOriginal)
</script>

<style scoped>
.resume-original .panel-header { display: flex; gap: var(--space-3); align-items: flex-start; flex-wrap: wrap; }
.resume-original h2 { margin: 0; font-size: 18px; }
.resume-original .panel-header p { margin: var(--space-1) 0 0; color: var(--color-text-muted); }
.resume-original-error { display: flex; align-items: baseline; flex-wrap: wrap; gap: var(--space-2); }
.resume-pdf-preview {
  display: block; width: 100%; height: clamp(420px, 68vh, 1000px);
  border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface);
}
</style>
