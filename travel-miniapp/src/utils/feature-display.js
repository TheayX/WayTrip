// 特色功能页共享的展示格式化方法，避免多个页面重复维护相同口径。
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
  emptyText = '暂无评分',
  suffix = ' 分'
} = {}) => {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? `${num.toFixed(1)}${suffix}` : emptyText
}
