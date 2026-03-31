<!-- 景点搜索页 -->
<template>
  <view class="ios-page">
    <!-- 搜索头部 -->
    <view class="search-header">
      <view class="search-input-wrap">
        <uni-search-bar
          v-model="keyword"
          placeholder="搜索景点名称"
          :focus="true"
          :radius="16"
          :clearButton="'auto'"
          :cancelButton="'none'"
          bgColor="#E3E3E8"
          @confirm="doSearch"
          @clear="clearKeyword"
        />
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

// 页面跳转方法
const goBack = () => {
  uni.navigateBack()
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
  background: #f4f6fb;
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
}

:deep(.search-input-wrap .uni-searchbar) {
  padding: 0;
  background: transparent;
}

:deep(.search-input-wrap .uni-searchbar__box) {
  height: 80rpx;
  border-radius: 36rpx;
}

:deep(.search-input-wrap .uni-searchbar__box-search-input) {
  font-size: 30rpx;
  color: #1C1C1E;
}

.cancel-btn {
  margin-left: 24rpx;
  color: #2563eb;
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
  border-radius: 36rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.04);
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
  color: #6b7280;
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
  color: #6b7280;
}
</style>
