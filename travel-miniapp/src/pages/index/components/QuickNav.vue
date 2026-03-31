<template>
  <view class="quick-grid">
    <view
      v-for="action in actions"
      :key="action.id"
      class="quick-item"
      @click="$emit('click', action)"
    >
      <view class="quick-icon" :class="`theme-${action.theme}`">
        <uni-icons :type="action.icon" size="28" :color="resolveThemeColor(action.theme)" />
      </view>
      <text class="quick-title">{{ action.title }}</text>
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
.quick-grid { padding: 0 32rpx; display: grid; grid-template-columns: repeat(4, 1fr); gap: 16rpx; margin-top: 10rpx; margin-bottom: 32rpx; }
.quick-item {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.quick-icon {
  width: 96rpx; height: 96rpx; border-radius: 32rpx; display: flex; align-items: center; justify-content: center;
  margin-bottom: 16rpx; box-shadow: 0 6rpx 16rpx rgba(0,0,0,0.03);
}
.quick-title { font-size: 26rpx; font-weight: 500; color: #374151; }

.theme-blue { background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); }
.theme-orange { background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%); }
.theme-amber { background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); }
.theme-emerald { background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%); }
.theme-purple { background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%); }
</style>
