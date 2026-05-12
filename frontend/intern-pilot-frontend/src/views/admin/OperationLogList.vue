<template>
  <PageContainer title="" description="查看系统关键操作、执行结果、请求来源和耗时。">
    <section class="panel toolbar">
      <el-input v-model="query.module" placeholder="模块" clearable />
      <el-select v-model="query.operationType" placeholder="操作类型" clearable>
        <el-option label="新增" value="CREATE" />
        <el-option label="修改" value="UPDATE" />
        <el-option label="删除" value="DELETE" />
        <el-option label="登录" value="LOGIN" />
        <el-option label="上传" value="UPLOAD" />
        <el-option label="AI操作" value="AI" />
        <el-option label="授权" value="GRANT" />
      </el-select>
      <el-input v-model="query.username" placeholder="操作人" clearable />
      <el-select v-model="query.success" placeholder="结果" clearable>
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="logs">
        <el-table-column prop="logId" label="ID" width="80" />
        <el-table-column prop="operatorUsername" label="操作人" width="130" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="operation" label="操作" min-width="150" show-overflow-tooltip />
        <el-table-column prop="operationType" label="类型" width="100" />
        <el-table-column prop="requestMethod" label="方法" width="90" />
        <el-table-column prop="requestUri" label="路径" min-width="220" show-overflow-tooltip />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="100" />
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
          <el-empty description="暂无操作日志" />
        </template>
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
    </section>

    <el-drawer v-model="detailVisible" title="操作日志详情" size="54%">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else-if="detail" class="detail-stack">
        <section class="panel flat">
          <h4>基本信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="日志ID">{{ detail.logId }}</el-descriptions-item>
            <el-descriptions-item label="操作人">{{ detail.operatorUsername || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模块">{{ detail.module }}</el-descriptions-item>
            <el-descriptions-item label="操作">{{ detail.operation }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ detail.operationType }}</el-descriptions-item>
            <el-descriptions-item label="结果">
              <el-tag :type="detail.success === 1 ? 'success' : 'danger'">
                {{ detail.success === 1 ? '成功' : '失败' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="耗时">{{ detail.costTime }} ms</el-descriptions-item>
            <el-descriptions-item label="时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="panel flat">
          <h4>请求信息</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="请求方法">{{ detail.requestMethod || '-' }}</el-descriptions-item>
            <el-descriptions-item label="请求路径">{{ detail.requestUri || '-' }}</el-descriptions-item>
            <el-descriptions-item label="IP">{{ detail.ipAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="User-Agent">{{ detail.userAgent || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="panel flat">
          <h4>参数摘要</h4>
          <pre class="log-text">{{ detail.requestParams || '未记录请求参数' }}</pre>
        </section>

        <section v-if="detail.errorMessage" class="panel flat">
          <h4>错误信息</h4>
          <pre class="log-text error">{{ detail.errorMessage }}</pre>
        </section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import {
  deleteOperationLogApi,
  getOperationLogDetailApi,
  getOperationLogListApi
} from '@/api/adminOperationLog'
import { formatDateTime } from '@/utils/format'

const logs = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detail = ref<any>(null)

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

async function loadList() {
  loading.value = true
  try {
    const res: any = await getOperationLogListApi({ ...query })
    logs.value = res.records || []
    total.value = res.total || 0
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
  } finally {
    detailLoading.value = false
  }
}

async function removeLog(row: any) {
  await ElMessageBox.confirm(`确认删除日志 #${row.logId}？`, '删除确认', {
    type: 'warning'
  })
  await deleteOperationLogApi(row.logId)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.pager {
  justify-content: flex-end;
  margin-top: 16px;
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
</style>
