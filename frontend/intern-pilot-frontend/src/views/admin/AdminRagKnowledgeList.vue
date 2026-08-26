<template>
  <PageContainer width="wide" title="RAG 知识库" description="维护岗位方向知识文档、文本切片和检索测试，用于增强 AI 分析质量。">
    <template #actions>
      <el-button v-if="canManage" type="primary" :icon="Plus" @click="openCreate">新增知识</el-button>
    </template>

    <div class="stat-grid compact-stats">
      <StatCard label="知识文档" :value="documents.length" :icon="Document" :loading="loading" />
      <StatCard label="启用中" :value="enabledCount" :icon="CircleCheck" :loading="loading" />
      <StatCard label="停用" :value="disabledCount" :icon="Warning" :loading="loading" />
      <StatCard label="文本切片" :value="chunkTotal" :icon="Files" :loading="loading" />
    </div>

    <FilterBar @reset="resetQuery">
      <template #filters>
        <el-input v-model="query.direction" aria-label="岗位方向" placeholder="岗位方向，例如 Java 后端" clearable @keyup.enter="loadDocuments" />
        <el-select v-model="query.knowledgeType" aria-label="知识类型" placeholder="知识类型" clearable>
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.enabled" aria-label="知识状态" placeholder="状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" :loading="loading" @click="loadDocuments">筛选</el-button>
        <el-button :icon="Search" @click="openSearch">测试检索</el-button>
      </template>
    </FilterBar>

    <TableShell
      :loading="loading"
      :empty="documents.length === 0"
      empty-title="暂无 RAG 知识文档"
      empty-hint="维护岗位方向知识后，AI 分析可以引用更稳定的上下文。建议先添加技能要求、面试重点、简历优化建议或学习路线。"
    >
      <template #empty-actions>
        <el-button v-if="canManage" type="primary" :icon="Plus" @click="openCreate">新增知识</el-button>
      </template>
      <el-table class="admin-data-table" style="min-width: 1180px" size="small" :data="documents">
        <el-table-column label="知识文档" min-width="270">
          <template #default="{ row }">
            <div class="doc-cell"><strong>{{ row.title }}</strong><span>{{ row.summary || '暂无摘要' }}</span></div>
          </template>
        </el-table-column>
        <el-table-column prop="direction" label="方向" width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="170">
          <template #default="{ row }"><el-tag effect="plain">{{ typeLabel(row.knowledgeType) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :status="row.enabled" :label="row.enabled === 1 ? '启用' : '停用'" size="small" /></template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.documentId)">详情</el-button>
            <el-button v-if="canManage" link type="primary" @click="openEdit(row.documentId)">编辑</el-button>
            <el-button v-if="canManage" link type="warning" :loading="pendingDocumentIds.has(row.documentId)" @click="rebuild(row)">重建</el-button>
            <el-button v-if="canManage" link type="danger" :loading="pendingDocumentIds.has(row.documentId)" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </TableShell>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑知识文档' : '新增知识文档'" :width="formDialogWidth" @closed="formRef?.clearValidate()">
      <el-form ref="formRef" v-loading="editingLoading" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="标题" prop="title" :error="formErrors.title">
          <el-input v-model="form.title" placeholder="例如：Java 后端实习岗位能力模型" />
        </el-form-item>
        <div class="form-grid two">
          <el-form-item label="岗位方向" prop="direction" :error="formErrors.direction">
            <el-input v-model="form.direction" placeholder="Java 后端 / AI 应用 / 前端开发" />
          </el-form-item>
          <el-form-item label="知识类型" prop="knowledgeType" :error="formErrors.knowledgeType">
            <el-select v-model="form.knowledgeType">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" placeholder="不填写时可由内容摘要补充" />
        </el-form-item>
        <el-form-item v-if="editingId" label="启用状态" prop="enabled">
          <el-switch v-model="enabledSwitch" />
        </el-form-item>
        <el-form-item label="知识内容" prop="content" :error="formErrors.content">
          <el-input v-model="form.content" type="textarea" :rows="12" placeholder="填写岗位技能要求、面试重点、简历优化建议、学习路线等内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="editingLoading || !canManage" @click="save">保存并生成切片</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="知识文档详情" :size="detailDrawerSize" @closed="invalidateDetail">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else-if="detail" class="detail-stack">
        <section class="panel flat">
          <div class="panel-header">
            <div><h2>{{ detail.title }}</h2><span>{{ detail.direction }} · {{ typeLabel(detail.knowledgeType) }}</span></div>
            <StatusTag :status="detail.enabled" :label="detail.enabled === 1 ? '启用' : '停用'" />
          </div>
          <p class="content-preview">{{ detail.summary || '暂无摘要' }}</p>
        </section>
        <section class="panel flat"><h2>知识内容</h2><p class="content-preview">{{ detail.content }}</p></section>
        <section class="panel flat">
          <div class="panel-header"><h2>文本切片</h2><span>{{ detail.chunks?.length || 0 }} 个 chunk</span></div>
          <el-timeline v-if="detail.chunks?.length">
            <el-timeline-item v-for="chunk in detail.chunks" :key="chunk.chunkId" :timestamp="`Chunk ${chunk.chunkIndex}`">
              <p class="chunk-content">{{ chunk.content }}</p>
              <el-tag size="small" effect="plain">{{ chunk.embeddingModel || '未生成向量' }}</el-tag>
            </el-timeline-item>
          </el-timeline>
          <AppEmpty v-else title="暂无切片" description="保存或重建知识文档后会生成文本切片" />
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="searchVisible" title="测试 RAG 检索" :width="searchDialogWidth" @closed="searchFormRef?.clearValidate()">
      <el-form ref="searchFormRef" :model="searchForm" :rules="searchRules" label-position="top">
        <div class="form-grid three">
          <el-form-item label="方向" prop="direction"><el-input v-model="searchForm.direction" placeholder="可选" /></el-form-item>
          <el-form-item label="类型" prop="knowledgeType">
            <el-select v-model="searchForm.knowledgeType" clearable>
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="TopK" prop="topK"><el-input-number v-model="searchForm.topK" :min="1" :max="20" /></el-form-item>
        </div>
        <el-form-item label="检索内容" prop="query">
          <el-input v-model="searchForm.query" type="textarea" :rows="4" placeholder="输入简历技能、岗位 JD 或面试准备问题" />
        </el-form-item>
      </el-form>
      <div class="search-results">
        <article v-for="item in searchResults" :key="item.chunkId" class="panel flat">
          <div class="panel-header"><h2>{{ item.title }}</h2><el-tag effect="plain">相似度 {{ item.similarity?.toFixed(4) }}</el-tag></div>
          <p class="content-preview">{{ item.content }}</p>
        </article>
        <AppEmpty v-if="!searching && searchResults.length === 0" title="暂无检索结果" description="输入问题并点击开始检索后，这里会展示命中的知识片段" />
      </div>
      <template #footer>
        <el-button @click="searchVisible = false">取消</el-button>
        <el-button type="primary" :loading="searching" @click="search">开始检索</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { CircleCheck, Document, Files, Plus, Search, Warning } from '@element-plus/icons-vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TableShell from '@/components/common/TableShell.vue'
