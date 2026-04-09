import { getSpotList } from '@/api/spot'

// 每次随机抽取只在单页内取样，避免一次性拉全量景点列表。
const RANDOM_PICK_PAGE_SIZE = 12
// 多尝试几次，尽量避开“与上一次重复”但又不把请求次数放得过高。
const RANDOM_PICK_MAX_ATTEMPTS = 5

// 随心一选当前先复用景点列表做随机抽取，后续切正式接口时只改这里。
/**
 * 随机抽取一个景点
 * 说明：先随机页，再在页内随机项，兼顾接口复用和请求成本。
 * @param {{ excludeSpotId?: number | string | null }} [options]
 * @returns {Promise<object | null>}
 */
export const fetchRandomPickSpot = async ({ excludeSpotId = null } = {}) => {
  const countRes = await getSpotList({ page: 1, pageSize: 1, sortBy: 'heat' })
  const total = countRes.data?.total || 0
  if (!total) return null

  const maxPage = Math.max(1, Math.ceil(total / RANDOM_PICK_PAGE_SIZE))
  let nextSpot = null
  let attempts = 0

  while (!nextSpot && attempts < RANDOM_PICK_MAX_ATTEMPTS) {
    const randomPage = Math.max(1, Math.ceil(Math.random() * maxPage))
    const res = await getSpotList({ page: randomPage, pageSize: RANDOM_PICK_PAGE_SIZE, sortBy: 'heat' })
    const list = (res.data?.list || []).filter(item => item?.id)
    // 优先排除当前已展示景点；如果整页只剩同一个景点，则退回原列表避免返回空。
    const candidates = list.filter(item => item.id !== excludeSpotId)
    const pool = candidates.length ? candidates : list
    if (pool.length) {
      nextSpot = pool[Math.floor(Math.random() * pool.length)]
    }
    attempts += 1
  }

  return nextSpot
}
