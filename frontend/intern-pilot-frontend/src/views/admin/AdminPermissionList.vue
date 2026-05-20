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

    <section class="panel admin-table-panel">
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

      <div v-loading="loading" class="mobile-card-list">
        <AppEmpty
          v-if="!permissions.length && !loading"
          title="暂无权限项"
          description="当前筛选条件下没有权限记录"
          hint="可以重置资源类型筛选后重新查看。"
        />
        <article v-for="row in permissions" v-else :key="row.permissionId" class="mobile-card">
          <div class="mobile-card-head">
            <div>
              <span>权限 ID #{{ row.permissionId }}</span>
              <h3>{{ row.permissionName || row.permissionCode }}</h3>
              <p>{{ row.permissionCode }}</p>
            </div>
            <el-tag :type="row.enabled ? 'success' : 'danger'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </div>
          <div class="mobile-meta-grid">
            <div><span>资源类型</span><strong>{{ row.resourceType || '-' }}</strong></div>
          </div>
          <p class="mobile-card-desc">{{ row.description || '暂无描述' }}</p>
        </article>
      </div>
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

.mobile-card-list {
  display: none;
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
  .mobile-card-head p,
  .mobile-card-desc {
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

  .mobile-card-desc {
    color: var(--color-text-muted);
    line-height: 1.7;
  }

  .mobile-meta-grid div {
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
}

@media (max-width: 520px) {
  .mobile-card-head {
    display: grid;
  }
}
</style>