import {
  createRagKnowledgeApi, deleteRagKnowledgeApi, getRagKnowledgeDetailApi, getRagKnowledgeListApi,
  rebuildRagKnowledgeApi, searchRagKnowledgeApi, updateRagKnowledgeApi
} from '@/api/adminRagKnowledge'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const typeOptions = [
  { label: '岗位方向介绍', value: 'JOB_DIRECTION' }, { label: '技能要求', value: 'SKILL_REQUIREMENT' },
  { label: '面试重点', value: 'INTERVIEW_POINT' }, { label: '简历优化建议', value: 'RESUME_ADVICE' },
  { label: '学习路线', value: 'LEARNING_PATH' }, { label: '项目建议', value: 'PROJECT_SUGGESTION' },
  { label: '其他', value: 'OTHER' }
]

const documents = ref<any[]>([])
const detail = ref<any>(null)
const searchResults = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const searching = ref(false)
const editingLoading = ref(false)
const detailLoading = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const searchVisible = ref(false)
const { responsiveDialogWidth, responsiveDrawerSize } = useResponsiveSize()
const formDialogWidth = responsiveDialogWidth('760px')
const searchDialogWidth = responsiveDialogWidth('760px')
const detailDrawerSize = responsiveDrawerSize('52%')
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const searchFormRef = ref<FormInstance>()
const pendingDocumentIds = ref(new Set<number>())
const query = reactive<any>({ direction: '', knowledgeType: '', enabled: undefined })
const form = reactive<any>({ title: '', direction: '', knowledgeType: 'SKILL_REQUIREMENT', summary: '', content: '', enabled: 1 })
const formErrors = reactive({ title: '', direction: '', knowledgeType: '', content: '' })
const searchForm = reactive<any>({ query: '', direction: '', knowledgeType: '', topK: 5 })
const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  direction: [{ required: true, message: '请输入岗位方向', trigger: 'blur' }],
  knowledgeType: [{ required: true, message: '请选择知识类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入知识内容', trigger: 'blur' }]
}
const searchRules: FormRules = { query: [{ required: true, message: '请输入检索内容', trigger: 'blur' }] }
let active = true
let listRequestId = 0
let editRequestId = 0
let detailRequestId = 0
let searchRequestId = 0
let saveInFlight = false

