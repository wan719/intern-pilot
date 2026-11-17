<template>
  <nav class="admin-sidebar-nav" aria-label="后台导航">
    <section v-for="group in visibleGroups" :key="group.label" class="admin-nav-group">
      <h2 class="admin-nav-group-label">{{ group.label }}</h2>
      <div class="admin-nav-items">
        <RouterLink
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          class="admin-nav-item"
          :class="{ 'is-active': route.path === item.path }"
          :aria-current="route.path === item.path ? 'page' : undefined"
        >
          <el-icon v-if="navIcons[item.path]" aria-hidden="true">
            <component :is="navIcons[item.path]" />
          </el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </div>
    </section>
  </nav>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { ChatLineSquare, DataBoard, Document, Key, List, Lock, User } from '@element-plus/icons-vue'
import type { AdminNavGroup } from '@/config/navigation'

const props = defineProps<{
  groups: AdminNavGroup[]
  can: (permission: string) => boolean
}>()

const route = useRoute()
const navIcons: Record<string, Component> = {
  '/admin/dashboard': DataBoard,
  '/admin/users': User,
  '/admin/roles': Key,
  '/admin/permissions': Lock,
  '/admin/rag-knowledge': Document,
  '/admin/operation-logs': List,
  '/admin/feedback': ChatLineSquare
}

const visibleGroups = computed(() =>
  props.groups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => props.can(item.permission))
    }))
    .filter((group) => group.items.length > 0)
)
</script>

<style scoped>
.admin-sidebar-nav {
  display: grid;
  align-content: start;
  gap: var(--space-5);
}

.admin-nav-group {
  display: grid;
  gap: var(--space-2);
}

.admin-nav-group-label {
  margin: 0;
  padding: 0 var(--space-3);
  color: #a9b9cb;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.admin-nav-items {
  display: grid;
  gap: var(--space-1);
}

.admin-nav-item {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: var(--space-3);
  border-radius: var(--radius-sm);
  color: color-mix(in srgb, var(--color-surface) 90%, transparent);
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  padding: var(--space-2) var(--space-3);
  text-decoration: none;
  transition: background-color var(--motion-fast), color var(--motion-fast);
}

.admin-nav-item .el-icon {
  flex: none;
  font-size: 17px;
}

.admin-nav-item:hover {
  background: var(--color-nav-soft);
  color: var(--color-surface);
}

.admin-nav-item.is-active {
  background: var(--color-primary-hover);
  color: var(--color-surface);
}

.admin-nav-item:focus-visible {
  outline: 3px solid var(--color-primary-border);
  outline-offset: 2px;
}
</style>
