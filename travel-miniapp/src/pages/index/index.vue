<template>
  <view class="ios-page">
    <view class="ios-header">
      <view class="header-top">
        <view>
          <text class="large-title">首页</text>
          <text class="sub-title">推荐内容、快捷入口和热门景点都集中在这里</text>
        </view>
        <image class="avatar-sm" :src="getAvatarUrl(userInfo?.avatar)" @click="goMine" />
      </view>
      <view class="search-bar" @click="goSearch">
        <uni-search-bar
          :modelValue="''"
          placeholder="搜索景点、攻略..."
          :clearButton="'none'"
          :cancelButton="'none'"
          :radius="20"
          :readonly="true"
          bgColor="#E9EEF5"
        />
      </view>
    </view>

    <view class="banner-container" v-if="banners.length">
      <swiper class="banner" indicator-dots indicator-active-color="#fff" autoplay circular>
        <swiper-item v-for="banner in banners" :key="banner.id" @click="handleBannerClick(banner)">
          <image class="banner-image" :src="getContentImageUrl(banner.imageUrl)" mode="aspectFill" />
        </swiper-item>
      </swiper>
    </view>

    <view v-if="!isLoggedIn" class="guest-banner" @click="goMine">
      <view class="guest-copy">
        <text class="guest-title">登录后可保存偏好，拿到更稳定的推荐结果</text>
      </view>
      <text class="guest-action">去登录</text>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">快捷导航</text>
        <text class="section-desc">已确定入口先保留，其他功能后续补充</text>
      </view>
      <view class="quick-grid">
        <view
          v-for="action in displayQuickActions"
          :key="action.id"
          class="quick-item"
          @click="handleQuickAction(action)"
        >
          <view class="quick-icon" :class="`theme-${action.theme}`">
            <uni-icons :type="action.icon" size="24" color="#1F2937" />
          </view>
          <text class="quick-title">{{ action.title }}</text>
          <text class="quick-note">{{ action.note }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">{{ recommendType }}</text>
        <view class="section-actions">
          <text class="section-link" @click="handleRefresh">换一批</text>
          <text class="section-link" @click="goRecommendList">查看更多</text>
        </view>
      </view>

      <view class="preference-tip" v-if="needPreference" @click="showPreferencePopup">
        <view>
          <text class="tip-text">选择感兴趣的景点分类，帮助冷启动推荐更准确</text>
          <text class="tip-subtext">当前偏好标签统一使用景点分类</text>
        </view>
        <text class="tip-arrow">›</text>
      </view>

      <view class="recommend-list" v-if="recommendPreview.length">
        <view class="recommend-card" v-for="spot in recommendPreview" :key="spot.id" @click="goSpotDetail(spot.id)">
          <image class="rec-img" :src="getContentImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="rec-content">
            <view class="rec-header">
              <text class="rec-name">{{ spot.name }}</text>
              <text class="rec-rating">★ {{ spot.avgRating || '4.5' }}</text>
            </view>
            <text class="rec-desc">{{ spot.intro || '暂无介绍，点击查看详情...' }}</text>
            <view class="rec-footer">
              <text class="rec-tag">{{ spot.categoryName || '景点' }}</text>
              <text class="rec-price">¥{{ spot.price }}</text>
            </view>
          </view>
        </view>
      </view>
      <view class="empty-tip" v-else>
        <text>当前暂无推荐内容</text>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">热门目的地</text>
        <text class="section-link" @click="goSpotList">查看全部</text>
      </view>
      <scroll-view class="hot-scroll" scroll-x :show-scrollbar="false" v-if="hotSpots.length">
        <view class="hot-card" v-for="spot in hotSpots" :key="spot.id" @click="goSpotDetail(spot.id)">
          <image class="hot-img" :src="getContentImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="hot-overlay">
            <text class="hot-name">{{ spot.name }}</text>
            <view class="hot-meta">
              <text class="hot-badge">{{ spot.categoryName || '热门' }}</text>
              <text class="hot-price">¥{{ spot.price }} 起</text>
            </view>
          </view>
        </view>
      </scroll-view>
      <view class="empty-tip" v-else>
        <text>当前暂无热门景点</text>
      </view>
    </view>

    <view class="preference-popup" v-if="preferenceVisible" @click.self="preferenceVisible = false">
      <view class="preference-content">
        <text class="preference-title">选择你感兴趣的景点分类</text>
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
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getBanners, getHotSpots, getRecommendations, refreshRecommendations } from '@/api/home'
import { updatePreferences } from '@/api/auth'
import { getFilters } from '@/api/spot'
import { promptLogin } from '@/utils/auth'
import { getAvatarUrl, getContentImageUrl } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn)

const banners = ref([])
const hotSpots = ref([])
const recommendations = ref([])
const recommendationType = ref('hot')
const needPreference = ref(false)

const preferenceVisible = ref(false)
const categories = ref([])
const selectedCategories = ref([])

