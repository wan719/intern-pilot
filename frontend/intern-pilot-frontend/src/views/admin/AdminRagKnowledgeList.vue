<template>
  <PageContainer title="RAG 知识库" description="维护岗位方向知识文档，生成文本切片和 Embedding，用于增强 AI 分析。">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增知识</el-button>
    </template>

    <section class="panel toolbar">
      <el-input v-model="query.direction" placeholder="岗位方向，如 Java后端" clearable />
      <el-select v-model="query.knowledgeType" placeholder="知识类型" clearable>
        <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.enabled" placeholder="状态" clearable>
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadDocuments">筛选</el-button>
      <el-button @click="openSearch">测试检索</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="documents">
        <el-table-column prop="title" label="标题" min-width="190" show-overflow-tooltip />
        <el-table-column prop="direction" label="方向" width="120" />
        <el-table-column label="类型" width="150">
          <template #default="{ row }">{{ typeLabel(row.knowledgeType) }}</template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.documentId)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row.documentId)">编辑</el-button>
            <el-button link type="primary" @click="rebuild(row.documentId)">重建</el-button>
            <el-button link type="danger" @click="remove(row.documentId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑知识文档' : '新增知识文档'" width="760px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="例如：Java后端实习岗位能力模型" />
        </el-form-item>
        <div class="form-grid two">
          <el-form-item label="岗位方向">
            <el-input v-model="form.direction" placeholder="Java后端 / AI应用 / 前端开发" />
          </el-form-item>
          <el-form-item label="知识类型">
            <el-select v-model="form.knowledgeType">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" placeholder="不填写时自动截取正文前 160 字" />
        </el-form-item>
        <el-form-item v-if="editingId" label="启用状态">
          <el-switch v-model="enabledSwitch" />
        </el-form-item>
        <el-form-item label="知识内容">
          <el-input v-model="form.content" type="textarea" :rows="12" placeholder="填写岗位技能要求、面试重点、简历优化建议等内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="知识文档详情" size="50%">
      <div v-if="detail" class="detail-stack">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="方向">{{ detail.direction }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeLabel(detail.knowledgeType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.enabled === 1 ? '启用' : '禁用' }}</el-descriptions-item>
        </el-descriptions>
        <section class="panel flat">
          <h4>知识内容</h4>
          <p class="content-preview">{{ detail.content }}</p>
        </section>
        <section class="panel flat">
          <div class="panel-header">
            <h4>文本切片</h4>
            <span>{{ detail.chunks?.length || 0 }} 个 chunk</span>
          </div>
          <el-timeline>
            <el-timeline-item v-for="chunk in detail.chunks" :key="chunk.chunkId" :timestamp="`Chunk ${chunk.chunkIndex}`">
              <p class="content-preview">{{ chunk.content }}</p>
              <el-tag size="small">{{ chunk.embeddingModel || '未生成向量' }}</el-tag>
            </el-timeline-item>
          </el-timeline>
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
            <h4>{{ item.title }}</h4>
            <el-tag>{{ item.similarity?.toFixed(4) }}</el-tag>
          </div>
          <p class="content-preview">{{ item.content }}</p>
        </article>
      </div>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
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
const formVisible = ref(false)
const detailVisible = ref(false)
const searchVisible = ref(false)
const editingId = ref<number>()

const query = reactive<any>({ direction: '', knowledgeType: '', enabled: undefined })
const form = reactive<any>({ title: '', direction: '', knowledgeType: 'SKILL_REQUIREMENT', summary: '', content: '', enabled: 1 })
const searchForm = reactive<any>({ query: '', direction: '', knowledgeType: '', topK: 5 })
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
  } catch {
    documents.value = []
  } finally {
    loading.value = false
  }
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
    ElMessage.success('保存成功')
    formVisible.value = false
    loadDocuments()
  } catch {
    // Error message is already shown by the request interceptor.
  } finally {
    saving.value = false
  }
}

async function openDetail(documentId: number) {
  detail.value = await getRagKnowledgeDetailApi(documentId)
  detailVisible.value = true
}

async function rebuild(documentId: number) {
  await rebuildRagKnowledgeApi(documentId)
  ElMessage.success('切片和向量已重建')
  loadDocuments()
}

async function remove(documentId: number) {
  await ElMessageBox.confirm('确认删除该知识文档？', '删除确认', { type: 'warning' })
  await deleteRagKnowledgeApi(documentId)
  ElMessage.success('删除成功')
  loadDocuments()
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
.content-preview {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
}

.search-results {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}
</style>
