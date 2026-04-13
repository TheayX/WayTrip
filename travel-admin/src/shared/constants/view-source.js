import { SOURCE_DISPLAY_TEXT } from '@/shared/constants/resource-display.js'

// 原始来源与算法挡位分层管理，避免展示文案和代码枚举混在一起。
export const sourceOptions = [
  { label: '首页', value: 'home' },
  { label: '搜索', value: 'search' },
  { label: '攻略', value: 'guide' },
  { label: '个性推荐', value: 'recommendation' },
  { label: '发现页', value: 'discover' },
  { label: '随心一选', value: 'random-pick' },
  { label: '穷游玩法', value: 'budget-travel' },
  { label: '游客口碑', value: 'traveler-reviews' },
  { label: '近期热看', value: 'trending-views' },
  { label: '景点大全', value: 'list' },
  { label: '附近探索', value: 'nearby' },
  { label: '相似景点', value: 'similar' },
  { label: '订单', value: 'order' },
  { label: '浏览记录', value: 'footprint' },
  { label: '收藏', value: 'favorite' },
  { label: '我的评价', value: 'review' },
  { label: '详情页', value: 'detail' }
]

// 推荐算法只消费有限几个挡位，页面里看到的来源会先映射到这里。
export const sourceBucketOptions = [
  { label: '首页挡位', value: 'home' },
  { label: '搜索挡位', value: 'search' },
  { label: '攻略挡位', value: 'guide' },
  { label: '推荐挡位', value: 'recommendation' },
  { label: '默认挡位', value: 'detail' }
]

// 原始来源到算法挡位的映射统一从这里维护，避免前后文案解释不一致。
export const sourceToBucketMap = {
  home: 'home',
  search: 'search',
  guide: 'guide',
  recommendation: 'recommendation',
  discover: 'recommendation',
  'random-pick': 'recommendation',
  'budget-travel': 'recommendation',
  'traveler-reviews': 'recommendation',
  'trending-views': 'recommendation',
  detail: 'detail',
  list: 'detail',
  nearby: 'detail',
  similar: 'detail',
  order: 'detail',
  footprint: 'detail',
  favorite: 'detail',
  review: 'detail'
}

export const getSourceLabel = (value) => {
  return sourceOptions.find(item => item.value === value)?.label || SOURCE_DISPLAY_TEXT.UNKNOWN
}

// 未识别来源统一退回默认 detail 挡位，保证算法侧有稳定兜底。
export const getSourceBucket = (source) => {
  return sourceToBucketMap[source] || 'detail'
}

export const getSourceBucketLabel = (source) => {
  const bucket = getSourceBucket(source)
  return sourceBucketOptions.find(item => item.value === bucket)?.label || bucket
}
