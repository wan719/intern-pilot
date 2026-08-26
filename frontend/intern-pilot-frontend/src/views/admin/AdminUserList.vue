<template>
  <PageContainer width="wide" title="用户管理" description="查询用户、查看账号状态，并维护用户角色。后台默认展示昵称，账号唯一性以 ID 为准。">
    <div class="stat-grid compact-stats">
      <StatCard label="筛选结果" :value="total" :icon="User" :loading="loading" />
      <StatCard label="本页启用" :value="enabledCount" :icon="CircleCheck" :loading="loading" />
      <StatCard label="本页禁用" :value="disabledCount" :icon="Warning" :loading="loading" />
      <StatCard label="角色数量" :value="roles.length" :icon="Files" />
    </div>

    <FilterBar @reset="resetQuery">
      <template #filters>
        <el-input v-model="query.keyword" aria-label="用户关键词" placeholder="昵称 / 用户名 / 邮箱" clearable @keyup.enter="search" />
        <el-input v-model="query.roleCode" aria-label="角色编码" placeholder="角色编码，例如 ADMIN" clearable @keyup.enter="search" />
        <el-select v-model="query.enabled" aria-label="账号状态" placeholder="状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" :loading="loading" @click="search">查询</el-button>
      </template>
    </FilterBar>

    <div v-if="canReadRoles && canUpdateUsers && roleOptionsState === 'error'" class="auxiliary-error" data-user-role-options-error>
      <el-alert title="角色选项加载失败，暂时无法分配角色" type="error" :closable="false" show-icon />
      <el-button @click="loadRoles">重试角色选项</el-button>
    </div>

    <TableShell
      :loading="loading"
      :empty="users.length === 0"
      empty-title="暂无用户"
      empty-hint="当前筛选条件下没有用户记录；可以重置筛选条件，或确认当前账号是否拥有用户读取权限。"
    >
      <el-table class="admin-data-table" style="min-width: 1420px" size="small" :data="users">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column label="用户" min-width="210">
          <template #default="{ row }">
            <div class="user-cell">
              <strong>{{ row.nickname || row.username || '-' }}</strong>
              <span v-if="row.username && row.nickname !== row.username">用户名：{{ row.username }}</span>
              <span v-if="row.email">{{ row.email }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="school" label="学校" min-width="140" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="140" show-overflow-tooltip />
        <el-table-column prop="grade" label="年级" width="100" show-overflow-tooltip />
        <el-table-column label="角色" min-width="190">
          <template #default="{ row }">
            <div class="role-tags">
              <el-tag v-for="role in row.roles || []" :key="role" :type="role === 'ADMIN' ? 'warning' : 'primary'" effect="plain">
                {{ role }}
              </el-tag>
              <span v-if="!(row.roles || []).length" class="muted">暂无角色</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <StatusTag :status="row.enabled" :label="row.enabled === 1 ? '启用' : '禁用'" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.userId)">详情</el-button>
            <el-button v-if="canAssignRoles" link type="warning" @click="openRoleDialog(row)">分配角色</el-button>
            <el-button
              v-if="hasPermission('user:update') && row.enabled === 1"
              link
              type="danger"
              :loading="pendingUserIds.has(row.userId)"
              @click="changeEnabled(row, false)"
            >禁用</el-button>
            <el-button
              v-else-if="hasPermission('user:update')"
              link
              type="success"
              :loading="pendingUserIds.has(row.userId)"
              @click="changeEnabled(row, true)"
            >启用</el-button>
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

    <el-drawer v-model="detailVisible" title="用户详情" :size="detailDrawerSize" @closed="invalidateDetail">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else-if="detail" class="detail-stack">
        <section class="panel flat">
          <div class="profile-head">
            <div class="avatar">{{ firstLetter(detail.nickname || detail.username || detail.email) }}</div>
            <div>
              <h2>{{ detail.nickname || detail.username || '-' }}</h2>
              <span>ID：{{ detail.userId }} · {{ detail.email || '未填写邮箱' }}</span>
            </div>
            <StatusTag :status="detail.enabled" :label="detail.enabled === 1 ? '启用' : '禁用'" />
          </div>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h2>账号信息</h2>
            <span>登录用户名保留为内部账号字段</span>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="昵称">{{ detail.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="登录用户名">{{ detail.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="学校">{{ detail.school || '-' }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ detail.major || '-' }}</el-descriptions-item>
            <el-descriptions-item label="年级">{{ detail.grade || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h2>角色与资产</h2>
            <span>{{ (detail.permissions || []).length }} 个权限点</span>
          </div>
          <div class="role-tags detail-tags">
            <el-tag v-for="role in detail.roles || []" :key="role" :type="role === 'ADMIN' ? 'warning' : 'primary'" effect="plain">{{ role }}</el-tag>
            <span v-if="!(detail.roles || []).length" class="muted">暂无角色</span>
          </div>
          <div class="asset-grid">
            <div><strong>{{ detail.resumeCount || 0 }}</strong><span>简历</span></div>
            <div><strong>{{ detail.jobCount || 0 }}</strong><span>岗位</span></div>
            <div><strong>{{ detail.analysisReportCount || 0 }}</strong><span>分析报告</span></div>
            <div><strong>{{ detail.applicationCount || 0 }}</strong><span>投递记录</span></div>
          </div>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="roleDialogVisible" title="分配角色" :width="roleDialogWidth" @closed="currentUser = null">
      <div class="role-dialog-tip">
        正在为 <strong>{{ currentUser?.nickname || currentUser?.username }}</strong> 分配角色。请谨慎授予 ADMIN。
      </div>
      <el-form label-position="top">
        <el-form-item label="可分配角色">
          <el-checkbox-group v-model="selectedRoleIds" class="role-checks">
            <el-checkbox v-for="role in roles" :key="role.roleId" :label="role.roleId">
              {{ role.roleCode }} - {{ role.roleName }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRoles" :disabled="!canAssignRoles" @click="submitRoles">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Files, User, Warning } from '@element-plus/icons-vue'
import FilterBar from '@/components/common/FilterBar.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TableShell from '@/components/common/TableShell.vue'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import { useAuthStore } from '@/stores/auth'
import { disableUserApi, enableUserApi, getAdminUserDetailApi, getAdminUserListApi, updateUserRolesApi } from '@/api/adminUser'
import { getAdminRoleListApi } from '@/api/adminRole'

const auth = useAuthStore()
const loading = ref(false)
const detailLoading = ref(false)
const savingRoles = ref(false)
const users = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<any>(null)
const roleDialogVisible = ref(false)
const { responsiveDialogWidth, responsiveDrawerSize } = useResponsiveSize()
const detailDrawerSize = responsiveDrawerSize('48%')
const roleDialogWidth = responsiveDialogWidth('440px')
const selectedRoleIds = ref<number[]>([])
const currentUser = ref<any>(null)
const roles = ref<any[]>([])
const roleOptionsState = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
const pendingUserIds = ref(new Set<number>())
let active = true
let listRequestId = 0
let roleRequestId = 0
let detailRequestId = 0

const query = reactive<any>({ keyword: '', roleCode: '', enabled: undefined, pageNum: 1, pageSize: 10 })
const enabledCount = computed(() => users.value.filter((item) => item.enabled === 1).length)
const disabledCount = computed(() => users.value.filter((item) => item.enabled !== 1).length)
const canReadRoles = computed(() => hasPermission('role:read'))
const canUpdateUsers = computed(() => hasPermission('user:update'))
const canAssignRoles = computed(() => canUpdateUsers.value && canReadRoles.value && roleOptionsState.value === 'success')

watch([canUpdateUsers, canReadRoles], ([canUpdate, canRead]) => {
  if (!canUpdate || !canRead) closeRoleDialog()
})

async function loadList() {
  const requestId = ++listRequestId
  loading.value = true
  try {
    const res: any = await getAdminUserListApi({ ...query })
    if (!active || requestId !== listRequestId) return
    users.value = res.records || []
    total.value = res.total || 0
  } catch (error: any) {
    if (!active || requestId !== listRequestId) return
    users.value = []
    total.value = 0
    ElMessage.error(error?.message || '用户列表加载失败，请稍后重试')
  } finally {
    if (active && requestId === listRequestId) loading.value = false
  }
}

async function loadRoles() {
  const requestId = ++roleRequestId
  if (!hasPermission('role:read')) {
    roles.value = []
    roleOptionsState.value = 'idle'
    closeRoleDialog()
    return
  }
  roleOptionsState.value = 'loading'
  try {
    const roleRes: any = await getAdminRoleListApi()
    if (!active || requestId !== roleRequestId) return
    if (!hasPermission('role:read')) {
      roles.value = []
      roleOptionsState.value = 'idle'
      closeRoleDialog()
      return
    }
    roles.value = roleRes || []
    roleOptionsState.value = 'success'
  } catch (error: any) {
    if (!active || requestId !== roleRequestId) return
    roles.value = []
    roleOptionsState.value = 'error'
    closeRoleDialog()
    ElMessage.error(error?.message || '角色列表加载失败')
  }
}

function search() { query.pageNum = 1; return loadList() }
function resetQuery() {
  Object.assign(query, { keyword: '', roleCode: '', enabled: undefined, pageNum: 1 })
  return loadList()
}
function handlePageChange(page: number) { query.pageNum = page; return loadList() }
function handleSizeChange(size: number) { query.pageSize = size; query.pageNum = 1; return loadList() }

async function showDetail(id: number) {
  const requestId = ++detailRequestId
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const result = await getAdminUserDetailApi(id)
    if (active && requestId === detailRequestId && detailVisible.value) detail.value = result
  } catch (error: any) {
    if (active && requestId === detailRequestId) ElMessage.error(error?.message || '用户详情加载失败')
  } finally {
    if (active && requestId === detailRequestId) detailLoading.value = false
  }
}

function invalidateDetail() {
  detailRequestId += 1
  detailLoading.value = false
  detail.value = null
}

function setUserPending(id: number, pending: boolean) {
  const next = new Set(pendingUserIds.value)
  if (pending) next.add(id)
  else next.delete(id)
  pendingUserIds.value = next
}

function isConfirmationDismissed(reason: unknown) {
  return reason === 'cancel' || reason === 'close'
}

async function changeEnabled(row: any, enable: boolean) {
  if (!hasPermission('user:update')) return
  if (pendingUserIds.value.has(row.userId)) return
  setUserPending(row.userId, true)
  try {
    const name = row.nickname || row.username || `ID ${row.userId}`
    try {
      await ElMessageBox.confirm(`确认${enable ? '启用' : '禁用'}用户 ${name}？`, '确认操作', { type: 'warning' })
    } catch (reason) {
      if (isConfirmationDismissed(reason)) return
      throw reason
    }
    if (!hasPermission('user:update')) return
    if (enable) await enableUserApi(row.userId)
    else await disableUserApi(row.userId)
    if (!active) return
    ElMessage.success('操作成功')
    await loadList()
  } finally {
    if (active) setUserPending(row.userId, false)
  }
}

function closeRoleDialog() {
  roleDialogVisible.value = false
  currentUser.value = null
  selectedRoleIds.value = []
}

function openRoleDialog(row: any) {
  if (!canAssignRoles.value) {
    closeRoleDialog()
    return
  }
  currentUser.value = row
  selectedRoleIds.value = roles.value.filter((role) => (row.roles || []).includes(role.roleCode)).map((role) => role.roleId)
  roleDialogVisible.value = true
}

async function submitRoles() {
  if (!canAssignRoles.value) {
    closeRoleDialog()
    return
  }
  if (!currentUser.value || savingRoles.value) return
  savingRoles.value = true
  const userId = currentUser.value.userId
  try {
    await updateUserRolesApi(userId, { roleIds: selectedRoleIds.value })
    if (!active) return
    ElMessage.success('保存成功')
    roleDialogVisible.value = false
    await loadList()
  } finally {
    if (active) savingRoles.value = false
  }
}

function hasPermission(permission: string) { return auth.hasPermission(permission) }
function firstLetter(value?: string) { return value?.trim()?.slice(0, 1)?.toUpperCase() || 'U' }

onMounted(() => { void Promise.all([loadRoles(), loadList()]) })
onBeforeUnmount(() => {
  active = false
  listRequestId += 1
  roleRequestId += 1
  detailRequestId += 1
})
</script>

<style scoped>
.compact-stats { grid-template-columns: repeat(4, minmax(0, 1fr)); }
:deep(.filter-bar__filters .el-input), :deep(.filter-bar__filters .el-select) { width: min(230px, 100%); }
.user-cell { display: grid; min-width: 0; gap: 4px; }
.user-cell strong, .user-cell span { overflow-wrap: anywhere; }
.user-cell span, .muted { color: var(--color-text-soft); font-size: 12px; }
.role-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.detail-tags { margin-bottom: var(--space-3); }
.pager { justify-content: flex-end; padding: var(--space-3); border-top: 1px solid var(--color-border-soft); }
.profile-head { display: grid; grid-template-columns: 48px minmax(0, 1fr) auto; gap: var(--space-3); align-items: center; }
.profile-head h2, .panel-header h2 { margin: 0; font-size: 16px; }
.profile-head span { color: var(--color-text-muted); }
.avatar { display: grid; width: 48px; height: 48px; place-items: center; border-radius: var(--radius-md); background: var(--color-primary-soft); color: var(--color-primary); font-size: 20px; font-weight: 800; }
.asset-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-2); }
.asset-grid div { padding: var(--space-3); border: 1px solid var(--color-border-soft); border-radius: var(--radius-md); background: var(--color-surface); }
.asset-grid strong, .asset-grid span { display: block; }
.asset-grid strong { font-variant-numeric: tabular-nums; font-size: 22px; }
.asset-grid span { margin-top: var(--space-1); color: var(--color-text-muted); font-size: 12px; }
.role-dialog-tip { margin-bottom: var(--space-3); padding: 10px 12px; border: 1px solid var(--color-primary-border); border-radius: var(--radius-md); background: var(--color-primary-soft); color: var(--color-text-muted); }
.role-checks { display: grid; gap: var(--space-2); }
.auxiliary-error { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); margin-bottom: var(--space-4); }
.auxiliary-error .el-alert { flex: 1; }

@media (max-width: 900px) {
  .compact-stats, .asset-grid, .profile-head { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 520px) {
  .compact-stats, .asset-grid, .profile-head { grid-template-columns: 1fr; }
  .auxiliary-error { align-items: stretch; flex-direction: column; }
  .pager { justify-content: flex-start; overflow-x: auto; }
}
</style>
