import { getSpotList } from '@/modules/spot/api.js'

const DEFAULT_PAGE_SIZE = 200

// 统一拉取景点选项，供轮播图、攻略等跨模块表单复用。
export async function fetchAllSpotOptions() {
  const allSpots = []
  let page = 1
  let total = 0

  do {
    // 后端列表接口按页返回，这里主动翻页，避免调用方各自重复拼装选项数据。
    const res = await getSpotList({ page, pageSize: DEFAULT_PAGE_SIZE })
    const list = res.data.list || []
    total = res.data.total || 0
    allSpots.push(...list)
    page += 1
  } while (allSpots.length < total)

  return allSpots
}
