import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 用户状态统一收口到 store，便于页面、请求层和登录流程共享同一份身份信息。
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
    // 登录后统一走同一套写入逻辑，避免 token 与用户信息落库口径不一致。
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
      // 偏好更新通常只回写局部字段，这里保留已有资料，避免把其他信息覆盖掉。
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
