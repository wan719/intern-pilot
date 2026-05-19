<template>
  <PageContainer title="" description="上传和管理你的简历，选择默认简历用于 AI 匹配分析。">
    <template #actions>
      <el-button type="primary" :icon="Upload" @click="uploadVisible = true">上传简历</el-button>
    </template>

    <div v-loading="loading" class="resume-summary-grid">
      <StatCard label="全部简历" :value="resumeStats.total" :icon="Document" />
      <StatCard label="默认简历" :value="resumeStats.defaultName" :icon="StarFilled" />
      <StatCard label="解析成功" :value="resumeStats.success" :icon="CircleCheckFilled" />
      <StatCard label="待处理/失败" :value="resumeStats.pendingOrFailed" :icon="WarningFilled" />
    </div>

    <section v-loading="loading" class="resume-assets">
      <AppEmpty
        v-if="!resumes.length && !loading"
        title="还没有上传简历"
        description="上传简历后即可开始 AI 匹配分析"
        hint="支持 PDF / DOCX 文件，上传后可查看解析结果并设置默认简历。"
      >
        <el-button type="primary" :icon="Upload" @click="uploadVisible = true">上传简历</el-button>
      </AppEmpty>

      <article
        v-for="item in resumes"
        v-else
        :key="item.resumeId"
        class="resume-card"
        :class="{ default: item.isDefault }"
      >
        <div class="resume-card-main">
          <div class="resume-file-icon">{{ item.fileType || 'CV' }}</div>
          <div class="resume-card-copy">
            <div class="resume-title-row">
              <h3>{{ item.resumeName || item.originalFileName || '未命名简历' }}</h3>
              <el-tag v-if="item.isDefault" type="primary" effect="plain">默认简历</el-tag>
              <el-tag :type="parseStatusType(item.parseStatus)" effect="plain">
                {{ parseStatusLabel(item.parseStatus) }}
              </el-tag>
            </div>
            <p>{{ item.originalFileName || '未记录原文件名' }}</p>
            <div class="resume-meta">
              <span>{{ item.fileType || '未知类型' }}</span>
              <span>{{ formatFileSize(item.fileSize) }}</span>
              <span>上传时间：{{ formatDateTime(item.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div class="resume-actions">
          <el-button link type="primary" @click="openDetail(item.resumeId)">详情</el-button>
          <el-button link type="primary" @click="goVersions(item.resumeId)">版本管理</el-button>
          <el-button v-if="!item.isDefault" link type="primary" @click="setDefault(item.resumeId)">设为默认</el-button>
          <el-button v-else link disabled>已默认</el-button>
          <el-button link type="danger" @click="removeResume(item.resumeId)">删除</el-button>
        </div>
      </article>
    </section>

    <el-dialog v-model="uploadVisible" title="上传简历" width="520px">
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

    <el-drawer v-model="detailVisible" title="简历详情" size="48%">
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
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import router from '@/router'
import { deleteResumeApi, getResumeDetailApi, getResumeListApi, setDefaultResumeApi, uploadResumeApi } from '@/api/resume'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const uploading = ref(false)
const uploadVisible = ref(false)
const detailVisible = ref(false)
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
  try {
    const res: any = await getResumeListApi({ pageNum: 1, pageSize: 100 })
    resumes.value = res.records || []
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

async function setDefault(id: number) {
  await setDefaultResumeApi(id)
  ElMessage.success('已设置默认简历')
  loadResumes()
}

async function removeResume(id: number) {
  await ElMessageBox.confirm('确认删除这份简历吗？', '删除简历', { type: 'warning' })
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
  gap: 18px;
  margin-bottom: 20px;
}

.resume-assets {
  display: grid;
  gap: 14px;
}

.resume-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.resume-card.default {
  border-color: var(--color-primary-border);
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.1);
}

.resume-card-main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 14px;
}

.resume-file-icon {
  display: grid;
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 800;
}

.resume-card-copy {
  min-width: 0;
}

.resume-title-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.resume-title-row h3 {
  margin: 0;
  font-size: 16px;
}

.resume-card-copy p {
  margin: 8px 0;
  color: var(--color-text-muted);
}

.resume-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--color-text-soft);
  font-size: 13px;
}

.resume-actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.upload-tip {
  margin-bottom: 16px;
}

.upload-hint {
  display: block;
  margin-top: 8px;
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
    grid-template-columns: 1fr;
  }

  .resume-card {
    align-items: stretch;
    flex-direction: column;
  }

  .resume-actions {
    justify-content: flex-start;
  }
}
</style>
