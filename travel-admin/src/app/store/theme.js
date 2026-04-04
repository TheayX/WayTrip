// 主题状态管理（Pinia Store）
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  THEME_MODE_DARK,
  THEME_MODE_LIGHT,
  THEME_MODE_SYSTEM
} from '@/shared/constants/theme.js'

/**
 * 主题状态 Store
 * 功能：管理后台全局主题模式、系统主题监听与持久化
 * 持久化：开启（用户选择的主题模式自动保存到 localStorage）
 */
export const useThemeStore = defineStore('theme', () => {
  // 用户选择的主题模式
  const themeMode = ref(THEME_MODE_SYSTEM)
  // 当前系统主题
  const systemTheme = ref(THEME_MODE_LIGHT)
  // 系统主题监听器
  let mediaQueryList = null
  let mediaQueryHandler = null

  /**
   * 根据系统偏好解析主题
   * @returns {'light' | 'dark'}
   */
  const resolveSystemTheme = () => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return THEME_MODE_LIGHT
    }

    return window.matchMedia('(prefers-color-scheme: dark)').matches
      ? THEME_MODE_DARK
      : THEME_MODE_LIGHT
  }

  /**
   * 当前实际生效的主题
   * 说明：当模式为 system 时，自动跟随系统设置。
   */
  const currentTheme = computed(() => {
    return themeMode.value === THEME_MODE_SYSTEM ? systemTheme.value : themeMode.value
  })

  /**
   * 判断当前是否为暗色主题
   */
  const isDark = computed(() => currentTheme.value === THEME_MODE_DARK)

  /**
   * 判断当前是否为系统跟随模式
   */
  const isSystemMode = computed(() => themeMode.value === THEME_MODE_SYSTEM)

  /**
   * 将主题同步到根节点
   * 说明：统一维护 data-theme、data-theme-mode 和 class，方便样式层消费。
   * @param {'light' | 'dark'} theme - 当前实际生效主题
   */
  const applyTheme = (theme) => {
    if (typeof document === 'undefined') return

    const root = document.documentElement
    const body = document.body
    const darkMode = theme === THEME_MODE_DARK

    root.dataset.theme = theme
    root.dataset.themeMode = themeMode.value
    root.style.colorScheme = darkMode ? THEME_MODE_DARK : THEME_MODE_LIGHT
    root.classList.toggle(THEME_MODE_DARK, darkMode)
    body?.classList.toggle('theme-dark', darkMode)
  }

  /**
   * 响应系统主题变化
   */
  const syncSystemTheme = () => {
    systemTheme.value = resolveSystemTheme()
    applyTheme(currentTheme.value)
  }

  /**
   * 绑定系统主题监听
   * 说明：只有在 system 模式下，主题才会跟随系统变化自动更新。
   */
  const bindSystemThemeListener = () => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
    if (mediaQueryList) return

    mediaQueryList = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQueryHandler = () => {
      syncSystemTheme()
    }

    if (typeof mediaQueryList.addEventListener === 'function') {
      mediaQueryList.addEventListener('change', mediaQueryHandler)
    } else if (typeof mediaQueryList.addListener === 'function') {
      mediaQueryList.addListener(mediaQueryHandler)
    }
  }

  /**
   * 设置主题模式
   * @param {'light' | 'dark' | 'system'} nextMode - 新主题模式
   */
  const setThemeMode = (nextMode) => {
    if (
      nextMode !== THEME_MODE_LIGHT &&
      nextMode !== THEME_MODE_DARK &&
      nextMode !== THEME_MODE_SYSTEM
    ) {
      themeMode.value = THEME_MODE_SYSTEM
    } else {
      themeMode.value = nextMode
    }

    applyTheme(currentTheme.value)
  }

  /**
   * 切换明暗主题
   * 说明：快捷切换只在 light 和 dark 间切换；system 模式由菜单显式选择。
   */
  const toggleTheme = () => {
    setThemeMode(isDark.value ? THEME_MODE_LIGHT : THEME_MODE_DARK)
  }

  /**
   * 初始化主题
   * 说明：首屏优先解析系统主题，再应用用户持久化模式。
   */
  const initTheme = () => {
    systemTheme.value = resolveSystemTheme()
    bindSystemThemeListener()
    applyTheme(currentTheme.value)
  }

  return {
    themeMode,
    systemTheme,
    currentTheme,
    isDark,
    isSystemMode,
    setThemeMode,
    toggleTheme,
    initTheme
  }
}, {
  // 开启持久化存储
  persist: true
})