const quickActions = [
  { id: 'spots', title: '景点列表', note: '全部景点', icon: 'location-filled', theme: 'blue', action: 'spot-list' },
  { id: 'guides', title: '攻略列表', note: '游玩攻略', icon: 'paperplane-filled', theme: 'orange', action: 'guide-list' }
]

const pendingQuickActions = Array.from({ length: 6 }, (_, index) => ({
  id: `pending-${index + 1}`,
  title: '待定入口',
  note: '后续再定',
  icon: 'more-filled',
  theme: 'slate',
  action: 'pending'
}))

const displayQuickActions = [
  ...quickActions.slice(0, 2),
  ...pendingQuickActions
]

const recommendType = computed(() => {
  const types = {
    personalized: '为你推荐',
    preference: '猜你喜欢',
    hot: '热门推荐'
  }
  return types[recommendationType.value] || '为你推荐'
})

const recommendPreview = computed(() => recommendations.value.slice(0, 4))

const fetchBanners = async () => {
  try {
    const res = await getBanners()
    banners.value = res.data?.list || []
  } catch (error) {
    console.error('获取轮播图失败', error)
  }
}

const fetchHotSpots = async () => {
  try {
    const res = await getHotSpots(6)
    hotSpots.value = res.data?.list || []
  } catch (error) {
    console.error('获取热门景点失败', error)
  }
}

const fetchRecommendations = async () => {
  try {
    const res = await getRecommendations(12)
    recommendations.value = res.data?.list || []
    recommendationType.value = res.data?.type || 'hot'
    needPreference.value = res.data?.needPreference || false
  } catch (error) {
    console.error('获取推荐失败', error)
  }
}

const fetchCategories = async () => {
  try {
    const res = await getFilters()
    categories.value = res.data?.categories || []
  } catch (error) {
    console.error('获取分类失败', error)
  }
}

