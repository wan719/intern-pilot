<template>
  <PageContainer width="wide" title="角色管理" description="查看系统角色，并维护角色拥有的权限集合。">
    <div class="stat-grid compact-stats">
      <StatCard label="角色总数" :value="roles.length" :icon="User" :loading="loading" />
      <StatCard label="启用角色" :value="enabledCount" :icon="CircleCheck" :loading="loading" />
      <StatCard label="停用角色" :value="disabledCount" :icon="Warning" :loading="loading" />
      <StatCard label="权限点" :value="permissions.length" :icon="Files" :loading="loading" />
    </div>

    <TableShell
      :loading="loading"
      :empty="roles.length === 0"
      empty-title="暂无角色"
      empty-hint="当前没有可展示的角色数据；请确认后台初始化数据和当前账号权限。"
    >
      <el-table class="admin-data-table" style="min-width: 900px" size="small" :data="roles">
        <el-table-column prop="roleId" label="ID" width="80" />
        <el-table-column label="角色" min-width="190">
          <template #default="{ row }">
            <div class="role-cell"><strong>{{ row.roleName }}</strong><span>{{ row.roleCode }}</span></div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="权限数" width="100">
          <template #default="{ row }">{{ (row.permissions || []).length }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <StatusTag :status="Boolean(row.enabled)" :label="row.enabled ? '启用' : '停用'" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('role:update')" link type="primary" @click="openDialog(row)">分配权限</el-button>
          </template>
        </el-table-column>
      </el-table>
    </TableShell>

    <el-dialog v-model="visible" title="分配权限" :width="permissionDialogWidth" @closed="currentRole = null">
      <div v-if="currentRole" class="role-dialog-tip">
        为 <strong>{{ currentRole.roleName }}</strong> 配置权限，保存后会影响该角色下所有用户。
      </div>
      <el-form label-position="top">
        <el-form-item label="权限集合">
          <div class="permission-groups">
            <section v-for="group in groupedPermissions" :key="group.resourceType" class="permission-group">
              <div class="permission-group-title"><strong>{{ group.resourceType || '未分组' }}</strong><span>{{ group.items.length }} 项</span></div>
              <el-checkbox-group v-model="selectedPermissionIds">
                <el-checkbox v-for="item in group.items" :key="item.permissionId" :label="item.permissionId">
                  {{ item.permissionName || item.permissionCode }}
                  <small>{{ item.permissionCode }}</small>
                </el-checkbox>
              </el-checkbox-group>
            </section>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!hasPermission('role:update')" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Files, User, Warning } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TableShell from '@/components/common/TableShell.vue'
import { useAuthStore } from '@/stores/auth'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import { getAdminRoleListApi, updateRolePermissionsApi } from '@/api/adminRole'
import { getAdminPermissionListApi } from '@/api/adminPermission'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const roles = ref<any[]>([])
const permissions = ref<any[]>([])
const visible = ref(false)
const { responsiveDialogWidth } = useResponsiveSize()
const permissionDialogWidth = responsiveDialogWidth('620px')
const currentRole = ref<any>(null)
const selectedPermissionIds = ref<number[]>([])
let active = true
let loadRequestId = 0

const enabledCount = computed(() => roles.value.filter((item) => item.enabled).length)
const disabledCount = computed(() => roles.value.filter((item) => !item.enabled).length)
const groupedPermissions = computed(() => {
  const map = new Map<string, any[]>()
  for (const permission of permissions.value) {
    const key = permission.resourceType || '未分组'
    map.set(key, [...(map.get(key) || []), permission])
  }
  return Array.from(map.entries()).map(([resourceType, items]) => ({ resourceType, items }))
})

async function loadData() {
  const requestId = ++loadRequestId
  loading.value = true
  try {
    const roleRes: any = await getAdminRoleListApi()
    const permissionRes: any = await getAdminPermissionListApi()
    if (!active || requestId !== loadRequestId) return
    roles.value = roleRes || []
    permissions.value = permissionRes || []
  } catch (error: any) {
    if (active && requestId === loadRequestId) ElMessage.error(error?.message || '角色与权限数据加载失败')
  } finally {
    if (active && requestId === loadRequestId) loading.value = false
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
  if (!currentRole.value || saving.value) return
  const roleId = currentRole.value.roleId
  saving.value = true
  try {
    await updateRolePermissionsApi(roleId, { permissionIds: selectedPermissionIds.value })
    if (!active) return
    ElMessage.success('保存成功')
    visible.value = false
    await loadData()
  } finally {
    if (active) saving.value = false
  }
}

function hasPermission(permission: string) { return auth.hasPermission(permission) }

onMounted(loadData)
onBeforeUnmount(() => {
  active = false
  loadRequestId += 1
})
</script>

<style scoped>
.compact-stats { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.role-cell { display: grid; gap: 4px; }
.role-cell span { color: var(--color-text-muted); font-size: 12px; }
.role-dialog-tip { margin-bottom: var(--space-3); padding: 10px 12px; border: 1px solid var(--color-primary-border); border-radius: var(--radius-md); background: var(--color-primary-soft); color: var(--color-text-muted); }
.permission-groups { display: grid; width: 100%; max-height: 58vh; gap: var(--space-3); overflow: auto; }
.permission-group { padding: var(--space-3); border: 1px solid var(--color-border-soft); border-radius: var(--radius-md); background: var(--color-surface-muted); }
.permission-group-title { display: flex; justify-content: space-between; margin-bottom: var(--space-2); }
.permission-group-title span, .permission-group small { color: var(--color-text-muted); font-size: 12px; }
.permission-group :deep(.el-checkbox-group) { display: grid; gap: var(--space-2); }
.permission-group small { display: block; }

@media (max-width: 900px) {
  .compact-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 520px) {
  .compact-stats { grid-template-columns: 1fr; }
}
</style>
