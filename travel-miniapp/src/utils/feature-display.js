// 特色功能页展示格式化工具，统一价格和评分文案口径。
// 首页、更多页和专题页都复用这里的格式规则，避免展示文本各写一套。

// 格式化专题页价格文案。
export const formatFeaturePrice = (value, {
  freeText = '免费',
  emptyText = '价格待补充',
  prefix = '¥'
} = {}) => {
  const num = Number(value)
  if (!Number.isFinite(num)) return emptyText
  return num <= 0 ? freeText : `${prefix}${num}`
}

// 格式化专题页评分文案。
export const formatFeatureRating = (value, {
  emptyText = '暂无评价',
  suffix = ' 分'
} = {}) => {
  const num = Number(value)
  // 评分统一保留一位小数，避免不同页面出现 4.5 / 4.50 / 4 等混用情况。
  return Number.isFinite(num) && num > 0 ? `${num.toFixed(1)}${suffix}` : emptyText
}
