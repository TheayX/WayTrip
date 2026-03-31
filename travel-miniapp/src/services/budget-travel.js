import { getBudgetGuideList } from '@/api/guide'
import { getSpotList } from '@/api/spot'

export const BUDGET_MAX_PRICE = 50
export const BUDGET_MODE_UNDER_50 = 'under50'
export const BUDGET_MODE_FREE = 'free'
export const BUDGET_MODE_OPTIONS = [
  { label: '免费', value: BUDGET_MODE_FREE },
  { label: '50 元以内', value: BUDGET_MODE_UNDER_50 }
]

const BUDGET_SPOT_PAGE_SIZE = 12
const BUDGET_SPOT_MAX_PAGES = 5
const BUDGET_SPOT_LIMIT = 12
const BUDGET_GUIDE_LIMIT = 8

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

// 景点预算筛选先基于现有景点列表实现，后续扩展后端参数时只改这里。
export const fetchBudgetTravelSpots = async ({
  budgetMode = BUDGET_MODE_UNDER_50,
  maxPrice = BUDGET_MAX_PRICE,
  limit = BUDGET_SPOT_LIMIT,
  maxPages = BUDGET_SPOT_MAX_PAGES
} = {}) => {
  const collected = []
  let page = 1
  let total = 0

  while (page <= maxPages && collected.length < limit) {
    const res = await getSpotList({ page, pageSize: BUDGET_SPOT_PAGE_SIZE, sortBy: 'heat' })
    const list = res.data?.list || []
    total = res.data?.total || 0
    const matchedList = list.filter((item) => {
      if (budgetMode === BUDGET_MODE_FREE) {
        return isFreePrice(item.price)
      }
      return isBudgetPrice(item.price, maxPrice)
    })
    collected.push(...matchedList)
    if (page * BUDGET_SPOT_PAGE_SIZE >= total) break
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

// 攻略预算列表走后端直出，页面层继续复用当前结构。
export const fetchBudgetTravelGuides = async ({
  budgetMode = BUDGET_MODE_UNDER_50,
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
