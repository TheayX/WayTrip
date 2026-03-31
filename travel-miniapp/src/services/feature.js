import { getBudgetGuideList } from '@/api/guide'
import { getReviewFeed } from '@/api/review'
import { getSpotList } from '@/api/spot'

export const BUDGET_MAX_PRICE = 50
export const BUDGET_MODE_ALL = 'budget'
export const BUDGET_MODE_FREE = 'free'
export const BUDGET_MODE_OPTIONS = [
  { label: '免费', value: BUDGET_MODE_FREE },
  { label: '50 元以内', value: BUDGET_MODE_ALL }
]

const BLINDBOX_PAGE_SIZE = 12
const BLINDBOX_MAX_ATTEMPTS = 5
const BUDGET_SPOT_MAX_PAGES = 5
const BUDGET_SPOT_LIMIT = 12
const BUDGET_GUIDE_LIMIT = 8
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

const isFreePrice = (value) => {
  const price = toPriceNumber(value)
  return price !== null && price === 0
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
  budgetMode = BUDGET_MODE_ALL,
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
    const matchedList = list.filter((item) => {
      if (budgetMode === BUDGET_MODE_FREE) {
        return isFreePrice(item.price)
      }
      return isBudgetPrice(item.price, maxPrice)
    })
    collected.push(...matchedList)
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

// 攻略预算列表改成后端直出，页面层继续复用当前结构。
export const fetchBudgetGuides = async ({
  budgetMode = BUDGET_MODE_ALL,
  maxPrice = BUDGET_MAX_PRICE,
  limit = BUDGET_GUIDE_LIMIT
} = {}) => {
  const res = await getBudgetGuideList({
    page: 1,
    pageSize: limit,
    priceMode: budgetMode,
    maxPrice
  })
  return res.data?.list || []
}

// 口碑流改成后端直出，避免前端逐景点聚合带来的漏数和空刷问题。
export const fetchReviewFeedPreview = async () => {
  const [positiveRes, negativeRes] = await Promise.all([
    getReviewFeed({ page: 1, pageSize: REVIEW_PAGE_SIZE, type: 'positive' }),
    getReviewFeed({ page: 1, pageSize: REVIEW_PAGE_SIZE, type: 'negative' })
  ])

  return {
    positive: positiveRes.data?.list || [],
    negative: negativeRes.data?.list || []
  }
}
