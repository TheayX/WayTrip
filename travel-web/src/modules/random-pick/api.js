// 随心一选当前先复用景点列表做随机抽取，后续切正式接口时只改这里。
import { getSpotList } from '@/modules/spot/api.js'

const RANDOM_PICK_PAGE_SIZE = 12
const RANDOM_PICK_MAX_ATTEMPTS = 5

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
    const candidates = list.filter(item => item.id !== excludeSpotId)
    const pool = candidates.length ? candidates : list
    if (pool.length) {
      nextSpot = pool[Math.floor(Math.random() * pool.length)]
    }
    attempts += 1
  }

  return nextSpot
}
