<!-- 订单详情页 -->
<template>
  <div class="page-container" v-if="order">
    <AccountPageHeader title="订单详情" subtitle="查看订单状态、联系人信息和价格明细。" />

    <div class="detail-layout">
      <div class="detail-main">
        <div class="status-card card" :class="order.status">
          <div class="status-info">
            <el-icon class="status-icon"><component :is="getStatusIcon(order.status)" /></el-icon>
            <div>
              <h2 class="status-text">{{ order.statusText }}</h2>
              <p class="status-desc">{{ getStatusDesc(order.status) }}</p>
              <p v-if="order.status === 'pending' && countdownText" class="countdown-text">
                剩余支付时间 {{ countdownText }}
              </p>
            </div>
          </div>
        </div>

        <div class="info-card card" :class="{ 'is-disabled': isInvalidSpot(order?.spotName) }" @click="openSpotDetail">
          <h3 class="card-title">景点信息</h3>
          <div class="spot-row">
            <img :src="getImageUrl(order.spotImage)" class="spot-thumb" alt="" />
            <div class="spot-info">
              <span class="spot-name">{{ resolveSpotDisplayName(order.spotName) }}</span>
              <span class="spot-date">游玩日期：{{ order.visitDate }}</span>
            </div>
            <el-icon v-if="!isInvalidSpot(order?.spotName)"><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="info-card card">
          <h3 class="card-title">订单信息</h3>
          <div class="info-row">
            <span class="label">订单编号</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="label">下单时间</span>
            <span class="value">{{ order.createdAt }}</span>
          </div>
          <div class="info-row" v-if="order.paidAt">
            <span class="label">支付时间</span>
            <span class="value">{{ order.paidAt }}</span>
          </div>
        </div>

        <div class="info-card card">
          <h3 class="card-title">联系人信息</h3>
          <div class="info-row">
            <span class="label">联系人</span>
            <span class="value">{{ order.contactName }}</span>
          </div>
          <div class="info-row">
            <span class="label">手机号</span>
            <span class="value">{{ order.contactPhone }}</span>
          </div>
        </div>

        <div class="info-card card">
          <h3 class="card-title">价格明细</h3>
          <div class="info-row">
            <span class="label">门票单价</span>
            <span class="value">￥{{ order.unitPrice }}</span>
          </div>
          <div class="info-row">
            <span class="label">购买数量</span>
            <span class="value">x{{ order.quantity }}</span>
          </div>
          <el-divider />
          <div class="info-row total">
            <span class="label">实付金额</span>
            <span class="total-price">￥{{ order.totalPrice }}</span>
          </div>
        </div>
      </div>

      <div class="detail-sidebar">
        <div class="sidebar-card card" v-if="showActions">
          <h3 class="card-title">操作</h3>
          <el-button
            v-if="order.canPay && !timeoutRefreshInProgress"
            type="primary"
            size="large"
            class="action-btn"
            @click="handlePay"
          >
            立即支付
          </el-button>
          <el-button v-if="order.canCancel" size="large" class="action-btn" @click="handleCancel">
            {{ order.status === 'paid' ? '申请退款' : '取消订单' }}
          </el-button>
          <el-button
            v-if="order.status === 'completed'"
            type="primary"
            size="large"
            class="action-btn"
            :disabled="isInvalidSpot(order?.spotName)"
            @click="handleReview"
          >
            去评价
          </el-button>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="page-container">
    <el-skeleton :rows="10" animated />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Timer, SuccessFilled, CircleCheckFilled, CircleCloseFilled, RefreshLeft, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountPageHeader from '@/modules/account/components/AccountPageHeader.vue'
import { getOrderDetail, payOrder, cancelOrder } from '@/modules/order/api.js'
import { getImageUrl } from '@/shared/api/client.js'
import { isWebInvalidSpotDisplay, resolveWebSpotDisplayName } from '@/shared/constants/resource-display.js'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'

// 订单详情页同时负责状态展示、倒计时和支付后刷新，因此把时序状态集中在页面层维护。
const route = useRoute()
const router = useRouter()

const order = ref(null)
const countdownText = ref('')
const detailLoading = ref(false)
const timeoutRefreshInProgress = ref(false)

let countdownTimer = null
let countdownTargetMs = null
let hasTriggeredTimeoutRefresh = false
let pendingTimeoutRefresh = false

const showActions = computed(() => {
  if (!order.value) return false
  return order.value.canPay || order.value.canCancel || order.value.status === 'completed'
})
const resolveSpotDisplayName = (spotName) => resolveWebSpotDisplayName(spotName)
const isInvalidSpot = (spotName) => isWebInvalidSpotDisplay(spotName)

// 状态图标和文案在详情页统一映射，避免模板里堆叠多段条件判断。
const getStatusIcon = (status) => {
  const map = {
    pending: Timer,
    paid: SuccessFilled,
    completed: CircleCheckFilled,
    cancelled: CircleCloseFilled,
    refunded: RefreshLeft
  }
  return map[status] || Document
}

const getStatusDesc = (status) => {
  const map = {
    pending: '请在 5 分钟内完成支付',
    paid: '已支付成功，祝您旅途愉快',
    completed: '订单已完成，可前往评价',
    cancelled: '订单已取消',
    refunded: '订单已退款'
  }
  return map[status] || ''
}

const parseDateTime = (value) => {
  if (!value) return null
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

const formatRemaining = (seconds) => {
  if (seconds <= 0) return '00:00'
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = Math.floor(seconds % 60)
  return `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`
}

const clearCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdownTargetMs = null
}

