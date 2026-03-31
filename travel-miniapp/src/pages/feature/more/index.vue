<template>
  <view class="more-page">
    <view class="hero-card">
      <text class="hero-title">更多功能</text>
      <text class="hero-desc">首页保留高频特色入口，常用能力统一收在这里。</text>
    </view>

    <view class="group-card" v-for="group in featureGroups" :key="group.title">
      <text class="group-title">{{ group.title }}</text>
      <view class="grid-list">
        <view
          class="grid-item"
          :class="{ disabled: item.available === false }"
          v-for="item in group.items"
          :key="item.id"
          @click="handleEntryClick(item)"
        >
          <view class="grid-icon" :class="`theme-${item.theme}`">
            <uni-icons :type="item.icon" size="24" :color="resolveThemeColor(item.theme)" />
          </view>
          <text class="grid-title">{{ item.title }}</text>
          <text class="grid-desc">{{ item.desc }}</text>
          <text v-if="item.available === false" class="grid-badge">敬请期待</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { featureThemeColorMap, getMoreFeatureGroups } from '@/constants/feature-navigation'

const resolveThemeColor = (theme) => featureThemeColorMap[theme] || '#4b5563'

const navigateTo = (url) => {
  uni.navigateTo({ url })
}

const handleEntryClick = (item) => {
  if (item.available === false || !item.url) {
    uni.showToast({ title: '功能开发中', icon: 'none' })
    return
  }
  navigateTo(item.url)
}

const featureGroups = getMoreFeatureGroups()
</script>

<style scoped>
.more-page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f4f6fb;
}

.hero-card,
.group-card {
  background: #fff;
  border-radius: 32rpx;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
}

.hero-card {
  padding: 34rpx 30rpx;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 55%, #f5f3ff 100%);
}

.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #111827;
}

.hero-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #64748b;
}

.group-card {
  margin-top: 24rpx;
  padding: 28rpx;
}

.group-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.grid-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
  margin-top: 24rpx;
}

.grid-item {
  position: relative;
  padding: 24rpx;
  border-radius: 28rpx;
  background: #f8fafc;
}

.grid-item.disabled {
  opacity: 0.86;
}

.grid-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.grid-title {
  display: block;
  margin-top: 18rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2937;
}

.grid-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.6;
  color: #64748b;
}

.grid-badge {
  position: absolute;
  top: 18rpx;
  right: 18rpx;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.06);
  color: #64748b;
  font-size: 20rpx;
}

.theme-blue { background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); }
.theme-orange { background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%); }
.theme-amber { background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); }
.theme-emerald { background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%); }
.theme-purple { background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%); }
</style>
