import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentUserApi } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref(null as any)

  const isLoggedIn = computed(() => !!token.value && !!user.value)

  function setLogin(newToken: string, newUser: any) {
    token.value = newToken
    user.value = newUser
    setToken(newToken)
  }

  function setUser(newUser: any) {
    user.value = newUser
  }

  async function fetchCurrentUser() {
    if (!getToken()) {
      user.value = null
      return null
    }
    const currentUser = await getCurrentUserApi()
    user.value = currentUser
    return currentUser
  }

  function hasPermission(permission: string) {
    return user.value?.permissions?.includes(permission) ?? false
  }

  function logout() {
    token.value = null
    user.value = null
    removeToken()
  }

  return {
    token,
    user,
    isLoggedIn,
    setLogin,
    setUser,
    fetchCurrentUser,
    hasPermission,
    logout
  }
})