<template>
  <PageContainer width="wide" title="用户管理">
    <AdminResultTable
      label="用户列表"
      :rows="users"
      :loading="loading"
      :error="listError"
      error-title="用户列表加载失败"
      retry-label="重试用户列表"
      empty-title="暂无用户"
      empty-hint="当前筛选条件下没有用户记录。"
      @retry="loadList"
    >
      <template #filters>
        <FilterBar class="user-filters" @reset="resetQuery">
          <template #filters>
            <div class="user-filter-field user-filter-keyword">
              <label for="admin-users-keyword">关键词</label>
              <el-input id="admin-users-keyword" v-model="query.keyword" aria-label="用户关键词" placeholder="昵称 / 用户名 / 邮箱" clearable @keyup.enter="search" />
            </div>
            <div class="user-filter-field">
              <label for="admin-users-role">角色</label>
              <el-input id="admin-users-role" v-model="query.roleCode" aria-label="角色编码" placeholder="角色编码，例如 ADMIN" clearable @keyup.enter="search" />
            </div>
            <div class="user-filter-field user-filter-state">
              <label for="admin-users-state">状态</label>
              <el-select id="admin-users-state" v-model="query.enabled" aria-label="账号状态" placeholder="全部状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </div>
          </template>
          <template #actions>
            <el-button type="primary" :loading="loading" @click="search">查询</el-button>
          </template>
        </FilterBar>
      </template>
      <template #after-filters>
        <div v-if="canReadRoles && canUpdateUsers && roleOptionsState === 'error'" class="auxiliary-error" data-user-role-options-error>
          <el-alert title="角色选项加载失败，暂时无法分配角色" type="error" :closable="false" show-icon />
          <el-button @click="loadRoles">重试角色选项</el-button>
        </div>
      </template>
      <template #toolbar>
        <dl class="compact-stats" aria-label="用户筛选结果">
          <div><dt>筛选结果</dt><dd>{{ loading ? '—' : total }}</dd></div>
          <div><dt>本页启用</dt><dd>{{ loading ? '—' : enabledCount }}</dd></div>
          <div><dt>本页禁用</dt><dd>{{ loading ? '—' : disabledCount }}</dd></div>
          <div><dt>角色数量</dt><dd>{{ roleOptionsState === 'success' ? roles.length : '—' }}</dd></div>
        </dl>
      </template>
      <el-table-column prop="userId" label="ID" width="64" />
      <el-table-column label="用户" min-width="144">
        <template #default="{ row }">
          <div class="user-cell">
            <span class="user-initial" aria-hidden="true">{{ firstLetter(row.nickname || row.username || row.email) }}</span>
            <div class="user-cell-name">
              <strong :title="row.nickname || row.username">{{ row.nickname || row.username || '-' }}</strong>
              <span v-if="row.username && row.nickname !== row.username" :title="row.username">{{ row.username }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="164" show-overflow-tooltip />
      <el-table-column prop="school" label="学校" min-width="94" show-overflow-tooltip />
      <el-table-column prop="major" label="专业" min-width="94" show-overflow-tooltip />
      <el-table-column prop="grade" label="年级" width="62" show-overflow-tooltip />
      <el-table-column label="角色" min-width="102">
        <template #default="{ row }">
          <div class="role-tags">
            <el-tag v-for="role in row.roles || []" :key="role" :type="role === 'ADMIN' ? 'warning' : 'primary'" effect="plain" size="small">
              {{ role }}
            </el-tag>
            <span v-if="!(row.roles || []).length" class="muted">暂无角色</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="74">
        <template #default="{ row }">
          <StatusTag class="user-status" :status="row.enabled" :label="row.enabled === 1 ? '启用' : '禁用'" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="158">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="166">
        <template #default="{ row }">
          <div class="user-row-actions">
            <el-button link type="primary" @click="showDetail(row.userId)">详情</el-button>
            <el-button v-if="canAssignRoles" link type="primary" @click="openRoleDialog(row)">分配角色</el-button>
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
          </div>
        </template>
      </el-table-column>
      <template v-if="total > 0" #pagination>
        <el-config-provider :locale="zhCn">
          <el-pagination
            class="pager"
            background
            layout="total, sizes, ->, prev, pager, next"
            :pager-count="5"
            :total="total"
            :current-page="query.pageNum"
            :page-size="query.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </el-config-provider>
      </template>
    </AdminResultTable>

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
          <el-descriptions :column="isMobile ? 1 : 2" border>
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
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import FilterBar from '@/components/common/FilterBar.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import AdminResultTable from './AdminResultTable.vue'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import { useAuthStore } from '@/stores/auth'
import { disableUserApi, enableUserApi, getAdminUserDetailApi, getAdminUserListApi, updateUserRolesApi } from '@/api/adminUser'
import { getAdminRoleListApi } from '@/api/adminRole'

const auth = useAuthStore()
const loading = ref(false)
const listError = ref('')
const detailLoading = ref(false)
const savingRoles = ref(false)
const users = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<any>(null)
const roleDialogVisible = ref(false)
const { isMobile, responsiveDialogWidth, responsiveDrawerSize } = useResponsiveSize()
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
  listError.value = ''
  try {
    const res: any = await getAdminUserListApi({ ...query })
    if (!active || requestId !== listRequestId) return
    users.value = res.records || []
    total.value = res.total || 0
  } catch (error: any) {
    if (!active || requestId !== listRequestId) return
    users.value = []
    total.value = 0
    listError.value = error?.response?.data?.message || error?.message || '请稍后重试'
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
.compact-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-3); margin: 0; }
.compact-stats div { display: flex; flex-wrap: wrap; align-items: baseline; gap: var(--space-2); min-width: 0; }
.compact-stats dt { color: var(--color-text-muted); font-size: 12px; }
.compact-stats dd { margin: 0; color: var(--color-text); font-weight: 700; font-variant-numeric: tabular-nums; }
.user-filter-field {
  display: flex;
  flex: 1 1 190px;
  min-width: 0;
  align-items: center;
  gap: var(--space-2);
}
.user-filter-keyword { flex-basis: 240px; }
.user-filter-state { flex-basis: 140px; }
.user-filter-field label { flex: none; color: var(--color-text); font-size: 13px; font-weight: 650; }
.user-filter-field :is(.el-input, .el-select) { min-width: 0; }
.user-cell { display: flex; min-width: 0; align-items: center; gap: var(--space-2); }
.user-cell-name { display: grid; min-width: 0; gap: 2px; }
.user-cell-name > * { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-cell-name span, .muted { color: var(--color-text-soft); font-size: 12px; }
.user-initial {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  place-items: center;
  border-radius: 50%;
  background: var(--color-primary-soft);
  color: var(--color-primary-hover);
  font-weight: 700;
}
.role-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.user-row-actions { display: flex; flex-wrap: nowrap; align-items: center; gap: var(--space-2); }
.user-row-actions :deep(.el-button + .el-button) { margin-left: 0; }
.detail-tags { margin-bottom: var(--space-3); }
.user-status {
  border-color: transparent;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-success) 12%, var(--color-surface));
  color: var(--color-success);
}
.user-status.status-tag--info {
  background: var(--color-bg);
  color: var(--color-text-muted);
}
.profile-head { display: grid; grid-template-columns: 48px minmax(0, 1fr) auto; gap: var(--space-3); align-items: center; }
.profile-head h2, .panel-header h2 { margin: 0; font-size: 16px; }
.profile-head span { color: var(--color-text-muted); overflow-wrap: anywhere; }
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
  .compact-stats, .asset-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .user-filter-field label { min-width: 42px; }
}

@media (max-width: 520px) {
  .profile-head { grid-template-columns: 48px minmax(0, 1fr); }
  .profile-head .status-tag { grid-column: 2; justify-self: start; }
  .auxiliary-error { align-items: stretch; flex-direction: column; }
}
</style>
