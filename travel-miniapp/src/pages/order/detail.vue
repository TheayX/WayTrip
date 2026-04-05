<!-- 订单详情页 -->
<template>
  <view class="ios-page" v-if="order">
    <!-- 订单状态区域 -->
    <view class="status-card" :class="order.status">
      <text class="status-kicker">订单状态</text>
      <text class="status-text">{{ order.statusText }}</text>
      <text class="status-desc">{{ getStatusDesc(order.status) }}</text>
      <text v-if="order.status === 'pending' && countdownText" class="countdown-text">
        剩余支付时间 {{ countdownText }}
      </text>
    </view>

    <!-- 景点信息区域 -->
    <view class="spot-card" @click="goSpot">
      <image class="spot-image" :src="getImageUrl(order.spotImage)" mode="aspectFill" />
      <view class="spot-info">
        <text class="spot-name">{{ order.spotName }}</text>
        <text class="visit-date">游玩日期：{{ order.visitDate }}</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <view class="info-card">
      <view class="info-title">订单信息</view>
      <view class="info-item">
        <text class="label">订单编号</text>
        <text class="value">{{ order.orderNo }}</text>
      </view>
      <view class="info-item">
        <text class="label">下单时间</text>
        <text class="value">{{ order.createdAt }}</text>
      </view>
      <view class="info-item" v-if="order.paidAt">
        <text class="label">支付时间</text>
        <text class="value">{{ order.paidAt }}</text>
      </view>
    </view>

    <view class="info-card">
      <view class="info-title">联系人信息</view>
      <view class="info-item">
        <text class="label">联系人</text>
        <text class="value">{{ order.contactName }}</text>
      </view>
      <view class="info-item">
        <text class="label">手机号</text>
        <text class="value">{{ order.contactPhone }}</text>
      </view>
    </view>

    <view class="info-card">
      <view class="info-title">价格明细</view>
      <view class="price-item">
        <text>门票单价</text>
        <text>¥{{ order.unitPrice }}</text>
      </view>
      <view class="price-item">
        <text>购买数量</text>
        <text>x{{ order.quantity }}</text>
      </view>
      <view class="price-item total">
        <text>实付金额</text>
        <text class="total-price">¥{{ order.totalPrice }}</text>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar" v-if="showActions">
      <button v-if="order.canCancel" class="action-btn cancel" @click="handleCancel">
        {{ order.status === 'paid' ? '申请退款' : '取消订单' }}
      </button>
      <button v-if="order.canPay && !timeoutRefreshInProgress" class="action-btn pay" @click="handlePay">立即支付</button>
      <button v-if="order.status === 'completed'" class="action-btn review" @click="handleReview">去评价</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getOrderDetail, payOrder, cancelOrder } from '@/api/order'
import { getImageUrl } from '@/utils/request'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'

// 页面数据状态
const order = ref(null)
const orderId = ref(null)
const countdownText = ref('')
const detailLoading = ref(false)
let countdownTimer = null
let countdownTargetMs = null
let hasTriggeredTimeoutRefresh = false
let pendingTimeoutRefresh = false
const timeoutRefreshInProgress = ref(false)
let timeoutRetryTimer = null
let timeoutRetryCount = 0
const MAX_TIMEOUT_REFRESH_RETRIES = 6
const TIMEOUT_REFRESH_RETRY_DELAY = 800

// 计算属性
const showActions = computed(() => {
  if (!order.value) return false
  return order.value.canPay || order.value.canCancel || order.value.status === 'completed'
})

// 数据加载方法
const fetchOrderDetail = async () => {
  if (!orderId.value || detailLoading.value) return

  detailLoading.value = true
  try {
    const res = await getOrderDetail(orderId.value)
    order.value = res.data
    if (order.value?.status !== 'pending') {
      clearTimeoutRetry()
      timeoutRefreshInProgress.value = false
      timeoutRetryCount = 0
    }
    setupCountdown()
  } catch (e) {
    uni.showToast({ title: '获取订单详情失败', icon: 'none' })
  } finally {
    detailLoading.value = false
    if (pendingTimeoutRefresh) {
      pendingTimeoutRefresh = false
      fetchOrderDetail()
      return
    }

    if (timeoutRefreshInProgress.value && order.value?.status === 'pending' && timeoutRetryCount < MAX_TIMEOUT_REFRESH_RETRIES) {
      scheduleTimeoutRetry()
    }
  }
}

