import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref('')
  const userInfo = ref(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)

  // 从本地存储初始化
  function initFromStorage() {
    const storedToken = uni.getStorageSync('token')
    const storedUserInfo = uni.getStorageSync('userInfo')
    
    if (storedToken) {
      token.value = storedToken
    }
    if (storedUserInfo) {
      userInfo.value = storedUserInfo
    }
  }

  // 设置 Token
  function setToken(newToken) {
    token.value = newToken
    uni.setStorageSync('token', newToken)
  }

  // 设置用户信息
  function setUserInfo(info) {
    userInfo.value = info
    uni.setStorageSync('userInfo', info)
  }

  // 登录
  function login(data) {
    setToken(data.token)
    setUserInfo(data.user)
  }

  // 登出
  function logout() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  // 检查登录状态
  function checkLogin() {
    return isLoggedIn.value
  }

  // 更新偏好设置
  function updatePreferences(preferences) {
    if (userInfo.value) {
      userInfo.value.preferences = preferences
      uni.setStorageSync('userInfo', userInfo.value)
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    initFromStorage,
    setToken,
    setUserInfo,
    login,
    logout,
    checkLogin,
    updatePreferences
  }
})
