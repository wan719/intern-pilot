<template>
  <PageContainer title="用户管理" description="查询用户、启用禁用用户并维护用户角色。">
    <section class="panel toolbar">
      <el-input v-model="query.keyword" placeholder="用户名 / 邮箱" clearable />
      <el-input v-model="query.roleCode" placeholder="角色编码，如 ADMIN" clearable />
      <el-select v-model="query.enabled" placeholder="状态" clearable>
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="users">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="school" label="学校" min-width="130" />
        <el-table-column prop="major" label="专业" min-width="120" />
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }">{{ (row.roles || []).join(', ') || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'danger'">
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

    <el-drawer v-model="detailVisible" title="用户详情" size="45%">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ (detail.roles || []).join(', ') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="权限数">{{ (detail.permissions || []).length }}</el-descriptions-item>
        <el-descriptions-item label="简历数">{{ detail.resumeCount }}</el-descriptions-item>
        <el-descriptions-item label="岗位数">{{ detail.jobCount }}</el-descriptions-item>
        <el-descriptions-item label="分析报告数">{{ detail.analysisReportCount }}</el-descriptions-item>
        <el-descriptions-item label="投递记录数">{{ detail.applicationCount }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="420px">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="role in roles" :key="role.roleId" :label="role.roleId">
          {{ role.roleCode }} - {{ role.roleName }}
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!hasPermission('user:update')" @click="submitRoles">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
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

async function loadList() {
  loading.value = true
  try {
    const res: any = await getAdminUserListApi({ ...query })
    users.value = res.records || []
    total.value = res.total || 0
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
  detail.value = await getAdminUserDetailApi(id)
  detailVisible.value = true
}

async function changeEnabled(row: any, enable: boolean) {
  await ElMessageBox.confirm(`确认${enable ? '启用' : '禁用'}用户 ${row.username}？`, '确认操作', { type: 'warning' })
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
  await updateUserRolesApi(currentUser.value.userId, { roleIds: selectedRoleIds.value })
  ElMessage.success('保存成功')
  roleDialogVisible.value = false
  loadList()
}

function hasPermission(permission: string) {
  return auth.hasPermission(permission)
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadList()])
})
</script>

<style scoped>
.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
