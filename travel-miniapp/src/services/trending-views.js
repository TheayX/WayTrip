import { getRecentViewedSpots } from '@/api/home'

// 默认取近两周数据，既能反映近期趋势，也不会把时间拉得过长导致热点失真。
const TRENDING_VIEW_DAYS = 14
// 首屏只展示一屏以内的热门项，避免“热看页”变成普通长列表。
const TRENDING_VIEW_LIMIT = 12

// 近期热看走首页聚合接口，页面层只负责展示。
/**
 * 获取近期热看景点
 * @param {{ days?: number, limit?: number }} [options]
 * @returns {Promise<{days: number, list: object[]}>}
 */
export const fetchTrendingViewSpots = async ({
  days = TRENDING_VIEW_DAYS,
  limit = TRENDING_VIEW_LIMIT
} = {}) => {
  const res = await getRecentViewedSpots(days, limit)
  return {
    // 如果后端没有回填统计天数，则沿用本次请求参数，保证页面文案稳定。
    days: res.data?.days || days,
    list: res.data?.list || []
  }
}
