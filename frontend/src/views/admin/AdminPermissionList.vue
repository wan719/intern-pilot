<template>
  <PageContainer width="wide" title="权限管理" description="只读查看系统权限点，帮助理解后台菜单、按钮和接口权限的对应关系。">
    <AdminResultTable
      label="权限列表"
      :rows="permissions"
      :loading="loading"
      :error="loadError"
      @retry="loadData"
      empty-title="暂无权限项"
      empty-hint="当前筛选条件下没有权限记录；可以重置资源类型筛选后重新查看。"
    >
      <template #filters>
        <FilterBar @reset="resetFilter">
          <template #filters>
            <el-select v-model="resourceType" aria-label="资源类型" placeholder="资源类型" clearable filterable>
              <el-option v-for="item in resourceTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </template>
          <template #actions>
            <el-button type="primary" :loading="loading" @click="loadData">筛选</el-button>
          </template>
        </FilterBar>
      </template>
      <template #summary>
        <AdminMetricGroup class="list-metrics" label="列表概览">
          <StatCard label="权限总数" :value="loadError ? '—' : permissions.length" :icon="Files" :loading="loading" />
          <StatCard label="启用权限" :value="loadError ? '—' : enabledCount" :icon="CircleCheck" :loading="loading" />
          <StatCard label="停用权限" :value="loadError ? '—' : disabledCount" :icon="Warning" :loading="loading" />
          <StatCard label="资源类型" :value="loadError ? '—' : resourceTypes.length" :icon="DataBoard" :loading="loading" />
        </AdminMetricGroup>
      </template>
      <el-table-column prop="permissionId" label="ID" width="80" />
      <el-table-column label="权限" min-width="270">
        <template #default="{ row }">
          <div class="permission-cell"><strong>{{ row.permissionName || row.permissionCode }}</strong><span>{{ row.permissionCode }}</span></div>
        </template>
      </el-table-column>
      <el-table-column prop="resourceType" label="资源类型" width="160" />
      <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="Boolean(row.enabled)" :label="row.enabled ? '启用' : '停用'" size="small" />
        </template>
      </el-table-column>
    </AdminResultTable>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { CircleCheck, DataBoard, Files, Warning } from '@element-plus/icons-vue'
import FilterBar from '@/components/common/FilterBar.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import AdminMetricGroup from './AdminMetricGroup.vue'
import AdminResultTable from './AdminResultTable.vue'
import { getAdminPermissionListApi } from '@/api/adminPermission'

const loading = ref(false)
const permissions = ref<any[]>([])
const resourceType = ref('')
const loadError = ref('')
let active = true
let loadRequestId = 0

const resourceTypes = computed(() => Array.from(new Set(permissions.value.map((item) => item.resourceType).filter(Boolean))).sort())
const enabledCount = computed(() => permissions.value.filter((item) => item.enabled).length)
const disabledCount = computed(() => permissions.value.filter((item) => !item.enabled).length)

async function loadData() {
  const requestId = ++loadRequestId
  loading.value = true
  loadError.value = ''
  try {
    const res: any = await getAdminPermissionListApi(resourceType.value ? { resourceType: resourceType.value } : undefined)
    if (active && requestId === loadRequestId) permissions.value = res || []
  } catch {
    if (active && requestId === loadRequestId) {
      permissions.value = []
      loadError.value = '权限列表加载失败'
    }
  } finally {
    if (active && requestId === loadRequestId) loading.value = false
  }
}

function resetFilter() {
  resourceType.value = ''
  return loadData()
}

onMounted(loadData)
onBeforeUnmount(() => {
  active = false
  loadRequestId += 1
})
</script>

<style scoped>
.list-metrics { margin-bottom: var(--space-4); }
:deep(.filter-bar__filters .el-select) { width: min(260px, 100%); }
.permission-cell { display: grid; gap: 4px; }
.permission-cell span { color: var(--color-text-muted); font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace; font-size: 12px; overflow-wrap: anywhere; }
</style>
