<template>
  <nav class="mobile-bottom-nav" aria-label="移动端求职旅程">
    <RouterLink
      v-for="item in coreItems"
      :key="item.key"
      class="mobile-bottom-link"
      :class="{ 'is-active': item.key === activeKey }"
      :to="item.path"
      :aria-current="item.key === activeKey ? 'page' : undefined"
    >
      <span class="mobile-bottom-marker" aria-hidden="true"></span>
      <span>{{ item.shortLabel }}</span>
    </RouterLink>
    <button
      class="mobile-bottom-more"
      :class="{ 'is-active': secondaryActive }"
      type="button"
      aria-label="更多求职旅程入口"
      :aria-expanded="drawerVisible"
      aria-controls="mobile-journey-more"
      @click="drawerVisible = true"
    >
      <span class="mobile-more-dots" aria-hidden="true">•••</span>
      <span>更多</span>
    </button>
  </nav>

  <div v-if="drawerVisible" class="mobile-nav-overlay" @keydown.esc="closeDrawer">
    <button class="mobile-nav-backdrop" type="button" aria-label="关闭更多导航" @click="closeDrawer"></button>
    <section
      id="mobile-journey-more"
      class="mobile-nav-drawer"
      role="dialog"
      aria-modal="true"
      aria-labelledby="mobile-journey-title"
    >
      <div class="mobile-nav-drawer-header">
        <div>
          <span>完整旅程</span>
          <h2 id="mobile-journey-title">更多求职阶段</h2>
        </div>
        <button type="button" aria-label="关闭更多导航" @click="closeDrawer">×</button>
      </div>
      <div class="mobile-nav-secondary">
        <RouterLink
          v-for="item in secondaryItems"
          :key="item.key"
          :to="item.path"
          :aria-current="item.key === activeKey ? 'page' : undefined"
          @click="closeDrawer"
        >
          <span>{{ item.shortLabel }}</span>
          <strong>{{ item.label }}</strong>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { resolveJourneyKey, type JourneyItem } from '@/config/navigation'

const props = defineProps<{ items: JourneyItem[] }>()
const route = useRoute()
const drawerVisible = ref(false)
const coreItems = computed(() => props.items.filter((item) => item.mobile))
const secondaryItems = computed(() => props.items.filter((item) => !item.mobile))
const activeKey = computed(() => resolveJourneyKey(route.path))
const secondaryActive = computed(() => secondaryItems.value.some((item) => item.key === activeKey.value))

function closeDrawer() {
  drawerVisible.value = false
}

watch(() => route.fullPath, closeDrawer)
</script>

<style scoped>
.mobile-bottom-nav,
.mobile-nav-overlay {
  display: none;
}

@media (max-width: 900px) {
  .mobile-bottom-nav {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 40;
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    padding: var(--space-2) var(--space-2) calc(var(--space-2) + env(safe-area-inset-bottom));
    border-top: 1px solid var(--color-border);
    background: color-mix(in srgb, var(--color-surface) 96%, transparent);
    backdrop-filter: blur(12px);
  }

  .mobile-bottom-link,
  .mobile-bottom-more {
    display: flex;
    min-width: 0;
    min-height: 48px;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: var(--space-1);
    padding: var(--space-1);
    border: 0;
    border-radius: var(--radius-sm);
    background: transparent;
    color: var(--color-text-muted);
    font: inherit;
    font-size: 11px;
    font-weight: 700;
    cursor: pointer;
  }

  .mobile-bottom-link.is-active,
  .mobile-bottom-more.is-active {
    background: var(--color-primary-soft);
    color: var(--color-primary-hover);
  }

  .mobile-bottom-link:focus-visible,
  .mobile-bottom-more:focus-visible,
  .mobile-nav-drawer button:focus-visible,
  .mobile-nav-secondary a:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  .mobile-bottom-marker {
    width: 18px;
    height: 3px;
    border-radius: 999px;
    background: currentColor;
  }

  .mobile-more-dots {
    height: 10px;
    font-size: 14px;
    letter-spacing: 2px;
    line-height: 4px;
  }

  .mobile-nav-overlay {
    position: fixed;
    inset: 0;
    z-index: 50;
    display: flex;
    align-items: flex-end;
  }

  .mobile-nav-backdrop {
    position: absolute;
    inset: 0;
    width: 100%;
    border: 0;
    background: color-mix(in srgb, var(--color-nav) 56%, transparent);
  }

  .mobile-nav-drawer {
    position: relative;
    width: 100%;
    padding: var(--space-5) var(--space-4) calc(var(--space-5) + env(safe-area-inset-bottom));
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    background: var(--color-surface);
    box-shadow: var(--shadow-floating);
  }

  .mobile-nav-drawer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--space-4);
  }

  .mobile-nav-drawer-header span {
    color: var(--color-primary-hover);
    font-size: 12px;
    font-weight: 700;
  }

  .mobile-nav-drawer-header h2 {
    margin: var(--space-1) 0 0;
    font-size: 18px;
  }

  .mobile-nav-drawer-header button {
    width: 40px;
    height: 40px;
    border: 1px solid var(--color-border);
    border-radius: 50%;
    background: var(--color-surface-muted);
    color: var(--color-text);
    font-size: 24px;
    cursor: pointer;
  }

  .mobile-nav-secondary {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-3);
  }

  .mobile-nav-secondary a {
    display: grid;
    gap: var(--space-1);
    padding: var(--space-4);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background: var(--color-surface-muted);
    color: var(--color-text);
  }

  .mobile-nav-secondary a[aria-current='page'] {
    border-color: var(--color-primary-border);
    background: var(--color-primary-soft);
  }

  .mobile-nav-secondary span {
    color: var(--color-text-muted);
    font-size: 12px;
  }

  .mobile-nav-secondary strong {
    font-size: 15px;
  }
}
</style>
