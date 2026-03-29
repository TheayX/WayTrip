import { getSpotList } from '@/api/spot'

const DEFAULT_PAGE_SIZE = 200

export async function fetchAllSpotOptions() {
  const allSpots = []
  let page = 1
  let total = 0

  do {
    const res = await getSpotList({ page, pageSize: DEFAULT_PAGE_SIZE })
    const list = res.data.list || []
    total = res.data.total || 0
    allSpots.push(...list)
    page += 1
  } while (allSpots.length < total)

  return allSpots
}
