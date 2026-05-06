// 图表主题配置

/**
 * 读取主题变量
 * @param {string} name - CSS 变量名
 * @param {string} fallback - 默认值
 * @returns {string}
 */
const getCssVar = (name, fallback) => {
  if (typeof window === 'undefined') return fallback
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

/**
 * 获取图表配色
 * 说明：图表不直接依赖页面硬编码颜色，统一由主题层输出。
 * @param {'light' | 'dark'} theme - 当前解析后的主题
 * @returns {Record<string, string | string[]>}
 */
export const getChartThemeTokens = (theme) => {
  const isDark = theme === 'dark'

  return {
    textPrimary: getCssVar('--wt-text-primary', '#0f172a'),
    textSecondary: getCssVar('--wt-text-secondary', '#94a3b8'),
    borderColor: getCssVar('--wt-border-default', '#dbe4f0'),
    splitLine: getCssVar('--wt-divider-soft', '#f1f5f9'),
    tooltipBackground: isDark ? getCssVar('--wt-surface-elevated', '#16243a') : 'rgba(255, 255, 255, 0.95)',
    panelBackground: getCssVar('--wt-surface-elevated', '#ffffff'),
    blue: getCssVar('--wt-accent-blue-text', '#3b82f6'),
    violet: getCssVar('--wt-accent-cyan-text', '#0891b2'),
    emerald: getCssVar('--wt-accent-emerald-text', '#10b981'),
    amber: getCssVar('--wt-accent-amber-text', '#f97316'),
    heatmap: isDark
      ? ['#0b1626', '#12324a', '#145c63', '#22d3ee', '#bfdbfe']
      : ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39']
  }
}
