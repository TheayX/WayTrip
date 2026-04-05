// 功能入口主题色统一收口，避免不同页面各自维护一份。
export const featureEntryThemeMap = {
  blue: '#2563eb',
  orange: '#f97316',
  amber: '#d97706',
  emerald: '#059669',
  purple: '#7c3aed'
}

const featureEntryRegistry = [
  { id: 'spots', title: '景点大全', desc: '按区域和分类浏览', icon: 'location-filled', theme: 'blue', url: '/pages/spot/list?sortBy=heat', section: 'browse' },
  { id: 'guides', title: '出游攻略', desc: '看路线和游玩经验', icon: 'paperplane-filled', theme: 'orange', url: '/pages/guide/list?sortBy=time', section: 'browse' },
  { id: 'recommend', title: '个性推荐', desc: '查看个性化推荐', icon: 'star-filled', theme: 'amber', url: '/pages/recommendation/index', section: 'browse' },
  { id: 'nearby', title: '附近探索', desc: '定位查看周边景点', icon: 'map-filled', theme: 'emerald', url: '/pages/spot/nearby', section: 'browse' },
  { id: 'random-pick', title: '随心一选', desc: '随机抽一个目的地', icon: 'gift-filled', theme: 'purple', url: '/pages/random-pick/index', section: 'feature' },
  { id: 'budget', title: '穷游玩法', desc: '低预算景点和攻略', icon: 'wallet-filled', theme: 'orange', url: '/pages/budget-travel/index', section: 'feature' },
  { id: 'reviews', title: '游客口碑', desc: '看游客真实评价', icon: 'chatboxes-filled', theme: 'blue', url: '/pages/traveler-reviews/index', section: 'feature' },
  { id: 'trending-views', title: '近期热看', desc: '看看最近浏览更高的景点', icon: 'eye-filled', theme: 'amber', url: '/pages/trending-views/index', section: 'feature' },
  { id: 'more', title: '更多', desc: '功能总览和后续扩展', icon: 'more-filled', theme: 'emerald', url: '/pages/more/index', section: 'feature' },
  { id: 'city-topic', title: '城市专题', desc: '按城市整理主题玩法', icon: 'flag-filled', theme: 'blue', section: 'placeholder', available: false },
  { id: 'holiday-plan', title: '假日玩法', desc: '节日和假期的出游专题', icon: 'calendar-filled', theme: 'orange', section: 'placeholder', available: false },
  { id: 'route-list', title: '路线清单', desc: '半日游和一日游路线组合', icon: 'paperclip', theme: 'amber', section: 'placeholder', available: false },
  { id: 'activity-zone', title: '活动专区', desc: '后续活动和限时专题入口', icon: 'notifications-filled', theme: 'purple', section: 'placeholder', available: false }
]

const homeEntryIds = ['spots', 'guides', 'recommend', 'nearby', 'random-pick', 'budget', 'reviews', 'more']

export const getFeatureEntryById = (id) => featureEntryRegistry.find(item => item.id === id) || null

export const getHomeEntryItems = () => homeEntryIds
  .map(id => getFeatureEntryById(id))
  .filter(Boolean)

export const getMoreEntryGroups = () => ([
  {
    title: '常用浏览',
    items: featureEntryRegistry.filter(item => item.section === 'browse')
  },
  {
    title: '特色功能',
    items: featureEntryRegistry.filter(item => item.section === 'feature' && item.id !== 'more')
  },
  {
    title: '即将上线',
    items: featureEntryRegistry.filter(item => item.section === 'placeholder')
  }
])
