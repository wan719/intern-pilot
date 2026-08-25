<template>
  <PageContainer width="wide" title="操作日志" description="查看系统关键操作、执行结果、请求来源和耗时，辅助定位后台风险。">
    <div class="stat-grid compact-stats">
      <StatCard label="当前结果总数" :value="total" :icon="Files" :loading="loading" />
      <StatCard label="本页成功" :value="successCount" :icon="CircleCheck" :loading="loading" />
      <StatCard label="本页失败" :value="failureCount" :icon="Warning" :loading="loading" />
      <StatCard label="平均耗时" :value="averageCostText" :icon="Clock" :loading="loading" />
    </div>

    <FilterBar @reset="resetQuery">
      <template #filters>
        <el-input v-model="query.module" aria-label="模块" placeholder="模块，例如 用户管理" clearable @keyup.enter="search" />
        <el-select v-model="query.operationType" aria-label="操作类型" placeholder="操作类型" clearable>
          <el-option label="新增" value="CREATE" /><el-option label="修改" value="UPDATE" />
          <el-option label="删除" value="DELETE" /><el-option label="登录" value="LOGIN" />
          <el-option label="上传" value="UPLOAD" /><el-option label="AI 操作" value="AI" />
          <el-option label="授权" value="GRANT" />
        </el-select>
        <el-input v-model="query.username" aria-label="操作人" placeholder="操作人" clearable @keyup.enter="search" />
        <el-select v-model="query.success" aria-label="执行结果" placeholder="结果" clearable>
          <el-option label="成功" :value="1" /><el-option label="失败" :value="0" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" :loading="loading" @click="search">查询</el-button>
      </template>
    </FilterBar>

    <TableShell
      :loading="loading"
      :empty="logs.length === 0"
      empty-title="暂无操作日志"
      empty-hint="当前筛选条件下没有操作日志；可以重置筛选条件，或稍后在系统产生操作后再查看。"
    >
      <el-table class="admin-data-table" style="min-width: 1540px" size="small" :data="logs">
        <el-table-column prop="logId" label="ID" width="80" />
        <el-table-column prop="operatorUsername" label="操作人" width="140" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="140" show-overflow-tooltip />
        <el-table-column prop="operation" label="操作" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }"><el-tag effect="plain">{{ operationTypeLabel(row.operationType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="请求" min-width="280" show-overflow-tooltip>
          <template #default="{ row }"><span class="request-line">{{ row.requestMethod || '-' }} {{ row.requestUri || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }"><StatusTag :status="row.success === 1 ? 'SUCCESS' : 'FAILED'" :label="row.success === 1 ? '成功' : '失败'" size="small" /></template>
        </el-table-column>
        <el-table-column label="耗时" width="110">
          <template #default="{ row }"><span :class="{ slow: Number(row.costTime || 0) >= 1000 }">{{ row.costTime || 0 }} ms</span></template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.logId)">详情</el-button>
            <el-button
              v-if="hasPermission('operation-log:delete')"
              link
              type="danger"
              :loading="pendingLogIds.has(row.logId)"
              @click="removeLog(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pager"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </TableShell>

    <el-drawer v-model="detailVisible" title="操作日志详情" :size="detailDrawerSize" @closed="invalidateDetail">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else-if="detail" class="detail-stack">
        <section class="panel flat">
          <div class="panel-header">
            <h2>基本信息</h2>
            <StatusTag :status="detail.success === 1 ? 'SUCCESS' : 'FAILED'" :label="detail.success === 1 ? '执行成功' : '执行失败'" />
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="日志 ID">{{ detail.logId }}</el-descriptions-item>
            <el-descriptions-item label="操作人">{{ detail.operatorUsername || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模块">{{ detail.module || '-' }}</el-descriptions-item>
            <el-descriptions-item label="操作">{{ detail.operation || '-' }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ operationTypeLabel(detail.operationType) }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ detail.costTime || 0 }} ms</el-descriptions-item>
            <el-descriptions-item label="时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </section>
        <section class="panel flat">
          <div class="panel-header"><h2>请求信息</h2><span>敏感字段已在前端展示时脱敏</span></div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="请求方法">{{ detail.requestMethod || '-' }}</el-descriptions-item>
            <el-descriptions-item label="请求路径">{{ detail.requestUri || '-' }}</el-descriptions-item>
            <el-descriptions-item label="IP">{{ detail.ipAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="User-Agent">{{ detail.userAgent || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>
        <section class="panel flat"><h2>参数摘要</h2><pre class="log-text">{{ maskSensitive(detail.requestParams) || '未记录请求参数' }}</pre></section>
        <section v-if="detail.errorMessage" class="panel flat"><h2>错误信息</h2><pre class="log-text error">{{ maskSensitive(detail.errorMessage) }}</pre></section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Clock, Files, Warning } from '@element-plus/icons-vue'
import FilterBar from '@/components/common/FilterBar.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TableShell from '@/components/common/TableShell.vue'
import { deleteOperationLogApi, getOperationLogDetailApi, getOperationLogListApi } from '@/api/adminOperationLog'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const logs = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detail = ref<any>(null)
const pendingLogIds = ref(new Set<number>())
const { responsiveDrawerSize } = useResponsiveSize()
const detailDrawerSize = responsiveDrawerSize('54%')
let active = true
let listRequestId = 0
let detailRequestId = 0

const query = reactive<{ module: string; operationType: string; username: string; success?: number; pageNum: number; pageSize: number }>({
  module: '', operationType: '', username: '', success: undefined, pageNum: 1, pageSize: 10
})
const successCount = computed(() => logs.value.filter((item) => item.success === 1).length)
const failureCount = computed(() => logs.value.filter((item) => item.success === 0).length)
const averageCostText = computed(() => {
  if (!logs.value.length) return '0 ms'
  return `${Math.round(logs.value.reduce((sum, item) => sum + Number(item.costTime || 0), 0) / logs.value.length)} ms`
})

async function loadList() {
  const requestId = ++listRequestId
  loading.value = true
  try {
    const res: any = await getOperationLogListApi({ ...query })
    if (!active || requestId !== listRequestId) return
    logs.value = res.records || []
    total.value = res.total || 0
  } catch (error: any) {
    if (!active || requestId !== listRequestId) return
    logs.value = []
    total.value = 0
    ElMessage.error(error?.message || '操作日志加载失败，请稍后重试')
  } finally {
    if (active && requestId === listRequestId) loading.value = false
  }
}

function search() { query.pageNum = 1; return loadList() }
function resetQuery() {
  Object.assign(query, { module: '', operationType: '', username: '', success: undefined, pageNum: 1 })
  return loadList()
}
function handlePageChange(page: number) { query.pageNum = page; return loadList() }
function handleSizeChange(size: number) { query.pageSize = size; query.pageNum = 1; return loadList() }

async function openDetail(id: number) {
  const requestId = ++detailRequestId
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const result = await getOperationLogDetailApi(id)
    if (active && requestId === detailRequestId && detailVisible.value) detail.value = result
  } catch (error: any) {
    if (active && requestId === detailRequestId) ElMessage.error(error?.message || '日志详情加载失败')
  } finally {
    if (active && requestId === detailRequestId) detailLoading.value = false
  }
}

function invalidateDetail() {
  detailRequestId += 1
  detailLoading.value = false
  detail.value = null
}

function setLogPending(id: number, pending: boolean) {
  const next = new Set(pendingLogIds.value)
  if (pending) next.add(id)
  else next.delete(id)
  pendingLogIds.value = next
}

function isConfirmationDismissed(reason: unknown) {
  return reason === 'cancel' || reason === 'close'
}

async function removeLog(row: any) {
  if (!hasPermission('operation-log:delete')) return
  if (pendingLogIds.value.has(row.logId)) return
  setLogPending(row.logId, true)
  try {
    try {
      await ElMessageBox.confirm(`确认删除日志 #${row.logId}？删除后无法在后台页面恢复。`, '删除确认', { type: 'warning' })
    } catch (reason) {
      if (isConfirmationDismissed(reason)) return
      throw reason
    }
    await deleteOperationLogApi(row.logId)
    if (!active) return
    ElMessage.success('删除成功')
    await loadList()
  } finally {
    if (active) setLogPending(row.logId, false)
  }
}

function hasPermission(permission: string) { return auth.hasPermission(permission) }
function operationTypeLabel(value: string) {
  const labels: Record<string, string> = { CREATE: '新增', UPDATE: '修改', DELETE: '删除', LOGIN: '登录', UPLOAD: '上传', AI: 'AI 操作', GRANT: '授权' }
  return labels[value] || value || '-'
}
function maskSensitive(value?: string) {
  if (!value) return ''
  return String(value)
    .replace(/("(?:password|token|authorization|apiKey|secret)"\s*:\s*)"[^"]*"/gi, '$1"******"')
    .replace(/((?:password|token|authorization|apiKey|secret)=)[^&\s]+/gi, '$1******')
}

onMounted(loadList)
onBeforeUnmount(() => {
  active = false
  listRequestId += 1
  detailRequestId += 1
})
</script>

<style scoped>
.compact-stats { grid-template-columns: repeat(4, minmax(0, 1fr)); }
:deep(.filter-bar__filters .el-input), :deep(.filter-bar__filters .el-select) { width: min(200px, 100%); }
.pager { justify-content: flex-end; padding: var(--space-3); border-top: 1px solid var(--color-border-soft); }
.request-line { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace; font-size: 13px; }
.slow { color: var(--color-warning); font-weight: 700; }
.panel h2 { margin: 0; font-size: 16px; }
.log-text { max-height: 260px; margin: 0; overflow: auto; white-space: pre-wrap; overflow-wrap: anywhere; color: var(--color-text); line-height: 1.6; }
.log-text.error { color: var(--color-danger); }

@media (max-width: 900px) {
  .compact-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 520px) {
  .compact-stats { grid-template-columns: 1fr; }
  .pager { justify-content: flex-start; overflow-x: auto; }
}
</style>
