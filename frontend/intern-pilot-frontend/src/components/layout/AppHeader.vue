<template>
  <header class="app-header">
    <div class="app-header-inner">
      <RouterLink class="header-brand" to="/dashboard" aria-label="InternPilot 工作台">
        <img :src="brandLogo" alt="" />
        <span>
          <strong>InternPilot</strong>
          <small>求职旅程</small>
        </span>
      </RouterLink>

      <JourneyNav :items="journeyItems" />

      <div class="header-user">
        <el-tag
          v-if="aiProvider"
          class="provider-tag"
          size="small"
          :type="aiProvider === 'deepseek' ? 'primary' : 'warning'"
          effect="dark"
        >
          {{ aiProvider === 'deepseek' ? 'DeepSeek' : 'Mock AI' }}
        </el-tag>

        <button class="header-action" type="button" :aria-label="aiTaskBadgeText" @click="aiTaskCenter.openDrawer">
          <el-icon><Cpu /></el-icon>
          <span class="header-action-label">AI 任务</span>
          <span v-if="aiTaskBadgeCount" class="header-action-badge">{{ aiTaskBadgeCount }}</span>
        </button>
        <button class="header-action" type="button" aria-label="打开意见反馈" @click="feedback.openDrawer">
          <el-icon><Message /></el-icon>
          <span class="header-action-label">反馈</span>
        </button>
        <button class="header-action header-action-icon" type="button" aria-label="刷新当前页面" @click="$emit('refresh')">
          <el-icon><Refresh /></el-icon>
        </button>

        <el-dropdown trigger="click" @command="handleCommand">
          <button class="account-trigger" type="button" aria-label="打开账号菜单">
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
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Cpu, Message, Refresh, Setting, User } from '@element-plus/icons-vue'
import brandLogo from '@/assets/brand-logo-optimized.png'
import { getAiProviderApi } from '@/api/health'
import { journeyItems } from '@/config/navigation'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'
import { useAuthStore } from '@/stores/auth'
import { useFeedbackStore } from '@/stores/feedback'
import JourneyNav from './JourneyNav.vue'

defineEmits<{ refresh: [] }>()

const router = useRouter()
const auth = useAuthStore()
const aiTaskCenter = useAiTaskCenterStore()
const feedback = useFeedbackStore()
const aiTaskBadgeCount = computed(() => aiTaskCenter.badgeCount + aiTaskCenter.cancelledTasks.length)
const aiTaskBadgeText = computed(() => {
  if (aiTaskCenter.runningTasks.length) return `AI 任务 ${aiTaskCenter.runningTasks.length} 个进行中`
  if (aiTaskCenter.failedTasks.length) return `有 ${aiTaskCenter.failedTasks.length} 个任务失败`
  if (aiTaskCenter.cancelledTasks.length) return `有 ${aiTaskCenter.cancelledTasks.length} 个任务已取消，待处理`
  if (aiTaskCenter.completedTasks.length) return `有 ${aiTaskCenter.completedTasks.length} 个结果可查看`
  return 'AI 任务中心'
})
const displayName = computed(() => auth.user?.nickname || auth.user?.username || auth.user?.email || '已登录用户')
const avatarUrl = computed(() => resolveAvatarUrl(auth.user?.avatarUrl))
const avatarText = computed(() => String(displayName.value || 'U').slice(0, 1).toUpperCase())
const accountSubtext = computed(() => {
  if (auth.user?.email) return auth.user.email
  if (auth.user?.roles?.length) return auth.user.roles.join(' / ')
  return '账号设置'
})
const adminPermissionKeys = ['admin:dashboard', 'user:read', 'role:read', 'permission:read', 'operation-log:read', 'rag:read', 'feedback:read']
const showAdminEntry = computed(() => adminPermissionKeys.some((key) => auth.hasPermission(key)))
const aiProvider = ref('')