const canManage = computed(() => authStore.hasPermission('rag:manage'))

watch(canManage, (allowed) => {
  if (!allowed) closeMutationForm()
})
const enabledCount = computed(() => documents.value.filter((item) => item.enabled === 1).length)
const disabledCount = computed(() => documents.value.filter((item) => item.enabled !== 1).length)
const chunkTotal = computed(() => documents.value.reduce((sum, item) => sum + Number(item.chunkCount || 0), 0))
const enabledSwitch = computed({
  get: () => form.enabled === 1,
  set: (value: boolean) => { form.enabled = value ? 1 : 0 }
})

async function loadDocuments() {
  const requestId = ++listRequestId
  loading.value = true
  try {
    const params = { ...query, pageNum: 1, pageSize: 100 }
    const res: any = await getRagKnowledgeListApi(params)
    if (active && requestId === listRequestId) documents.value = res.records || []
  } catch (error: any) {
    if (active && requestId === listRequestId) {
      documents.value = []
      ElMessage.error(error?.message || 'RAG 知识库加载失败，请稍后重试')
    }
  } finally {
    if (active && requestId === listRequestId) loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { direction: '', knowledgeType: '', enabled: undefined })
  return loadDocuments()
}

function resetForm() {
  Object.assign(form, { title: '', direction: '', knowledgeType: 'SKILL_REQUIREMENT', summary: '', content: '', enabled: 1 })
  Object.assign(formErrors, { title: '', direction: '', knowledgeType: '', content: '' })
  editingId.value = undefined
}

function validateFormFields() {
  Object.assign(formErrors, {
    title: form.title?.trim() ? '' : '请输入标题',
    direction: form.direction?.trim() ? '' : '请输入岗位方向',
    knowledgeType: form.knowledgeType ? '' : '请选择知识类型',
    content: form.content?.trim() ? '' : '请输入知识内容'
  })
  return !Object.values(formErrors).some(Boolean)
}

function closeMutationForm() {
  editRequestId += 1
  formVisible.value = false
  editingLoading.value = false
  resetForm()
  void nextTick(() => formRef.value?.clearValidate())
}

function guardManagePermission() {
  if (authStore.hasPermission('rag:manage')) return true
  closeMutationForm()
  return false
}

function openCreate() {
  if (!guardManagePermission()) return
  editRequestId += 1
  resetForm()
  formVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

async function openEdit(documentId: number) {
  if (!guardManagePermission()) return
  const requestId = ++editRequestId
  editingLoading.value = true
  try {
    const res: any = await getRagKnowledgeDetailApi(documentId)
    if (!active || requestId !== editRequestId) return
    if (!guardManagePermission()) return
    Object.assign(form, {
      title: res.title, direction: res.direction, knowledgeType: res.knowledgeType,
      summary: res.summary, content: res.content, enabled: res.enabled
    })
    editingId.value = documentId
    formVisible.value = true
    await nextTick()
    formRef.value?.clearValidate()
  } finally {
    if (active && requestId === editRequestId) editingLoading.value = false
  }
}

async function save() {
  if (!guardManagePermission()) return
  if (saveInFlight) return
  saveInFlight = true
  try {
    await nextTick()
    if (!validateFormFields()) return
    if (!formRef.value || !active) return
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    if (!guardManagePermission()) return
    saving.value = true
    if (editingId.value) await updateRagKnowledgeApi(editingId.value, form)
    else await createRagKnowledgeApi(form)
    if (!active) return
    ElMessage.success('保存成功，切片会用于后续检索')
    formVisible.value = false
    await loadDocuments()
  } finally {
    saveInFlight = false
    if (active) saving.value = false
  }
}

async function openDetail(documentId: number) {
  const requestId = ++detailRequestId
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const result = await getRagKnowledgeDetailApi(documentId)
    if (active && requestId === detailRequestId && detailVisible.value) detail.value = result
  } finally {
    if (active && requestId === detailRequestId) detailLoading.value = false
  }
}

