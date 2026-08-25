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
      ref="moreButton"
      class="mobile-bottom-more"
      :class="{ 'is-active': secondaryActive }"
      type="button"
      :aria-label="moreButtonLabel"
      :aria-current="secondaryActiveItem ? 'page' : undefined"
      :aria-expanded="drawerVisible"
      aria-controls="mobile-journey-more"
      @click="openDrawer"
    >
      <span class="mobile-more-dots" aria-hidden="true">•••</span>
      <span>{{ secondaryActiveItem ? `${secondaryActiveItem.shortLabel} · 更多` : '更多' }}</span>
    </button>
  </nav>

  <el-drawer
    id="mobile-journey-more"
    v-model="drawerVisible"
    class="mobile-nav-drawer"
    title="更多求职阶段"
    direction="btt"
    size="auto"
    :append-to-body="false"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    @keydown.esc.stop="closeDrawer"
    @opened="focusFirstSecondaryLink"
    @close-auto-focus="restoreMoreFocus"
  >
    <div ref="drawerBody">
      <span class="mobile-nav-drawer-eyebrow">完整旅程</span>
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
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { resolveJourneyKey, type JourneyItem } from '@/config/navigation'

const props = defineProps<{ items: JourneyItem[] }>()
const route = useRoute()
const drawerVisible = ref(false)
const moreButton = ref<HTMLButtonElement>()
const drawerBody = ref<HTMLElement>()
const coreItems = computed(() => props.items.filter((item) => item.mobile))
const secondaryItems = computed(() => props.items.filter((item) => !item.mobile))
const activeKey = computed(() => resolveJourneyKey(route.path))
const secondaryActiveItem = computed(() => secondaryItems.value.find((item) => item.key === activeKey.value))
const secondaryActive = computed(() => Boolean(secondaryActiveItem.value))
const moreButtonLabel = computed(() =>
  secondaryActiveItem.value
    ? `更多求职旅程入口，当前阶段：${secondaryActiveItem.value.label}`
    : '更多求职旅程入口'
)

function openDrawer() {
  drawerVisible.value = true
}

function closeDrawer() {
  drawerVisible.value = false
}

async function focusFirstSecondaryLink() {
  await nextTick()
  drawerBody.value?.querySelector<HTMLAnchorElement>('a')?.focus()
}

function restoreMoreFocus() {
  moreButton.value?.focus()
}

watch(() => route.fullPath, closeDrawer)
</script>

<style scoped>
.mobile-bottom-nav {
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
  :deep(.mobile-nav-drawer button:focus-visible),
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

  :deep(.mobile-nav-drawer) {
    max-height: min(70vh, 420px);
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  }

  :deep(.mobile-nav-drawer .el-drawer__header) {
    margin-bottom: 0;
    padding: var(--space-5) var(--space-4) var(--space-3);
    color: var(--color-text);
  }

  :deep(.mobile-nav-drawer .el-drawer__title) {
    font-size: 18px;
    font-weight: 700;
  }

  :deep(.mobile-nav-drawer .el-drawer__body) {
    padding: 0 var(--space-4) calc(var(--space-5) + env(safe-area-inset-bottom));
  }

  .mobile-nav-drawer-eyebrow {
    display: block;
    margin-bottom: var(--space-3);
    color: var(--color-primary-hover);
    font-size: 12px;
    font-weight: 700;
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
