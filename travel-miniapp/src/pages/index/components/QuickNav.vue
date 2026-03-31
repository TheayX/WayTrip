<template>
  <view class="quick-grid">
    <view
      v-for="action in actions"
      :key="action.id"
      class="quick-item"
      @click="$emit('click', action)"
    >
      <view class="quick-item-inner">
        <view class="quick-icon" :class="`theme-${action.theme}`">
          <uni-icons :type="action.icon" size="28" :color="resolveThemeColor(action.theme)" />
        </view>
        <text class="quick-title">{{ action.title }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { featureThemeColorMap } from '@/constants/feature-navigation'

defineProps({
  actions: { type: Array, default: () => [] }
})
defineEmits(['click'])

const resolveThemeColor = (theme) => {
  return featureThemeColorMap[theme] || '#4b5563'
}
</script>

<style scoped>
.quick-grid {
  padding: 0 24rpx;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  margin-top: 10rpx;
  margin-bottom: 32rpx;
}

.quick-item {
  min-width: 0;
}

.quick-item-inner {
  height: 100%;
  min-height: 144rpx;
  padding: 20rpx 14rpx 16rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.quick-icon {
  width: 92rpx;
  height: 92rpx;
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14rpx;
  box-shadow: 0 6rpx 16rpx rgba(0,0,0,0.03);
}

.quick-title {
  font-size: 25rpx;
  font-weight: 600;
  color: #374151;
  line-height: 1.35;
}

.theme-blue { background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); }
.theme-orange { background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%); }
.theme-amber { background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); }
.theme-emerald { background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%); }
.theme-purple { background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%); }
</style>
