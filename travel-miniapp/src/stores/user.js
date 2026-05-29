import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 用户状态仓库，统一维护登录态、用户资料与本地持久化读写。
// 页面、请求层和登录流程都通过这里共享同一份身份信息，避免各自维护副本。
export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null)
  const isLoggedIn = computed(() => !!token.value)

  // 从本地缓存恢复登录态。
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

  // 写入并持久化登录令牌。
  function setToken(newToken) {
    token.value = newToken
    uni.setStorageSync('token', newToken)
  }

  // 写入并持久化用户资料。
  function setUserInfo(info) {
    userInfo.value = info
    uni.setStorageSync('userInfo', info)
  }

  // 登录成功后同步写入令牌和用户资料。
  function login(data) {
    // 登录后统一走同一套写入逻辑，避免 token 与用户信息落库口径不一致。
    setToken(data.token)
    setUserInfo(data.user)
  }

  // 退出登录并清空本地身份缓存。
  function logout() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  // 返回当前登录态。
  function checkLogin() {
    return isLoggedIn.value
  }

  // 局部合并用户偏好信息。
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
