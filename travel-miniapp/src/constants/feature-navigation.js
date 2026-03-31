// 功能入口主题色统一收口，避免首页和更多页各自维护一份。
export const featureThemeColorMap = {
  blue: '#2563eb',
  orange: '#f97316',
  amber: '#d97706',
  emerald: '#059669',
  purple: '#7c3aed'
}

const featureEntries = [
  { id: 'spots', title: '全部景点', desc: '按区域和分类浏览', icon: 'location-filled', theme: 'blue', url: '/pages/spot/list?sortBy=heat', section: 'browse' },
  { id: 'guides', title: '游玩攻略', desc: '看路线和游玩经验', icon: 'paperplane-filled', theme: 'orange', url: '/pages/guide/list?sortBy=time', section: 'browse' },
  { id: 'recommend', title: '推荐景点', desc: '查看个性化推荐', icon: 'star-filled', theme: 'amber', url: '/pages/recommendation/index', section: 'browse' },
  { id: 'nearby', title: '附近探索', desc: '定位查看周边景点', icon: 'map-filled', theme: 'emerald', url: '/pages/spot/nearby', section: 'browse' },
  { id: 'blindbox', title: '每日盲盒', desc: '随机抽一个目的地', icon: 'gift-filled', theme: 'purple', url: '/pages/feature/blindbox/index', section: 'feature' },
  { id: 'budget', title: '穷游特惠', desc: '低预算景点和攻略', icon: 'wallet-filled', theme: 'orange', url: '/pages/feature/budget/index', section: 'feature' },
  { id: 'reviews', title: '真实口碑', desc: '看游客真实评价', icon: 'chatboxes-filled', theme: 'blue', url: '/pages/feature/reviews/index', section: 'feature' },
  { id: 'more', title: '更多', desc: '功能总览和后续扩展', icon: 'grid-filled', theme: 'emerald', url: '/pages/feature/more/index', section: 'feature' }
]

export const homeQuickActionIds = ['spots', 'guides', 'recommend', 'nearby', 'blindbox', 'budget', 'reviews', 'more']

export const getFeatureEntryById = (id) => featureEntries.find(item => item.id === id) || null

export const getHomeQuickActions = () => homeQuickActionIds
  .map(id => getFeatureEntryById(id))
  .filter(Boolean)

export const getMoreFeatureGroups = () => ([
  {
    title: '常用浏览',
    items: featureEntries.filter(item => item.section === 'browse')
  },
  {
    title: '特色功能',
    items: featureEntries.filter(item => item.section === 'feature' && item.id !== 'more')
  }
])
