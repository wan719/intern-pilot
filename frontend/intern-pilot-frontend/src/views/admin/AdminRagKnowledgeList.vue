<template>
  <PageContainer title="RAG 知识库" description="维护岗位方向知识文档、文本切片和检索测试，用于增强 AI 分析质量。">
    <template #actions>
      <el-button v-if="canManage" type="primary" :icon="Plus" @click="openCreate">新增知识</el-button>
    </template>

    <div class="stat-grid compact-stats">
      <StatCard label="知识文档" :value="documents.length" :icon="Document" />
      <StatCard label="启用中" :value="enabledCount" :icon="CircleCheck" />
      <StatCard label="停用" :value="disabledCount" :icon="Warning" />
      <StatCard label="文本切片" :value="chunkTotal" :icon="Files" />
    </div>

    <section class="panel toolbar">
      <el-input v-model="query.direction" placeholder="岗位方向，例如 Java 后端" clearable />
      <el-select v-model="query.knowledgeType" placeholder="知识类型" clearable>
        <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.enabled" placeholder="状态" clearable>
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="loadDocuments">筛选</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <el-button :icon="Search" @click="openSearch">测试检索</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="documents">
        <el-table-column label="知识文档" min-width="250">
          <template #default="{ row }">
            <div class="doc-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.summary || '暂无摘要' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="direction" label="方向" width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="160">
          <template #default="{ row }">
            <el-tag effect="plain">{{ typeLabel(row.knowledgeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" effect="plain">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.documentId)">详情</el-button>
            <el-button v-if="canManage" link type="primary" @click="openEdit(row.documentId)">编辑</el-button>
            <el-button v-if="canManage" link type="warning" @click="rebuild(row)">重建</el-button>
            <el-button v-if="canManage" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AppEmpty
            title="暂无 RAG 知识文档"
            description="维护岗位方向知识后，AI 分析可以引用更稳定的上下文"
            hint="建议先添加技能要求、面试重点、简历优化建议或学习路线。"
          >
            <el-button v-if="canManage" type="primary" :icon="Plus" @click="openCreate">新增知识</el-button>
          </AppEmpty>
        </template>
      </el-table>
    </section>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑知识文档' : '新增知识文档'" width="760px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="例如：Java 后端实习岗位能力模型" />
        </el-form-item>
        <div class="form-grid two">
          <el-form-item label="岗位方向">
            <el-input v-model="form.direction" placeholder="Java 后端 / AI 应用 / 前端开发" />
          </el-form-item>
          <el-form-item label="知识类型">
            <el-select v-model="form.knowledgeType">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" placeholder="不填写时可由内容摘要补充" />
        </el-form-item>
        <el-form-item v-if="editingId" label="启用状态">
          <el-switch v-model="enabledSwitch" />
        </el-form-item>
        <el-form-item label="知识内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="填写岗位技能要求、面试重点、简历优化建议、学习路线等内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存并生成切片</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="知识文档详情" size="52%">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else-if="detail" class="detail-stack">
        <section class="panel flat">
          <div class="panel-header">
            <div>
              <h3>{{ detail.title }}</h3>
              <span>{{ detail.direction }} · {{ typeLabel(detail.knowledgeType) }}</span>
            </div>
            <el-tag :type="detail.enabled === 1 ? 'success' : 'info'" effect="plain">
              {{ detail.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </div>
          <p class="content-preview">{{ detail.summary || '暂无摘要' }}</p>
        </section>

        <section class="panel flat">
          <h3>知识内容</h3>
          <p class="content-preview">{{ detail.content }}</p>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h3>文本切片</h3>
            <span>{{ detail.chunks?.length || 0 }} 个 chunk</span>
          </div>
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

    <el-dialog v-model="searchVisible" title="测试 RAG 检索" width="760px">
      <el-form :model="searchForm" label-position="top">
        <div class="form-grid three">
          <el-form-item label="方向">
            <el-input v-model="searchForm.direction" placeholder="可选" />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="searchForm.knowledgeType" clearable>
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="TopK">
            <el-input-number v-model="searchForm.topK" :min="1" :max="20" />
          </el-form-item>
        </div>
        <el-form-item label="检索内容">
          <el-input v-model="searchForm.query" type="textarea" :rows="4" placeholder="输入简历技能、岗位 JD 或面试准备问题" />
        </el-form-item>
      </el-form>
      <el-button type="primary" :loading="searching" @click="search">开始检索</el-button>
      <div class="search-results">
        <article v-for="item in searchResults" :key="item.chunkId" class="panel flat">
          <div class="panel-header">
            <h3>{{ item.title }}</h3>
            <el-tag effect="plain">相似度 {{ item.similarity?.toFixed(4) }}</el-tag>
          </div>
          <p class="content-preview">{{ item.content }}</p>
        </article>
        <AppEmpty v-if="!searching && searchResults.length === 0" title="暂无检索结果" description="输入问题并点击开始检索后，这里会展示命中的知识片段" />
      </div>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Document, Files, Plus, Search, Warning } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import {
  createRagKnowledgeApi,
  deleteRagKnowledgeApi,
  getRagKnowledgeDetailApi,
  getRagKnowledgeListApi,
  rebuildRagKnowledgeApi,
  searchRagKnowledgeApi,
  updateRagKnowledgeApi
} from '@/api/adminRagKnowledge'
import { formatDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const typeOptions = [
  { label: '岗位方向介绍', value: 'JOB_DIRECTION' },
  { label: '技能要求', value: 'SKILL_REQUIREMENT' },
  { label: '面试重点', value: 'INTERVIEW_POINT' },
  { label: '简历优化建议', value: 'RESUME_ADVICE' },
  { label: '学习路线', value: 'LEARNING_PATH' },
  { label: '项目建议', value: 'PROJECT_SUGGESTION' },
  { label: '其他', value: 'OTHER' }
]

const documents = ref<any[]>([])
const detail = ref<any>(null)
const searchResults = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const searching = ref(false)
const detailLoading = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const searchVisible = ref(false)
const editingId = ref<number>()

const query = reactive<any>({ direction: '', knowledgeType: '', enabled: undefined })
const form = reactive<any>({ title: '', direction: '', knowledgeType: 'SKILL_REQUIREMENT', summary: '', content: '', enabled: 1 })
const searchForm = reactive<any>({ query: '', direction: '', knowledgeType: '', topK: 5 })
const canManage = computed(() => authStore.hasPermission('rag:manage'))
const enabledCount = computed(() => documents.value.filter((item) => item.enabled === 1).length)
const disabledCount = computed(() => documents.value.filter((item) => item.enabled !== 1).length)
const chunkTotal = computed(() => documents.value.reduce((sum, item) => sum + Number(item.chunkCount || 0), 0))
const enabledSwitch = computed({
  get: () => form.enabled === 1,
  set: (value: boolean) => {
    form.enabled = value ? 1 : 0
  }
})

async function loadDocuments() {
  loading.value = true
  try {
    const params = { ...query, pageNum: 1, pageSize: 100 }
    const res: any = await getRagKnowledgeListApi(params)
    documents.value = res.records || []
  } catch (error: any) {
    documents.value = []
    ElMessage.error(error?.message || 'RAG 知识库加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.direction = ''
  query.knowledgeType = ''
  query.enabled = undefined
  loadDocuments()
}

function resetForm() {
  Object.assign(form, { title: '', direction: '', knowledgeType: 'SKILL_REQUIREMENT', summary: '', content: '', enabled: 1 })
  editingId.value = undefined
}

function openCreate() {
  resetForm()
  formVisible.value = true
}

async function openEdit(documentId: number) {
  const res: any = await getRagKnowledgeDetailApi(documentId)
  Object.assign(form, {
    title: res.title,
    direction: res.direction,
    knowledgeType: res.knowledgeType,
    summary: res.summary,
    content: res.content,
    enabled: res.enabled
  })
  editingId.value = documentId
  formVisible.value = true
}

async function save() {
  if (!form.title || !form.direction || !form.content) {
    ElMessage.warning('请填写标题、岗位方向和知识内容')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateRagKnowledgeApi(editingId.value, form)
    } else {
      await createRagKnowledgeApi(form)
    }
    ElMessage.success('保存成功，切片会用于后续检索')
    formVisible.value = false
    loadDocuments()
  } finally {
    saving.value = false
  }
}

async function openDetail(documentId: number) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getRagKnowledgeDetailApi(documentId)
  } finally {
    detailLoading.value = false
  }
}

async function rebuild(row: any) {
  await ElMessageBox.confirm(`确认重建「${row.title}」的文本切片和向量？`, '重建确认', { type: 'warning' })
  await rebuildRagKnowledgeApi(row.documentId)
  ElMessage.success('切片和向量已重建')
  loadDocuments()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除知识文档「${row.title}」？删除后无法恢复。`, '删除确认', { type: 'warning' })
  await deleteRagKnowledgeApi(row.documentId)
  ElMessage.success('删除成功')
  await loadDocuments()
}

function openSearch() {
  Object.assign(searchForm, { query: '', direction: query.direction || '', knowledgeType: query.knowledgeType || '', topK: 5 })
  searchResults.value = []
  searchVisible.value = true
}

async function search() {
  if (!searchForm.query) {
    ElMessage.warning('请输入检索内容')
    return
  }
  searching.value = true
  try {
    const res: any = await searchRagKnowledgeApi(searchForm)
    searchResults.value = res || []
  } finally {
    searching.value = false
  }
}

function typeLabel(value: string) {
  return typeOptions.find((item) => item.value === value)?.label || value
}

onMounted(loadDocuments)
</script>

<style scoped>
.compact-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.doc-cell {
  display: grid;
  gap: 5px;
}

.doc-cell strong {
  color: var(--color-text);
}

.doc-cell span {
  display: -webkit-box;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.content-preview,
.chunk-content {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
}

.chunk-content {
  margin-bottom: 8px;
}

.search-results {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

@media (max-width: 900px) {
  .compact-stats {
    grid-template-columns: 1fr;
  }
}
</style>