function invalidateDetail() {
  detailRequestId += 1
  detailLoading.value = false
  detail.value = null
}

function setDocumentPending(id: number, pending: boolean) {
  const next = new Set(pendingDocumentIds.value)
  if (pending) next.add(id)
  else next.delete(id)
  pendingDocumentIds.value = next
}

function isConfirmationDismissed(reason: unknown) {
  return reason === 'cancel' || reason === 'close'
}

async function rebuild(row: any) {
  if (!guardManagePermission()) return
  if (pendingDocumentIds.value.has(row.documentId)) return
  setDocumentPending(row.documentId, true)
  try {
    try {
      await ElMessageBox.confirm(`确认重建「${row.title}」的文本切片和向量？`, '重建确认', { type: 'warning' })
    } catch (reason) {
      if (isConfirmationDismissed(reason)) return
      throw reason
    }
    if (!guardManagePermission()) return
    await rebuildRagKnowledgeApi(row.documentId)
    if (!active) return
    ElMessage.success('切片和向量已重建')
    await loadDocuments()
  } finally {
    if (active) setDocumentPending(row.documentId, false)
  }
}

async function remove(row: any) {
  if (!guardManagePermission()) return
  if (pendingDocumentIds.value.has(row.documentId)) return
  setDocumentPending(row.documentId, true)
  try {
    try {
      await ElMessageBox.confirm(`确认删除知识文档「${row.title}」？删除后无法恢复。`, '删除确认', { type: 'warning' })
    } catch (reason) {
      if (isConfirmationDismissed(reason)) return
      throw reason
    }
    if (!guardManagePermission()) return
    await deleteRagKnowledgeApi(row.documentId)
    if (!active) return
    ElMessage.success('删除成功')
    await loadDocuments()
  } finally {
    if (active) setDocumentPending(row.documentId, false)
  }
}

function openSearch() {
  searchRequestId += 1
  Object.assign(searchForm, { query: '', direction: query.direction || '', knowledgeType: query.knowledgeType || '', topK: 5 })
  searchResults.value = []
  searchVisible.value = true
  void nextTick(() => searchFormRef.value?.clearValidate())
}

async function search() {
  await nextTick()
  if (!searchFormRef.value) return
  const valid = await searchFormRef.value.validate().catch(() => false)
  if (!valid) return
  const requestId = ++searchRequestId
  searching.value = true
  try {
    const res: any = await searchRagKnowledgeApi(searchForm)
    if (active && requestId === searchRequestId && searchVisible.value) searchResults.value = res || []
  } finally {
    if (active && requestId === searchRequestId) searching.value = false
  }
}

function typeLabel(value: string) { return typeOptions.find((item) => item.value === value)?.label || value }

onMounted(loadDocuments)
onBeforeUnmount(() => {
  active = false
  listRequestId += 1
  editRequestId += 1
  detailRequestId += 1
  searchRequestId += 1
})
</script>

<style scoped>
.compact-stats { grid-template-columns: repeat(4, minmax(0, 1fr)); }
:deep(.filter-bar__filters .el-input), :deep(.filter-bar__filters .el-select) { width: min(220px, 100%); }
.doc-cell { display: grid; gap: 5px; }
.doc-cell span { display: -webkit-box; overflow: hidden; color: var(--color-text-muted); font-size: 13px; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.panel h2 { margin: 0; font-size: 16px; }
.content-preview, .chunk-content { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.7; }
.chunk-content { margin-bottom: var(--space-2); }
.search-results { display: grid; gap: var(--space-3); margin-top: var(--space-3); }

@media (max-width: 900px) {
  .compact-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 520px) {
  .compact-stats { grid-template-columns: 1fr; }
}
</style>
