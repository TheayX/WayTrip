import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getAdminInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const adminInfo = ref(null)

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('admin_token', newToken)
  }

  const clearToken = () => {
    token.value = ''
    adminInfo.value = null
    localStorage.removeItem('admin_token')
  }

  const loginAction = async (username, password) => {
    const res = await login(username, password)
    setToken(res.data.token)
    adminInfo.value = res.data.admin
    return res
  }

  const getInfo = async () => {
    const res = await getAdminInfo()
    adminInfo.value = res.data
    return res
  }

  const logout = () => {
    clearToken()
  }

  return {
    token,
    adminInfo,
    setToken,
    clearToken,
    loginAction,
    getInfo,
    logout
  }
})
