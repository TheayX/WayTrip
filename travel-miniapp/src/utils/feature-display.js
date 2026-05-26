// 特色功能页共享的展示格式化方法，避免多个页面重复维护相同口径。
// 价格和评分文案在首页、更多页、专题页统一复用，保证展示规则一致。
export const formatFeaturePrice = (value, {
  freeText = '免费',
  emptyText = '价格待补充',
  prefix = '¥'
} = {}) => {
  const num = Number(value)
  if (!Number.isFinite(num)) return emptyText
  return num <= 0 ? freeText : `${prefix}${num}`
}

export const formatFeatureRating = (value, {
  emptyText = '暂无评价',
  suffix = ' 分'
} = {}) => {
  const num = Number(value)
  // 评分统一保留一位小数，避免不同页面出现 4.5 / 4.50 / 4 等混用情况。
  return Number.isFinite(num) && num > 0 ? `${num.toFixed(1)}${suffix}` : emptyText
}
