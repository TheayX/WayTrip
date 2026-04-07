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

export const resolveSpotDetailSource = (source) => {
  return typeof source === 'string' && source.trim() ? source : SPOT_DETAIL_SOURCE.DETAIL
}

export const buildSpotDetailRoute = (spotId, source = SPOT_DETAIL_SOURCE.DETAIL, options = {}) => {
  const query = {
    source: resolveSpotDetailSource(source)
  }

  if (options.openReview) {
    query.openReview = '1'
  }

  return {
    path: `/spots/${spotId}`,
    query
  }
}

