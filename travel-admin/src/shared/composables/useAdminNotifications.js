import { computed, ref } from 'vue'
import { getUserList } from '@/modules/user-ops/api/user.js'
import { getOrderList } from '@/modules/order/api.js'

const DEFAULT_PAGE_SIZE = 5
const RECENT_HOURS = 24

const toTimestamp = (value) => {
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

const isRecent = (value, hours = RECENT_HOURS) => {
  const timestamp = toTimestamp(value)
  if (!timestamp) return false
  return Date.now() - timestamp <= hours * 60 * 60 * 1000
}

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatRelativeTime = (value) => {
  const timestamp = toTimestamp(value)
  if (!timestamp) return ''

  const delta = Math.max(0, Date.now() - timestamp)
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (delta < minute) return '刚刚'
  if (delta < hour) return `${Math.max(1, Math.floor(delta / minute))} 分钟前`
  if (delta < day) return `${Math.max(1, Math.floor(delta / hour))} 小时前`
  if (delta < 7 * day) return `${Math.max(1, Math.floor(delta / day))} 天前`
  return formatDateTime(value)
}

const maskPhone = (phone) => {
  const normalized = String(phone || '').trim()
  if (!normalized) return ''
  if (/^1\d{10}$/.test(normalized)) {
    return `${normalized.slice(0, 3)}****${normalized.slice(7)}`
  }
  return normalized
}

const normalizeUserNotification = (item) => ({
  id: `user-${item.id}`,
  type: 'user',
  typeLabel: '新用户',
  title: item.nickname || '未命名用户',
  description: [maskPhone(item.phone), item.createdAt ? formatRelativeTime(item.createdAt) : ''].filter(Boolean).join(' · '),
  timeLabel: formatRelativeTime(item.createdAt),
  createdAt: item.createdAt,
  route: {
    path: '/user',
    query: item.nickname ? { nickname: item.nickname } : {}
  }
})

const normalizeOrderNotification = (item) => {
  const amount = item.totalPrice ?? item.amount ?? item.payAmount ?? item.price
  return {
    id: `order-${item.id}`,
    type: 'order',
    typeLabel: '新订单',
    title: item.orderNo || `订单 #${item.id}`,
    description: [item.spotName || '景点订单', amount !== undefined ? `¥${amount}` : '', item.statusText || item.status || ''].filter(Boolean).join(' · '),
    timeLabel: formatRelativeTime(item.createdAt),
    createdAt: item.createdAt,
    route: {
      path: '/order',
      query: item.orderNo ? { orderNo: item.orderNo } : {}
    }
  }
}

const sortByLatest = (items) => items.sort((left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt))

export function useAdminNotifications() {
  const loading = ref(false)
  const errorMessage = ref('')
  const lastLoadedAt = ref('')
  const userNotifications = ref([])
  const orderNotifications = ref([])

  const notificationSections = computed(() => ([
    {
      key: 'user',
      title: '新注册用户',
      count: userNotifications.value.length,
      items: userNotifications.value
    },
    {
      key: 'order',
      title: '新订单',
      count: orderNotifications.value.length,
      items: orderNotifications.value
    }
  ]))

  const notificationCount = computed(() => userNotifications.value.length + orderNotifications.value.length)
  const hasNotifications = computed(() => notificationCount.value > 0)
  const lastLoadedLabel = computed(() => (lastLoadedAt.value ? `最近更新于 ${formatRelativeTime(lastLoadedAt.value)}` : ''))

  const loadNotifications = async () => {
    loading.value = true
    errorMessage.value = ''

    try {
      const [userRes, orderRes] = await Promise.all([
        getUserList({ page: 1, pageSize: DEFAULT_PAGE_SIZE }),
        getOrderList({ page: 1, pageSize: DEFAULT_PAGE_SIZE })
      ])

      userNotifications.value = sortByLatest(
        (userRes.data?.list || [])
          .filter((item) => isRecent(item.createdAt))
          .map(normalizeUserNotification)
      )
      orderNotifications.value = sortByLatest(
        (orderRes.data?.list || [])
          .filter((item) => isRecent(item.createdAt))
          .map(normalizeOrderNotification)
      )
      lastLoadedAt.value = new Date().toISOString()
    } catch (error) {
      errorMessage.value = error?.response?.data?.message || error?.message || '消息加载失败'
      userNotifications.value = []
      orderNotifications.value = []
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    errorMessage,
    lastLoadedAt,
    lastLoadedLabel,
    notificationSections,
    notificationCount,
    hasNotifications,
    loadNotifications
  }
}

void useAdminNotifications


