import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

type ResponsiveSizeOptions = {
  mobileBreakpoint?: number
  tabletBreakpoint?: number
}

const DEFAULT_MOBILE_BREAKPOINT = 640
const DEFAULT_TABLET_BREAKPOINT = 1024
const DEFAULT_MOBILE_DIALOG_WIDTH = 'calc(100vw - 28px)'

export function useResponsiveSize(options: ResponsiveSizeOptions = {}) {
  const mobileBreakpoint = options.mobileBreakpoint ?? DEFAULT_MOBILE_BREAKPOINT
  const tabletBreakpoint = options.tabletBreakpoint ?? DEFAULT_TABLET_BREAKPOINT
  const viewportWidth = ref(typeof window === 'undefined' ? DEFAULT_TABLET_BREAKPOINT : window.innerWidth)

  const isMobile = computed(() => viewportWidth.value <= mobileBreakpoint)
  const isTablet = computed(() => viewportWidth.value > mobileBreakpoint && viewportWidth.value <= tabletBreakpoint)

  function handleResize() {
    viewportWidth.value = window.innerWidth
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
  })

  function responsiveDialogWidth(desktop = '560px', mobile = DEFAULT_MOBILE_DIALOG_WIDTH) {
    return computed(() => (isMobile.value ? mobile : desktop))
  }

  function responsiveDrawerSize(desktop = '48%', tablet = '72%', mobile = '100%') {
    return computed(() => {
      if (isMobile.value) return mobile
      if (isTablet.value) return tablet
      return desktop
    })
  }

  return {
    viewportWidth,
    isMobile,
    isTablet,
    responsiveDialogWidth,
    responsiveDrawerSize
  }
}
