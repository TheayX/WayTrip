import { getRecentViewedSpots } from '@/api/home'

const TRENDING_VIEW_DAYS = 14
const TRENDING_VIEW_LIMIT = 12

// 近期热看走首页聚合接口，页面层只负责展示。
export const fetchTrendingViewSpots = async ({
  days = TRENDING_VIEW_DAYS,
  limit = TRENDING_VIEW_LIMIT
} = {}) => {
  const res = await getRecentViewedSpots(days, limit)
  return {
    days: res.data?.days || days,
    list: res.data?.list || []
  }
}
