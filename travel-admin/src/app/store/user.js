// 用户状态管理（Pinia Store）
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getAdminInfo } from '@/modules/system/api/auth.js'

/**
 * 用户状态 Store
 * 功能：管理员登录、信息获取、Token 管理
 * 持久化：开启（数据自动保存到 localStorage）
 */
export const useUserStore = defineStore('user', () => {
  // Token（登录凭证）
  const token = ref('')
  // 管理员信息
  const adminInfo = ref(null)

  /**
   * 设置 Token
   * @param {string} newToken - 新的 Token
   */
  const setToken = (newToken) => {
    token.value = newToken
  }

  /**
   * 清除 Token 和用户信息（退出登录时使用）
   */
  const clearToken = () => {
    token.value = ''
    adminInfo.value = null
  }

  /**
   * 管理员登录
   * @param {string} username - 用户名
   * @param {string} password - 密码
   * @returns {Promise} 登录响应结果
   */
  const loginAction = async (username, password) => {
    const res = await login(username, password)
    setToken(res.data.token)
    adminInfo.value = res.data.admin
    return res
  }

  /**
   * 获取管理员信息（登录后调用）
   * @returns {Promise} 管理员信息响应结果
   */
  const getInfo = async () => {
    const res = await getAdminInfo()
    adminInfo.value = res.data
    return res
  }

  /**
   * 退出登录（清除 Token 和信息）
   */
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
}, {
  // 开启持久化存储
  persist: true
})
