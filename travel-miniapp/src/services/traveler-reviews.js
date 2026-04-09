import { getReviewFeed } from '@/api/review'

// 口碑页只取首屏够用的数据量，后续如果做分页再统一调整这里。
const REVIEW_PAGE_SIZE = 10

// 游客口碑走后端直出，避免前端逐景点聚合评论。
/**
 * 获取游客口碑流
 * 说明：并行拉取正向和负向评价，供页面切换页签时直接展示。
 * @returns {Promise<{positive: object[], negative: object[]}>}
 */
export const fetchTravelerReviewFeed = async () => {
  const [positiveRes, negativeRes] = await Promise.all([
    getReviewFeed({ page: 1, pageSize: REVIEW_PAGE_SIZE, type: 'positive' }),
    getReviewFeed({ page: 1, pageSize: REVIEW_PAGE_SIZE, type: 'negative' })
  ])

  return {
    // 接口层统一兜底空数组，页面层可以直接按列表渲染。
    positive: positiveRes.data?.list || [],
    negative: negativeRes.data?.list || []
  }
}
