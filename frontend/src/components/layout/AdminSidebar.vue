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
          {{ item.label }}
        </RouterLink>
      </div>
    </section>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { AdminNavGroup } from '@/config/navigation'

const props = defineProps<{
  groups: AdminNavGroup[]
  can: (permission: string) => boolean
}>()

const route = useRoute()

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
  gap: var(--space-5);
}

.admin-nav-group {
  display: grid;
  gap: var(--space-2);
}

.admin-nav-group-label {
  margin: 0;
  padding: 0 var(--space-3);
  color: color-mix(in srgb, var(--color-surface) 78%, transparent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.admin-nav-items {
  display: grid;
  gap: var(--space-1);
}

.admin-nav-item {
  display: block;
  border-radius: var(--radius-sm);
  color: color-mix(in srgb, var(--color-surface) 90%, transparent);
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  padding: 10px var(--space-3);
  text-decoration: none;
  transition: background-color var(--motion-fast), color var(--motion-fast);
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
