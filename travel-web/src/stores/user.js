// 用户状态管理（Pinia Store）
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态 Store
 * 功能：用户登录、登出、Token 管理、用户信息维护
 * 持久化：开启（数据自动保存到 localStorage）
 */
export const useUserStore = defineStore('user', () => {
  // Token（登录凭证）
  const token = ref('')
  // 用户信息
  const userInfo = ref(null)

  /**
   * 是否已登录（计算属性）
   * @returns {boolean} true=已登录，false=未登录
   */
  const isLoggedIn = computed(() => !!token.value)

  /**
   * 设置 Token
   * @param {string} newToken - 新的 Token
   */
  function setToken(newToken) {
    token.value = newToken
  }

  /**
   * 设置用户信息
   * @param {Object} info - 用户信息对象
   */
  function setUserInfo(info) {
    userInfo.value = info
  }

  /**
   * 登录（同时保存 Token 和用户信息）
   * @param {Object} data - 登录响应数据（包含 token 和 user）
   */
  function login(data) {
    setToken(data.token)
    setUserInfo(data.user)
  }

  /**
   * 退出登录（清除 Token、用户信息和持久化数据）
   */
  function logout() {
    token.value = ''
    userInfo.value = null

    // 清除持久化数据，防止页面刷新后恢复过期的 Token
    localStorage.removeItem('user')
    sessionStorage.removeItem('user')
  }

  /**
   * 更新用户偏好设置
   * @param {Object} preferences - 偏好设置数据
   */
  function updatePreferences(preferences) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...preferences }
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    setUserInfo,
    login,
    logout,
    updatePreferences
  }
}, {
  // 开启持久化存储
  persist: true
})