const clearTimeoutRetry = () => {
  if (timeoutRetryTimer) {
    clearTimeout(timeoutRetryTimer)
    timeoutRetryTimer = null
  }
}

const scheduleTimeoutRetry = () => {
  clearTimeoutRetry()
  timeoutRetryTimer = setTimeout(() => {
    timeoutRetryCount += 1
    fetchOrderDetail()
  }, TIMEOUT_REFRESH_RETRY_DELAY)
}

const triggerTimeoutRefresh = () => {
  timeoutRefreshInProgress.value = true
  timeoutRetryCount = 0
  clearTimeoutRetry()

  if (detailLoading.value) {
    pendingTimeoutRefresh = true
    return
  }

  fetchOrderDetail()
}

// 工具方法
const getStatusDesc = (status) => {
  const descs = {
    pending: '请在5分钟内完成支付',
    paid: '订单已支付，请按时前往游玩',
    completed: '订单已完成，可去评价',
    cancelled: '订单已取消',
    refunded: '订单已退款'
  }
  return descs[status] || ''
}

const parseDateTime = (value) => {
  if (!value) return null
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return null
  return date
}

const formatRemaining = (seconds) => {
  if (seconds <= 0) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const clearCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const setupCountdown = () => {
  clearCountdown()
  countdownTargetMs = null
  if (!order.value || order.value.status !== 'pending') return

  if (timeoutRefreshInProgress.value) {
    return
  }

  countdownText.value = ''
  hasTriggeredTimeoutRefresh = false

  const createdAt = parseDateTime(order.value.createdAt)
  if (!createdAt) return
  countdownTargetMs = createdAt.getTime() + 5 * 60 * 1000

  const tick = () => {
    const remaining = Math.floor((countdownTargetMs - Date.now()) / 1000)
    countdownText.value = formatRemaining(remaining)
    if (remaining <= 0 && !hasTriggeredTimeoutRefresh) {
      // 倒计时归零后只刷新一次，避免页面在状态切换期间重复请求。
      hasTriggeredTimeoutRefresh = true
      clearCountdown()
      triggerTimeoutRefresh()
    }
  }

  tick()
  countdownTimer = setInterval(tick, 1000)
}

// 页面跳转方法
const goSpot = () => {
  uni.navigateTo({ url: buildSpotDetailUrl(order.value.spotId, SPOT_DETAIL_SOURCE.ORDER) })
}

// 交互处理方法
const handlePay = async () => {
  try {
    await payOrder(orderId.value)
    uni.showToast({ title: '支付成功', icon: 'success' })
    fetchOrderDetail()
  } catch (e) {
    uni.showToast({ title: e.message || '支付失败', icon: 'none' })
  }
}

const handleCancel = () => {
  uni.showModal({
    title: '提示',
    content: order.value.status === 'paid' ? '确定要申请退款吗？' : '确定要取消该订单吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(orderId.value)
          uni.showToast({ title: '操作成功', icon: 'success' })
          fetchOrderDetail()
        } catch (e) {
          uni.showToast({ title: e.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

const handleReview = () => {
  uni.navigateTo({ url: buildSpotDetailUrl(order.value.spotId, SPOT_DETAIL_SOURCE.ORDER, { openReview: true }) })
}

// 生命周期
onLoad((options) => {
  orderId.value = options.id
  fetchOrderDetail()
})

onShow(() => {
  if (orderId.value) {
    fetchOrderDetail()
  }
})

onUnload(() => {
  clearCountdown()
  clearTimeoutRetry()
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.94), rgba(245, 247, 250, 0.9) 48%, rgba(238, 242, 247, 1) 100%),
    linear-gradient(180deg, #fafafa 0%, #eef2f7 100%);
  padding-bottom: 180rpx;
}

.status-card {
  margin: 32rpx;
  padding: 60rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 40rpx;
  position: relative;
  overflow: hidden;
  box-shadow: 0 16rpx 40rpx rgba(0, 0, 0, 0.08);
}

.status-card.pending {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  box-shadow: 0 16rpx 40rpx rgba(245, 158, 11, 0.22);
}
.status-card.paid {
  background: linear-gradient(135deg, #18181b, #3f3f46);
  box-shadow: 0 16rpx 40rpx rgba(24, 24, 27, 0.2);
}
.status-card.completed {
  background: linear-gradient(135deg, #0f766e, #115e59);
  box-shadow: 0 16rpx 40rpx rgba(15, 118, 110, 0.2);
}
.status-card.cancelled {
  background: linear-gradient(135deg, #64748b, #475569);
  box-shadow: 0 16rpx 40rpx rgba(100, 116, 139, 0.25);
}
.status-card.refunded {
  background: linear-gradient(135deg, #be123c, #9f1239);
  box-shadow: 0 16rpx 40rpx rgba(190, 24, 93, 0.22);
}

.status-kicker {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 18rpx;
}

.status-text {
  font-size: 40rpx;
  color: #fff;
  font-weight: 800;
  margin-bottom: 12rpx;
  letter-spacing: 2rpx;
}

.status-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

.countdown-text {
  margin-top: 12rpx;
  font-size: 28rpx;
  color: #fff;
  font-weight: 700;
  background: rgba(0,0,0,0.15);
  padding: 8rpx 24rpx;
  border-radius: 99rpx;
}

.spot-card {
  display: flex;
  align-items: center;
  margin: 0 32rpx 24rpx;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 36rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.spot-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: 28rpx;
  margin-right: 24rpx;
  background: #f1f5f9;
}

.spot-info {
  flex: 1;
}

.spot-name {
  font-size: 32rpx;
  color: #1e293b;
  font-weight: 700;
  display: block;
  margin-bottom: 12rpx;
}

.visit-date {
  font-size: 26rpx;
  color: #64748b;
}

.arrow {
  font-size: 40rpx;
  color: #cbd5e1;
  padding-left: 16rpx;
}

.info-card {
  margin: 0 32rpx 24rpx;
  padding: 32rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 36rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.info-title {
  font-size: 32rpx;
  color: #1e293b;
  font-weight: 700;
  margin-bottom: 24rpx;
  padding-bottom: 20rpx;
  border-bottom: 2rpx dashed #f1f5f9;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
}

.info-item .label {
  font-size: 28rpx;
  color: #64748b;
}

.info-item .value {
  font-size: 28rpx;
  color: #1e293b;
  font-weight: 500;
}

.price-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: #64748b;
}

.price-item.total {
  border-top: 2rpx dashed #f1f5f9;
  margin-top: 16rpx;
  padding-top: 24rpx;
  font-size: 30rpx;
  color: #1e293b;
  font-weight: 600;
}

.total-price {
  color: #9f1239;
  font-size: 40rpx;
  font-weight: 800;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  padding: 24rpx 32rpx;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 -8rpx 24rpx rgba(17, 24, 39, 0.04);
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
}

.action-btn {
  padding: 0 48rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  border-radius: 99rpx;
  border: none;
  font-weight: 700;
  margin: 0;
}

.action-btn.cancel {
  background: rgba(17, 24, 39, 0.06);
  color: #52525b;
}

.action-btn.pay {
  background: #18181b;
  color: #fff;
  box-shadow: 0 10rpx 24rpx rgba(24, 24, 27, 0.18);
}

.action-btn.review {
  background: rgba(255, 255, 255, 0.72);
  color: #18181b;
  border: 1rpx solid rgba(15, 23, 42, 0.12);
}
</style>
