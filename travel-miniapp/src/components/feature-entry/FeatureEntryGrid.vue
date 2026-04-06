<template>
  <view class="entry-grid-shell">
    <view class="entry-grid">
      <view
        v-for="entry in entries"
        :key="entry.id"
        class="entry-item"
        @click="$emit('click', entry)"
      >
        <view class="entry-icon" :class="`theme-${entry.theme}`">
          <uni-icons :type="entry.icon" size="24" :color="resolveThemeColor(entry.theme)" />
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
.entry-grid-shell {
  margin: 10rpx 32rpx 8rpx;
  padding: 12rpx 18rpx 4rpx;
  border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(18rpx);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8rpx 8rpx;
}

.entry-item {
  min-width: 0;
  min-height: 108rpx;
  padding: 10rpx 10rpx 10rpx;
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.entry-icon {
  width: 70rpx;
  height: 70rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
}

.entry-title {
  font-size: 23rpx;
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
