<template>
  <view class="ios-page" v-if="order">
    <view class="status-card" :class="order.status">
      <text class="status-icon">{{ getStatusIcon(order.status) }}</text>
      <text class="status-text">{{ order.statusText }}</text>
      <text class="status-desc">{{ getStatusDesc(order.status) }}</text>
      <text v-if="order.status === 'pending' && countdownText" class="countdown-text">
        剩余支付时间 {{ countdownText }}
      </text>
    </view>

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

    <view class="bottom-bar" v-if="showActions">
      <button v-if="order.canCancel" class="action-btn cancel" @click="handleCancel">
        {{ order.status === 'paid' ? '申请退款' : '取消订单' }}
      </button>
      <button v-if="order.canPay" class="action-btn pay" @click="handlePay">立即支付</button>
      <button v-if="order.status === 'completed'" class="action-btn review" @click="handleReview">去评价</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getOrderDetail, payOrder, cancelOrder } from '@/api/order'
import { getImageUrl } from '@/utils/request'

const order = ref(null)
const orderId = ref(null)
const countdownText = ref('')
let countdownTimer = null
let countdownTargetMs = null

const showActions = computed(() => {
  if (!order.value) return false
  return order.value.canPay || order.value.canCancel || order.value.status === 'completed'
})

const fetchOrderDetail = async () => {
  try {
    const res = await getOrderDetail(orderId.value)
    order.value = res.data
    setupCountdown()
  } catch (e) {
    uni.showToast({ title: '获取订单详情失败', icon: 'none' })
  }
}

const getStatusIcon = (status) => {
  const icons = {
    pending: '⏳',
    paid: '✅',
    completed: '🎉',
    cancelled: '❌',
    refunded: '💰'
  }
  return icons[status] || '🧾'
}

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
  countdownText.value = ''
  countdownTargetMs = null
  if (!order.value || order.value.status !== 'pending') return

  const createdAt = parseDateTime(order.value.createdAt)
  if (!createdAt) return
  countdownTargetMs = createdAt.getTime() + 5 * 60 * 1000

  const tick = () => {
    const remaining = Math.floor((countdownTargetMs - Date.now()) / 1000)
    countdownText.value = formatRemaining(remaining)
    if (remaining <= 0) {
      clearCountdown()
      fetchOrderDetail()
    }
  }

  tick()
  countdownTimer = setInterval(tick, 1000)
}

const goSpot = () => {
  uni.navigateTo({ url: `/pages/spot/detail?id=${order.value.spotId}&source=order` })
}

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
  uni.navigateTo({ url: `/pages/spot/detail?id=${order.value.spotId}&openReview=1&source=order` })
}

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
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background: #F2F2F7;
  padding-bottom: 160rpx;
}

.status-card {
  padding: 48rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
}

.status-card.pending { background: linear-gradient(135deg, #FF9500, #FFCC00); }
.status-card.paid { background: linear-gradient(135deg, #007AFF, #5AC8FA); }
.status-card.completed { background: linear-gradient(135deg, #34C759, #30D158); }
.status-card.cancelled { background: linear-gradient(135deg, #8E8E93, #AEAEB2); }
.status-card.refunded { background: linear-gradient(135deg, #FF3B30, #FF8A80); }

.status-icon {
  font-size: 64rpx;
  margin-bottom: 16rpx;
}

.status-text {
  font-size: 36rpx;
  color: #fff;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.status-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.countdown-text {
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #fff;
  font-weight: 600;
}

.spot-card {
  display: flex;
  align-items: center;
  margin: 24rpx 32rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.spot-image {
  width: 140rpx;
  height: 100rpx;
  border-radius: 16rpx;
  margin-right: 20rpx;
}

.spot-info {
  flex: 1;
}

.spot-name {
  font-size: 30rpx;
  color: #1C1C1E;
  font-weight: 600;
  display: block;
  margin-bottom: 8rpx;
}

.visit-date {
  font-size: 26rpx;
  color: #8E8E93;
}

.arrow {
  font-size: 36rpx;
  color: #C7C7CC;
}

.info-card {
  margin: 0 32rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.info-title {
  font-size: 30rpx;
  color: #1C1C1E;
  font-weight: 600;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1px solid #F2F2F7;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}

.info-item .label {
  font-size: 28rpx;
  color: #8E8E93;
}

.info-item .value {
  font-size: 28rpx;
  color: #1C1C1E;
}

.price-item {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  font-size: 28rpx;
  color: #8E8E93;
}

.price-item.total {
  border-top: 1px solid #F2F2F7;
  margin-top: 12rpx;
  padding-top: 20rpx;
  font-size: 30rpx;
  color: #1C1C1E;
}

.total-price {
  color: #FF3B30;
  font-size: 36rpx;
  font-weight: 700;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  padding: 20rpx 32rpx;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 -1rpx 0 rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.action-btn {
  padding: 0 48rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  border-radius: 44rpx;
  border: none;
}

.action-btn.cancel {
  background: #F2F2F7;
  color: #8E8E93;
}

.action-btn.pay {
  background: #007AFF;
  color: #fff;
}

.action-btn.review {
  background: #10B981;
  color: #fff;
}
</style>
