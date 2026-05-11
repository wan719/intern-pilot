<template>
  <PageContainer title="权限管理" description="只读查看权限列表。">
    <section class="panel toolbar">
      <el-select v-model="resourceType" placeholder="资源类型" clearable filterable>
        <el-option v-for="item in resourceTypes" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" @click="loadData">筛选</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="permissions">
        <el-table-column prop="permissionId" label="ID" width="80" />
        <el-table-column prop="permissionCode" label="权限编码" min-width="220" />
        <el-table-column prop="permissionName" label="权限名称" min-width="150" />
        <el-table-column prop="resourceType" label="资源类型" width="150" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { getAdminPermissionListApi } from '@/api/adminPermission'

const loading = ref(false)
const permissions = ref<any[]>([])
const resourceType = ref('')

const resourceTypes = computed(() => {
  return Array.from(new Set(permissions.value.map((item) => item.resourceType).filter(Boolean))).sort()
})

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
