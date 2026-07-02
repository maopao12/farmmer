import { defineStore } from 'pinia'
import { authApi, plotApi } from '@/api'
import { connectWebSocket, disconnectWebSocket, unsubscribeAll, onStatusChange } from '@/utils/websocket'

export const useAppStore = defineStore('app', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || '',
    plots: [],
    currentPlotId: null,
    wsStatus: 'disconnected'  // connected | disconnected | reconnecting | error
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 'ADMIN' || state.user?.role === 'SUPER_ADMIN',
    currentPlot: (state) => state.plots.find(p => p.id === state.currentPlotId)
  },

  actions: {
    initWsListener() {
      onStatusChange((status) => {
        this.wsStatus = status
      })
    },

    async login(username, password) {
      const res = await authApi.login({ username, password })
      this.token = res.token
      localStorage.setItem('token', res.token)
      this.user = { id: res.userId, username: res.username, realName: res.realName, role: res.role }
      this.initWsListener()
      connectWebSocket()
    },

    async fetchUser() {
      try {
        this.user = await authApi.me()
      } catch {
        this.logout()
      }
    },

    async fetchPlots() {
      const res = await plotApi.all()
      this.plots = Array.isArray(res) ? res : (res.records || [])
      if (this.plots.length > 0 && !this.currentPlotId) {
        this.currentPlotId = this.plots[0].id
      }
    },

    logout() {
      this.token = ''
      this.user = null
      this.plots = []
      this.currentPlotId = null
      localStorage.removeItem('token')
      unsubscribeAll()
      disconnectWebSocket()
    }
  }
})
