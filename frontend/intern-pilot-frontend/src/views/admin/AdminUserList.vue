<template>
  <PageContainer title="用户管理" description="查询用户、查看账号状态，并维护用户角色。后台默认展示昵称，账号唯一性以 ID 为准。">
    <div class="stat-grid compact-stats">
      <StatCard label="筛选结果" :value="total" :icon="User" />
      <StatCard label="本页启用" :value="enabledCount" :icon="CircleCheck" />
      <StatCard label="本页禁用" :value="disabledCount" :icon="Warning" />
      <StatCard label="角色数量" :value="roles.length" :icon="Files" />
    </div>

    <section class="panel toolbar">
      <el-input v-model="query.keyword" placeholder="昵称 / 用户名 / 邮箱" clearable />
      <el-input v-model="query.roleCode" placeholder="角色编码，例如 ADMIN" clearable />
      <el-select v-model="query.enabled" placeholder="状态" clearable>
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="users">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column label="用户" min-width="190">
          <template #default="{ row }">
            <div class="user-cell">
              <strong>{{ row.nickname || row.username || '-' }}</strong>
              <span v-if="row.username && row.nickname !== row.username">用户名：{{ row.username }}</span>
              <span v-if="row.email">{{ row.email }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="school" label="学校" min-width="130" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="130" show-overflow-tooltip />
        <el-table-column prop="grade" label="年级" width="100" show-overflow-tooltip />
        <el-table-column label="角色" min-width="180">
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
            <el-tag :type="row.enabled === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.userId)">详情</el-button>
            <el-button v-if="hasPermission('user:update')" link type="warning" @click="openRoleDialog(row)">
              分配角色
            </el-button>
            <el-button
              v-if="hasPermission('user:update') && row.enabled === 1"
              link
              type="danger"
              @click="changeEnabled(row, false)"
            >
              禁用
            </el-button>
            <el-button
              v-else-if="hasPermission('user:update')"
              link
              type="success"
              @click="changeEnabled(row, true)"
            >
              启用
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AppEmpty
            title="暂无用户"
            description="当前筛选条件下没有用户记录"
            hint="可以重置筛选条件，或确认当前账号是否拥有用户读取权限。"
          />
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

    <el-drawer v-model="detailVisible" title="用户详情" size="48%">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else-if="detail" class="detail-stack">
        <section class="panel flat">
          <div class="profile-head">
            <div class="avatar">{{ firstLetter(detail.nickname || detail.username || detail.email) }}</div>
            <div>
              <h3>{{ detail.nickname || detail.username || '-' }}</h3>
              <span>ID：{{ detail.userId }} · {{ detail.email || '未填写邮箱' }}</span>
            </div>
            <el-tag :type="detail.enabled === 1 ? 'success' : 'danger'" effect="plain">
              {{ detail.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h3>账号信息</h3>
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
            <h3>角色与资产</h3>
            <span>{{ (detail.permissions || []).length }} 个权限点</span>
          </div>
          <div class="role-tags detail-tags">
            <el-tag v-for="role in detail.roles || []" :key="role" :type="role === 'ADMIN' ? 'warning' : 'primary'" effect="plain">
              {{ role }}
            </el-tag>
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

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="440px">
      <div class="role-dialog-tip">
        正在为 <strong>{{ currentUser?.nickname || currentUser?.username }}</strong> 分配角色。请谨慎授予 ADMIN。
      </div>
      <el-checkbox-group v-model="selectedRoleIds" class="role-checks">
        <el-checkbox v-for="role in roles" :key="role.roleId" :label="role.roleId">
          {{ role.roleCode }} - {{ role.roleName }}
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRoles" :disabled="!hasPermission('user:update')" @click="submitRoles">
          保存
        </el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Files, User, Warning } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import { formatDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import {
  disableUserApi,
  enableUserApi,
  getAdminUserDetailApi,
  getAdminUserListApi,
  updateUserRolesApi
} from '@/api/adminUser'
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
const selectedRoleIds = ref<number[]>([])
const currentUser = ref<any>(null)
const roles = ref<any[]>([])

const query = reactive<any>({
  keyword: '',
  roleCode: '',
  enabled: undefined,
  pageNum: 1,
  pageSize: 10
})

const enabledCount = computed(() => users.value.filter((item) => item.enabled === 1).length)
const disabledCount = computed(() => users.value.filter((item) => item.enabled !== 1).length)

async function loadList() {
  loading.value = true
  try {
    const res: any = await getAdminUserListApi({ ...query })
    users.value = res.records || []
    total.value = res.total || 0
  } catch (error: any) {
    users.value = []
    total.value = 0
    ElMessage.error(error?.message || '用户列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  const roleRes: any = await getAdminRoleListApi()
  roles.value = roleRes || []
}

function search() {
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  query.keyword = ''
  query.roleCode = ''
  query.enabled = undefined
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

async function showDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getAdminUserDetailApi(id)
  } catch (error: any) {
    ElMessage.error(error?.message || '用户详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function changeEnabled(row: any, enable: boolean) {
  const name = row.nickname || row.username || `ID ${row.userId}`
  await ElMessageBox.confirm(`确认${enable ? '启用' : '禁用'}用户 ${name}？`, '确认操作', { type: 'warning' })
  if (enable) {
    await enableUserApi(row.userId)
  } else {
    await disableUserApi(row.userId)
  }
  ElMessage.success('操作成功')
  loadList()
}

function openRoleDialog(row: any) {
  currentUser.value = row
  selectedRoleIds.value = []
  for (const role of roles.value) {
    if ((row.roles || []).includes(role.roleCode)) {
      selectedRoleIds.value.push(role.roleId)
    }
  }
  roleDialogVisible.value = true
}

async function submitRoles() {
  if (!currentUser.value) return
  savingRoles.value = true
  try {
    await updateUserRolesApi(currentUser.value.userId, { roleIds: selectedRoleIds.value })
    ElMessage.success('保存成功')
    roleDialogVisible.value = false
    await loadList()
  } finally {
    savingRoles.value = false
  }
}

function hasPermission(permission: string) {
  return auth.hasPermission(permission)
}

function firstLetter(value?: string) {
  return value?.trim()?.slice(0, 1)?.toUpperCase() || 'U'
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadList()])
})
</script>

<style scoped>
.compact-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.user-cell {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.user-cell strong,
.user-cell span {
  overflow-wrap: anywhere;
}

.user-cell strong {
  color: var(--color-text);
  font-weight: 700;
}

.user-cell span,
.muted {
  color: var(--color-text-soft);
  font-size: 12px;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.detail-tags {
  margin-bottom: 14px;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}

.profile-head {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
}

.avatar {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 20px;
  font-weight: 800;
}

.profile-head h3 {
  margin: 0 0 4px;
}

.profile-head span {
  color: var(--color-text-muted);
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.asset-grid div {
  padding: 12px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.asset-grid strong,
.asset-grid span {
  display: block;
}

.asset-grid strong {
  font-size: 22px;
}

.asset-grid span {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 12px;
}

.role-dialog-tip {
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-text-muted);
}

.role-checks {
  display: grid;
  gap: 8px;
}

@media (max-width: 900px) {
  .compact-stats,
  .asset-grid,
  .profile-head {
    grid-template-columns: 1fr;
  }
}
</style>
