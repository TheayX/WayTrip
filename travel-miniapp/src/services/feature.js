import { getGuideDetail, getGuideList } from '@/api/guide'
import { getHotSpots } from '@/api/home'
import { getSpotReviews } from '@/api/review'
import { getSpotList } from '@/api/spot'

export const BUDGET_MAX_PRICE = 50

const BLINDBOX_PAGE_SIZE = 12
const BLINDBOX_MAX_ATTEMPTS = 5
const BUDGET_SPOT_MAX_PAGES = 5
const BUDGET_SPOT_LIMIT = 12
const BUDGET_GUIDE_LIMIT = 8
const REVIEW_HOT_SPOT_LIMIT = 6
const REVIEW_PAGE_SIZE = 10

const toNumber = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

const toPriceNumber = (value) => {
  if (value === undefined || value === null) return null
  if (typeof value === 'number') return Number.isFinite(value) ? value : null

  const normalized = String(value).replace(/[^\d.]/g, '')
  const num = Number(normalized)
  return Number.isFinite(num) ? num : null
}

const isBudgetPrice = (value, maxPrice = BUDGET_MAX_PRICE) => {
  const price = toPriceNumber(value)
  return price !== null && price >= 0 && price <= maxPrice
}

const dedupeById = (list) => {
  const map = new Map()
  list.forEach((item) => {
    if (item?.id && !map.has(item.id)) {
      map.set(item.id, item)
    }
  })
  return Array.from(map.values())
}

// 当前先复用景点列表做随机抽取，后续切正式盲盒接口时只改这里。
export const fetchBlindboxSpot = async ({ excludeSpotId = null } = {}) => {
  const countRes = await getSpotList({ page: 1, pageSize: 1, sortBy: 'heat' })
  const total = countRes.data?.total || 0
  if (!total) return null

  const maxPage = Math.max(1, Math.ceil(total / BLINDBOX_PAGE_SIZE))
  let nextSpot = null
  let attempts = 0

  while (!nextSpot && attempts < BLINDBOX_MAX_ATTEMPTS) {
    const randomPage = Math.max(1, Math.ceil(Math.random() * maxPage))
    const res = await getSpotList({ page: randomPage, pageSize: BLINDBOX_PAGE_SIZE, sortBy: 'heat' })
    const list = (res.data?.list || []).filter(item => item?.id)
    const candidates = list.filter(item => item.id !== excludeSpotId)
    const pool = candidates.length ? candidates : list
    if (pool.length) {
      nextSpot = pool[Math.floor(Math.random() * pool.length)]
    }
    attempts += 1
  }

  return nextSpot
}

// 先基于现有景点列表封装预算筛选，后续改成后端过滤时页面不需要跟着改。
export const fetchBudgetSpots = async ({
  maxPrice = BUDGET_MAX_PRICE,
  limit = BUDGET_SPOT_LIMIT,
  maxPages = BUDGET_SPOT_MAX_PAGES
} = {}) => {
  const collected = []
  let page = 1
  let total = 0

  while (page <= maxPages && collected.length < limit) {
    const res = await getSpotList({ page, pageSize: BLINDBOX_PAGE_SIZE, sortBy: 'heat' })
    const list = res.data?.list || []
    total = res.data?.total || 0
    collected.push(...list.filter(item => isBudgetPrice(item.price, maxPrice)))
    if (page * BLINDBOX_PAGE_SIZE >= total) break
    page += 1
  }

  return dedupeById(collected)
    .sort((a, b) => {
      const priceDiff = (toPriceNumber(a.price) || 0) - (toPriceNumber(b.price) || 0)
      if (priceDiff !== 0) return priceDiff
      return (toNumber(b.heatScore) || 0) - (toNumber(a.heatScore) || 0)
    })
    .slice(0, limit)
}

// 攻略预算筛选单独封装，后续无论切后端接口还是做分页，页面都不用重写。
export const fetchBudgetGuides = async ({
  maxPrice = BUDGET_MAX_PRICE,
  limit = BUDGET_GUIDE_LIMIT
} = {}) => {
  const res = await getGuideList({ page: 1, pageSize: limit, sortBy: 'time' })
  const guideList = res.data?.list || []

  const detailList = await Promise.all(
    guideList.map(async (guide) => {
      try {
        const detailRes = await getGuideDetail(guide.id)
        const detail = detailRes.data
        const relatedSpots = detail?.relatedSpots || []
        const budgetSpots = relatedSpots.filter(spot => isBudgetPrice(spot.price, maxPrice))
        if (!budgetSpots.length) return null

        const numericPrices = budgetSpots
          .map(spot => toPriceNumber(spot.price))
          .filter(price => price !== null)
        const minPrice = numericPrices.length ? Math.min(...numericPrices) : 0

        return {
          ...guide,
          relatedCount: budgetSpots.length,
          priceLabel: minPrice <= 0 ? '含免费景点' : `${minPrice} 元起`
        }
      } catch (error) {
        console.error(`加载攻略详情失败: ${guide.id}`, error)
        return null
      }
    })
  )

  return detailList.filter(Boolean)
}

// 口碑流先复用热门景点评论聚合，后续切全站评价流接口时仍然只改这里。
export const fetchReviewFeedPreview = async () => {
  const hotRes = await getHotSpots(REVIEW_HOT_SPOT_LIMIT)
  const spotList = hotRes.data?.list || []

  const reviewGroups = await Promise.all(
    spotList.map(async (spot) => {
      try {
        const reviewRes = await getSpotReviews(spot.id, 1, REVIEW_PAGE_SIZE)
        return reviewRes.data?.list || []
      } catch (error) {
        console.error(`加载景点评价失败: ${spot.id}`, error)
        return []
      }
    })
  )

  const reviewList = dedupeById(
    reviewGroups
      .flat()
      .filter(item => item?.comment && item.comment.trim())
  ).sort((a, b) => {
    const timeA = new Date(a.createdAt || 0).getTime()
    const timeB = new Date(b.createdAt || 0).getTime()
    return timeB - timeA
  })

  return {
    positive: reviewList.filter(item => Number(item.score) >= 4),
    negative: reviewList.filter(item => Number(item.score) <= 2)
  }
}
