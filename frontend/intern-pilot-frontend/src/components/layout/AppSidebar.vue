<template>
  <aside class="app-sidebar">
    <div class="brand">
      <div class="brand-mark">IP</div>
      <div>
        <strong>InternPilot</strong>
        <span>AI 求职工作台</span>
      </div>
    </div>

    <el-menu
      :default-active="route.path"
      :default-openeds="['analysis-group', 'admin-group']"
      class="sidebar-menu"
    >
      <el-menu-item index="/dashboard" @click="go('/dashboard')">
        <el-icon><DataBoard /></el-icon>
        <span>数据看板</span>
      </el-menu-item>
      <el-menu-item index="/resumes" @click="go('/resumes')">
        <el-icon><Document /></el-icon>
        <span>简历管理</span>
      </el-menu-item>
      <el-menu-item index="/jobs" @click="go('/jobs')">
        <el-icon><Briefcase /></el-icon>
        <span>岗位管理</span>
      </el-menu-item>

      <el-sub-menu index="analysis-group">
        <template #title>
          <div class="analysis-title">
            <el-icon><MagicStick /></el-icon>
            <span>AI 分析</span>
          </div>
        </template>
        <el-menu-item index="/analysis/match" @click="go('/analysis/match')">
          <el-icon><MagicStick /></el-icon>
          <span>匹配分析</span>
        </el-menu-item>
        <el-menu-item index="/analysis/reports" @click="go('/analysis/reports')">
          <el-icon><Tickets /></el-icon>
          <span>分析报告</span>
        </el-menu-item>
        <el-menu-item index="/interview-questions" @click="go('/interview-questions')">
          <el-icon><QuestionFilled /></el-icon>
          <span>AI 面试题</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/applications" @click="go('/applications')">
        <el-icon><List /></el-icon>
        <span>投递记录</span>
      </el-menu-item>

      <el-sub-menu v-if="showAdminGroup" index="admin-group">
        <template #title>
          <div class="analysis-title">
            <el-icon><Setting /></el-icon>
            <span>管理员后台</span>
          </div>
        </template>
        <el-menu-item v-if="hasPermission('dashboard:admin:read')" index="/admin/dashboard" @click="go('/admin/dashboard')">
          <el-icon><DataBoard /></el-icon>
          <span>后台看板</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('admin:user:read')" index="/admin/users" @click="go('/admin/users')">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('admin:role:read')" index="/admin/roles" @click="go('/admin/roles')">
          <el-icon><Avatar /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('admin:permission:read')" index="/admin/permissions" @click="go('/admin/permissions')">
          <el-icon><Lock /></el-icon>
          <span>权限管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('system:log:read')" index="/admin/operation-logs" @click="go('/admin/operation-logs')">
          <el-icon><Memo /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Avatar,
  Briefcase,
  DataBoard,
  Document,
  List,
  Lock,
  MagicStick,
  Memo,
  QuestionFilled,
  Setting,
  Tickets,
  User
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

function go(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}

function hasPermission(permission: string) {
  return auth.user?.permissions?.includes(permission)
}

const showAdminGroup = computed(() => {
  const keys = ['dashboard:admin:read', 'admin:user:read', 'admin:role:read', 'admin:permission:read', 'system:log:read']
  return keys.some((key) => hasPermission(key))
})
</script>

<style scoped>
.analysis-title {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}
</style>
