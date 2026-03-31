<template>
  <view class="entry-grid">
    <view
      v-for="entry in entries"
      :key="entry.id"
      class="entry-item"
      @click="$emit('click', entry)"
    >
      <view class="entry-item-inner">
        <view class="entry-icon" :class="`theme-${entry.theme}`">
          <uni-icons :type="entry.icon" size="28" :color="resolveThemeColor(entry.theme)" />
        </view>
        <text class="entry-title">{{ entry.title }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { featureEntryThemeMap } from '@/constants/feature-entry-registry'

defineProps({
  entries: { type: Array, default: () => [] }
})
defineEmits(['click'])

const resolveThemeColor = (theme) => {
  return featureEntryThemeMap[theme] || '#4b5563'
}
</script>

<style scoped>
.entry-grid {
  padding: 0 24rpx;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  margin-top: 10rpx;
  margin-bottom: 32rpx;
}

.entry-item {
  min-width: 0;
}

.entry-item-inner {
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

.entry-icon {
  width: 92rpx;
  height: 92rpx;
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14rpx;
  box-shadow: 0 6rpx 16rpx rgba(0,0,0,0.03);
}

.entry-title {
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
