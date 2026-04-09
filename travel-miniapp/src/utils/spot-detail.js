// 景点详情跳转与来源统一收口，避免各页面各自拼接 URL。
export const SPOT_DETAIL_SOURCE = {
  HOME: 'home',
  LIST: 'list',
  SEARCH: 'search',
  NEARBY: 'nearby',
  GUIDE: 'guide',
  RECOMMENDATION: 'recommendation',
  DISCOVER: 'discover',
  ORDER: 'order',
  SIMILAR: 'similar',
  RANDOM_PICK: 'random-pick',
  BUDGET_TRAVEL: 'budget-travel',
  TRAVELER_REVIEWS: 'traveler-reviews',
  TRENDING_VIEWS: 'trending-views',
  FOOTPRINT: 'footprint',
  FAVORITE: 'favorite',
  REVIEW: 'review',
  DETAIL: 'detail'
}

/**
 * 构建景点详情页跳转地址
 * @param {number|string} spotId
 * @param {string} [source]
 * @param {{ openReview?: boolean }} [options]
 * @returns {string}
 */
export const buildSpotDetailUrl = (spotId, source = SPOT_DETAIL_SOURCE.DETAIL, options = {}) => {
  const query = [
    `id=${encodeURIComponent(String(spotId))}`,
    // 来源参数统一保留，便于详情页埋点、回流分析和特殊逻辑识别入口来源。
    `source=${encodeURIComponent(source)}`
  ]

  if (options.openReview) {
    query.push('openReview=1')
  }

  return `/pages/spot/detail?${query.join('&')}`
}
