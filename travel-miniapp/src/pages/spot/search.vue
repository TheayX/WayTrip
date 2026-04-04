<!-- 景点搜索页 -->
<template>
  <view class="ios-page">
    <view class="hero-card">
      <text class="hero-title">搜索景点</text>
      <text class="hero-subtitle">输入景点名、城市或目的地关键词，快速定位下一次出发。</text>
    </view>

    <view class="search-header">
      <view class="search-input-wrap">
        <uni-search-bar
          v-model="keyword"
          placeholder="搜索景点名称 / 城市"
          :focus="true"
          :radius="16"
          :clearButton="'auto'"
          :cancelButton="'none'"
          bgColor="#E3E3E8"
          @confirm="doSearch"
          @clear="clearKeyword"
        />
      </view>
      <button
        class="search-btn"
        :disabled="!keyword.trim()"
        @click="doSearch"
      >
        搜索
      </button>
    </view>

    <!-- 搜索结果 -->
    <scroll-view class="search-result" scroll-y @scrolltolower="loadMore">
      <view v-if="searched && resultList.length" class="result-state">
        <text class="result-state-title">搜索结果</text>
        <text class="result-state-desc">共找到 {{ resultList.length }} 个与“{{ keyword }}”相关的景点</text>
      </view>

      <view 
        class="result-card" 
        v-for="spot in resultList" 
        :key="spot.id"
        @click="goDetail(spot.id)"
      >
        <image class="result-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
        <view class="result-info">
          <text class="result-name">{{ spot.name }}</text>
          <text class="result-region">{{ spot.regionName }}</text>
          <text class="result-price">¥{{ spot.price }}</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-if="searched && resultList.length === 0">
        <text class="empty-text">暂无搜索结果</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { searchSpots } from '@/api/spot'
import { promptLogin } from '@/utils/auth'
import { getImageUrl } from '@/utils/request'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'

// 页面数据状态
const keyword = ref('')
const resultList = ref([])
const page = ref(1)
const hasMore = ref(true)
const searched = ref(false)

// 数据加载方法
const doSearch = async () => {
  if (!keyword.value.trim()) return
  
  searched.value = true
  page.value = 1
  
  try {
    const res = await searchSpots(keyword.value, 1, 10)
    resultList.value = res.data.list || []
    hasMore.value = resultList.value.length < res.data.total
  } catch (e) {
    console.error('搜索失败', e)
  }
}

const loadMore = async () => {
  if (!hasMore.value || !keyword.value.trim()) return
  
  page.value++
  try {
    const res = await searchSpots(keyword.value, page.value, 10)
    const list = res.data.list || []
    resultList.value = [...resultList.value, ...list]
    hasMore.value = resultList.value.length < res.data.total
  } catch (e) {
    console.error('加载更多失败', e)
  }
}

// 交互处理方法
const clearKeyword = () => {
  keyword.value = ''
  resultList.value = []
  searched.value = false
}

const goDetail = (id) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: buildSpotDetailUrl(id, SPOT_DETAIL_SOURCE.SEARCH) })
}
</script>

<style scoped>
.ios-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.94), rgba(245, 247, 250, 0.9) 48%, rgba(238, 242, 247, 1) 100%),
    linear-gradient(180deg, #fafafa 0%, #eef2f7 100%);
}

.hero-card {
  margin: 24rpx 24rpx 16rpx;
  padding: 28rpx 28rpx 24rpx;
  border-radius: 30rpx;
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

/* 搜索头部 */
.search-header {
  display: flex;
  align-items: center;
  padding: 0 24rpx 20rpx;
}

.search-input-wrap {
  flex: 1;
}

:deep(.search-input-wrap .uni-searchbar) {
  padding: 0;
  background: transparent;
}

:deep(.search-input-wrap .uni-searchbar__box) {
  height: 80rpx;
  border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.76) !important;
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  box-shadow:
    0 12rpx 30rpx rgba(15, 23, 42, 0.06),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

:deep(.search-input-wrap .uni-searchbar__box-search-input) {
  font-size: 30rpx;
  color: #18181b;
}

.search-btn {
  margin-left: 24rpx;
  min-width: 132rpx;
  height: 80rpx;
  border: none;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ca8a04 0%, #f59e0b 100%);
  color: #fffdf5;
  font-size: 28rpx;
  font-weight: 600;
  line-height: 80rpx;
  box-shadow: 0 14rpx 28rpx rgba(202, 138, 4, 0.22);
}

.search-btn[disabled] {
  background: #d4d4d8;
  color: rgba(255, 255, 255, 0.9);
  box-shadow: none;
}

/* 搜索结果 */
.search-result {
  flex: 1;
  padding: 0 24rpx 32rpx;
}

.result-state {
  margin-bottom: 20rpx;
  padding: 24rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.72);
  border: 1rpx solid rgba(255, 255, 255, 0.82);
  box-shadow:
    0 16rpx 40rpx rgba(15, 23, 42, 0.06),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.result-state-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #18181b;
}

.result-state-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  line-height: 1.5;
  color: #71717a;
}

.result-card {
  display: flex;
  background: rgba(255, 255, 255, 0.76);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 36rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.result-image {
  width: 200rpx;
  height: 150rpx;
  flex-shrink: 0;
}

.result-info {
  flex: 1;
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.result-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #18181b;
}

.result-region {
  font-size: 24rpx;
  color: #71717a;
}

.result-price {
  font-size: 32rpx;
  color: #9f1239;
  font-weight: 600;
}

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #71717a;
}
</style>
