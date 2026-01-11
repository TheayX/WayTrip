<template>
  <view class="order-list-page">
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
    <scroll-view 
      scroll-y 
      class="order-list"
      @scrolltolower="loadMore"
    >
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
              v-if="order.status === 'pending'" 
              class="action-btn cancel" 
              @click.stop="handleCancel(order)"
            >取消订单</button>
            <button 
              v-if="order.status === 'pending'" 
              class="action-btn pay" 
              @click.stop="handlePay(order)"
            >去支付</button>
            <button 
              v-if="order.status === 'paid'" 
              class="action-btn cancel" 
              @click.stop="handleCancel(order)"
            >申请退款</button>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-else-if="!loading">
        <text class="empty-icon">📋</text>
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
const pagination = reactive({
  page: 1,
  pageSize: 10
})

// 获取订单列表
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

// 切换标签
const switchTab = (value) => {
  currentTab.value = value
  fetchOrders(true)
}

// 加载更多
const loadMore = () => {
  if (!noMore.value && !loading.value) {
    pagination.page++
    fetchOrders()
  }
}

// 跳转详情
const goDetail = (id) => {
  uni.navigateTo({
    url: `/pages/order/detail?id=${id}`
  })
}


// 取消订单
const handleCancel = async (order) => {
  uni.showModal({
    title: '提示',
    content: '确定要取消该订单吗？',
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

// 去支付
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
.order-list-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 标签栏 */
.tabs {
  display: flex;
  background: #fff;
  padding: 0 10rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #409EFF;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 4rpx;
  background: #409EFF;
  border-radius: 2rpx;
}

/* 订单列表 */
.order-list {
  flex: 1;
  padding: 20rpx;
}

.order-card {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.order-no {
  font-size: 24rpx;
  color: #999;
}

.order-status {
  font-size: 26rpx;
  font-weight: bold;
}

.order-status.pending {
  color: #e6a23c;
}

.order-status.paid {
  color: #409EFF;
}

.order-status.completed {
  color: #67c23a;
}

.order-status.cancelled {
  color: #909399;
}

.order-content {
  display: flex;
  padding: 24rpx;
}

.spot-image {
  width: 160rpx;
  height: 120rpx;
  border-radius: 12rpx;
  margin-right: 20rpx;
}

.spot-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.spot-name {
  font-size: 28rpx;
  color: #333;
  font-weight: bold;
}

.visit-date {
  font-size: 24rpx;
  color: #999;
}

.price-row {
  display: flex;
  align-items: baseline;
}

.price {
  font-size: 32rpx;
  color: #ff6b6b;
  font-weight: bold;
}

.quantity {
  font-size: 24rpx;
  color: #999;
  margin-left: 12rpx;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  padding: 16rpx 24rpx;
  border-top: 1rpx solid #f5f5f5;
  gap: 20rpx;
}

.action-btn {
  padding: 12rpx 32rpx;
  font-size: 26rpx;
  border-radius: 32rpx;
  line-height: 1.5;
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

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

/* 加载状态 */
.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}
</style>
