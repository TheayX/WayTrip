<template>
  <view class="ios-page">
    <!-- iOS 风格头部 -->
    <view class="ios-header">
      <view class="header-top">
        <text class="large-title">探索</text>
        <image class="avatar-sm" :src="userInfo?.avatar || '/static/default-avatar.png'" @click="goMine"/>
      </view>
      <view class="search-bar" @click="goSearch">
        <image class="search-icon" src="/static/搜索.png" />
        <text class="search-placeholder">搜索景点、攻略...</text>
      </view>
    </view>

    <!-- 轮播图 -->
    <view class="banner-container" v-if="banners.length">
      <swiper class="banner" indicator-dots indicator-active-color="#fff" autoplay circular>
        <swiper-item v-for="banner in banners" :key="banner.id" @click="handleBannerClick(banner)">
          <image class="banner-image" :src="getImageUrl(banner.imageUrl)" mode="aspectFill" />
        </swiper-item>
      </swiper>
    </view>

    <!-- 热门目的地 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">热门目的地</text>
        <text class="section-more" @click="goSpotList">查看全部</text>
      </view>
      <scroll-view class="hot-scroll" scroll-x :show-scrollbar="false" v-if="hotSpots.length">
        <view class="hot-card" v-for="spot in hotSpots" :key="spot.id" @click="goSpotDetail(spot.id)">
          <image class="hot-img" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="hot-overlay">
            <text class="hot-name">{{ spot.name }}</text>
            <view class="hot-badge">¥{{ spot.price }} 起</view>
          </view>
        </view>
      </scroll-view>
      <view class="empty-tip" v-else>
        <text>暂无热门景点</text>
      </view>
    </view>

    <!-- 为你推荐 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">{{ recommendType }}</text>
        <text class="section-more" @click="handleRefresh">换一批</text>
      </view>
      
      <!-- 偏好设置提示 -->
      <view class="preference-tip" v-if="needPreference" @click="showPreferencePopup">
        <text class="tip-text">设置偏好标签，获取更精准的推荐</text>
        <text class="tip-arrow">›</text>
      </view>

      <view class="recommend-list" v-if="recommendations.length">
        <view class="recommend-card" v-for="spot in recommendations" :key="spot.id" @click="goSpotDetail(spot.id)">
          <image class="rec-img" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="rec-content">
            <view class="rec-header">
              <text class="rec-name">{{ spot.name }}</text>
              <text class="rec-rating">★ {{ spot.avgRating || '4.5' }}</text>
            </view>
            <text class="rec-desc">{{ spot.intro || '暂无介绍，点击查看详情...' }}</text>
            <view class="rec-footer">
              <text class="rec-tag">{{ spot.categoryName }}</text>
              <text class="rec-price">¥{{ spot.price }}</text>
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
            class="preference-tag-item"
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
import { useUserStore } from '@/stores/user'

// 用户信息
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

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
    await setPreferences(selectedCategories.value)
    uni.showToast({ title: '设置成功', icon: 'success' })
    preferenceVisible.value = false
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
  }
}

// 跳转
const goSpotDetail = (spotId) => {
  uni.navigateTo({ url: `/pages/spot/detail?id=${spotId}` })
}

const goSpotList = () => {
  uni.switchTab({ url: '/pages/spot/list' })
}

const goMine = () => {
  uni.switchTab({ url: '/pages/mine/index' })
}

const goSearch = () => {
  uni.navigateTo({ url: '/pages/spot/search' })
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
/* iOS 风格页面 */
.ios-page {
  background-color: #F2F2F7;
  min-height: 100vh;
  padding-bottom: 40rpx;
}

/* 头部 */
.ios-header {
  padding: 88rpx 32rpx 20rpx;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.large-title {
  font-size: 60rpx;
  font-weight: 800;
  color: #000;
  letter-spacing: -1px;
}

.avatar-sm {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #eee;
}

/* 搜索栏 */
.search-bar {
  background: #E3E3E8;
  height: 80rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
}

.search-icon {
  width: 32rpx;
  height: 32rpx;
}

.search-placeholder {
  color: #8E8E93;
  font-size: 30rpx;
  margin-left: 12rpx;
}

/* 轮播图 */
.banner-container {
  padding: 32rpx;
}

.banner {
  width: 100%;
  height: 360rpx;
  border-radius: 32rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.08);
}

.banner-image {
  width: 100%;
  height: 360rpx;
  border-radius: 32rpx;
}

/* 板块通用 */
.section {
  margin-bottom: 40rpx;
}

.section-header {
  padding: 0 32rpx 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.section-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #000;
}

.section-more {
  color: #007AFF;
  font-size: 30rpx;
}

/* 热门横向滚动 */
.hot-scroll {
  white-space: nowrap;
  padding-left: 32rpx;
}

.hot-card {
  display: inline-block;
  width: 300rpx;
  height: 400rpx;
  margin-right: 24rpx;
  border-radius: 32rpx;
  overflow: hidden;
  position: relative;
  box-shadow: 0 8rpx 20rpx rgba(0,0,0,0.1);
}

.hot-img {
  width: 100%;
  height: 100%;
}

.hot-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 30rpx 20rpx;
  background: linear-gradient(to top, rgba(0,0,0,0.6), transparent);
}

.hot-name {
  display: block;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hot-badge {
  display: inline-block;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(10px);
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  color: #fff;
  font-size: 22rpx;
  margin-top: 8rpx;
}

/* 偏好设置提示 */
.preference-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 24rpx 32rpx;
  margin: 0 32rpx 24rpx;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}

.tip-text {
  font-size: 28rpx;
  color: #007AFF;
}

.tip-arrow {
  font-size: 32rpx;
  color: #C7C7CC;
}

/* 推荐列表 */
.recommend-list {
  padding: 0 32rpx;
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.recommend-card {
  background: #fff;
  border-radius: 32rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}

.rec-img {
  width: 100%;
  height: 300rpx;
}

.rec-content {
  padding: 24rpx;
}

.rec-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.rec-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #1c1c1e;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rec-rating {
  color: #FF9500;
  font-size: 26rpx;
  font-weight: 600;
  margin-left: 16rpx;
}

.rec-desc {
  font-size: 28rpx;
  color: #8E8E93;
  line-height: 1.4;
  margin-bottom: 16rpx;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.rec-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rec-tag {
  font-size: 22rpx;
  color: #007AFF;
  background: rgba(0, 122, 255, 0.1);
  padding: 6rpx 16rpx;
  border-radius: 100rpx;
}

.rec-price {
  font-size: 32rpx;
  color: #FF3B30;
  font-weight: 600;
}

/* 空状态 */
.empty-tip {
  text-align: center;
  padding: 60rpx;
  color: #8E8E93;
  font-size: 28rpx;
}

/* 偏好设置弹窗 */
.preference-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.preference-content {
  width: 600rpx;
  background: #fff;
  border-radius: 28rpx;
  padding: 40rpx;
}

.preference-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #000;
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

.preference-tag-item {
  padding: 16rpx 28rpx;
  background: #F2F2F7;
  border-radius: 100rpx;
  font-size: 26rpx;
  color: #666;
}

.preference-tag-item.active {
  background: #007AFF;
  color: #fff;
}

.preference-actions {
  display: flex;
  gap: 20rpx;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  border: none;
}

.cancel-btn {
  background: #F2F2F7;
  color: #666;
}

.confirm-btn {
  background: #007AFF;
  color: #fff;
}
</style>
