<template>
  <view class="index-page">
    <!-- 轮播图 -->
    <swiper class="banner" indicator-dots autoplay circular v-if="banners.length">
      <swiper-item v-for="banner in banners" :key="banner.id" @click="handleBannerClick(banner)">
        <image class="banner-image" :src="getImageUrl(banner.imageUrl)" mode="aspectFill" />
      </swiper-item>
    </swiper>
    <view class="banner-placeholder" v-else>
      <text>暂无轮播图</text>
    </view>

    <!-- 热门推荐 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">🔥 热门推荐</text>
        <text class="section-more" @click="goSpotList">更多</text>
      </view>
      <scroll-view class="hot-list" scroll-x v-if="hotSpots.length">
        <view 
          class="hot-item" 
          v-for="spot in hotSpots" 
          :key="spot.id"
          @click="goSpotDetail(spot.id)"
        >
          <image class="hot-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="hot-info">
            <text class="hot-name">{{ spot.name }}</text>
            <view class="hot-meta">
              <text class="hot-rating">⭐ {{ spot.avgRating || '暂无' }}</text>
              <text class="hot-price">¥{{ spot.price }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
      <view class="empty-tip" v-else>
        <text>暂无热门景点</text>
      </view>
    </view>

    <!-- 个性化推荐 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">✨ {{ recommendType }}</text>
        <text class="section-refresh" @click="handleRefresh">换一批</text>
      </view>
      
      <!-- 偏好设置提示 -->
      <view class="preference-tip" v-if="needPreference" @click="showPreferencePopup">
        <text class="tip-icon">💡</text>
        <text class="tip-text">设置偏好标签，获取更精准的推荐</text>
        <text class="tip-arrow">›</text>
      </view>

      <view class="recommend-list" v-if="recommendations.length">
        <view 
          class="recommend-item card" 
          v-for="spot in recommendations" 
          :key="spot.id"
          @click="goSpotDetail(spot.id)"
        >
          <image class="recommend-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="recommend-info">
            <text class="recommend-name">{{ spot.name }}</text>
            <text class="recommend-category">{{ spot.categoryName }}</text>
            <view class="recommend-meta">
              <text class="recommend-rating">⭐ {{ spot.avgRating || '暂无' }}</text>
              <text class="recommend-price">¥{{ spot.price }}</text>
            </view>
          </view>
        </view>
      </view>
      <view class="empty-tip" v-else>
        <text>暂无推荐</text>
      </view>
    </view>

    <!-- 偏好设置弹窗 -->
    <view class="preference-popup" v-if="preferenceVisible" @click.self="preferenceVisible = false">
      <view class="preference-content">
        <text class="preference-title">选择你感兴趣的类型</text>
        <view class="preference-tags">
          <view 
            v-for="cat in categories" 
            :key="cat.id"
            class="preference-tag"
            :class="{ active: selectedCategories.includes(cat.id) }"
            @click="toggleCategory(cat.id)"
          >
            {{ cat.name }}
          </view>
        </view>
        <view class="preference-actions">
          <button class="cancel-btn" @click="preferenceVisible = false">取消</button>
          <button class="confirm-btn" @click="savePreferences">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getHotSpots, getRecommendations, refreshRecommendations, getBanners } from '@/api/home'
import { getFilters } from '@/api/spot'
import { setPreferences } from '@/api/auth'
import { getImageUrl } from '@/utils/request'

// 数据
const banners = ref([])
const hotSpots = ref([])
const recommendations = ref([])
const recommendationType = ref('hot')
const needPreference = ref(false)

// 偏好设置
const preferenceVisible = ref(false)
const categories = ref([])
const selectedCategories = ref([])

// 推荐类型文案
const recommendType = computed(() => {
  const types = {
    personalized: '为你推荐',
    preference: '根据偏好推荐',
    hot: '热门推荐'
  }
  return types[recommendationType.value] || '为你推荐'
})

// 获取轮播图
const fetchBanners = async () => {
  try {
    const res = await getBanners()
    banners.value = res.data?.list || []
  } catch (e) {
    console.error('获取轮播图失败', e)
  }
}

// 获取热门景点
const fetchHotSpots = async () => {
  try {
    const res = await getHotSpots(6)
    hotSpots.value = res.data?.list || []
  } catch (e) {
    console.error('获取热门景点失败', e)
  }
}


// 获取个性化推荐
const fetchRecommendations = async () => {
  try {
    const res = await getRecommendations(10)
    recommendations.value = res.data?.list || []
    recommendationType.value = res.data?.type || 'hot'
    needPreference.value = res.data?.needPreference || false
  } catch (e) {
    console.error('获取推荐失败', e)
  }
}

