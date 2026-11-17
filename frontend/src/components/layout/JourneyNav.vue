<template>
  <nav class="journey-nav" aria-label="求职旅程">
    <RouterLink
      v-for="item in items"
      :key="item.key"
      class="journey-nav-link"
      :class="{ 'is-active': item.key === activeKey }"
      :to="item.path"
      :aria-current="item.key === activeKey ? 'page' : undefined"
    >
      <el-icon aria-hidden="true"><component :is="icons[item.key]" /></el-icon>
      <span>{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { House, Document, Suitcase, MagicStick, ChatDotRound, Promotion } from '@element-plus/icons-vue'
import { resolveJourneyKey, type JourneyItem } from '@/config/navigation'

defineProps<{ items: JourneyItem[] }>()

const icons = {
  dashboard: House,
  resumes: Document,
  jobs: Suitcase,
  analysis: MagicStick,
  interview: ChatDotRound,
  applications: Promotion
}

const route = useRoute()
const activeKey = computed(() => resolveJourneyKey(route.path))
</script>

<style scoped>
.journey-nav {
  display: grid;
  gap: var(--space-1);
}

.journey-nav-link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: 44px;
  padding: 0 var(--space-3);
  border-radius: var(--radius-sm);
  border-inline-start: 3px solid transparent;
  color: var(--color-text-muted);
  font-size: 14px;
  font-weight: 600;
  transition: background-color var(--motion-fast), color var(--motion-fast);
}

.journey-nav-link .el-icon {
  font-size: 18px;
}

.journey-nav-link:hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.journey-nav-link.is-active {
  border-inline-start-color: var(--color-primary-hover);
  background: var(--color-primary-subtle);
  color: var(--color-primary-hover);
}

.journey-nav-link:focus-visible {
  outline: 2px solid var(--color-primary-hover);
  outline-offset: 2px;
}
</style>
