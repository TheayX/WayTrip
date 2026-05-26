import { getBudgetGuideList } from '@/api/guide'
import { getSpotList } from '@/api/spot'

// 预算阈值和模式配置，页面层直接复用这些常量保证一致性。
export const BUDGET_MAX_PRICE = 50  // 最大预算阈值
export const BUDGET_MODE_UNDER_50 = 'under50'  // 50元以内模式
export const BUDGET_MODE_FREE = 'free'  // 免费模式
export const BUDGET_MODE_OPTIONS = [
  { label: '免费', value: BUDGET_MODE_FREE },
  { label: '50 元以内', value: BUDGET_MODE_UNDER_50 }
]

// 分页和数量限制：景点先在前端做多页聚合筛选，攻略走后端直出。
const BUDGET_SPOT_PAGE_SIZE = 12   // 每页景点数
const BUDGET_SPOT_MAX_PAGES = 5    // 最多翻几页
const BUDGET_SPOT_LIMIT = 12       // 最终返回的景点数量
const BUDGET_GUIDE_LIMIT = 8       // 返回的攻略数量

// 工具函数：安全转换为数字，避免 NaN 污染后续计算。
const toNumber = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

// 工具函数：提取价格数值，兼容字符串价格（如 "¥39.9"）。
const toPriceNumber = (value) => {
  if (value === undefined || value === null) return null
  if (typeof value === 'number') return Number.isFinite(value) ? value : null

  const normalized = String(value).replace(/[^\d.]/g, '')  // 移除所有非数字字符
  const num = Number(normalized)
  return Number.isFinite(num) ? num : null
}

// 判断是否在预算范围内（0 ~ maxPrice）。
const isBudgetPrice = (value, maxPrice = BUDGET_MAX_PRICE) => {
  const price = toPriceNumber(value)
  return price !== null && price >= 0 && price <= maxPrice
}

// 判断是否免费（价格为 0）。
const isFreePrice = (value) => {
  const price = toPriceNumber(value)
  return price !== null && price === 0
}

// 按 ID 去重，避免多页聚合时出现重复景点。
const dedupeById = (list) => {
  const map = new Map()
  list.forEach((item) => {
    if (item?.id && !map.has(item.id)) {
      map.set(item.id, item)
    }
  })
  return Array.from(map.values())
}

// 预算景点列表先基于现有景点接口做多页聚合筛选，后续如果后端支持价格过滤参数，只改这里即可。
/**
 * 获取预算景点列表
 * 说明：先随机翻页收集候选，再在前端做价格筛选、去重和排序。
 * @param {{ budgetMode?: string, maxPrice?: number, limit?: number, maxPages?: number }} [options]
 * @returns {Promise<object[]>}
 */
export const fetchBudgetTravelSpots = async ({
  budgetMode = BUDGET_MODE_UNDER_50,
  maxPrice = BUDGET_MAX_PRICE,
  limit = BUDGET_SPOT_LIMIT,
  maxPages = BUDGET_SPOT_MAX_PAGES
} = {}) => {
  const collected = []
  let page = 1
  let total = 0

  // 循环分页请求，直到收集到足够的景点或达到最大页数
  while (page <= maxPages && collected.length < limit) {
    const res = await getSpotList({ page, pageSize: BUDGET_SPOT_PAGE_SIZE, sortBy: 'heat' })
    const list = res.data?.list || []
    total = res.data?.total || 0
    
    // 筛选符合预算的景点
    const matchedList = list.filter((item) => {
      if (budgetMode === BUDGET_MODE_FREE) {
        return isFreePrice(item.price)
      }
      return isBudgetPrice(item.price, maxPrice)
    })
    collected.push(...matchedList)
    
    // 如果已经遍历完所有数据，提前退出
    if (page * BUDGET_SPOT_PAGE_SIZE >= total) break
    page += 1
  }

  // 去重 → 排序（价格升序，同价位热度降序）→ 截取前 limit 条
  return dedupeById(collected)
    .sort((a, b) => {
      const priceDiff = (toPriceNumber(a.price) || 0) - (toPriceNumber(b.price) || 0)
      if (priceDiff !== 0) return priceDiff
      return (toNumber(b.heatScore) || 0) - (toNumber(a.heatScore) || 0)
    })
    .slice(0, limit)
}

// 预算攻略列表走后端直出，页面层继续复用当前结构。
/**
 * 获取预算攻略列表
 * 说明：后端已按价格模式筛选，前端只负责展示。
 * @param {{ budgetMode?: string, maxPrice?: number, limit?: number }} [options]
 * @returns {Promise<object[]>}
 */
export const fetchBudgetTravelGuides = async ({
  budgetMode = BUDGET_MODE_UNDER_50,
  maxPrice = BUDGET_MAX_PRICE,
  limit = BUDGET_GUIDE_LIMIT
} = {}) => {
  const res = await getBudgetGuideList({
    page: 1,
    pageSize: limit,
    priceMode: budgetMode,  // 'free' 或 'under50'
    maxPrice
  })
  return res.data?.list || []
}