const handleRefresh = async () => {
  uni.showLoading({ title: '加载中...' })
  try {
    const res = await refreshRecommendations(12)
    recommendations.value = res.data?.list || []
    recommendationType.value = res.data?.type || 'hot'
    needPreference.value = res.data?.needPreference || false
    uni.showToast({ title: '已刷新', icon: 'none' })
  } catch (error) {
    console.error('刷新推荐失败', error)
    uni.showToast({ title: '刷新失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

const showPreferencePopup = async () => {
  if (!categories.value.length) {
    await fetchCategories()
  }
  preferenceVisible.value = true
}

const toggleCategory = (id) => {
  const index = selectedCategories.value.indexOf(id)
  if (index > -1) {
    selectedCategories.value.splice(index, 1)
    return
  }
  if (selectedCategories.value.length >= 5) {
    uni.showToast({ title: '最多选择5个', icon: 'none' })
    return
  }
  selectedCategories.value.push(id)
}

const savePreferences = async () => {
  if (!selectedCategories.value.length) {
    uni.showToast({ title: '请至少选择一个', icon: 'none' })
    return
  }

  try {
    const categoryNames = selectedCategories.value
      .map(id => categories.value.find(cat => cat.id === id)?.name)
      .filter(Boolean)
    await updatePreferences({ categoryIds: selectedCategories.value })
    userStore.updatePreferences({
      preferences: categoryNames,
      preferenceCategoryIds: [...selectedCategories.value],
      preferenceCategoryNames: categoryNames
    })
    preferenceVisible.value = false
    uni.showToast({ title: '设置成功', icon: 'success' })
    await handleRefresh()
  } catch (error) {
    console.error('保存偏好失败', error)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

const handleBannerClick = (banner) => {
  if (banner.spotId) {
    goSpotDetail(banner.spotId)
  }
}

const handleQuickAction = (action) => {
  switch (action.action) {
    case 'spot-list':
      goSpotList()
      break
    case 'guide-list':
      goGuideList()
      break
    case 'pending':
      uni.showToast({ title: '入口功能待确定', icon: 'none' })
      break
    default:
      break
  }
}

const goSpotDetail = (spotId) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/spot/detail?id=${spotId}&source=home` })
}

const goSpotList = () => {
  uni.navigateTo({ url: '/pages/spot/list?sortBy=heat' })
}

const goGuideList = () => {
  uni.navigateTo({ url: '/pages/guide/list?sortBy=time' })
}

const goRecommendList = () => {
  uni.navigateTo({ url: '/pages/recommendation/index' })
}

const goSearch = () => {
  uni.navigateTo({ url: '/pages/spot/search' })
}

const goMine = () => {
  uni.switchTab({ url: '/pages/mine/index' })
}

onPullDownRefresh(async () => {
  await Promise.all([fetchBanners(), fetchHotSpots(), fetchRecommendations()])
  uni.stopPullDownRefresh()
})

onShow(() => {
  fetchBanners()
  fetchHotSpots()
  fetchRecommendations()
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background: #f4f6fb;
  padding-bottom: 48rpx;
}

.ios-header {
  padding: 88rpx 32rpx 24rpx;
  background: linear-gradient(180deg, #ffffff 0%, #eef4ff 100%);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.large-title {
  display: block;
  font-size: 56rpx;
  font-weight: 800;
  color: #111827;
}

.sub-title {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.avatar-sm {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #dbe4f0;
}

.search-bar {
  pointer-events: auto;
}

:deep(.search-bar .uni-searchbar) {
  padding: 0;
  background: transparent;
}

:deep(.search-bar .uni-searchbar__box) {
  height: 80rpx;
  border-radius: 20rpx;
}

.banner-container {
  padding: 28rpx 32rpx 12rpx;
}

.banner,
.banner-image {
  width: 100%;
  height: 320rpx;
  border-radius: 28rpx;
}

.banner {
  overflow: hidden;
  box-shadow: 0 10rpx 28rpx rgba(31, 41, 55, 0.12);
}

.guest-banner {
  margin: 16rpx 32rpx 0;
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 6rpx 16rpx rgba(31, 41, 55, 0.06);
}

.guest-title {
  display: block;
  font-size: 28rpx;
  line-height: 1.5;
  color: #1f2937;
}

.guest-action {
  font-size: 24rpx;
  font-weight: 600;
  color: #2563eb;
}

.section {
  margin-top: 28rpx;
}

.section-header {
  padding: 0 32rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.section-title {
  font-size: 38rpx;
  font-weight: 700;
  color: #111827;
}

.section-desc {
  font-size: 24rpx;
  color: #6b7280;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.section-link {
  font-size: 26rpx;
  color: #2563eb;
}

.quick-grid {
  padding: 0 32rpx;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20rpx;
}

.quick-item {
  padding: 24rpx 12rpx;
  background: #ffffff;
  border-radius: 24rpx;
  text-align: center;
  box-shadow: 0 6rpx 16rpx rgba(31, 41, 55, 0.05);
}

.quick-icon {
  width: 80rpx;
  height: 80rpx;
  margin: 0 auto 14rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.theme-blue { background: #dbeafe; }
.theme-orange { background: #ffedd5; }
.theme-green { background: #dcfce7; }
.theme-purple { background: #ede9fe; }
.theme-pink { background: #fce7f3; }
.theme-red { background: #fee2e2; }
.theme-yellow { background: #fef3c7; }
.theme-teal { background: #ccfbf1; }
.theme-slate { background: #e5e7eb; }

.quick-title {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: #111827;
}

.quick-note {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #6b7280;
}

.preference-tip {
  margin: 0 32rpx 24rpx;
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 6rpx 16rpx rgba(31, 41, 55, 0.05);
}

.tip-text {
  display: block;
  font-size: 28rpx;
  color: #2563eb;
}

.tip-subtext {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #6b7280;
}

.tip-arrow {
  font-size: 34rpx;
  color: #9ca3af;
}

.recommend-list {
  padding: 0 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.recommend-card {
  background: #ffffff;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.06);
}

.rec-img {
  width: 100%;
  height: 280rpx;
}

.rec-content {
  padding: 24rpx;
}

.rec-header,
.rec-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.rec-header {
  margin-bottom: 12rpx;
}

.rec-name {
  flex: 1;
  font-size: 32rpx;
  font-weight: 600;
  color: #111827;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.rec-rating {
  font-size: 24rpx;
  color: #f59e0b;
}

.rec-desc {
  display: -webkit-box;
  margin-bottom: 18rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: #6b7280;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.rec-tag {
  font-size: 22rpx;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.1);
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
}

.rec-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #ef4444;
}

.hot-scroll {
  padding-left: 32rpx;
  white-space: nowrap;
}

.hot-card {
  display: inline-block;
  width: 280rpx;
  height: 360rpx;
  margin-right: 20rpx;
  border-radius: 28rpx;
  overflow: hidden;
  position: relative;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.08);
}

.hot-img {
  width: 100%;
  height: 100%;
}

.hot-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24rpx 20rpx;
  background: linear-gradient(180deg, rgba(17, 24, 39, 0) 0%, rgba(17, 24, 39, 0.82) 100%);
}

.hot-name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
}

.hot-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 10rpx;
}

.hot-badge,
.hot-price {
  font-size: 22rpx;
  color: #ffffff;
}

.empty-tip {
  padding: 48rpx 32rpx;
  text-align: center;
  font-size: 26rpx;
  color: #6b7280;
}

.preference-popup {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.preference-content {
  width: 620rpx;
  background: #ffffff;
  border-radius: 28rpx;
  padding: 40rpx;
}

.preference-title {
  display: block;
  margin-bottom: 28rpx;
  font-size: 34rpx;
  font-weight: 700;
  text-align: center;
  color: #111827;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 28rpx;
}

.preference-tag-item {
  padding: 16rpx 24rpx;
  border-radius: 999rpx;
  background: #eef2f7;
  font-size: 24rpx;
  color: #4b5563;
}

.preference-tag-item.active {
  background: #2563eb;
  color: #ffffff;
}

.preference-actions {
  display: flex;
  gap: 20rpx;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 42rpx;
  font-size: 28rpx;
  border: none;
}

.cancel-btn {
  background: #eef2f7;
  color: #4b5563;
}

.confirm-btn {
  background: #2563eb;
  color: #ffffff;
}
</style>
