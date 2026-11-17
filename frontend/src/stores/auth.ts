import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentUserApi } from '@/api/auth'
import { saveAdminTabSession } from '@/utils/adminTabSession'
import { getToken, removeToken, setToken } from '@/utils/token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref(null as any)
  const loginGeneration = ref(0)

  const isLoggedIn = computed(() => !!token.value && !!user.value)

  function setLogin(newToken: string, newUser: any) {
    saveAdminTabSession(newUser?.userId ?? newUser?.id ?? null, [])
    token.value = newToken
    user.value = newUser
    setToken(newToken)
    loginGeneration.value += 1
  }

  function setUser(newUser: any) {
    user.value = newUser
  }

  async function fetchCurrentUser() {
    const sessionToken = getToken()
    if (!sessionToken) {
      user.value = null
      return null
    }
    const currentUser = await getCurrentUserApi()
    if (getToken() !== sessionToken) return null
    user.value = currentUser
    return currentUser
  }

  function hasPermission(permission: string) {
    return user.value?.permissions?.includes(permission) ?? false
  }

  function logout() {
    saveAdminTabSession(user.value?.userId ?? user.value?.id ?? null, [])
    token.value = null
    user.value = null
    removeToken()
  }

  return {
    token,
    user,
    isLoggedIn,
    loginGeneration,
    setLogin,
    setUser,
    fetchCurrentUser,
    hasPermission,
    logout
  }
})
