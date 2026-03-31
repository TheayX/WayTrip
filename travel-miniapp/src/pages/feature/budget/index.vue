<template>
  <view class="budget-page">
    <view class="hero-card">
      <text class="hero-title">穷游特惠</text>
      <text class="hero-desc">先把预算压下来，再挑值得去的景点和攻略。</text>
      <view class="hero-badge">当前口径：免费或 50 元以内</view>
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

    <view v-if="activeTab === 'spots'" class="content-section">
      <view class="section-tip">优先展示低价且热度更高的景点。</view>

      <view class="spot-card" v-for="item in budgetSpots" :key="item.id" @click="goSpotDetail(item.id)">
        <image class="spot-image" :src="getImageUrl(item.coverImage)" mode="aspectFill" />
        <view class="spot-content">
          <view class="spot-header">
            <text class="spot-title">{{ item.name }}</text>
            <text class="spot-price">{{ formatPrice(item.price) }}</text>
          </view>
          <text class="spot-desc">{{ item.description || item.intro || '便宜也能玩得尽兴。' }}</text>
          <view class="spot-meta">
            <text class="meta-tag">{{ item.regionName || '区域待补充' }}</text>
            <text class="meta-tag">{{ item.categoryName || '景点' }}</text>
            <text class="meta-rating">{{ formatRating(item.avgRating) }}</text>
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
import { BUDGET_MAX_PRICE, fetchBudgetGuides, fetchBudgetSpots } from '@/services/feature'
import { promptLogin } from '@/utils/auth'
import { getImageUrl } from '@/utils/request'

// 页面数据状态
const tabs = [
  { label: '景点', value: 'spots' },
  { label: '攻略', value: 'guides' }
]
const activeTab = ref('spots')
const budgetSpots = ref([])
const budgetGuides = ref([])
const loadingSpots = ref(false)
const loadingGuides = ref(false)

// 计算属性
const currentLoading = computed(() => (activeTab.value === 'spots' ? loadingSpots.value : loadingGuides.value))
const currentLoadingText = computed(() => (activeTab.value === 'spots' ? '正在筛选低预算景点...' : '正在整理低预算攻略...'))

// 工具方法
const formatPrice = (value) => {
  const num = Number(value)
  if (!Number.isFinite(num)) return '价格待补充'
  return num <= 0 ? '¥0 免费' : `¥${num}`
}

const formatRating = (value) => {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? `${num.toFixed(1)} 分` : '暂无评分'
}

// 数据加载方法
const loadBudgetSpots = async () => {
  if (loadingSpots.value) return
  loadingSpots.value = true

  try {
    budgetSpots.value = await fetchBudgetSpots({ maxPrice: BUDGET_MAX_PRICE })
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
    budgetGuides.value = await fetchBudgetGuides({ maxPrice: BUDGET_MAX_PRICE })
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

.content-section {
  margin-top: 22rpx;
}

.section-tip {
  margin-bottom: 20rpx;
  font-size: 24rpx;
  color: #94a3b8;
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
