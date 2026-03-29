import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 基础状态
  const token = ref('')
  const userInfo = ref(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)

  // 内部方法
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

  function setToken(newToken) {
    token.value = newToken
    uni.setStorageSync('token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    uni.setStorageSync('userInfo', info)
  }

  function login(data) {
    setToken(data.token)
    setUserInfo(data.user)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  function checkLogin() {
    return isLoggedIn.value
  }

  function updatePreferences(preferences) {
    if (userInfo.value) {
      userInfo.value = {
        ...userInfo.value,
        ...preferences
      }
      uni.setStorageSync('userInfo', userInfo.value)
    }
  }

  // 对外暴露方法
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
