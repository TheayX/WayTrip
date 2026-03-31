<!-- 订单列表页 -->
<template>
  <view class="ios-page">
    <!-- 顶部状态切换 -->
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

    <!-- 列表区域 -->
    <scroll-view scroll-y class="order-list" @scrolltolower="loadMore">
      <view v-if="orderList.length">
        <view class="order-card" v-for="order in orderList" :key="order.id" @click="goDetail(order.id)">
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

          <view class="order-actions" v-if="showActions(order)">
            <button v-if="order.canCancel" class="action-btn cancel" @click.stop="handleCancel(order)">
              {{ order.status === 'paid' ? '申请退款' : '取消订单' }}
            </button>
            <button v-if="order.status === 'pending'" class="action-btn pay" @click.stop="handlePay(order)">去支付</button>
            <button v-if="order.status === 'completed'" class="action-btn review" @click.stop="handleReview(order)">
              去评价
            </button>
          </view>
        </view>
      </view>

      <view class="empty" v-else-if="!loading">
        <text class="empty-text">暂无订单</text>
      </view>

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
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getOrderList, cancelOrder } from '@/api/order'
import { getImageUrl } from '@/utils/request'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'

// 常量配置
const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'pending' },
  { label: '已支付', value: 'paid' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

// 页面数据状态
const currentTab = ref('')
const orderList = ref([])
const loading = ref(false)
const noMore = ref(false)
const pagination = reactive({ page: 1, pageSize: 10 })

// 工具方法
const showActions = (order) => {
  return order.status === 'pending' || order.status === 'paid' || order.status === 'completed'
}

// 数据加载方法
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

// 交互处理方法
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
          uni.showToast({ title: '操作成功', icon: 'success' })
          fetchOrders(true)
        } catch (e) {
          uni.showToast({ title: e.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

const handlePay = (order) => {
  uni.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
}

const handleReview = (order) => {
  uni.navigateTo({ url: buildSpotDetailUrl(order.spotId, SPOT_DETAIL_SOURCE.ORDER, { openReview: true }) })
}

// 生命周期
onLoad((options) => {
  currentTab.value = options?.status || ''
})

onShow(() => {
  fetchOrders(true)
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background: #f4f6fb;
  display: flex;
  flex-direction: column;
}

/* Glassmorphism Tab Bar */
.tabs {
  display: flex;
  padding: 16rpx 24rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(244, 246, 251, 0.85);
  backdrop-filter: blur(12px);
  gap: 16rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  font-size: 26rpx;
  color: #64748b;
  border-radius: 99rpx;
  font-weight: 500;
  transition: all 0.3s ease;
}

.tab-item.active {
  background: #2563eb;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 6rpx 16rpx rgba(37, 99, 235, 0.25);
}

.order-list {
  flex: 1;
  width: 100%;
  box-sizing: border-box;
  padding: 24rpx 32rpx;
}

.order-card {
  background: #fff;
  border-radius: 36rpx;
  margin-bottom: 32rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.04);
  width: 100%;
  box-sizing: border-box;
  padding: 28rpx;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20rpx;
  border-bottom: 2rpx dashed #f1f5f9;
  margin-bottom: 24rpx;
}

.order-no {
  font-size: 24rpx;
  color: #94a3b8;
  font-family: monospace;
}

.order-status {
  font-size: 24rpx;
  font-weight: 700;
  padding: 6rpx 16rpx;
  border-radius: 12rpx;
}

.order-status.pending { background: #fff7ed; color: #ea580c; }
.order-status.paid { background: #eff6ff; color: #2563eb; }
.order-status.completed { background: #ecfdf5; color: #10b981; }
.order-status.cancelled { background: #f1f5f9; color: #64748b; }

.order-content {
  display: flex;
  margin-bottom: 24rpx;
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
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.spot-name {
  font-size: 32rpx;
  color: #1e293b;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.visit-date {
  font-size: 24rpx;
  color: #64748b;
  margin-bottom: 16rpx;
}

.price-row {
  display: flex;
  align-items: baseline;
}

.price {
  font-size: 36rpx;
  color: #ef4444;
  font-weight: 800;
}

.quantity {
  font-size: 24rpx;
  color: #94a3b8;
  margin-left: 12rpx;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 24rpx;
  border-top: 2rpx dashed #f1f5f9;
  gap: 16rpx;
}

.action-btn {
  padding: 0 40rpx;
  height: 68rpx;
  line-height: 68rpx;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: 99rpx;
  border: none;
  margin: 0;
}

.action-btn.cancel {
  background: #f8fafc;
  color: #64748b;
}

.action-btn.pay {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(37, 99, 235, 0.3);
}

.action-btn.review {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(16, 185, 129, 0.3);
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #94a3b8;
}

.loading-more,
.no-more {
  text-align: center;
  padding: 40rpx;
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
