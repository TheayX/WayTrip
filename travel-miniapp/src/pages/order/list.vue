<template>
  <view class="ios-page">
    <!-- 状态筛选 -->
    <view class="tabs">
      <view 
        v-for="tab in tabs" 
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- 订单列表 -->
    <scroll-view scroll-y class="order-list" @scrolltolower="loadMore">
      <view v-if="orderList.length">
        <view 
          class="order-card" 
          v-for="order in orderList" 
          :key="order.id"
          @click="goDetail(order.id)"
        >
          <view class="order-header">
            <text class="order-no">订单号：{{ order.orderNo }}</text>
            <text class="order-status" :class="order.status">{{ order.statusText }}</text>
          </view>

          <view class="order-content">
            <image class="spot-image" :src="getImageUrl(order.spotImage)" mode="aspectFill" />
            <view class="spot-info">
              <text class="spot-name">{{ order.spotName }}</text>
              <text class="visit-date">游玩日期：{{ order.visitDate }}</text>
              <view class="price-row">
                <text class="price">¥{{ order.totalPrice }}</text>
                <text class="quantity">x{{ order.quantity }}</text>
              </view>
            </view>
          </view>

          <view class="order-actions" v-if="order.status === 'pending' || order.status === 'paid'">
            <button 
              v-if="order.canCancel" 
              class="action-btn cancel" 
              @click.stop="handleCancel(order)"
            >{{ order.status === 'paid' ? '申请退款' : '取消订单' }}</button>
            <button 
              v-if="order.status === 'pending'" 
              class="action-btn pay" 
              @click.stop="handlePay(order)"
            >去支付</button>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-else-if="!loading">
        <text class="empty-text">暂无订单</text>
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      <view class="no-more" v-else-if="noMore && orderList.length">
        <text>没有更多了</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getOrderList, cancelOrder, payOrder } from '@/api/order'
import { getImageUrl } from '@/utils/request'

const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'pending' },
  { label: '已支付', value: 'paid' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

const currentTab = ref('')
const orderList = ref([])
const loading = ref(false)
const noMore = ref(false)
const pagination = reactive({ page: 1, pageSize: 10 })

const fetchOrders = async (refresh = false) => {
  if (loading.value) return
  if (!refresh && noMore.value) return

  if (refresh) {
    pagination.page = 1
    noMore.value = false
  }

  loading.value = true
  try {
    const res = await getOrderList({
      status: currentTab.value || undefined,
      page: pagination.page,
      pageSize: pagination.pageSize
    })

    const list = res.data.list || []
    if (refresh) {
      orderList.value = list
    } else {
      orderList.value.push(...list)
    }

    if (list.length < pagination.pageSize) {
      noMore.value = true
    }
  } catch (e) {
    console.error('获取订单列表失败', e)
  } finally {
    loading.value = false
  }
}

const switchTab = (value) => {
  currentTab.value = value
  fetchOrders(true)
}

const loadMore = () => {
  if (!noMore.value && !loading.value) {
    pagination.page++
    fetchOrders()
  }
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
}

const handleCancel = async (order) => {
  uni.showModal({
    title: '提示',
    content: order.status === 'paid' ? '确定要申请退款吗？' : '确定要取消该订单吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(order.id)
          uni.showToast({ title: '订单已取消', icon: 'success' })
          fetchOrders(true)
        } catch (e) {
          uni.showToast({ title: e.message || '取消失败', icon: 'none' })
        }
      }
    }
  })
}

const handlePay = async (order) => {
  try {
    await payOrder(order.id)
    uni.showToast({ title: '支付成功', icon: 'success' })
    fetchOrders(true)
  } catch (e) {
    uni.showToast({ title: e.message || '支付失败', icon: 'none' })
  }
}

onShow(() => {
  fetchOrders(true)
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background: #F2F2F7;
  display: flex;
  flex-direction: column;
}

/* 标签栏 */
.tabs {
  display: flex;
  background: #fff;
  padding: 0 16rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  font-size: 28rpx;
  color: #8E8E93;
  position: relative;
}

.tab-item.active {
  color: #007AFF;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 4rpx;
  background: #007AFF;
  border-radius: 2rpx;
}

/* 订单列表 */
.order-list {
  flex: 1;
  padding: 24rpx 32rpx;
}

.order-card {
  background: #fff;
  border-radius: 24rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1px solid #F2F2F7;
}

.order-no {
  font-size: 24rpx;
  color: #8E8E93;
}

.order-status {
  font-size: 26rpx;
  font-weight: 600;
}

.order-status.pending { color: #FF9500; }
.order-status.paid { color: #007AFF; }
.order-status.completed { color: #34C759; }
.order-status.cancelled { color: #8E8E93; }

.order-content {
  display: flex;
  padding: 24rpx;
}

.spot-image {
  width: 160rpx;
  height: 120rpx;
  border-radius: 16rpx;
  margin-right: 20rpx;
}

.spot-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.spot-name {
  font-size: 30rpx;
  color: #1C1C1E;
  font-weight: 600;
}

.visit-date {
  font-size: 24rpx;
  color: #8E8E93;
}

.price-row {
  display: flex;
  align-items: baseline;
}

.price {
  font-size: 32rpx;
  color: #FF3B30;
  font-weight: 600;
}

.quantity {
  font-size: 24rpx;
  color: #8E8E93;
  margin-left: 12rpx;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  padding: 16rpx 24rpx;
  border-top: 1px solid #F2F2F7;
  gap: 20rpx;
}

.action-btn {
  padding: 0 32rpx;
  height: 64rpx;
  line-height: 64rpx;
  font-size: 26rpx;
  border-radius: 32rpx;
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

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #8E8E93;
}

/* 加载状态 */
.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #8E8E93;
}
</style>
