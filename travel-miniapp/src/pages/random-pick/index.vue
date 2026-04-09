<!-- 随心一选页面 -->
<template>
  <view class="random-pick-page">
    <view class="hero-card" v-if="spot" @click="goDetail">
      <image class="hero-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
      <view class="hero-overlay"></view>
      <view class="hero-content">
        <text class="eyebrow">随心一选</text>
        <text class="title">{{ spot.name }}</text>
        <text class="subtitle">{{ spot.regionName || '旅行目的地' }} · {{ spot.categoryName || '景点推荐' }}</text>
        <text class="desc">{{ spot.description || spot.intro || fallbackCopy }}</text>

        <view class="meta-row">
          <view class="meta-pill">
            <uni-icons type="star-filled" size="14" color="#f59e0b" />
            <text>{{ formatRandomPickRating(spot.avgRating) }}</text>
          </view>
          <view class="meta-pill">
            <uni-icons type="wallet-filled" size="14" color="#7c3aed" />
            <text>{{ formatRandomPickPrice(spot.price) }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="loading-card" v-else-if="loading">
      <view class="loading-orb"></view>
      <text class="loading-title">正在随机选择景点</text>
      <text class="loading-desc">从现有景点里随机抽取一个目的地。</text>
    </view>

    <view class="empty-card" v-else-if="!loading">
      <uni-icons type="info" size="28" color="#94a3b8" />
      <text class="empty-title">暂时没有抽到合适的景点</text>
      <text class="empty-desc">可以再试一次，换个灵感。</text>
    </view>

    <view class="tips-card">
      <text class="tips-title">今日灵感</text>
      <text class="tips-text">{{ randomReason }}</text>
    </view>

    <view class="action-bar">
      <button class="ghost-btn" :disabled="loading" @click="drawSpot">
        {{ loading ? '抽取中...' : '换一个' }}
      </button>
      <button class="primary-btn" :disabled="!spot" @click="goDetail">
        去看看
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchRandomPickSpot } from '@/services/random-pick'
import { promptLogin } from '@/utils/auth'
import { formatFeaturePrice, formatFeatureRating } from '@/utils/feature-display'
import { getImageUrl } from '@/utils/request'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'

// 页面状态同时记录上一次抽中的景点，避免连续命中同一条结果。
const spot = ref(null)
const loading = ref(false)
const lastSpotId = ref(null)

const randomPickReasons = [
  '今天不纠结，直接把选择交给运气。',
  '这张卡片适合说走就走的冲动。',
  '换个平时不会主动点开的目的地，可能更有惊喜。',
  '先出发，再决定要不要认真做攻略。'
]

const fallbackCopy = '给自己一个随机出发的理由，今天就去看看新的风景。'
const randomReason = ref(randomPickReasons[0])

// 灵感文案随机切换，让重复抽取时的交互反馈不至于过于单调。
const getRandomReason = () => {
  const index = Math.floor(Math.random() * randomPickReasons.length)
  return randomPickReasons[index]
}

const formatRandomPickRating = (value) => formatFeatureRating(value)
const formatRandomPickPrice = (value) => formatFeaturePrice(value)

// 抽取逻辑统一收口在这里，便于后续扩展更多过滤条件或埋点。
const drawSpot = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const nextSpot = await fetchRandomPickSpot({ excludeSpotId: lastSpotId.value })
    spot.value = nextSpot || null
    lastSpotId.value = nextSpot?.id || null
    randomReason.value = getRandomReason()
  } catch (error) {
    console.error('加载随心一选失败', error)
    uni.showToast({ title: '抽取失败，请稍后重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 详情页属于登录后能力，这里在入口处拦截能减少无效跳转。
const goDetail = () => {
  if (!spot.value?.id) return
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: buildSpotDetailUrl(spot.value.id, SPOT_DETAIL_SOURCE.RANDOM_PICK) })
}

onLoad(() => {
  drawSpot()
})
</script>

<style scoped>
.random-pick-page {
  min-height: 100vh;
  padding: 28rpx 24rpx 40rpx;
  background:
    radial-gradient(circle at top, rgba(190, 24, 93, 0.16), transparent 32%),
    linear-gradient(180deg, #241525 0%, #3b1f38 42%, #f7f8fb 42%, #eef2f7 100%);
}

.hero-card {
  position: relative;
  height: 860rpx;
  border-radius: 40rpx;
  overflow: hidden;
  box-shadow: 0 22rpx 50rpx rgba(31, 22, 55, 0.24);
}

.hero-image {
  width: 100%;
  height: 100%;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(17, 24, 39, 0.12) 0%, rgba(17, 24, 39, 0.72) 100%);
}

.hero-content {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 36rpx 32rpx 40rpx;
  color: #fff;
}

.eyebrow {
  display: inline-block;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 22rpx;
}

.title {
  display: block;
  margin-top: 18rpx;
  font-size: 52rpx;
  font-weight: 700;
  line-height: 1.2;
}

.subtitle {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.88);
}

.desc {
  display: block;
  margin-top: 18rpx;
  font-size: 28rpx;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.92);
}

.meta-row {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 24rpx;
}

.tips-card,
.empty-card,
.loading-card {
  margin-top: 24rpx;
  padding: 28rpx;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 32rpx;
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.06);
}

.tips-title,
.empty-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #1f2937;
}

.tips-text,
.empty-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #64748b;
}

.loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-top: 44rpx;
  padding-bottom: 44rpx;
}

.loading-orb {
  width: 96rpx;
  height: 96rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #7c3aed 0%, #c4b5fd 100%);
  box-shadow: 0 18rpx 36rpx rgba(124, 58, 237, 0.22);
}

.loading-title {
  display: block;
  margin-top: 22rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.loading-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #64748b;
}

.action-bar {
  display: flex;
  gap: 18rpx;
  margin-top: 28rpx;
}

.ghost-btn,
.primary-btn {
  flex: 1;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
}

.ghost-btn {
  background: rgba(255, 255, 255, 0.86);
  color: #881337;
  border: 2rpx solid rgba(190, 24, 93, 0.12);
}

.primary-btn {
  background: linear-gradient(135deg, #881337 0%, #9f1239 100%);
  color: #fff;
}

.ghost-btn[disabled],
.primary-btn[disabled] {
  opacity: 0.6;
}
</style>
