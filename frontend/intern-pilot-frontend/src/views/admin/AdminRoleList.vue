<template>
  <PageContainer title="角色管理" description="查看角色并分配权限。">
    <section class="panel">
      <el-table v-loading="loading" :data="roles">
        <el-table-column prop="roleId" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" width="140" />
        <el-table-column prop="roleName" label="角色名称" width="140" />
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column label="权限数" width="90">
          <template #default="{ row }">{{ (row.permissions || []).length }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">分配权限</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="visible" title="分配权限" width="520px">
      <div class="permission-groups">
        <section v-for="group in groupedPermissions" :key="group.resourceType" class="permission-group">
          <strong>{{ group.resourceType || '未分组' }}</strong>
          <el-checkbox-group v-model="selectedPermissionIds">
            <el-checkbox v-for="item in group.items" :key="item.permissionId" :label="item.permissionId">
              {{ item.permissionCode }}
            </el-checkbox>
          </el-checkbox-group>
        </section>
      </div>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import { getAdminRoleListApi, updateRolePermissionsApi } from '@/api/adminRole'
import { getAdminPermissionListApi } from '@/api/adminPermission'

const loading = ref(false)
const roles = ref<any[]>([])
const permissions = ref<any[]>([])
const visible = ref(false)
const currentRole = ref<any>(null)
const selectedPermissionIds = ref<number[]>([])

const groupedPermissions = computed(() => {
  const map = new Map<string, any[]>()
  for (const permission of permissions.value) {
    const key = permission.resourceType || '未分组'
    map.set(key, [...(map.get(key) || []), permission])
  }
  return Array.from(map.entries()).map(([resourceType, items]) => ({ resourceType, items }))
})

async function loadData() {
  loading.value = true
  try {
    const roleRes: any = await getAdminRoleListApi()
    const permissionRes: any = await getAdminPermissionListApi()
    roles.value = roleRes || []
    permissions.value = permissionRes || []
  } finally {
    loading.value = false
  }
}

function openDialog(role: any) {
  currentRole.value = role
  const rolePermissionCodes = new Set(role.permissions || [])
  selectedPermissionIds.value = permissions.value
    .filter((item) => rolePermissionCodes.has(item.permissionCode))
    .map((item) => item.permissionId)
  visible.value = true
}

async function submit() {
  if (!currentRole.value) return
  await updateRolePermissionsApi(currentRole.value.roleId, { permissionIds: selectedPermissionIds.value })
  ElMessage.success('保存成功')
  visible.value = false
}

onMounted(loadData)
</script>

<style scoped>
.permission-groups {
  display: grid;
  gap: 16px;
  max-height: 55vh;
  overflow: auto;
}

.permission-group {
  display: grid;
  gap: 8px;
}

.permission-group :deep(.el-checkbox-group) {
  display: grid;
  gap: 6px;
}
</style>