// 刷新推荐
const handleRefresh = async () => {
  uni.showLoading({ title: '加载中...' })
  try {
    const res = await refreshRecommendations(10)
    recommendations.value = res.data?.list || []
    recommendationType.value = res.data?.type || 'hot'
    needPreference.value = res.data?.needPreference || false
    uni.showToast({ title: '已刷新', icon: 'none' })
  } catch (e) {
    uni.showToast({ title: '刷新失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res = await getFilters()
    categories.value = res.data?.categories || []
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

// 显示偏好设置弹窗
const showPreferencePopup = async () => {
  if (categories.value.length === 0) {
    await fetchCategories()
  }
  preferenceVisible.value = true
}

// 切换分类选择
const toggleCategory = (id) => {
  const index = selectedCategories.value.indexOf(id)
  if (index > -1) {
    selectedCategories.value.splice(index, 1)
  } else {
    if (selectedCategories.value.length < 5) {
      selectedCategories.value.push(id)
    } else {
      uni.showToast({ title: '最多选择5个', icon: 'none' })
    }
  }
}

// 保存偏好
const savePreferences = async () => {
  if (selectedCategories.value.length === 0) {
    uni.showToast({ title: '请至少选择一个', icon: 'none' })
    return
  }
  
  try {
    // 调用保存偏好接口
    await setPreferences(selectedCategories.value)
    uni.showToast({ title: '设置成功', icon: 'success' })
    preferenceVisible.value = false
    
    // 刷新推荐
    handleRefresh()
  } catch (e) {
    console.error('保存偏好失败', e)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

// 轮播图点击
const handleBannerClick = (banner) => {
  if (banner.spotId) {
    goSpotDetail(banner.spotId)
  } else if (banner.linkUrl) {
    // 外部链接
  }
}

// 跳转景点详情
const goSpotDetail = (spotId) => {
  uni.navigateTo({
    url: `/pages/spot/detail?id=${spotId}`
  })
}

// 跳转景点列表
const goSpotList = () => {
  uni.switchTab({
    url: '/pages/spot/list'
  })
}

// 下拉刷新
onPullDownRefresh(async () => {
  await Promise.all([fetchBanners(), fetchHotSpots(), fetchRecommendations()])
  uni.stopPullDownRefresh()
})

// 页面显示时刷新
onShow(() => {
  fetchBanners()
  fetchHotSpots()
  fetchRecommendations()
})
</script>

<style scoped>
.index-page {
  padding-bottom: 20rpx;
  background: #f5f5f5;
  min-height: 100vh;
}

/* 轮播图 */
.banner {
  width: 100%;
  height: 360rpx;
}

.banner-image {
  width: 100%;
  height: 100%;
}

.banner-placeholder {
  width: 100%;
  height: 360rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 28rpx;
}

/* 区块 */
.section {
  margin-top: 20rpx;
  padding: 0 20rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.section-more,
.section-refresh {
  font-size: 26rpx;
  color: #409EFF;
}

/* 热门推荐横向滚动 */
.hot-list {
  white-space: nowrap;
}

.hot-item {
  display: inline-block;
  width: 280rpx;
  margin-right: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.hot-image {
  width: 280rpx;
  height: 200rpx;
}

.hot-info {
  padding: 16rpx;
}

.hot-name {
  font-size: 28rpx;
  color: #333;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: bold;
}

.hot-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12rpx;
}

.hot-rating {
  font-size: 24rpx;
  color: #ff9500;
}

.hot-price {
  font-size: 28rpx;
  color: #ff6b6b;
  font-weight: bold;
}

/* 偏好设置提示 */
.preference-tip {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #fff9e6 0%, #fff3cd 100%);
  padding: 20rpx 24rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
}

.tip-icon {
  font-size: 32rpx;
  margin-right: 12rpx;
}

.tip-text {
  flex: 1;
  font-size: 26rpx;
  color: #856404;
}

.tip-arrow {
  font-size: 32rpx;
  color: #856404;
}

/* 个性化推荐 */
.recommend-list {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.recommend-item {
  width: calc(50% - 10rpx);
  padding: 0;
  overflow: hidden;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.recommend-image {
  width: 100%;
  height: 200rpx;
}

.recommend-info {
  padding: 16rpx;
}

.recommend-name {
  font-size: 28rpx;
  color: #333;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: bold;
}

.recommend-category {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
  display: block;
}

.recommend-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12rpx;
}

.recommend-rating {
  font-size: 24rpx;
  color: #ff9500;
}

.recommend-price {
  font-size: 28rpx;
  color: #ff6b6b;
  font-weight: bold;
}

/* 空状态 */
.empty-tip {
  text-align: center;
  padding: 40rpx;
  color: #999;
  font-size: 28rpx;
}

/* 偏好设置弹窗 */
.preference-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.preference-content {
  width: 600rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
}

.preference-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  text-align: center;
  display: block;
  margin-bottom: 30rpx;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 30rpx;
}

.preference-tag {
  padding: 16rpx 28rpx;
  background: #f5f5f5;
  border-radius: 32rpx;
  font-size: 26rpx;
  color: #666;
}

.preference-tag.active {
  background: #409EFF;
  color: #fff;
}

.preference-actions {
  display: flex;
  gap: 20rpx;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.confirm-btn {
  background: #409EFF;
  color: #fff;
}
</style>
