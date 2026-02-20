<template>
  <view class="ios-page">
    <!-- 收藏列表 -->
    <scroll-view class="favorite-list" scroll-y @scrolltolower="loadMore">
      <view 
        class="favorite-card" 
        v-for="spot in favoriteList" 
        :key="spot.id"
        @click="goDetail(spot.id)"
      >
        <image class="spot-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
        <view class="spot-info">
          <text class="spot-name">{{ spot.name }}</text>
          <text class="spot-region">{{ spot.regionName }} · {{ spot.categoryName }}</text>
          <view class="spot-bottom">
            <text class="spot-rating">★ {{ spot.avgRating }}</text>
            <text class="spot-price">¥{{ spot.price }}</text>
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
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getFavoriteList, removeFavorite } from '@/api/favorite'
import { getImageUrl } from '@/utils/request'

const favoriteList = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const hasMore = computed(() => favoriteList.value.length < total.value)

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

const removeFavoriteHandler = async (spotId) => {
  try {
    await removeFavorite(spotId)
    favoriteList.value = favoriteList.value.filter(item => item.id !== spotId)
    total.value--
    uni.showToast({ title: '已取消收藏', icon: 'none' })
  } catch (e) {
    console.error('取消收藏失败', e)
  }
}

const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchFavoriteList()
  }
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/spot/detail?id=${id}` })
}

onShow(() => {
  fetchFavoriteList(true)
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background: #F2F2F7;
}

.favorite-list {
  height: 100vh;
  width: 100%;
  box-sizing: border-box;
  padding: 24rpx calc(32rpx + env(safe-area-inset-right)) 24rpx calc(32rpx + env(safe-area-inset-left));
}

.favorite-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  width: 100%;
  box-sizing: border-box;
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
  font-size: 30rpx;
  font-weight: 600;
  color: #1C1C1E;
}

.spot-region {
  font-size: 24rpx;
  color: #8E8E93;
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
  color: #FF9500;
  font-weight: 600;
}

.spot-price {
  font-size: 28rpx;
  color: #FF3B30;
  font-weight: 600;
}

.remove-btn {
  padding: 20rpx;
  color: #8E8E93;
  font-size: 26rpx;
}

/* 加载状态 */
.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  color: #8E8E93;
  font-size: 26rpx;
}

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 32rpx;
  color: #1C1C1E;
  margin-bottom: 10rpx;
}

.empty-tip {
  font-size: 26rpx;
  color: #8E8E93;
}
</style>
