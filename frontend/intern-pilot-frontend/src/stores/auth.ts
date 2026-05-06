import { defineStore } from 'pinia'
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
    logout() {
      this.token = null
      this.user = null
      removeToken()
    }
  }
})