function resolveAvatarUrl(url?: string) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
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
  if (auth.hasPermission('feedback:read')) return '/admin/feedback'
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
.app-header-inner {
  display: flex;
  width: min(1480px, 100%);
  min-width: 0;
  height: 100%;
  margin: 0 auto;
  align-items: center;
  gap: var(--space-5);
}

.header-brand {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-surface);
}

.header-brand img {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  object-fit: cover;
}

.header-brand span,
.header-brand strong,
.header-brand small {
  display: block;
}

.header-brand strong {
  font-size: 15px;
  letter-spacing: 0.01em;
}

.header-brand small {
  margin-top: 2px;
  color: var(--color-border-strong);
  font-size: 11px;
}

.journey-nav {
  flex: 1 1 auto;
  justify-content: center;
}

.header-user {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--space-2);
}

.provider-tag {
  flex: 0 0 auto;
}

.header-action {
  position: relative;
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  gap: var(--space-1);
  padding: 0 var(--space-3);
  border: 1px solid var(--color-nav-soft);
  border-radius: var(--radius-sm);
  background: var(--color-nav-soft);
  color: var(--color-surface);
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color var(--motion-fast),
    background-color var(--motion-fast);
}

.header-action:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-hover);
}

.header-action:focus-visible,
.account-trigger:focus-visible,
.header-brand:focus-visible {
  outline: 2px solid var(--color-primary-border);
  outline-offset: 2px;
}

.header-action-icon {
  width: 38px;
  justify-content: center;
  padding: 0;
}

.header-action-badge {
  display: grid;
  min-width: 18px;
  height: 18px;
  place-items: center;
  padding: 0 var(--space-1);
  border-radius: 999px;
  background: var(--color-danger);
  color: var(--color-surface);
  font-size: 10px;
}

.account-trigger {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  max-width: 220px;
  height: 40px;
  padding: var(--space-1) var(--space-2) var(--space-1) var(--space-1);
  border: 1px solid var(--color-nav-soft);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-surface);
  cursor: pointer;
  transition:
    background-color var(--motion-fast),
    border-color var(--motion-fast);
}

.account-trigger:hover {
  border-color: var(--color-primary);
  background: var(--color-nav-soft);
}

.account-avatar,
.account-card-avatar {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-surface);
  font-weight: 700;
}

.account-avatar {
  width: 30px;
  height: 30px;
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

.account-meta strong,
.account-meta small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-meta strong {
  max-width: 112px;
  font-size: 13px;
  font-weight: 600;
}

.account-meta small {
  max-width: 112px;
  margin-top: 2px;
  color: var(--color-border-strong);
  font-size: 11px;
}

.account-card {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  width: 240px;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.account-card strong,
.account-card p {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-card strong {
  display: block;
}

.account-card p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  font-size: 12px;
}

.account-dropdown :deep(.el-dropdown-menu__item) {
  gap: var(--space-2);
  height: 38px;
}

@media (max-width: 1320px) {
  .provider-tag,
  .header-action-label,
  .account-meta {
    display: none;
  }

  .header-action {
    width: 38px;
    justify-content: center;
    padding: 0;
  }

  .header-action-badge {
    position: absolute;
    top: -5px;
    right: -5px;
  }

  .account-trigger {
    width: 40px;
    justify-content: center;
    padding: var(--space-1);
  }

  .account-trigger > .el-icon {
    display: none;
  }
}

@media (max-width: 900px) {
  .app-header-inner {
    gap: var(--space-2);
  }

  .journey-nav {
    display: none;
  }

  .header-brand {
    margin-right: auto;
  }
}

@media (max-width: 420px) {
  .header-brand small {
    display: none;
  }

  .header-brand img {
    width: 32px;
    height: 32px;
  }

  .header-user {
    gap: var(--space-1);
  }

  .header-action,
  .account-trigger {
    width: 36px;
    min-height: 36px;
    height: 36px;
  }

  .account-avatar {
    width: 28px;
    height: 28px;
  }
}
</style>
