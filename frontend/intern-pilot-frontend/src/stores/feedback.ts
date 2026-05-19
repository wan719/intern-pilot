import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  createFeedbackApi,
  deleteFeedbackApi,
  listAdminFeedbackApi,
  listMyFeedbackApi,
  replyFeedbackApi,
  updateFeedbackStatusApi
} from '@/api/feedback'

export type FeedbackType = 'BUG' | 'SUGGESTION' | 'UI_UX' | 'AI_RESULT' | 'PERFORMANCE' | 'OTHER'
export type FeedbackStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'IGNORED'

export interface Feedback {
  id: number
  userId?: number
  userName?: string
  userEmail?: string
  type: FeedbackType
  typeDescription?: string
  title: string
  content: string
  pageUrl?: string
  contact?: string
  allowContact?: boolean
  browserInfo?: string
  status: FeedbackStatus
  statusDescription?: string
  adminReply?: string
  handledByName?: string
  handledAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface FeedbackCreateRequest {
  type: string
  title: string
  content: string
  contact?: string
  allowContact?: boolean
  pageUrl?: string
  browserInfo?: string
}

export const useFeedbackStore = defineStore('feedback', () => {
  const drawerVisible = ref(false)
  const feedbackList = ref<Feedback[]>([])
  const loading = ref(false)

  const pendingCount = computed(() => feedbackList.value.filter((item) => item.status === 'PENDING').length)

  function openDrawer() {
    drawerVisible.value = true
  }

  function closeDrawer() {
    drawerVisible.value = false
  }

  async function submitFeedback(data: FeedbackCreateRequest) {
    return createFeedbackApi(data)
  }

  async function fetchMyFeedbackList() {
    loading.value = true
    try {
      const result: any = await listMyFeedbackApi()
      feedbackList.value = Array.isArray(result) ? result : []
      return feedbackList.value
    } finally {
      loading.value = false
    }
  }

  async function fetchFeedbackList(params?: { type?: string; status?: string }) {
    loading.value = true
    try {
      const result: any = await listAdminFeedbackApi(params)
      feedbackList.value = Array.isArray(result) ? result : []
      return feedbackList.value
    } finally {
      loading.value = false
    }
  }

  async function updateStatus(id: number, status: FeedbackStatus) {
    const result: any = await updateFeedbackStatusApi(id, { status })
    replaceFeedback(result)
  }

  async function deleteFeedback(id: number) {
    await deleteFeedbackApi(id)
    feedbackList.value = feedbackList.value.filter((item) => item.id !== id)
  }

  async function reply(id: number, reply: string) {
    const result: any = await replyFeedbackApi(id, { reply })
    replaceFeedback(result)
  }

  function replaceFeedback(feedback?: Feedback) {
    if (!feedback?.id) return
    const index = feedbackList.value.findIndex((item) => item.id === feedback.id)
    if (index >= 0) {
      feedbackList.value[index] = feedback
    }
  }

  return {
    drawerVisible,
    feedbackList,
    loading,
    pendingCount,
    openDrawer,
    closeDrawer,
    submitFeedback,
    fetchMyFeedbackList,
    fetchFeedbackList,
    updateStatus,
    deleteFeedback,
    reply
  }
})
