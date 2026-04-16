// 后台导航分组常量
export const NAVIGATION_GROUPS = [
  { key: 'dashboard', title: '仪表盘', icon: 'Odometer' },
  { key: 'content', title: '内容管理', icon: 'Files' },
  { key: 'transaction', title: '交易管理', icon: 'Tickets' },
  { key: 'user-ops', title: '用户运营', icon: 'UserFilled' },
  { key: 'recommendation', title: '推荐系统', icon: 'MagicStick' },
  { key: 'ai-service', title: 'AI 客服中心', icon: 'ChatDotSquare' },
  { key: 'system', title: '系统管理', icon: 'Setting' }
]

// 分组映射供菜单、搜索和面包屑按 key 快速读取元信息。
export const NAVIGATION_GROUP_MAP = NAVIGATION_GROUPS.reduce((acc, item) => {
  acc[item.key] = item
  return acc
}, {})
