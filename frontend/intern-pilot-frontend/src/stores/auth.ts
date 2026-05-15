import { defineStore } from 'pinia'
import { getCurrentUserApi } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: null as any
  }),
  actions: {
    setLogin(token: string, user: any) {
      this.token = token
      this.user = user
      setToken(token)
    },
    setUser(user: any) {
      this.user = user
    },
    async fetchCurrentUser() {
      if (!getToken()) {
        this.user = null
        return null
      }
      const user = await getCurrentUserApi()
      this.user = user
      return user
    },
    hasPermission(permission: string) {
      return this.user?.permissions?.includes(permission) ?? false
    },
    logout() {
      this.token = null
      this.user = null
      removeToken()
    }
  }
})
