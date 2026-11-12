<template>
  <nav class="journey-nav" aria-label="求职旅程">
    <RouterLink
      v-for="(item, index) in items"
      :key="item.key"
      class="journey-nav-link"
      :class="{ 'is-active': item.key === activeKey }"
      :to="item.path"
      :aria-current="item.key === activeKey ? 'page' : undefined"
    >
      <span class="journey-nav-step" aria-hidden="true">{{ index + 1 }}</span>
      <span>{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { resolveJourneyKey, type JourneyItem } from '@/config/navigation'

defineProps<{ items: JourneyItem[] }>()

const route = useRoute()
const activeKey = computed(() => resolveJourneyKey(route.path))
</script>

<style scoped>
.journey-nav {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: var(--space-1);
}

.journey-nav-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  min-height: 40px;
  padding: 0 var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--color-border-strong);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  transition:
    background-color var(--motion-fast),
    color var(--motion-fast);
}

.journey-nav-link:hover {
  background: var(--color-nav-soft);
  color: var(--color-surface);
}

.journey-nav-link:focus-visible {
  outline: 2px solid var(--color-primary-border);
  outline-offset: 2px;
}

.journey-nav-link.is-active {
  background: var(--color-primary-hover);
  color: var(--color-surface);
}

.journey-nav-step {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border: 1px solid currentColor;
  border-radius: 50%;
  font-size: 11px;
  line-height: 1;
}

@media (max-width: 1100px) {
  .journey-nav-link {
    padding-right: var(--space-2);
    padding-left: var(--space-2);
  }

  .journey-nav-step {
    display: none;
  }
}
</style>
