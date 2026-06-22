import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const loading = ref(false)

  function isLoggedIn() {
    return !!token.value
  }

  function getRole() {
    return userInfo.value?.role || ''
  }

  async function handleLogin(username, password) {
    loading.value = true
    try {
      const { data } = await request.post('/api/auth/login', { username, password })
      token.value = data.accessToken
      refreshToken.value = data.refreshToken
      userInfo.value = data.user

      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('userInfo', JSON.stringify(data.user))

      // 仅允许 admin 角色登录后台
      if (userInfo.value?.role !== 'admin') {
        await handleLogout()
        throw new Error('仅管理员可访问后台管理系统')
      }

      router.push('/admin/stats')
      return { success: true }
    } catch (error) {
      const msg = error?.message
        || error?.response?.data?.message
        || error?.response?.data?.error
        || '登录失败'
      return { success: false, message: msg }
    } finally {
      loading.value = false
    }
  }

  async function handleLogout() {
    try {
      await request.post('/api/auth/logout')
    } catch (e) {
      // ignore
    }
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.clear()
    router.push('/login')
  }

  return { token, refreshingToken: refreshToken, userInfo, loading, isLoggedIn, getRole, handleLogin, handleLogout }
})
