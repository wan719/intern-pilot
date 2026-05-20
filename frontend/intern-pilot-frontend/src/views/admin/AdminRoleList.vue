<template>
  <PageContainer title="角色管理" description="查看系统角色，并维护角色拥有的权限集合。">
    <div class="stat-grid compact-stats">
      <StatCard label="角色总数" :value="roles.length" :icon="User" />
      <StatCard label="启用角色" :value="enabledCount" :icon="CircleCheck" />
      <StatCard label="停用角色" :value="disabledCount" :icon="Warning" />
      <StatCard label="权限点" :value="permissions.length" :icon="Files" />
    </div>

    <section class="panel admin-table-panel">
      <el-table v-loading="loading" :data="roles">
        <el-table-column prop="roleId" label="ID" width="80" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <div class="role-cell">
              <strong>{{ row.roleName }}</strong>
              <span>{{ row.roleCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="权限数" width="100">
          <template #default="{ row }">{{ (row.permissions || []).length }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('role:update')" link type="primary" @click="openDialog(row)">
              分配权限
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AppEmpty
            title="暂无角色"
            description="当前没有可展示的角色数据"
            hint="请确认后台初始化数据和当前账号权限。"
          />
        </template>
      </el-table>

      <div v-loading="loading" class="mobile-card-list">
        <AppEmpty
          v-if="!roles.length && !loading"
          title="暂无角色"
          description="当前没有可展示的角色数据"
          hint="请确认后台初始化数据和当前账号权限。"
        />
        <article v-for="row in roles" v-else :key="row.roleId" class="mobile-card">
          <div class="mobile-card-head">
            <div>
              <span>角色 ID #{{ row.roleId }}</span>
              <h3>{{ row.roleName }}</h3>
              <p>{{ row.roleCode }}</p>
            </div>
            <el-tag :type="row.enabled ? 'success' : 'danger'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </div>
          <p class="mobile-card-desc">{{ row.description || '暂无描述' }}</p>
          <div class="mobile-meta-grid">
            <div><span>权限数</span><strong>{{ (row.permissions || []).length }}</strong></div>
          </div>
          <div class="mobile-actions">
            <el-button v-if="hasPermission('role:update')" type="primary" plain @click="openDialog(row)">
              分配权限
            </el-button>
          </div>
        </article>
      </div>
    </section>

    <el-dialog v-model="visible" title="分配权限" :width="permissionDialogWidth">
      <div v-if="currentRole" class="role-dialog-tip">
        为 <strong>{{ currentRole.roleName }}</strong> 配置权限，保存后会影响该角色下所有用户。
      </div>
      <div class="permission-groups">
        <section v-for="group in groupedPermissions" :key="group.resourceType" class="permission-group">
          <div class="permission-group-title">
            <strong>{{ group.resourceType || '未分组' }}</strong>
            <span>{{ group.items.length }} 项</span>
          </div>
          <el-checkbox-group v-model="selectedPermissionIds">
            <el-checkbox v-for="item in group.items" :key="item.permissionId" :label="item.permissionId">
              {{ item.permissionName || item.permissionCode }}
              <small>{{ item.permissionCode }}</small>
            </el-checkbox>
          </el-checkbox-group>
        </section>
      </div>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!hasPermission('role:update')" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Files, User, Warning } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
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
  saving.value = true
  try {
    await updateRolePermissionsApi(currentRole.value.roleId, { permissionIds: selectedPermissionIds.value })
    ElMessage.success('保存成功')
    visible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

function hasPermission(permission: string) {
  return auth.hasPermission(permission)
}

onMounted(loadData)
</script>

<style scoped>
.compact-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.role-cell {
  display: grid;
  gap: 4px;
}

.role-cell strong {
  color: var(--color-text);
}

.role-cell span {
  color: var(--color-text-muted);
  font-size: 12px;
}

.mobile-card-list {
  display: none;
}

.role-dialog-tip {
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-text-muted);
}

.permission-groups {
  display: grid;
  gap: 14px;
  max-height: 58vh;
  overflow: auto;
}

.permission-group {
  padding: 12px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.permission-group-title {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.permission-group-title span {
  color: var(--color-text-muted);
  font-size: 12px;
}

.permission-group :deep(.el-checkbox-group) {
  display: grid;
  gap: 8px;
}

.permission-group small {
  display: block;
  color: var(--color-text-soft);
  font-size: 12px;
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

  .mobile-meta-grid {
    display: grid;
    gap: 8px;
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
  }

  .mobile-meta-grid span {
    color: var(--color-text-soft);
    font-size: 12px;
  }

  .mobile-meta-grid strong {
    margin-top: 4px;
    color: var(--color-text);
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
  .mobile-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
