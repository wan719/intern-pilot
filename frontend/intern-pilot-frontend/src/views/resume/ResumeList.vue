<template>
  <PageContainer title="" width="wide">
    <template #hero>
      <PageHero
        eyebrow="求职旅程 · 第一步"
        title="简历中心"
        description="整理可靠的求职材料，选择默认简历后即可用于 AI 匹配分析。"
      >
        <template #actions>
          <el-button type="primary" :icon="Upload" @click="uploadVisible = true">上传简历</el-button>
        </template>
      </PageHero>
    </template>

    <div v-if="!loadError" class="resume-summary-grid">
      <StatCard label="全部简历" :value="resumeStats.total" :icon="Document" :loading="loading" />
      <StatCard label="默认简历" :value="resumeStats.defaultName" :icon="StarFilled" :loading="loading" />
      <StatCard label="解析成功" :value="resumeStats.success" :icon="CircleCheckFilled" :loading="loading" />
      <StatCard label="待处理/失败" :value="resumeStats.pendingOrFailed" :icon="WarningFilled" :loading="loading" />
    </div>

    <section class="resume-content" :aria-busy="loading">
      <div v-if="loading" class="resume-loading" aria-live="polite" aria-label="正在加载简历列表">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="loadError" class="resume-error" role="alert">
        <div>
          <strong>简历列表暂时无法加载</strong>
          <span>请检查网络连接后重试，已有简历不会受到影响。</span>
        </div>
        <el-button data-resume-retry type="primary" plain @click="loadResumes">重新加载</el-button>
      </div>

      <AppEmpty
        v-else-if="!resumes.length"
        title="还没有上传简历"
        description="上传简历后即可开始 AI 匹配分析"
        hint="支持 PDF / DOCX 文件，上传后可查看解析结果并设置默认简历。"
      >
        <el-button data-resume-empty-action type="primary" :icon="Upload" @click="uploadVisible = true">上传简历</el-button>
      </AppEmpty>

      <template v-else>
        <section class="resume-desktop" aria-label="简历列表">
          <el-table :data="resumes" row-key="resumeId">
            <el-table-column label="简历" min-width="280">
              <template #default="{ row }">
                <div class="resume-name-cell">
                  <div class="resume-file-icon" aria-hidden="true">{{ row.fileType || 'CV' }}</div>
                  <div>
                    <strong class="resume-name" :title="resumeDisplayName(row)">{{ resumeDisplayName(row) }}</strong>
                    <span class="resume-original-name" :title="row.originalFileName || '未记录原文件名'">
                      {{ row.originalFileName || '未记录原文件名' }}
                    </span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="190">
              <template #default="{ row }">
                <div class="resume-statuses">
                  <el-tag v-if="row.isDefault" type="primary" effect="plain">默认简历</el-tag>
                  <el-tag :type="parseStatusType(row.parseStatus)" effect="plain">{{ parseStatusLabel(row.parseStatus) }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="文件" width="150">
              <template #default="{ row }">
                <span>{{ row.fileType || '未知类型' }} · {{ formatFileSize(row.fileSize) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="310" fixed="right">
              <template #default="{ row }">
                <div class="responsive-actions" role="group" :aria-label="`${resumeDisplayName(row)}操作`">
                  <el-button link type="primary" @click="openDetail(row.resumeId)">详情</el-button>
                  <el-button link type="primary" @click="goVersions(row.resumeId)">版本管理</el-button>
                  <el-button v-if="!row.isDefault" link type="primary" @click="setDefault(row.resumeId)">设为默认</el-button>
                  <el-button v-else link disabled>已默认</el-button>
                  <el-button link type="danger" @click="removeResume(row.resumeId)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="resume-mobile" aria-label="移动端简历列表">
          <article
            v-for="item in resumes"
            :key="item.resumeId"
            class="resume-card"
            :class="{ 'resume-card--default': item.isDefault }"
          >
            <header class="resume-card__header">
              <div class="resume-file-icon" aria-hidden="true">{{ item.fileType || 'CV' }}</div>
              <div class="resume-card__title">
                <h2 class="resume-name" :title="resumeDisplayName(item)">{{ resumeDisplayName(item) }}</h2>
                <span class="resume-original-name" :title="item.originalFileName || '未记录原文件名'">
                  {{ item.originalFileName || '未记录原文件名' }}
                </span>
              </div>
            </header>
            <dl class="resume-card__facts">
              <div>
                <dt>默认状态</dt>
                <dd>{{ item.isDefault ? '默认简历' : '普通简历' }}</dd>
              </div>
              <div>
                <dt>解析状态</dt>
                <dd><el-tag :type="parseStatusType(item.parseStatus)" effect="plain">{{ parseStatusLabel(item.parseStatus) }}</el-tag></dd>
              </div>
              <div>
                <dt>文件</dt>
                <dd>{{ item.fileType || '未知类型' }} · {{ formatFileSize(item.fileSize) }}</dd>
              </div>
              <div>
                <dt>上传时间</dt>
                <dd>{{ formatDateTime(item.createdAt) }}</dd>
              </div>
            </dl>
            <div class="responsive-actions" role="group" :aria-label="`${resumeDisplayName(item)}操作`">
              <el-button link type="primary" @click="openDetail(item.resumeId)">详情</el-button>
              <el-button link type="primary" @click="goVersions(item.resumeId)">版本管理</el-button>
              <el-button v-if="!item.isDefault" link type="primary" @click="setDefault(item.resumeId)">设为默认</el-button>
              <el-button v-else link disabled>已默认</el-button>
              <el-button link type="danger" @click="removeResume(item.resumeId)">删除</el-button>
            </div>
          </article>
        </section>
      </template>
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
          <el-input v-model="uploadForm.resumeName" placeholder="例如：Java 后端实习简历.pdf" />
        </el-form-item>
        <el-form-item label="文件">
          <el-upload drag :auto-upload="false" :limit="1" :on-change="onFileChange" :on-remove="onFileRemove">
            <el-icon><UploadFilled /></el-icon>
            <div>拖拽 PDF / DOCX 到这里，或点击选择</div>
            <template #tip>
              <span class="upload-hint">建议上传内容完整、排版清晰的简历，AI 分析效果会更稳定。</span>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="简历详情" :size="detailDrawerSize">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { CircleCheckFilled, Document, StarFilled, Upload, UploadFilled, WarningFilled } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHero from '@/components/common/PageHero.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import router from '@/router'
import { deleteResumeApi, getResumeDetailApi, getResumeListApi, setDefaultResumeApi, uploadResumeApi } from '@/api/resume'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'

const loading = ref(false)
const loadError = ref(false)
const uploading = ref(false)
const uploadVisible = ref(false)
const detailVisible = ref(false)
const { responsiveDialogWidth, responsiveDrawerSize } = useResponsiveSize()
const uploadDialogWidth = responsiveDialogWidth('520px')
const detailDrawerSize = responsiveDrawerSize('48%')
const resumes = ref<any[]>([])
const detail = ref<any>(null)
const selectedFile = ref<File>()
const uploadForm = reactive({ resumeName: '' })

const resumeStats = computed(() => {
  const defaultResume = resumes.value.find((item) => item.isDefault)
  return {
    total: resumes.value.length,
    defaultName: defaultResume?.resumeName || defaultResume?.originalFileName || '-',
    success: resumes.value.filter((item) => item.parseStatus === 'SUCCESS').length,
    pendingOrFailed: resumes.value.filter((item) => item.parseStatus !== 'SUCCESS').length
  }
})

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
  selectedFile.value = file.raw
}

function onFileRemove() {
  selectedFile.value = undefined
}

async function submitUpload() {
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
    loadResumes()
  } finally {
    uploading.value = false
  }
}

async function openDetail(id: number) {
  detail.value = await getResumeDetailApi(id)
  detailVisible.value = true
}

function goVersions(id: number) {
  router.push(`/resumes/${id}/versions`)
}

function resumeDisplayName(item: any) {
  return item?.resumeName || item?.originalFileName || '未命名简历'
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
</script>

<style scoped>
.resume-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.resume-content,
.resume-mobile {
  display: grid;
  gap: var(--space-3);
}

.resume-content {
  min-height: 260px;
}

.resume-loading,
.resume-error,
.resume-desktop,
.resume-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.resume-loading {
  padding: var(--space-5);
}

.resume-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border-color: color-mix(in srgb, var(--color-danger) 28%, var(--color-border));
  background: color-mix(in srgb, var(--color-danger) 6%, var(--color-surface));
}

.resume-error div {
  display: grid;
  gap: var(--space-1);
}

.resume-error strong {
  color: var(--color-text);
}

.resume-error span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.resume-desktop {
  overflow: hidden;
}

.resume-mobile {
  display: none;
}

.resume-name-cell,
.resume-card__header {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: var(--space-3);
}

.resume-name-cell > div:last-child,
.resume-card__title {
  display: grid;
  min-width: 0;
  gap: var(--space-1);
}

.resume-file-icon {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  color: var(--color-primary-hover);
  font-size: 13px;
  font-weight: 800;
}

.resume-name,
.resume-original-name {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.resume-name {
  color: var(--color-text);
}

.resume-original-name {
  color: var(--color-text-muted);
  font-size: 12px;
}

.resume-statuses,
.responsive-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-1);
}

.resume-card {
  padding: var(--space-4);
}

.resume-card--default {
  border-color: var(--color-primary-border);
}

.resume-card__title h2 {
  margin: 0;
  font-size: 16px;
}

.resume-card__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  margin: var(--space-4) 0;
}

.resume-card__facts div {
  min-width: 0;
}

.resume-card__facts dt {
  color: var(--color-text-muted);
  font-size: 13px;
}

.resume-card__facts dd {
  margin: var(--space-1) 0 0;
  color: var(--color-text);
  overflow-wrap: anywhere;
}

.upload-tip {
  margin-bottom: 16px;
}

.upload-hint {
  display: block;
  margin-top: var(--space-2);
  color: var(--color-text-muted);
  font-size: 12px;
}

.resume-advice {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-muted);
  line-height: 1.8;
}

@media (max-width: 900px) {
  .resume-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .resume-summary-grid {
    grid-template-columns: 1fr;
  }

  .resume-desktop {
    display: none;
  }

  .resume-mobile {
    display: grid;
  }

  .resume-error {
    align-items: stretch;
    flex-direction: column;
  }

  .resume-card__facts {
    grid-template-columns: 1fr;
  }
}
</style>
