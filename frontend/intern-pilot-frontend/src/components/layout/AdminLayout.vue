<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand">
        <div class="brand-mark">IP</div>
        <div>
          <strong>管理后台</strong>
          <span>系统配置与运营管理</span>
        </div>
      </div>

      <el-menu :default-active="route.path" class="sidebar-menu">
        <el-menu-item v-if="hasPermission('admin:dashboard')" index="/admin/dashboard" @click="go('/admin/dashboard')">
          <el-icon><DataBoard /></el-icon>
          <span>后台看板</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('user:read')" index="/admin/users" @click="go('/admin/users')">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('role:read')" index="/admin/roles" @click="go('/admin/roles')">
          <el-icon><Avatar /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('permission:read')" index="/admin/permissions" @click="go('/admin/permissions')">
          <el-icon><Lock /></el-icon>
          <span>权限管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('operation-log:read')" index="/admin/operation-logs" @click="go('/admin/operation-logs')">
          <el-icon><Memo /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('rag:read')" index="/admin/rag-knowledge" @click="go('/admin/rag-knowledge')">
          <el-icon><Tickets /></el-icon>
          <span>RAG 知识库</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div>
          <span class="eyebrow">Admin Console</span>
          <h2>管理后台</h2>
        </div>
        <div class="admin-header-actions">
          <el-button :icon="Refresh" circle @click="refreshPage" />
          <el-button :icon="Back" @click="router.push('/dashboard')">返回用户工作台</el-button>
        </div>
      </header>

      <router-view :key="refreshKey" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Avatar, Back, DataBoard, Lock, Memo, Refresh, Tickets, User } from '@element-plus/icons-vue'
import { getCurrentUserApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const refreshKey = ref(0)

function hasPermission(permission: string) {
  return auth.hasPermission(permission)
}

function go(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}

function refreshPage() {
  refreshKey.value += 1
}

onMounted(async () => {
  try {
    auth.setUser(await getCurrentUserApi())
  } catch {
    // Request interceptor handles invalid sessions.
  }
})
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  min-height: 100vh;
  background: var(--color-bg);
}

.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  border-right: 1px solid var(--color-border);
  background: var(--color-surface);
}

.admin-main {
  min-width: 0;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 28px;
  border-bottom: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
}

.admin-header h2 {
  margin: 2px 0 0;
  font-size: 20px;
  font-weight: 700;
}

.admin-header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

@media (max-width: 900px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: static;
    height: auto;
  }

  .admin-header {
    align-items: flex-start;
    flex-direction: column;
    height: auto;
    gap: 12px;
    padding: 16px 20px;
  }
}
</style>
