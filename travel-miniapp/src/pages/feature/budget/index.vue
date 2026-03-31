<template>
  <view class="budget-page">
    <view class="hero-card">
      <text class="hero-title">穷游特惠</text>
      <text class="hero-desc">先把预算压下来，再挑值得去的景点和攻略。</text>
      <view class="hero-badge">当前口径：免费或 50 元以内</view>
      <view class="hero-stats">
        <view class="hero-stat">
          <text class="hero-stat-value">{{ budgetSpots.length }}</text>
          <text class="hero-stat-label">低预算景点</text>
        </view>
        <view class="hero-stat">
          <text class="hero-stat-value">{{ budgetGuides.length }}</text>
          <text class="hero-stat-label">低预算攻略</text>
        </view>
      </view>
    </view>

    <view class="tab-bar">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: activeTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view class="filter-card">
      <text class="filter-title">预算口径</text>
      <view class="filter-options">
        <view
          v-for="option in budgetModes"
          :key="option.value"
          class="filter-chip"
          :class="{ active: budgetMode === option.value }"
          @click="switchBudgetMode(option.value)"
        >
          {{ option.label }}
        </view>
      </view>
    </view>

    <view v-if="activeTab === 'spots'" class="content-section">
      <view class="section-tip">优先展示低价且热度更高的景点。</view>
      <view class="mode-card">
        <text class="mode-title">景点模式</text>
        <text class="mode-desc">当前先按热度抓取前几页景点，再按预算口径过滤，后续可直接切服务端预算筛选。</text>
      </view>

      <view class="spot-card" v-for="item in budgetSpots" :key="item.id" @click="goSpotDetail(item.id)">
        <image class="spot-image" :src="getImageUrl(item.coverImage)" mode="aspectFill" />
        <view class="spot-content">
          <view class="spot-header">
            <text class="spot-title">{{ item.name }}</text>
            <text class="spot-price">{{ formatBudgetPrice(item.price) }}</text>
          </view>
          <text class="spot-desc">{{ item.description || item.intro || '便宜也能玩得尽兴。' }}</text>
          <view class="spot-meta">
            <text class="meta-tag">{{ item.regionName || '区域待补充' }}</text>
            <text class="meta-tag">{{ item.categoryName || '景点' }}</text>
            <text class="meta-rating">{{ formatBudgetRating(item.avgRating) }}</text>
          </view>
        </view>
      </view>

      <view class="empty-card" v-if="!loadingSpots && !budgetSpots.length">
        <text class="empty-title">暂时没找到低预算景点</text>
        <text class="empty-desc">可以稍后再试，或去“更多”页浏览全部景点。</text>
      </view>
    </view>

    <view v-else class="content-section">
      <view class="section-tip">根据攻略关联景点的价格，先筛出更适合穷游的内容。</view>
      <view class="mode-card">
        <text class="mode-title">攻略模式</text>
        <text class="mode-desc">当前先取攻略列表，再根据详情中的关联景点价格做预算判断，后续可换成后端直出列表。</text>
      </view>

      <view class="guide-card" v-for="item in budgetGuides" :key="item.id" @click="goGuideDetail(item.id)">
        <image class="guide-image" :src="getImageUrl(item.coverImage)" mode="aspectFill" />
        <view class="guide-content">
          <view class="guide-header">
            <text class="guide-title">{{ item.title }}</text>
            <text class="guide-price">{{ item.priceLabel }}</text>
          </view>
          <text class="guide-summary">{{ item.summary || '这篇攻略里提到的景点更适合低预算出行。' }}</text>
          <view class="guide-meta">
            <text class="meta-tag">{{ item.category || '攻略' }}</text>
            <text class="meta-tag">👁 {{ item.viewCount || 0 }}</text>
            <text class="meta-tag">关联景点 {{ item.relatedCount }}</text>
          </view>
        </view>
      </view>

      <view class="empty-card" v-if="!loadingGuides && !budgetGuides.length">
        <text class="empty-title">暂时没筛出低预算攻略</text>
        <text class="empty-desc">当前版本是前端按关联景点价格筛选，后续可以再换成后端直出。</text>
      </view>
    </view>

    <view class="loading-row" v-if="currentLoading">
      <text>{{ currentLoadingText }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { BUDGET_MAX_PRICE, BUDGET_MODE_ALL, BUDGET_MODE_FREE, fetchBudgetGuides, fetchBudgetSpots } from '@/services/feature'
import { promptLogin } from '@/utils/auth'
import { formatFeaturePrice, formatFeatureRating } from '@/utils/feature-display'
import { getImageUrl } from '@/utils/request'

// 页面数据状态
const tabs = [
  { label: '景点', value: 'spots' },
  { label: '攻略', value: 'guides' }
]
const budgetModes = [
  { label: '免费', value: BUDGET_MODE_FREE },
  { label: '50 元以内', value: BUDGET_MODE_ALL }
]
const activeTab = ref('spots')
const budgetMode = ref(BUDGET_MODE_ALL)
const budgetSpots = ref([])
const budgetGuides = ref([])
const loadingSpots = ref(false)
const loadingGuides = ref(false)

// 计算属性
const currentLoading = computed(() => (activeTab.value === 'spots' ? loadingSpots.value : loadingGuides.value))
const currentLoadingText = computed(() => (activeTab.value === 'spots' ? '正在筛选低预算景点...' : '正在整理低预算攻略...'))

// 工具方法
const formatBudgetPrice = (value) => formatFeaturePrice(value, { freeText: '¥0 免费' })
const formatBudgetRating = (value) => formatFeatureRating(value)

// 数据加载方法
const loadBudgetSpots = async () => {
  if (loadingSpots.value) return
  loadingSpots.value = true

  try {
    budgetSpots.value = await fetchBudgetSpots({
      budgetMode: budgetMode.value,
      maxPrice: BUDGET_MAX_PRICE
    })
  } catch (error) {
    console.error('加载穷游景点失败', error)
    uni.showToast({ title: '加载景点失败', icon: 'none' })
  } finally {
    loadingSpots.value = false
  }
}

const loadBudgetGuides = async () => {
  if (loadingGuides.value || budgetGuides.value.length) return
  loadingGuides.value = true

  try {
    budgetGuides.value = await fetchBudgetGuides({
      budgetMode: budgetMode.value,
      maxPrice: BUDGET_MAX_PRICE
    })
  } catch (error) {
    console.error('加载穷游攻略失败', error)
    uni.showToast({ title: '加载攻略失败', icon: 'none' })
  } finally {
    loadingGuides.value = false
  }
}

// 交互处理方法
const switchTab = (value) => {
  activeTab.value = value
  if (value === 'spots') {
    loadBudgetSpots()
    return
  }
  loadBudgetGuides()
}

const switchBudgetMode = (value) => {
  if (budgetMode.value === value) return
  budgetMode.value = value
  budgetSpots.value = []
  budgetGuides.value = []
  if (activeTab.value === 'spots') {
    loadBudgetSpots()
    return
  }
  loadBudgetGuides()
}

// 页面跳转方法
const goSpotDetail = (id) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/spot/detail?id=${id}&source=budget` })
}

const goGuideDetail = (id) => {
  if (!promptLogin('登录后可查看攻略详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/guide/detail?id=${id}` })
}

