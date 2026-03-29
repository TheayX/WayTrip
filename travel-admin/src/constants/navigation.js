export const NAVIGATION_GROUPS = [
  { key: 'dashboard', title: '仪表盘', icon: 'Odometer', single: true },
  { key: 'recommendation', title: '推荐系统', icon: 'MagicStick', single: true },
  { key: 'content', title: '内容管理', icon: 'Files' },
  { key: 'transaction', title: '交易运营', icon: 'Tickets', single: true },
  { key: 'user-ops', title: '用户运营', icon: 'UserFilled' },
  { key: 'system', title: '系统管理', icon: 'Setting' }
]

export const NAVIGATION_GROUP_MAP = NAVIGATION_GROUPS.reduce((acc, item) => {
  acc[item.key] = item
  return acc
}, {})
