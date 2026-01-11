<template>
  <view class="favorite-page">
    <!-- 收藏列表 -->
    <scroll-view 
      class="favorite-list" 
      scroll-y 
      @scrolltolower="loadMore"
    >
      <view 
        class="favorite-item card" 
        v-for="spot in favoriteList" 
        :key="spot.id"
        @click="goDetail(spot.id)"
      >
        <image class="spot-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
        <view class="spot-info">
          <text class="spot-name">{{ spot.name }}</text>
          <text class="spot-region">{{ spot.regionName }} · {{ spot.categoryName }}</text>
          <view class="spot-bottom">
            <text class="spot-rating">⭐ {{ spot.avgRating }}</text>
            <text class="spot-price price">¥{{ spot.price }}</text>
          </view>
        </view>
        <view class="remove-btn" @click.stop="removeFavoriteHandler(spot.id)">
          <text>取消</text>
        </view>
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      <view class="no-more" v-if="!hasMore && favoriteList.length > 0">
        <text>没有更多了</text>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-if="!loading && favoriteList.length === 0">
        <text class="empty-icon">❤️</text>
        <text class="empty-text">暂无收藏</text>
        <text class="empty-tip">去发现喜欢的景点吧~</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getFavoriteList, removeFavorite } from '@/api/favorite'
import { getImageUrl } from '@/utils/request'

// 列表数据
const favoriteList = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const hasMore = computed(() => favoriteList.value.length < total.value)

// 获取收藏列表
const fetchFavoriteList = async (isRefresh = false) => {
  if (loading.value) return
  loading.value = true

  try {
    const res = await getFavoriteList(isRefresh ? 1 : page.value, pageSize.value)
    const list = res.data.list || []
    
    if (isRefresh) {
      favoriteList.value = list
      page.value = 1
    } else {
      favoriteList.value = [...favoriteList.value, ...list]
    }
    total.value = res.data.total || 0
    page.value++
  } catch (e) {
    console.error('获取收藏列表失败', e)
  } finally {
    loading.value = false
  }
}

// 取消收藏
const removeFavoriteHandler = async (spotId) => {
  try {
    await removeFavorite(spotId)
    // 从列表中移除
    favoriteList.value = favoriteList.value.filter(item => item.id !== spotId)
    total.value--
    uni.showToast({ title: '已取消收藏', icon: 'none' })
  } catch (e) {
    console.error('取消收藏失败', e)
  }
}

// 加载更多
const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchFavoriteList()
  }
}

// 跳转详情
const goDetail = (id) => {
  uni.navigateTo({
    url: `/pages/spot/detail?id=${id}`
  })
}

// 页面显示时刷新
onShow(() => {
  fetchFavoriteList(true)
})
</script>

<style scoped>
.favorite-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.favorite-list {
  height: 100vh;
  padding: 20rpx;
}

.favorite-item {
  display: flex;
  align-items: center;
  padding: 0;
  overflow: hidden;
}

.spot-image {
  width: 200rpx;
  height: 150rpx;
  flex-shrink: 0;
}

.spot-info {
  flex: 1;
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 150rpx;
}

.spot-name {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
}

.spot-region {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.spot-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8rpx;
}

.spot-rating {
  font-size: 24rpx;
  color: #ff9500;
}

.spot-price {
  font-size: 28rpx;
}

.remove-btn {
  padding: 20rpx;
  color: #999;
  font-size: 24rpx;
}

/* 加载状态 */
.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  color: #999;
  font-size: 26rpx;
}

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 32rpx;
  color: #333;
  margin-bottom: 10rpx;
}

.empty-tip {
  font-size: 26rpx;
  color: #999;
}
</style>
