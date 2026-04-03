// 游客口碑走后端直出，避免前端逐景点聚合评论。
import { getReviewFeed } from '@/modules/review/api.js'

const REVIEW_PAGE_SIZE = 10

export const fetchTravelerReviewFeed = async () => {
  const [positiveRes, negativeRes] = await Promise.all([
    getReviewFeed({ page: 1, pageSize: REVIEW_PAGE_SIZE, type: 'positive' }),
    getReviewFeed({ page: 1, pageSize: REVIEW_PAGE_SIZE, type: 'negative' })
  ])

  return {
    positive: positiveRes.data?.list || [],
    negative: negativeRes.data?.list || []
  }
}
