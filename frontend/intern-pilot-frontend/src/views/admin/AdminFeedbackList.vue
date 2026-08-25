<template>
  <PageContainer width="wide" title="用户反馈" description="查看、处理和回复用户在系统内提交的问题与建议。">
    <div class="stat-grid compact-stats">
      <StatCard label="反馈总数" :value="feedbacks.length" :icon="Message" :loading="loading" />
      <StatCard label="待处理" :value="statusCount.PENDING" :icon="Warning" :loading="loading" />
      <StatCard label="处理中" :value="statusCount.PROCESSING" :icon="Clock" :loading="loading" />
      <StatCard label="已解决" :value="statusCount.RESOLVED" :icon="CircleCheck" :loading="loading" />
    </div>

    <FilterBar @reset="resetQuery">
      <template #filters>
        <el-select v-model="query.type" aria-label="反馈类型" placeholder="反馈类型" clearable>
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.status" aria-label="处理状态" placeholder="处理状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" :loading="loading" @click="loadData">筛选</el-button>
      </template>
    </FilterBar>

    <TableShell
      :loading="loading"
      :empty="feedbacks.length === 0"
      empty-title="暂无反馈"
      empty-hint="当前筛选条件下没有用户反馈；用户提交反馈后会显示在这里。"
    >
      <el-table class="admin-data-table" style="min-width: 1360px" size="small" :data="feedbacks">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="反馈" min-width="280">
          <template #default="{ row }"><div class="feedback-cell"><strong>{{ row.title }}</strong><span>{{ row.content }}</span></div></template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }"><el-tag effect="plain">{{ typeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }"><div class="user-cell"><strong>{{ row.userName || `用户 #${row.userId}` }}</strong><span>{{ row.userEmail || '-' }}</span></div></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" :label="statusLabel(row.status)" size="small" /></template>
        </el-table-column>
        <el-table-column prop="pageUrl" label="页面路径" min-width="170" show-overflow-tooltip />
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="auth.hasPermission('feedback:write')" link type="warning" @click="openStatus(row)">状态</el-button>
            <el-button v-if="auth.hasPermission('feedback:write')" link type="primary" @click="openReply(row)">回复</el-button>
            <el-button
              v-if="auth.hasPermission('feedback:delete')"
              link
              type="danger"
              :loading="deletingIds.has(row.id)"
              @click="remove(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </TableShell>

    <el-drawer v-model="detailVisible" title="反馈详情" :size="detailDrawerSize" class="feedback-detail-drawer">
      <div v-if="selected" class="detail-stack">
        <section class="panel flat">
          <div class="panel-header">
            <div><h2>{{ selected.title }}</h2><span>{{ selected.userName || `用户 #${selected.userId}` }} · {{ selected.userEmail || '未记录邮箱' }}</span></div>
            <StatusTag :status="selected.status" :label="statusLabel(selected.status)" />
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
          <div class="panel-header"><h2>管理员回复</h2><span>{{ selected.handledByName || '-' }} · {{ formatDateTime(selected.handledAt) }}</span></div>
          <p class="content">{{ selected.adminReply }}</p>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="statusVisible" title="更新处理状态" :width="dialogWidth" @closed="statusFormRef?.clearValidate()">
      <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-position="top">
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="statusForm.status">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!auth.hasPermission('feedback:write')" @click="submitStatus">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replyVisible" title="回复反馈" :width="dialogWidth" @closed="replyFormRef?.clearValidate()">
      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-position="top">
        <el-form-item label="回复内容" prop="reply" :error="replyError">
          <el-input v-model="replyForm.reply" type="textarea" :rows="5" placeholder="请输入处理说明或回复内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!auth.hasPermission('feedback:write')" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { CircleCheck, Clock, Message, Warning } from '@element-plus/icons-vue'
import FilterBar from '@/components/common/FilterBar.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TableShell from '@/components/common/TableShell.vue'
import { useAuthStore } from '@/stores/auth'
import { useFeedbackStore, type Feedback, type FeedbackStatus } from '@/stores/feedback'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'

const auth = useAuthStore()
const store = useFeedbackStore()
const loading = computed(() => store.loading)
const feedbacks = computed(() => store.feedbackList)
const detailVisible = ref(false)
const statusVisible = ref(false)
const replyVisible = ref(false)
const saving = ref(false)
const selected = ref<Feedback | null>(null)
const deletingIds = ref(new Set<number>())
const { responsiveDrawerSize, responsiveDialogWidth } = useResponsiveSize()
const detailDrawerSize = responsiveDrawerSize('48%')
const dialogWidth = responsiveDialogWidth('560px')
const statusFormRef = ref<FormInstance>()
const replyFormRef = ref<FormInstance>()
const query = reactive({ type: '', status: '' })
const statusForm = reactive<{ status: FeedbackStatus }>({ status: 'PENDING' })
const replyForm = reactive({ reply: '' })
const replyError = ref('')
const statusRules: FormRules = { status: [{ required: true, message: '请选择处理状态', trigger: 'change' }] }
const replyRules: FormRules = {
  reply: [{ required: true, whitespace: true, message: '请输入回复内容', trigger: 'blur' }]
}
let active = true
let queuedListParams: { type?: string; status?: string } | null = null
let listFlight: Promise<void> | null = null
let writeInFlight = false