const fetchDetail = async ({ autoCancelOnPending = false } = {}) => {
  // 倒计时超时后会复用同一套拉取逻辑，必要时顺带触发自动取消并刷新状态。
  if (detailLoading.value) return

  detailLoading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res.data

    if (autoCancelOnPending && order.value?.status === 'pending') {
      await cancelOrder(order.value.id)
      const refreshed = await getOrderDetail(route.params.id)
      order.value = refreshed.data
      // ElMessage.warning('订单支付超时，已自动取消')
    }

    if (order.value?.status !== 'pending') {
      timeoutRefreshInProgress.value = false
    }

    setupCountdown()
  } catch (_error) {
    ElMessage.error('获取订单详情失败')
  } finally {
    detailLoading.value = false
    if (pendingTimeoutRefresh) {
      pendingTimeoutRefresh = false
      void fetchDetail({ autoCancelOnPending: true })
    }
  }
}

const triggerTimeoutRefresh = async () => {
  timeoutRefreshInProgress.value = true

  if (detailLoading.value) {
    pendingTimeoutRefresh = true
    return
  }

  await fetchDetail({ autoCancelOnPending: true })
}

const setupCountdown = () => {
  // 只对待支付订单启动倒计时，其他状态直接清掉残留定时器。
  clearCountdown()
  countdownText.value = ''

  if (!order.value || order.value.status !== 'pending' || timeoutRefreshInProgress.value) {
    return
  }

  const createdAt = parseDateTime(order.value.createdAt)
  if (!createdAt) return

  hasTriggeredTimeoutRefresh = false
  countdownTargetMs = createdAt.getTime() + 5 * 60 * 1000

  const tick = () => {
    const remaining = Math.floor((countdownTargetMs - Date.now()) / 1000)
    countdownText.value = formatRemaining(remaining)

    if (remaining <= 0 && !hasTriggeredTimeoutRefresh) {
      hasTriggeredTimeoutRefresh = true
      clearCountdown()
      void triggerTimeoutRefresh()
    }
  }

  tick()
  countdownTimer = window.setInterval(tick, 1000)
}

const handlePay = async () => {
  try {
    await ElMessageBox.confirm('确定要支付该订单吗？', '支付确认', { type: 'info' })
    const idempotentKey = `${order.value.id}-${Date.now()}`
    await payOrder(order.value.id, idempotentKey)
    ElMessage.success('支付成功')
    timeoutRefreshInProgress.value = false
    void fetchDetail()
  } catch (_error) {
  }
}

const handleCancel = async () => {
  try {
    const message = order.value.status === 'paid' ? '确定要申请退款吗？' : '确定要取消订单吗？'
    await ElMessageBox.confirm(message, '提示', { type: 'warning' })
    await cancelOrder(order.value.id)
    ElMessage.success('操作成功')
    timeoutRefreshInProgress.value = false
    void fetchDetail()
  } catch (_error) {
  }
}

const handleReview = () => {
  // 评价入口直接回到景点详情，并通过 query/状态打开评价面板。
  if (isInvalidSpot(order.value?.spotName)) return
  router.push(buildSpotDetailRoute(order.value.spotId, SPOT_DETAIL_SOURCE.ORDER, { openReview: true }))
}

const openSpotDetail = () => {
  if (isInvalidSpot(order.value?.spotName)) return
  router.push(buildSpotDetailRoute(order.value.spotId, SPOT_DETAIL_SOURCE.ORDER))
}

onMounted(() => {
  void fetchDetail()
})

onUnmounted(() => {
  clearCountdown()
})
</script>

<style lang="scss" scoped>
.detail-layout {
  display: flex;
  gap: 24px;
  margin-top: 8px;
}

.detail-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.status-card {
  padding: 24px;
  border-radius: 12px;

  &.pending { background: linear-gradient(135deg, #fff7e6, #fff1cc); }
  &.paid { background: linear-gradient(135deg, #e6f7e6, #c8f0c8); }
  &.completed { background: linear-gradient(135deg, #e6f0ff, #ccdeff); }
  &.cancelled { background: linear-gradient(135deg, #ffe6e6, #ffd6d6); }
  &.refunded { background: linear-gradient(135deg, #fce7f3, #fbcfe8); }
}

.status-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-icon {
  font-size: 40px;
}

.status-text {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}

.status-desc {
  font-size: 14px;
  color: #606266;
}

.countdown-text {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #8a5a00;
}

.info-card {
  padding: 20px;
  border-radius: 12px;
}

.info-card.is-disabled {
  cursor: default;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.spot-row {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.info-card.is-disabled .spot-row {
  cursor: default;
}

.spot-thumb {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.spot-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spot-name {
  font-weight: 600;
}

.spot-date {
  font-size: 13px;
  color: #909399;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;

  .label { color: #909399; }
  .value { color: #303133; }

  &.total {
    font-weight: 600;
    font-size: 16px;
  }
}

.total-price {
  font-size: 22px;
  color: #f56c6c;
  font-weight: 700;
}

.sidebar-card {
  padding: 20px;
  border-radius: 12px;
  position: sticky;
  top: 80px;
}

.action-btn {
  width: 100%;
  border-radius: 8px;
  margin-bottom: 8px;
  margin-left: 0 !important;

  &:last-child {
    margin-bottom: 0;
  }
}

@media (max-width: 992px) {
  .detail-layout {
    flex-direction: column;
  }

  .detail-sidebar {
    width: 100%;
  }
}
</style>
