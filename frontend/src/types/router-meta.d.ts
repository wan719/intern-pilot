import 'vue-router'
import type { JourneyKey } from '@/config/navigation'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    public?: boolean
    permission?: string
    journey?: JourneyKey
    mobilePrimary?: boolean
  }
}
