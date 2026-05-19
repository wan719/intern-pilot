<template>
  <PageContainer title="用户反馈" description="查看、处理和回复用户在系统内提交的问题与建议。">
    <div class="stat-grid compact-stats">
      <StatCard label="反馈总数" :value="feedbacks.length" :icon="Message" />
      <StatCard label="待处理" :value="statusCount.PENDING" :icon="Warning" />
      <StatCard label="处理中" :value="statusCount.PROCESSING" :icon="Clock" />
      <StatCard label="已解决" :value="statusCount.RESOLVED" :icon="CircleCheck" />
    </div>

    <section class="panel toolbar">
      <el-select v-model="query.type" placeholder="反馈类型" clearable>
        <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="处理状态" clearable>
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="loadData">筛选</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="feedbacks">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="反馈" min-width="260">
          <template #default="{ row }">
            <div class="feedback-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.content }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag effect="plain">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <strong>{{ row.userName || `用户 #${row.userId}` }}</strong>
              <span>{{ row.userEmail || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pageUrl" label="页面路径" min-width="160" show-overflow-tooltip />
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="auth.hasPermission('feedback:write')" link type="warning" @click="openStatus(row)">状态</el-button>
            <el-button v-if="auth.hasPermission('feedback:write')" link type="primary" @click="openReply(row)">回复</el-button>
            <el-button v-if="auth.hasPermission('feedback:delete')" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AppEmpty title="暂无反馈" description="当前筛选条件下没有用户反馈" hint="用户提交反馈后会显示在这里。" />
        </template>
      </el-table>
    </section>

    <el-drawer v-model="detailVisible" title="反馈详情" size="48%">
      <div v-if="selected" class="detail-stack">
        <section class="panel flat">
          <div class="panel-header">
            <div>
              <h3>{{ selected.title }}</h3>
              <span>{{ selected.userName || `用户 #${selected.userId}` }} · {{ selected.userEmail || '未记录邮箱' }}</span>
            </div>
            <el-tag :type="statusTagType(selected.status)" effect="plain">{{ statusLabel(selected.status) }}</el-tag>
          </div>
          <p class="content">{{ selected.content }}</p>
        </section>
        <section class="panel flat">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="反馈类型">{{ typeLabel(selected.type) }}</el-descriptions-item>
            <el-descriptions-item label="页面路径">{{ selected.pageUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">{{ selected.contact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="允许联系">{{ selected.allowContact ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="浏览器信息">{{ selected.browserInfo || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>
        <section v-if="selected.adminReply" class="panel flat">
          <div class="panel-header">
            <h3>管理员回复</h3>
            <span>{{ selected.handledByName || '-' }} · {{ formatDateTime(selected.handledAt) }}</span>
          </div>
          <p class="content">{{ selected.adminReply }}</p>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="statusVisible" title="更新处理状态" width="420px">
      <el-form :model="statusForm" label-position="top">
        <el-form-item label="处理状态">
          <el-select v-model="statusForm.status">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitStatus">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replyVisible" title="回复反馈" width="560px">
      <el-form :model="replyForm" label-position="top">
        <el-form-item label="回复内容">
          <el-input v-model="replyForm.reply" type="textarea" :rows="5" placeholder="请输入处理说明或回复内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Clock, Message, Warning } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import { useAuthStore } from '@/stores/auth'
import { useFeedbackStore, type Feedback, type FeedbackStatus } from '@/stores/feedback'
import { formatDateTime } from '@/utils/format'

const auth = useAuthStore()
const store = useFeedbackStore()
const loading = computed(() => store.loading)
const feedbacks = computed(() => store.feedbackList)
const detailVisible = ref(false)
const statusVisible = ref(false)
const replyVisible = ref(false)
const saving = ref(false)
const selected = ref<Feedback | null>(null)

const query = reactive({ type: '', status: '' })
const statusForm = reactive<{ status: FeedbackStatus }>({ status: 'PENDING' })
const replyForm = reactive({ reply: '' })

const typeOptions = [
  { label: '功能异常', value: 'BUG' },
  { label: '使用建议', value: 'SUGGESTION' },
  { label: '页面体验', value: 'UI_UX' },
  { label: 'AI 结果', value: 'AI_RESULT' },
  { label: '性能问题', value: 'PERFORMANCE' },
  { label: '其他', value: 'OTHER' }
]

const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已忽略', value: 'IGNORED' }
]

const statusCount = computed(() => ({
  PENDING: feedbacks.value.filter((item) => item.status === 'PENDING').length,
  PROCESSING: feedbacks.value.filter((item) => item.status === 'PROCESSING').length,
  RESOLVED: feedbacks.value.filter((item) => item.status === 'RESOLVED').length
}))

async function loadData() {
  await store.fetchFeedbackList({
    type: query.type || undefined,
    status: query.status || undefined
  })
}

function resetQuery() {
  query.type = ''
  query.status = ''
  loadData()
}

function openDetail(row: Feedback) {
  selected.value = row
  detailVisible.value = true
}

function openStatus(row: Feedback) {
  selected.value = row
  statusForm.status = row.status
  statusVisible.value = true
}

function openReply(row: Feedback) {
  selected.value = row
  replyForm.reply = row.adminReply || ''
  replyVisible.value = true
}

async function submitStatus() {
  if (!selected.value) return
  saving.value = true
  try {
    await store.updateStatus(selected.value.id, statusForm.status)
    ElMessage.success('状态已更新')
    statusVisible.value = false
  } finally {
    saving.value = false
  }
}

async function submitReply() {
  if (!selected.value) return
  if (!replyForm.reply.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  saving.value = true
  try {
    await store.reply(selected.value.id, replyForm.reply.trim())
    ElMessage.success('回复已提交')
    replyVisible.value = false
  } finally {
    saving.value = false
  }
}

async function remove(row: Feedback) {
  await ElMessageBox.confirm(`确认删除反馈「${row.title}」？`, '删除确认', { type: 'warning' })
  await store.deleteFeedback(row.id)
  ElMessage.success('删除成功')
}

function typeLabel(value: string) {
  return typeOptions.find((item) => item.value === value)?.label || value
}

function statusLabel(value: string) {
  return statusOptions.find((item) => item.value === value)?.label || value
}

function statusTagType(value: string) {
  if (value === 'PENDING') return 'warning'
  if (value === 'PROCESSING') return 'primary'
  if (value === 'RESOLVED') return 'success'
  return 'info'
}

onMounted(loadData)
</script>

<style scoped>
.compact-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.feedback-cell,
.user-cell {
  display: grid;
  gap: 4px;
}

.feedback-cell strong,
.user-cell strong {
  color: var(--color-text);
}

.feedback-cell span,
.user-cell span {
  display: -webkit-box;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 13px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.content {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.8;
}

@media (max-width: 900px) {
  .compact-stats {
    grid-template-columns: 1fr;
  }
}
</style>
