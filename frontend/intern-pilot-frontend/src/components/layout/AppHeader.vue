<template>
  <header class="app-header">
    <div>
      <span class="eyebrow">InternPilot</span>
      <h2>求职工作台</h2>
    </div>
    <div class="header-user">
      <el-tag v-if="aiProvider" size="small" :type="aiProvider === 'deepseek' ? 'primary' : 'warning'" effect="plain">
        {{ aiProvider === 'deepseek' ? 'DeepSeek' : 'Mock AI' }}
      </el-tag>
      <el-button :icon="Refresh" circle @click="$emit('refresh')" />
      <el-dropdown trigger="click" @command="handleCommand">
        <button class="account-trigger" type="button">
          <span class="account-avatar">
            <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像" />
            <span v-else>{{ avatarText }}</span>
          </span>
          <span class="account-meta">
            <strong>{{ displayName }}</strong>
            <small>{{ accountSubtext }}</small>
          </span>
          <el-icon><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu class="account-dropdown">
            <div class="account-card">
              <span class="account-card-avatar">
                <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像" />
                <span v-else>{{ avatarText }}</span>
              </span>
              <div>
                <strong>{{ displayName }}</strong>
                <p>{{ auth.user?.email || '未绑定邮箱' }}</p>
              </div>
            </div>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </el-dropdown-item>
            <el-dropdown-item v-if="showAdminEntry" command="admin">
              <el-icon><Setting /></el-icon>
              <span>管理后台</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Refresh, Setting, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getAiProviderApi } from '@/api/health'

defineEmits<{ refresh: [] }>()

const router = useRouter()
const auth = useAuthStore()
const displayName = computed(() => auth.user?.nickname || auth.user?.username || auth.user?.email || '已登录用户')
const avatarUrl = computed(() => resolveAvatarUrl(auth.user?.avatarUrl))
const avatarText = computed(() => String(displayName.value || 'U').slice(0, 1).toUpperCase())
const accountSubtext = computed(() => {
  if (auth.user?.email) {
    return auth.user.email
  }
  if (auth.user?.roles?.length) {
    return auth.user.roles.join(' / ')
  }
  return '账号设置'
})
const adminPermissionKeys = ['admin:dashboard', 'user:read', 'role:read', 'permission:read', 'operation-log:read', 'rag:read']
const showAdminEntry = computed(() => adminPermissionKeys.some((key) => auth.hasPermission(key)))
const aiProvider = ref('')

function resolveAvatarUrl(url?: string) {
  if (!url) {
    return ''
  }
  if (/^https?:\/\//i.test(url)) {
    return url
  }
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  return `${baseUrl}${url}`
}

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/user/center')
    return
  }
  if (command === 'admin') {
    router.push(resolveAdminHome())
    return
  }
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  }
}

function resolveAdminHome() {
  if (auth.hasPermission('admin:dashboard')) return '/admin/dashboard'
  if (auth.hasPermission('user:read')) return '/admin/users'
  if (auth.hasPermission('role:read')) return '/admin/roles'
  if (auth.hasPermission('permission:read')) return '/admin/permissions'
  if (auth.hasPermission('operation-log:read')) return '/admin/operation-logs'
  if (auth.hasPermission('rag:read')) return '/admin/rag-knowledge'
  return '/403'
}

onMounted(async () => {
  try {
    const res: any = await getAiProviderApi()
    aiProvider.value = res?.provider || ''
  } catch {
    aiProvider.value = ''
  }
})
</script>

<style scoped>
.account-trigger {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  max-width: 260px;
  height: 42px;
  padding: 4px 10px 4px 6px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text);
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.account-trigger:hover {
  border-color: var(--color-border);
  background: var(--color-surface-muted);
}

.account-avatar,
.account-card-avatar {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-weight: 700;
}

.account-avatar {
  width: 32px;
  height: 32px;
  font-size: 13px;
}

.account-card-avatar {
  width: 40px;
  height: 40px;
  font-size: 16px;
}

.account-avatar img,
.account-card-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.account-meta {
  display: grid;
  min-width: 0;
  text-align: left;
  line-height: 1.2;
}

.account-meta strong {
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-meta small {
  overflow: hidden;
  margin-top: 2px;
  color: var(--color-text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-card {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 240px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--color-border-soft);
}

.account-card strong {
  display: block;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-card p {
  max-width: 160px;
  margin: 4px 0 0;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-dropdown :deep(.el-dropdown-menu__item) {
  gap: 8px;
  height: 38px;
}

@media (max-width: 720px) {
  .account-meta {
    display: none;
  }

  .account-trigger {
    width: 42px;
    padding: 4px;
    justify-content: center;
  }

  .account-trigger .el-icon {
    display: none;
  }
}

</style>
