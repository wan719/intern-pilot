<template>
  <PageContainer title="权限管理" description="只读查看系统权限点，帮助理解后台菜单、按钮和接口权限的对应关系。">
    <div class="stat-grid compact-stats">
      <StatCard label="权限总数" :value="permissions.length" :icon="Files" />
      <StatCard label="启用权限" :value="enabledCount" :icon="CircleCheck" />
      <StatCard label="停用权限" :value="disabledCount" :icon="Warning" />
      <StatCard label="资源类型" :value="resourceTypes.length" :icon="DataBoard" />
    </div>

    <section class="panel toolbar">
      <el-select v-model="resourceType" placeholder="资源类型" clearable filterable>
        <el-option v-for="item in resourceTypes" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="loadData">筛选</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="permissions">
        <el-table-column prop="permissionId" label="ID" width="80" />
        <el-table-column label="权限" min-width="260">
          <template #default="{ row }">
            <div class="permission-cell">
              <strong>{{ row.permissionName || row.permissionCode }}</strong>
              <span>{{ row.permissionCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="resourceType" label="资源类型" width="150" />
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <AppEmpty
            title="暂无权限项"
            description="当前筛选条件下没有权限记录"
            hint="可以重置资源类型筛选后重新查看。"
          />
        </template>
      </el-table>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CircleCheck, DataBoard, Files, Warning } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import { getAdminPermissionListApi } from '@/api/adminPermission'

const loading = ref(false)
const permissions = ref<any[]>([])
const resourceType = ref('')

const resourceTypes = computed(() => {
  return Array.from(new Set(permissions.value.map((item) => item.resourceType).filter(Boolean))).sort()
})
const enabledCount = computed(() => permissions.value.filter((item) => item.enabled).length)
const disabledCount = computed(() => permissions.value.filter((item) => !item.enabled).length)

async function loadData() {
  loading.value = true
  try {
    const res: any = await getAdminPermissionListApi(resourceType.value ? { resourceType: resourceType.value } : undefined)
    permissions.value = res || []
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  resourceType.value = ''
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.compact-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.permission-cell {
  display: grid;
  gap: 4px;
}

.permission-cell strong {
  color: var(--color-text);
}

.permission-cell span {
  color: var(--color-text-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
}

@media (max-width: 900px) {
  .compact-stats {
    grid-template-columns: 1fr;
  }
}
</style>
