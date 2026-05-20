<template>
  <PageContainer title="操作日志" description="查看系统关键操作、执行结果、请求来源和耗时，辅助定位后台风险。">
    <div class="stat-grid compact-stats">
      <StatCard label="当前结果总数" :value="total" :icon="Files" />
      <StatCard label="本页成功" :value="successCount" :icon="CircleCheck" />
      <StatCard label="本页失败" :value="failureCount" :icon="Warning" />
      <StatCard label="平均耗时" :value="averageCostText" :icon="Clock" />
    </div>

    <section class="panel toolbar">
      <el-input v-model="query.module" placeholder="模块，例如 用户管理" clearable />
      <el-select v-model="query.operationType" placeholder="操作类型" clearable>
        <el-option label="新增" value="CREATE" />
        <el-option label="修改" value="UPDATE" />
        <el-option label="删除" value="DELETE" />
        <el-option label="登录" value="LOGIN" />
        <el-option label="上传" value="UPLOAD" />
        <el-option label="AI 操作" value="AI" />
        <el-option label="授权" value="GRANT" />
      </el-select>
      <el-input v-model="query.username" placeholder="操作人" clearable />
      <el-select v-model="query.success" placeholder="结果" clearable>
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </section>

    <section class="panel admin-table-panel">
      <el-table v-loading="loading" :data="logs">
        <el-table-column prop="logId" label="ID" width="80" />
        <el-table-column prop="operatorUsername" label="操作人" width="140" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="130" show-overflow-tooltip />
        <el-table-column prop="operation" label="操作" min-width="170" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag effect="plain">{{ operationTypeLabel(row.operationType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请求" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="request-line">{{ row.requestMethod || '-' }} {{ row.requestUri || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">
            <span :class="{ slow: Number(row.costTime || 0) >= 1000 }">{{ row.costTime || 0 }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.logId)">详情</el-button>
            <el-button link type="danger" @click="removeLog(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AppEmpty
            title="暂无操作日志"
            description="当前筛选条件下没有操作日志"
            hint="可以重置筛选条件，或稍后在系统产生操作后再查看。"
          />
        </template>
      </el-table>

      <div v-loading="loading" class="mobile-card-list">
        <AppEmpty
          v-if="!logs.length && !loading"
          title="暂无操作日志"
          description="当前筛选条件下没有操作日志"
          hint="可以重置筛选条件，或稍后在系统产生操作后再查看。"
        />
        <article v-for="row in logs" v-else :key="row.logId" class="mobile-card">
          <div class="mobile-card-head">
            <div>
              <span>日志 ID #{{ row.logId }} · {{ operationTypeLabel(row.operationType) }}</span>
              <h3>{{ row.operation || '-' }}</h3>
              <p>{{ row.module || '-' }} / {{ row.operatorUsername || '-' }}</p>
            </div>
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </div>

          <div class="request-line mobile-request">{{ row.requestMethod || '-' }} {{ row.requestUri || '-' }}</div>

          <div class="mobile-meta-grid">
            <div><span>耗时</span><strong :class="{ slow: Number(row.costTime || 0) >= 1000 }">{{ row.costTime || 0 }} ms</strong></div>
            <div><span>时间</span><strong>{{ formatDateTime(row.createdAt) }}</strong></div>
          </div>

          <div class="mobile-actions">
            <el-button type="primary" plain @click="openDetail(row.logId)">详情</el-button>
            <el-button type="danger" plain @click="removeLog(row)">删除</el-button>
          </div>
        </article>
      </div>

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
    </section>

    <el-drawer v-model="detailVisible" title="操作日志详情" :size="detailDrawerSize">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else-if="detail" class="detail-stack">
        <section class="panel flat">
          <div class="panel-header">
            <h3>基本信息</h3>
            <el-tag :type="detail.success === 1 ? 'success' : 'danger'" effect="plain">
              {{ detail.success === 1 ? '执行成功' : '执行失败' }}
            </el-tag>
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
          <div class="panel-header">
            <h3>请求信息</h3>
            <span>敏感字段已在前端展示时脱敏</span>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="请求方法">{{ detail.requestMethod || '-' }}</el-descriptions-item>
            <el-descriptions-item label="请求路径">{{ detail.requestUri || '-' }}</el-descriptions-item>
            <el-descriptions-item label="IP">{{ detail.ipAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="User-Agent">{{ detail.userAgent || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="panel flat">
          <h3>参数摘要</h3>
          <pre class="log-text">{{ maskSensitive(detail.requestParams) || '未记录请求参数' }}</pre>
        </section>

        <section v-if="detail.errorMessage" class="panel flat">
          <h3>错误信息</h3>
          <pre class="log-text error">{{ maskSensitive(detail.errorMessage) }}</pre>
        </section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Clock, Files, Warning } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import {
  deleteOperationLogApi,
  getOperationLogDetailApi,
  getOperationLogListApi
} from '@/api/adminOperationLog'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'

const logs = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detail = ref<any>(null)
const { responsiveDrawerSize } = useResponsiveSize()
const detailDrawerSize = responsiveDrawerSize('54%')

const query = reactive<{
  module: string
  operationType: string
  username: string
  success?: number
  pageNum: number
  pageSize: number
}>({
  module: '',
  operationType: '',
  username: '',
  success: undefined,
  pageNum: 1,
  pageSize: 10
})

const successCount = computed(() => logs.value.filter((item) => item.success === 1).length)
const failureCount = computed(() => logs.value.filter((item) => item.success === 0).length)
const averageCostText = computed(() => {
  if (!logs.value.length) return '0 ms'
  const totalCost = logs.value.reduce((sum, item) => sum + Number(item.costTime || 0), 0)
  return `${Math.round(totalCost / logs.value.length)} ms`
})

async function loadList() {
  loading.value = true
  try {
    const res: any = await getOperationLogListApi({ ...query })
    logs.value = res.records || []
    total.value = res.total || 0
  } catch (error: any) {
    logs.value = []
    total.value = 0
    ElMessage.error(error?.message || '操作日志加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  query.module = ''
  query.operationType = ''
  query.username = ''
  query.success = undefined
  query.pageNum = 1
  loadList()
}

function handlePageChange(page: number) {
  query.pageNum = page
  loadList()
}

function handleSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  loadList()
}

async function openDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getOperationLogDetailApi(id)
  } catch (error: any) {
    ElMessage.error(error?.message || '日志详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function removeLog(row: any) {
  await ElMessageBox.confirm(`确认删除日志 #${row.logId}？删除后无法在后台页面恢复。`, '删除确认', {
    type: 'warning'
  })
  await deleteOperationLogApi(row.logId)
  ElMessage.success('删除成功')
  loadList()
}

function operationTypeLabel(value: string) {
  const labels: Record<string, string> = {
    CREATE: '新增',
    UPDATE: '修改',
    DELETE: '删除',
    LOGIN: '登录',
    UPLOAD: '上传',
    AI: 'AI 操作',
    GRANT: '授权'
  }
  return labels[value] || value || '-'
}

function maskSensitive(value?: string) {
  if (!value) return ''
  return String(value)
    .replace(/("(?:password|token|authorization|apiKey|secret)"\s*:\s*)"[^"]*"/gi, '$1"******"')
    .replace(/((?:password|token|authorization|apiKey|secret)=)[^&\s]+/gi, '$1******')
}

onMounted(loadList)
</script>

<style scoped>
.compact-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}

.mobile-card-list {
  display: none;
}

.request-line {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 13px;
}

.slow {
  color: var(--color-warning);
  font-weight: 700;
}

.log-text {
  max-height: 260px;
  overflow: auto;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: #344054;
  line-height: 1.6;
}

.log-text.error {
  color: #b42318;
}

@media (max-width: 900px) {
  .compact-stats {
    grid-template-columns: 1fr;
  }

  .admin-table-panel :deep(.el-table) {
    display: none;
  }

  .mobile-card-list {
    display: grid;
    gap: 12px;
  }

  .mobile-card {
    display: grid;
    gap: 12px;
    padding: 14px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background: var(--color-surface);
  }

  .mobile-card-head {
    display: flex;
    gap: 12px;
    align-items: flex-start;
    justify-content: space-between;
  }

  .mobile-card-head span,
  .mobile-card-head p {
    margin: 0;
    color: var(--color-text-soft);
    font-size: 12px;
  }

  .mobile-card-head h3 {
    margin: 4px 0;
    overflow-wrap: anywhere;
    font-size: 16px;
    line-height: 1.5;
  }

  .mobile-request {
    overflow-wrap: anywhere;
    padding: 10px;
    border: 1px solid var(--color-border-soft);
    border-radius: var(--radius-md);
    background: var(--color-surface-muted);
  }

  .mobile-meta-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .mobile-meta-grid div {
    min-width: 0;
    padding: 10px;
    border: 1px solid var(--color-border-soft);
    border-radius: var(--radius-md);
    background: var(--color-surface-muted);
  }

  .mobile-meta-grid span,
  .mobile-meta-grid strong {
    display: block;
    overflow-wrap: anywhere;
  }

  .mobile-meta-grid span {
    color: var(--color-text-soft);
    font-size: 12px;
  }

  .mobile-meta-grid strong {
    margin-top: 4px;
    color: var(--color-text);
    font-size: 13px;
  }

  .mobile-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .mobile-actions .el-button {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 520px) {
  .mobile-card-head,
  .mobile-meta-grid,
  .mobile-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
