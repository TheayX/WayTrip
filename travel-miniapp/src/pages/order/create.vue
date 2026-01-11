<template>
  <view class="order-create-page" v-if="spot">
    <!-- 景点信息 -->
    <view class="spot-card card">
      <image class="spot-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
      <view class="spot-info">
        <text class="spot-name">{{ spot.name }}</text>
        <text class="spot-price">¥{{ spot.price }}/人</text>
      </view>
    </view>

    <!-- 订单表单 -->
    <view class="form-card card">
      <view class="form-item">
        <text class="form-label">游玩日期</text>
        <picker mode="date" :start="minDate" @change="onDateChange">
          <view class="form-value picker">
            <text>{{ form.visitDate || '请选择日期' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">购票数量</text>
        <view class="quantity-control">
          <view class="qty-btn" @click="changeQuantity(-1)">-</view>
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
    <view class="price-card card">
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
import { getImageUrl } from '@/utils/request'

const spot = ref(null)
const spotId = ref(null)
const submitting = ref(false)

const form = reactive({
  visitDate: '',
  quantity: 1,
  contactName: '',
  contactPhone: ''
})

// 最小日期（明天）
const minDate = computed(() => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0]
})

// 总价
const totalPrice = computed(() => {
  if (!spot.value) return '0.00'
  return (spot.value.price * form.quantity).toFixed(2)
})

// 获取景点信息
const fetchSpotDetail = async () => {
  try {
    const res = await getSpotDetail(spotId.value)
    spot.value = res.data
  } catch (e) {
    uni.showToast({ title: '获取景点信息失败', icon: 'none' })
  }
}

// 日期选择
const onDateChange = (e) => {
  form.visitDate = e.detail.value
}

// 数量调整
const changeQuantity = (delta) => {
  const newQty = form.quantity + delta
  if (newQty >= 1 && newQty <= 99) {
    form.quantity = newQty
  }
}

// 生成幂等键
const generateIdempotentKey = () => {
  return `${spotId.value}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}


// 提交订单
const handleSubmit = async () => {
  // 表单验证
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
    
    // 跳转到订单详情
    setTimeout(() => {
      uni.redirectTo({
        url: `/pages/order/detail?id=${res.data.id}`
      })
    }, 1500)
  } catch (e) {
    uni.showToast({ title: e.message || '创建订单失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad((options) => {
  spotId.value = options.spotId
  fetchSpotDetail()
})
</script>

<style scoped>
.order-create-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

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
  width: 160rpx;
  height: 120rpx;
  border-radius: 12rpx;
  margin-right: 20rpx;
}

.spot-info {
  flex: 1;
}

.spot-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 12rpx;
}

.spot-price {
  font-size: 28rpx;
  color: #ff6b6b;
}

/* 表单 */
.form-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.form-item:last-child {
  border-bottom: none;
}

.form-label {
  width: 160rpx;
  font-size: 28rpx;
  color: #333;
  flex-shrink: 0;
}

.form-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.form-value.picker {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.arrow {
  color: #999;
  font-size: 32rpx;
}

.form-input {
  flex: 1;
  font-size: 28rpx;
  text-align: right;
}

/* 数量控制 */
.quantity-control {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.qty-btn {
  width: 56rpx;
  height: 56rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: #333;
}

.qty-value {
  width: 80rpx;
  text-align: center;
  font-size: 32rpx;
  color: #333;
}

/* 价格明细 */
.price-row {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: #666;
}

.price-row.total {
  border-top: 1rpx solid #f5f5f5;
  margin-top: 12rpx;
  padding-top: 24rpx;
  font-size: 30rpx;
  color: #333;
}

.total-price {
  color: #ff6b6b;
  font-size: 36rpx;
  font-weight: bold;
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
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.total-info {
  display: flex;
  align-items: baseline;
}

.total-info .label {
  font-size: 28rpx;
  color: #666;
}

.total-info .amount {
  font-size: 40rpx;
  color: #ff6b6b;
  font-weight: bold;
}

.submit-btn {
  width: 280rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #ff6b6b;
  color: #fff;
  font-size: 32rpx;
  border-radius: 40rpx;
}

.submit-btn[disabled] {
  background: #ccc;
}
</style>
