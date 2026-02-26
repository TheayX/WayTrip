import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken) {
    token.value = newToken
  }

  function setUserInfo(info) {
    userInfo.value = info
  }

  function login(data) {
    setToken(data.token)
    setUserInfo(data.user)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
  }

  function updatePreferences(preferences) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, preferences }
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
  persist: true
})