onLoad(() => {
  loadBudgetSpots()
})
</script>

<style scoped>
.budget-page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f4f6fb;
}

.hero-card,
.tab-bar,
.spot-card,
.guide-card,
.empty-card {
  background: #fff;
  border-radius: 32rpx;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
}

.hero-card {
  padding: 32rpx 28rpx;
  background: linear-gradient(135deg, #fff7ed 0%, #ffffff 56%, #fffbeb 100%);
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

.hero-badge {
  display: inline-block;
  margin-top: 18rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(249, 115, 22, 0.12);
  color: #ea580c;
  font-size: 22rpx;
}

.hero-stats {
  display: flex;
  gap: 18rpx;
  margin-top: 22rpx;
}

.hero-stat {
  flex: 1;
  padding: 18rpx 20rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.72);
}

.hero-stat-value {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #111827;
}

.hero-stat-label {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #64748b;
}

.tab-bar {
  display: flex;
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 10rpx;
}

.tab-item {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  text-align: center;
  border-radius: 24rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #6b7280;
}

.tab-item.active {
  background: #f97316;
  color: #fff;
}

.filter-card {
  margin-top: 18rpx;
  padding: 20rpx 22rpx;
  border-radius: 26rpx;
  background: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
}

.filter-title {
  display: block;
  font-size: 24rpx;
  color: #64748b;
}

.filter-options {
  display: flex;
  gap: 12rpx;
  margin-top: 14rpx;
}

.filter-chip {
  min-width: 140rpx;
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  text-align: center;
  background: #f8fafc;
  font-size: 24rpx;
  color: #64748b;
}

.filter-chip.active {
  background: rgba(249, 115, 22, 0.12);
  color: #ea580c;
  font-weight: 600;
}

.content-section {
  margin-top: 22rpx;
}

.section-tip {
  margin-bottom: 20rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.mode-card {
  margin-bottom: 20rpx;
  padding: 22rpx 24rpx;
  border-radius: 26rpx;
  background: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
}

.mode-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #1f2937;
}

.mode-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.7;
  color: #64748b;
}

.spot-card,
.guide-card {
  overflow: hidden;
  margin-bottom: 22rpx;
}

.spot-image,
.guide-image {
  width: 100%;
  height: 280rpx;
}

.spot-content,
.guide-content {
  padding: 24rpx;
}

.spot-header,
.guide-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.spot-title,
.guide-title {
  flex: 1;
  font-size: 32rpx;
  font-weight: 700;
  color: #111827;
}

.spot-price,
.guide-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #ea580c;
}

.spot-desc,
.guide-summary {
  display: block;
  margin-top: 14rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: #64748b;
}

.spot-meta,
.guide-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.meta-tag,
.meta-rating {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #f8fafc;
  font-size: 22rpx;
  color: #475569;
}

.empty-card {
  padding: 36rpx 28rpx;
}

.empty-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #1f2937;
}

.empty-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: #64748b;
}

.loading-row {
  text-align: center;
  padding: 16rpx 0 8rpx;
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
