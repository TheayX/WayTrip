<template>
  <view class="order-detail-page" v-if="order">
    <!-- 订单状态 -->
    <view class="status-card" :class="order.status">
      <text class="status-icon">{{ getStatusIcon(order.status) }}</text>
      <text class="status-text">{{ order.statusText }}</text>
      <text class="status-desc">{{ getStatusDesc(order.status) }}</text>
    </view>

    <!-- 景点信息 -->
    <view class="spot-card card" @click="goSpot">
      <image class="spot-image" :src="getImageUrl(order.spotImage)" mode="aspectFill" />
      <view class="spot-info">
        <text class="spot-name">{{ order.spotName }}</text>
        <text class="visit-date">游玩日期：{{ order.visitDate }}</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <!-- 订单信息 -->
    <view class="info-card card">
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
      <view class="info-item" v-if="order.completedAt">
        <text class="label">完成时间</text>
        <text class="value">{{ order.completedAt }}</text>
      </view>
      <view class="info-item" v-if="order.cancelledAt">
        <text class="label">取消时间</text>
        <text class="value">{{ order.cancelledAt }}</text>
      </view>
    </view>

    <!-- 联系人信息 -->
    <view class="info-card card">
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

    <!-- 价格明细 -->
    <view class="price-card card">
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

    <!-- 底部操作 -->
    <view class="bottom-bar" v-if="order.canPay || order.canCancel">
      <button 
        v-if="order.canCancel" 
        class="action-btn cancel" 
        @click="handleCancel"
      >{{ order.status === 'paid' ? '申请退款' : '取消订单' }}</button>
      <button 
        v-if="order.canPay" 
        class="action-btn pay" 
        @click="handlePay"
      >立即支付</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getOrderDetail, payOrder, cancelOrder } from '@/api/order'
import { getImageUrl } from '@/utils/request'

const order = ref(null)
const orderId = ref(null)

// 获取订单详情
const fetchOrderDetail = async () => {
  try {
    const res = await getOrderDetail(orderId.value)
    order.value = res.data
  } catch (e) {
    uni.showToast({ title: '获取订单详情失败', icon: 'none' })
  }
}

// 状态图标
const getStatusIcon = (status) => {
  const icons = {
    pending: '⏳',
    paid: '✅',
    completed: '🎉',
    cancelled: '❌'
  }
  return icons[status] || '📋'
}

// 状态描述
const getStatusDesc = (status) => {
  const descs = {
    pending: '请在30分钟内完成支付',
    paid: '订单已支付，请按时前往游玩',
    completed: '感谢您的使用，期待再次光临',
    cancelled: '订单已取消'
  }
  return descs[status] || ''
}

// 跳转景点详情
const goSpot = () => {
  uni.navigateTo({
    url: `/pages/spot/detail?id=${order.value.spotId}`
  })
}


// 支付订单
const handlePay = async () => {
  try {
    await payOrder(orderId.value)
    uni.showToast({ title: '支付成功', icon: 'success' })
    fetchOrderDetail()
  } catch (e) {
    uni.showToast({ title: e.message || '支付失败', icon: 'none' })
  }
}

// 取消订单
const handleCancel = () => {
  uni.showModal({
    title: '提示',
    content: order.value.status === 'paid' ? '确定要申请退款吗？' : '确定要取消该订单吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(orderId.value)
          uni.showToast({ title: '订单已取消', icon: 'success' })
          fetchOrderDetail()
        } catch (e) {
          uni.showToast({ title: e.message || '取消失败', icon: 'none' })
        }
      }
    }
  })
}

onLoad((options) => {
  orderId.value = options.id
  fetchOrderDetail()
})
</script>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

/* 状态卡片 */
.status-card {
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.status-card.pending {
  background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
}

.status-card.paid {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.status-card.completed {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.status-card.cancelled {
  background: linear-gradient(135deg, #bdc3c7 0%, #2c3e50 100%);
}

.status-icon {
  font-size: 64rpx;
  margin-bottom: 16rpx;
}

.status-text {
  font-size: 36rpx;
  color: #fff;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.status-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 卡片通用 */
.card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}

/* 景点卡片 */
.spot-card {
  display: flex;
  align-items: center;
}

.spot-image {
  width: 140rpx;
  height: 100rpx;
  border-radius: 12rpx;
  margin-right: 20rpx;
}

.spot-info {
  flex: 1;
}

.spot-name {
  font-size: 30rpx;
  color: #333;
  font-weight: bold;
  display: block;
  margin-bottom: 8rpx;
}

.visit-date {
  font-size: 26rpx;
  color: #999;
}

.arrow {
  font-size: 36rpx;
  color: #ccc;
}

/* 信息卡片 */
.info-title {
  font-size: 30rpx;
  color: #333;
  font-weight: bold;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}

.info-item .label {
  font-size: 28rpx;
  color: #999;
}

.info-item .value {
  font-size: 28rpx;
  color: #333;
}

/* 价格明细 */
.price-item {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  font-size: 28rpx;
  color: #666;
}

.price-item.total {
  border-top: 1rpx solid #f5f5f5;
  margin-top: 12rpx;
  padding-top: 20rpx;
  font-size: 30rpx;
  color: #333;
}

.total-price {
  color: #ff6b6b;
  font-size: 36rpx;
  font-weight: bold;
}

/* 底部操作 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.action-btn {
  padding: 0 48rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 30rpx;
  border-radius: 40rpx;
}

.action-btn.cancel {
  background: #fff;
  color: #666;
  border: 1rpx solid #ddd;
}

.action-btn.pay {
  background: #ff6b6b;
  color: #fff;
}
</style>
