// 主题能力封装
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/app/store/theme.js'

/**
 * 主题组合式函数
 * 说明：统一暴露主题模式、解析后的主题和切换方法，避免页面重复拼接状态。
 */
export const useTheme = () => {
  const themeStore = useThemeStore()
  const { themeMode, currentTheme, isDark, isSystemMode } = storeToRefs(themeStore)

  return {
    themeStore,
    themeMode,
    currentTheme,
    isDark,
    isSystemMode,
    setThemeMode: themeStore.setThemeMode,
    initTheme: themeStore.initTheme,
    toggleTheme: themeStore.toggleTheme
  }
}
