<template>
  <PageContainer class="resume-center" title="简历中心" width="wide">
    <template #hero>
      <header class="resume-heading">
        <div>
          <h1>简历中心</h1>
          <p>管理求职简历、查看解析结果和版本。</p>
        </div>
        <Button class="resume-upload-action ip:min-h-[44px] ip:border-0" type="button" @click="uploadVisible = true">
          <el-icon aria-hidden="true"><Upload /></el-icon>
          上传简历
        </Button>
      </header>
    </template>

    <div v-if="!loading && !loadError" class="resume-default ip:flex ip:flex-wrap ip:items-baseline ip:gap-x-3" data-resume-default>
      <span>默认简历</span>
      <strong>{{ defaultResume ? resumeDisplayName(defaultResume) : '尚未设置' }}</strong>
    </div>

    <section class="resume-content" aria-labelledby="resume-library-heading" :aria-busy="loading">
      <div class="resume-library-heading">
        <h2 id="resume-library-heading">我的简历</h2>
        <span v-if="!loading && !loadError">{{ resumes.length }} 份</span>
      </div>

      <div v-if="loading" class="resume-loading" aria-live="polite" aria-label="正在加载简历列表">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="loadError" class="resume-error" role="alert">
        <div>
          <strong>简历列表暂时无法加载</strong>
          <p>请稍后重试。</p>
        </div>
        <el-button data-resume-retry @click="loadResumes">重新加载</el-button>
      </div>

      <AppEmpty
        v-else-if="!resumes.length"
        title="还没有上传简历"
        description="上传简历后即可开始 AI 匹配分析"
      >
        <el-button data-resume-empty-action @click="uploadVisible = true">上传简历</el-button>
      </AppEmpty>

      <div v-else class="resume-list-shell">
        <div class="resume-list-columns" aria-hidden="true">
          <span>简历名称</span>
          <span>文件</span>
          <span>解析状态</span>
          <span>默认标记</span>
          <span>上传时间</span>
          <span>操作</span>
        </div>
        <ul class="resume-list" role="list" aria-labelledby="resume-library-heading">
          <li v-for="item in resumes" :key="item.resumeId" class="resume-row">
            <div class="resume-row__name">
              <span class="resume-file-icon" aria-hidden="true"><el-icon><Document /></el-icon></span>
              <div>
                <h3 class="resume-name">{{ resumeDisplayName(item) }}</h3>
                <span class="resume-original-name">{{ item.originalFileName || '未记录原文件名' }}</span>
              </div>
            </div>
            <span class="resume-row__metadata">{{ item.fileType || '—' }} · {{ formatFileSize(item.fileSize) }}</span>
            <div><el-tag :type="parseStatusType(item.parseStatus)" effect="plain">{{ parseStatusLabel(item.parseStatus) }}</el-tag></div>
            <div><el-tag v-if="item.isDefault" type="primary" effect="plain">默认简历</el-tag></div>
            <div class="resume-row__metadata">
              <span class="sr-only">上传时间</span>
              <time v-if="item.createdAt" :datetime="item.createdAt">{{ formatDateTime(item.createdAt) }}</time>
              <span v-else>—</span>
            </div>
            <div class="resume-row__actions" role="group" :aria-label="resumeDisplayName(item) + '操作'">
              <el-button link type="primary" @click="openDetail(item.resumeId)">详情</el-button>
              <el-button link type="primary" @click="goVersions(item.resumeId)">版本管理</el-button>
              <el-dropdown trigger="click" @command="handleResumeCommand($event, item.resumeId)">
                <el-button link type="primary">
                  更多 <el-icon aria-hidden="true"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="!item.isDefault" command="default">设为默认</el-dropdown-item>
                    <el-dropdown-item command="delete">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </li>
        </ul>
      </div>
    </section>

    <el-dialog v-model="uploadVisible" title="上传简历" :width="uploadDialogWidth">
      <el-alert
        class="upload-tip"
        type="info"
        :closable="false"
        title="支持 PDF / DOCX 文件。简历名称可不填，系统会默认使用文件名。"
      />
      <el-form label-position="top">
        <el-form-item label="简历名称">
          <el-input v-model="uploadForm.resumeName" :disabled="uploading" placeholder="例如：Java 后端实习简历.pdf" />
        </el-form-item>
        <el-form-item label="文件">
          <el-upload ref="uploadControl" drag :disabled="uploading" :auto-upload="false" :limit="1" :on-change="onFileChange" :on-remove="onFileRemove">
            <el-icon><UploadFilled /></el-icon>
            <div>拖拽 PDF / DOCX 到这里，或点击选择</div>
            <template #tip>
              <span class="upload-hint">建议上传内容完整、排版清晰的简历，AI 分析效果会更稳定。</span>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <p v-if="uploadFailed" class="resume-upload-error" data-resume-upload-error role="alert">
        上传失败，已保留所选文件，请重试。
      </p>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="简历详情" :size="detailDrawerSize" @close="closeDetail">
      <div v-if="detail" class="detail-stack">
        <section class="panel flat">
          <div class="panel-header">
            <h3>基础信息</h3>
            <el-tag :type="parseStatusType(detail.parseStatus)" effect="plain">
              {{ parseStatusLabel(detail.parseStatus) }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="名称">{{ detail.resumeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="文件">{{ detail.originalFileName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ detail.fileType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="大小">{{ formatFileSize(detail.fileSize) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h3>AI 使用建议</h3>
            <span>默认简历会优先用于匹配分析</span>
          </div>
          <ul class="resume-advice">
            <li>确保项目经历、技能关键词和实习经历尽量完整。</li>
            <li>如解析文本明显缺失，建议调整文件格式后重新上传。</li>
            <li>针对不同岗位可使用版本管理维护定向简历。</li>
          </ul>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h3>解析文本</h3>
            <span>用于 AI 匹配分析的主要文本输入</span>
          </div>
          <div class="text-preview">{{ detail.parsedText || '暂无解析文本' }}</div>
        </section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadInstance } from 'element-plus'
import { ArrowDown, Document, Upload, UploadFilled } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import { Button } from '@/components/ui/button'
import '@/styles/resume-utilities.css'
import router from '@/router'
import { deleteResumeApi, getResumeDetailApi, getResumeListApi, setDefaultResumeApi, uploadResumeApi } from '@/api/resume'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'

const loading = ref(false)
const loadError = ref(false)
const uploading = ref(false)
const uploadFailed = ref(false)
const uploadControl = ref<UploadInstance>()
const uploadVisible = ref(false)
const detailVisible = ref(false)
const { responsiveDialogWidth, responsiveDrawerSize } = useResponsiveSize()
const uploadDialogWidth = responsiveDialogWidth('520px')
const detailDrawerSize = responsiveDrawerSize('48%')
const resumes = ref<any[]>([])
const detail = ref<any>(null)
const selectedFile = ref<File>()
const uploadForm = reactive({ resumeName: '' })
let detailRequestId = 0
let detailPageActive = true

const defaultResume = computed(() => resumes.value.find((item) => item.isDefault))

async function loadResumes() {
  loading.value = true
  loadError.value = false
  try {
    const res: any = await getResumeListApi({ pageNum: 1, pageSize: 100 })
    resumes.value = res.records || []
  } catch {
    resumes.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function onFileChange(file: UploadFile) {
  uploadFailed.value = false
  selectedFile.value = file.raw
}

function onFileRemove() {
  selectedFile.value = undefined
}

async function submitUpload() {
  if (uploading.value) return
  uploadFailed.value = false
  if (!selectedFile.value) {
    ElMessage.warning('请选择简历文件')
    return
  }
  const data = new FormData()
  data.append('file', selectedFile.value)
  data.append('resumeName', uploadForm.resumeName || selectedFile.value.name)
  uploading.value = true
  try {
    await uploadResumeApi(data)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    selectedFile.value = undefined
    uploadForm.resumeName = ''
    uploadControl.value?.clearFiles()
    loadResumes()
  } catch {
    uploadFailed.value = true
  } finally {
    uploading.value = false
  }
}

async function openDetail(id: number) {
  const requestId = ++detailRequestId
  const result = await getResumeDetailApi(id)
  if (!detailPageActive || requestId !== detailRequestId) return
  detail.value = result
  detailVisible.value = true
}

function closeDetail() {
  detailRequestId += 1
  detailVisible.value = false
  detail.value = null
}

function goVersions(id: number) {
  router.push(`/resumes/${id}/versions`)
}

function resumeDisplayName(item: any) {
  return item?.resumeName || item?.originalFileName || '未命名简历'
}

function handleResumeCommand(command: unknown, id: number) {
  if (command === 'default') return setDefault(id)
  if (command === 'delete') return removeResume(id)
}

async function setDefault(id: number) {
  await setDefaultResumeApi(id)
  ElMessage.success('已设置默认简历')
  loadResumes()
}

async function removeResume(id: number) {
  try {
    await ElMessageBox.confirm('确认删除这份简历吗？', '删除简历', { type: 'warning' })
  } catch {
    return
  }
  await deleteResumeApi(id)
  ElMessage.success('已删除')
  loadResumes()
}

function parseStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    SUCCESS: '已解析',
    PROCESSING: '解析中',
    PENDING: '待解析',
    FAILED: '解析失败'
  }
  return labels[String(status || '').toUpperCase()] || status || '待解析'
}

function parseStatusType(status?: string) {
  const value = String(status || '').toUpperCase()
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED') return 'danger'
  if (value === 'PROCESSING') return 'primary'
  return 'info'
}

function formatFileSize(value?: number | string) {
  const size = Number(value)
  if (!Number.isFinite(size) || size <= 0) {
    return '-'
  }
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

onMounted(loadResumes)
onBeforeUnmount(() => {
  detailPageActive = false
  detailRequestId += 1
})
</script>

<style scoped>
.resume-center {
  --resume-workspace-bg: #f8f7f3;
  max-width: 1184px;
  margin-inline: auto;
  background: var(--resume-workspace-bg);
  color: var(--color-text);
}

.resume-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.resume-heading h1 {
  margin: 0;
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.2;
}

.resume-heading p {
  margin: var(--space-2) 0 0;
  color: var(--color-text-muted);
}

.resume-upload-action {
  flex: none;
  cursor: pointer;
}

.resume-upload-action:focus-visible {
  outline: 2px solid var(--color-primary-hover);
  outline-offset: 3px;
}

.resume-default {
  min-width: 0;
  margin-bottom: var(--space-5);
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-lg);
  padding: var(--space-3) var(--space-4);
  background: color-mix(in srgb, var(--color-primary-soft) 40%, var(--color-surface));
}

.resume-default span,
.resume-library-heading span,
.resume-row__metadata {
  color: var(--color-text-muted);
  font-size: 13px;
}

.resume-default strong,
.resume-name {
  overflow-wrap: anywhere;
}

.resume-library-heading {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.resume-library-heading h2,
.resume-name {
  margin: 0;
}

.resume-library-heading h2 {
  font-size: 20px;
}

.resume-loading,
.resume-error,
.resume-list-shell {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.resume-loading,
.resume-error {
  padding: var(--space-4);
}

.resume-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  border-color: color-mix(in srgb, var(--color-danger) 28%, var(--color-border));
  background: color-mix(in srgb, var(--color-danger) 6%, var(--color-surface));
}

.resume-error p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
}

.resume-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.resume-list-columns,
.resume-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px 80px 74px 132px 186px;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
}

.resume-list-columns {
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 12px;
}

.resume-row {
  border-top: 1px solid var(--color-border);
}

.resume-row__name,
.resume-row__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.resume-row__name > div {
  min-width: 0;
}

.resume-file-icon {
  display: grid;
  width: 34px;
  height: 38px;
  flex: none;
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  color: var(--color-primary-hover);
}

.resume-name {
  font-size: 15px;
}

.resume-original-name {
  display: block;
  margin-top: 2px;
  color: var(--color-text-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.resume-row__actions {
  flex-wrap: wrap;
}

.resume-row__actions :deep(.el-button) {
  min-height: 40px;
  margin-left: 0;
  padding-inline: 4px;
}

.resume-row__actions :deep(.el-button:focus-visible) {
  outline: 2px solid var(--color-primary-hover);
  outline-offset: 2px;
}

.upload-tip {
  margin-bottom: var(--space-4);
}

.upload-hint {
  display: block;
  margin-top: var(--space-2);
  color: var(--color-text-muted);
  font-size: 12px;
}

.resume-upload-error {
  color: var(--color-danger);
}

.resume-advice {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-muted);
  line-height: 1.8;
}

@media (max-width: 940px) {
  .resume-list-shell {
    border: 0;
    background: transparent;
  }

  .resume-list-columns {
    display: none;
  }

  .resume-list {
    display: grid;
    gap: var(--space-3);
  }

  .resume-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-3);
    background: var(--color-surface);
  }

  .resume-row__name,
  .resume-row__actions {
    grid-column: 1 / -1;
  }

  .resume-row__actions {
    border-top: 1px solid var(--color-border);
    padding-top: var(--space-1);
  }
}

@media (max-width: 560px) {
  .resume-heading,
  .resume-error {
    align-items: stretch;
    flex-direction: column;
  }

  .resume-row__actions :deep(.el-button) {
    min-height: 44px;
  }
}
</style>
