<!-- 订单创建页 -->
<template>
  <view class="ios-page" v-if="spot">
    <view class="hero-card">
      <text class="hero-title">确认订单</text>
      <text class="hero-subtitle">核对出行日期、人数与联系人信息后提交订单。</text>
    </view>

    <!-- 景点信息 -->
    <view class="spot-card">
      <image class="spot-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
      <view class="spot-info">
        <text class="spot-name">{{ spot.name }}</text>
        <text class="spot-price">¥{{ spot.price }}/人</text>
      </view>
    </view>

    <!-- 订单表单 -->
    <view class="form-card">
      <view class="form-item">
        <text class="form-label">游玩日期</text>
        <picker mode="date" :start="minDate" @change="onDateChange">
          <view class="form-value picker">
            <text :class="{ placeholder: !form.visitDate }">{{ form.visitDate || '请选择日期' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">购票数量</text>
        <view class="quantity-control">
          <view class="qty-btn" @click="changeQuantity(-1)">−</view>
          <text class="qty-value">{{ form.quantity }}</text>
          <view class="qty-btn" @click="changeQuantity(1)">+</view>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">联系人</text>
        <input 
          class="form-input" 
          v-model="form.contactName" 
          placeholder="请输入联系人姓名"
          maxlength="50"
        />
      </view>

      <view class="form-item">
        <text class="form-label">手机号</text>
        <input 
          class="form-input" 
          v-model="form.contactPhone" 
          placeholder="请输入手机号"
          type="number"
          maxlength="11"
        />
      </view>
    </view>

    <!-- 价格明细 -->
    <view class="price-card">
      <view class="price-row">
        <text>门票单价</text>
        <text>¥{{ spot.price }}</text>
      </view>
      <view class="price-row">
        <text>购买数量</text>
        <text>x{{ form.quantity }}</text>
      </view>
      <view class="price-row total">
        <text>合计</text>
        <text class="total-price">¥{{ totalPrice }}</text>
      </view>
    </view>

    <!-- 底部提交 -->
    <view class="bottom-bar">
      <view class="total-info">
        <text class="label">应付：</text>
        <text class="amount">¥{{ totalPrice }}</text>
      </view>
      <button class="submit-btn" @click="handleSubmit" :disabled="submitting">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getSpotDetail } from '@/api/spot'
import { createOrder } from '@/api/order'
import { guardLoginPage } from '@/utils/auth'
import { getImageUrl } from '@/utils/request'

// 页面数据状态
const spot = ref(null)
const spotId = ref(null)
const submitting = ref(false)
const accessGranted = ref(false)

const form = reactive({
  visitDate: '',
  quantity: 1,
  contactName: '',
  contactPhone: ''
})

// 计算属性
const minDate = computed(() => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0]
})

const totalPrice = computed(() => {
  if (!spot.value) return '0.00'
  return (spot.value.price * form.quantity).toFixed(2)
})

// 数据加载方法
const fetchSpotDetail = async () => {
  try {
    const res = await getSpotDetail(spotId.value)
    spot.value = res.data
  } catch (e) {
    uni.showToast({ title: '获取景点信息失败', icon: 'none' })
  }
}

// 交互处理方法
const onDateChange = (e) => {
  form.visitDate = e.detail.value
}

const changeQuantity = (delta) => {
  const newQty = form.quantity + delta
  if (newQty >= 1 && newQty <= 99) {
    form.quantity = newQty
  }
}

const generateIdempotentKey = () => {
  return `${spotId.value}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

const handleSubmit = async () => {
  if (!form.visitDate) {
    uni.showToast({ title: '请选择游玩日期', icon: 'none' })
    return
  }
  if (!form.contactName.trim()) {
    uni.showToast({ title: '请输入联系人姓名', icon: 'none' })
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.contactPhone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const res = await createOrder({
      spotId: spotId.value,
      quantity: form.quantity,
      visitDate: form.visitDate,
      contactName: form.contactName.trim(),
      contactPhone: form.contactPhone,
      idempotentKey: generateIdempotentKey()
    })

    uni.showToast({ title: '订单创建成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/order/detail?id=${res.data.id}` })
    }, 1500)
  } catch (e) {
    uni.showToast({ title: e.message || '创建订单失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

// 生命周期
onLoad((options) => {
  if (!guardLoginPage('登录后可创建订单，是否现在去登录？')) {
    return
  }

  accessGranted.value = true
  spotId.value = options.spotId
  if (accessGranted.value) {
    fetchSpotDetail()
  }
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.94), rgba(245, 247, 250, 0.9) 48%, rgba(238, 242, 247, 1) 100%),
    linear-gradient(180deg, #fafafa 0%, #eef2f7 100%);
  padding-bottom: 160rpx;
}

.hero-card {
  margin: 24rpx 32rpx 0;
  padding: 28rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.58) 100%);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 600;
  color: #18181b;
}

.hero-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #52525b;
}

.spot-card {
  display: flex;
  align-items: center;
  margin: 24rpx 32rpx;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 36rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.spot-image {
  width: 160rpx;
  height: 120rpx;
  border-radius: 36rpx;
  margin-right: 20rpx;
}

.spot-info {
  flex: 1;
}

.spot-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1C1C1E;
  display: block;
  margin-bottom: 12rpx;
}

.spot-price {
  font-size: 28rpx;
  color: #9f1239;
  font-weight: 600;
}

/* 表单卡片 */
.form-card {
  margin: 0 32rpx 24rpx;
  padding: 0 24rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 36rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.form-item {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1px solid #F2F2F7;
}

.form-item:last-child {
  border-bottom: none;
}

.form-label {
  width: 160rpx;
  font-size: 30rpx;
  color: #1C1C1E;
  flex-shrink: 0;
}

.form-value {
  flex: 1;
  font-size: 30rpx;
  color: #1C1C1E;
}

.form-value.picker {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-value .placeholder {
  color: #9ca3af;
}

.arrow {
  color: #9ca3af;
  font-size: 32rpx;
}

.form-input {
  flex: 1;
  font-size: 30rpx;
  text-align: right;
  color: #1C1C1E;
}

/* 数量控制 */
.quantity-control {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.qty-btn {
  width: 60rpx;
  height: 60rpx;
  background: rgba(17, 24, 39, 0.06);
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #1C1C1E;
}

.qty-value {
  width: 80rpx;
  text-align: center;
  font-size: 32rpx;
  color: #1C1C1E;
  font-weight: 600;
}

/* 价格卡片 */
.price-card {
  margin: 0 32rpx 24rpx;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 36rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  font-size: 28rpx;
  color: #6b7280;
}

.price-row.total {
  border-top: 1px solid #F2F2F7;
  margin-top: 12rpx;
  padding-top: 20rpx;
  font-size: 30rpx;
  color: #1C1C1E;
}

.total-price {
  color: #9f1239;
  font-size: 36rpx;
  font-weight: 700;
}

/* 底部栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.04);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.total-info {
  display: flex;
  align-items: baseline;
}

.total-info .label {
  font-size: 28rpx;
  color: #6b7280;
}

.total-info .amount {
  font-size: 40rpx;
  color: #9f1239;
  font-weight: 700;
}

.submit-btn {
  width: 280rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: #18181b;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 44rpx;
  border: none;
}

.submit-btn[disabled] {
  background: #C7C7CC;
}
</style>