const typeOptions = [
  { label: '功能异常', value: 'BUG' }, { label: '使用建议', value: 'SUGGESTION' },
  { label: '页面体验', value: 'UI_UX' }, { label: 'AI 结果', value: 'AI_RESULT' },
  { label: '性能问题', value: 'PERFORMANCE' }, { label: '其他', value: 'OTHER' }
]
const statusOptions = [
  { label: '待处理', value: 'PENDING' }, { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' }, { label: '已忽略', value: 'IGNORED' }
]
const statusCount = computed(() => ({
  PENDING: feedbacks.value.filter((item) => item.status === 'PENDING').length,
  PROCESSING: feedbacks.value.filter((item) => item.status === 'PROCESSING').length,
  RESOLVED: feedbacks.value.filter((item) => item.status === 'RESOLVED').length
}))

function loadData() {
  queuedListParams = { type: query.type || undefined, status: query.status || undefined }
  if (listFlight) return listFlight
  listFlight = (async () => {
    while (active && queuedListParams) {
      const params = queuedListParams
      queuedListParams = null
      try {
        await store.fetchFeedbackList(params)
      } catch (error: any) {
        if (active) ElMessage.error(error?.message || '反馈列表加载失败，请稍后重试')
      }
    }
  })().finally(() => { listFlight = null })
  return listFlight
}

function resetQuery() {
  Object.assign(query, { type: '', status: '' })
  return loadData()
}
function openDetail(row: Feedback) { selected.value = row; detailVisible.value = true }
function closeWriteDialogs() {
  statusVisible.value = false
  replyVisible.value = false
}
function guardWritePermission() {
  if (auth.hasPermission('feedback:write')) return true
  closeWriteDialogs()
  return false
}
function openStatus(row: Feedback) {
  if (!guardWritePermission()) return
  selected.value = row
  statusForm.status = row.status
  statusVisible.value = true
  void nextTick(() => statusFormRef.value?.clearValidate())
}
function openReply(row: Feedback) {
  if (!guardWritePermission()) return
  selected.value = row
  replyForm.reply = row.adminReply || ''
  replyError.value = ''
  replyVisible.value = true
  void nextTick(() => replyFormRef.value?.clearValidate())
}

async function submitStatus() {
  if (!guardWritePermission()) return
  if (!selected.value || writeInFlight) return
  writeInFlight = true
  try {
    await nextTick()
    if (!statusFormRef.value || !active) return
    const valid = await statusFormRef.value.validate().catch(() => false)
    if (!valid) return
    if (!guardWritePermission()) return
    saving.value = true
    await store.updateStatus(selected.value.id, statusForm.status)
    if (!active) return
    ElMessage.success('状态已更新')
    statusVisible.value = false
  } finally {
    writeInFlight = false
    if (active) saving.value = false
  }
}

async function submitReply() {
  if (!guardWritePermission()) return
  if (!selected.value || writeInFlight) return
  writeInFlight = true
  try {
    await nextTick()
    replyError.value = replyForm.reply.trim() ? '' : '请输入回复内容'
    if (replyError.value) return
    if (!replyFormRef.value || !active) return
    const valid = await replyFormRef.value.validate().catch(() => false)
    if (!valid) return
    if (!guardWritePermission()) return
    saving.value = true
    await store.reply(selected.value.id, replyForm.reply.trim())
    if (!active) return
    ElMessage.success('回复已提交')
    replyVisible.value = false
  } finally {
    writeInFlight = false
    if (active) saving.value = false
  }
}

function setDeleting(id: number, deleting: boolean) {
  const next = new Set(deletingIds.value)
  if (deleting) next.add(id)
  else next.delete(id)
  deletingIds.value = next
}

function isConfirmationDismissed(reason: unknown) {
  return reason === 'cancel' || reason === 'close'
}

async function remove(row: Feedback) {
  if (!auth.hasPermission('feedback:delete')) return
  if (deletingIds.value.has(row.id)) return
  setDeleting(row.id, true)
  try {
    try {
      await ElMessageBox.confirm(`确认删除反馈「${row.title}」？`, '删除确认', { type: 'warning' })
    } catch (reason) {
      if (isConfirmationDismissed(reason)) return
      throw reason
    }
    await store.deleteFeedback(row.id)
    if (active) ElMessage.success('删除成功')
  } finally {
    if (active) setDeleting(row.id, false)
  }
}

function typeLabel(value: string) { return typeOptions.find((item) => item.value === value)?.label || value }
function statusLabel(value: string) { return statusOptions.find((item) => item.value === value)?.label || value }

onMounted(loadData)
onBeforeUnmount(() => {
  active = false
  queuedListParams = null
})
</script>

<style scoped>
.compact-stats { grid-template-columns: repeat(4, minmax(0, 1fr)); }
:deep(.filter-bar__filters .el-select) { width: min(240px, 100%); }
.feedback-cell, .user-cell { display: grid; gap: 4px; }
.feedback-cell span, .user-cell span { display: -webkit-box; overflow: hidden; color: var(--color-text-muted); font-size: 13px; overflow-wrap: anywhere; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.panel h2 { margin: 0; font-size: 16px; }
.content { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; }

@media (max-width: 900px) {
  .compact-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 640px) {
  .compact-stats { grid-template-columns: 1fr; }
  :deep(.feedback-detail-drawer .el-drawer__header) { margin-bottom: 12px; padding: 16px 16px 10px; }
  :deep(.feedback-detail-drawer .el-drawer__body) { padding: 0 16px 16px; }
}
</style>
