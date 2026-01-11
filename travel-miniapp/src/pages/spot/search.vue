<template>
  <view class="ios-page">
    <!-- 搜索头部 -->
    <view class="search-header">
      <view class="search-input-wrap">
        <image class="search-icon" src="/static/搜索.png" />
        <input 
          class="search-input" 
          v-model="keyword" 
          placeholder="搜索景点名称"
          focus
          confirm-type="search"
          @confirm="doSearch"
        />
        <text class="clear-btn" v-if="keyword" @click="clearKeyword">✕</text>
      </view>
      <text class="cancel-btn" @click="goBack">取消</text>
    </view>

    <!-- 搜索结果 -->
    <scroll-view class="search-result" scroll-y @scrolltolower="loadMore">
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
import { getImageUrl } from '@/utils/request'

const keyword = ref('')
const resultList = ref([])
const page = ref(1)
const hasMore = ref(true)
const searched = ref(false)

// 搜索
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

// 加载更多
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

// 清空关键词
const clearKeyword = () => {
  keyword.value = ''
  resultList.value = []
  searched.value = false
}

// 返回
const goBack = () => {
  uni.navigateBack()
}

// 跳转详情
const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/spot/detail?id=${id}` })
}
</script>

<style scoped>
.ios-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #F2F2F7;
}

/* 搜索头部 */
.search-header {
  display: flex;
  align-items: center;
  padding: 60rpx 32rpx 20rpx;
  background: #fff;
}

.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #E3E3E8;
  border-radius: 16rpx;
  padding: 16rpx 20rpx;
}

.search-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 30rpx;
  color: #1C1C1E;
}

.clear-btn {
  color: #8E8E93;
  padding: 10rpx;
  font-size: 28rpx;
}

.cancel-btn {
  margin-left: 24rpx;
  color: #007AFF;
  font-size: 30rpx;
}

/* 搜索结果 */
.search-result {
  flex: 1;
  padding: 24rpx 32rpx;
}

.result-card {
  display: flex;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
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
  color: #1C1C1E;
}

.result-region {
  font-size: 24rpx;
  color: #8E8E93;
}

.result-price {
  font-size: 32rpx;
  color: #FF3B30;
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
  color: #8E8E93;
}
</style>
