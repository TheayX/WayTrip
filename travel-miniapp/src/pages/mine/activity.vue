<!-- 我的互动页 -->
<template>
  <view class="activity-page">
    <!-- 顶部类型切换 -->
    <view class="tab-bar">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        <text class="tab-label">{{ tab.label }}</text>
      </view>
    </view>

    <!-- 内容区域 -->
    <scroll-view class="activity-scroll" scroll-y @scrolltolower="loadMore">
      <template v-if="activeTab === 'browse'">
        <view v-if="footprintList.length" class="card-list">
          <view
            v-for="item in footprintList"
            :key="item.id"
            class="browse-card"
            @click="goSpot(item.id, 'footprint')"
          >
            <image class="browse-image" :src="getImageUrl(item.coverImage)" mode="aspectFill" />
            <view class="browse-body">
              <text class="browse-name">{{ item.name }}</text>
              <text class="browse-meta">{{ item.regionName || '景点' }}</text>
              <text class="browse-time">浏览于 {{ formatViewedTime(item.viewedAt) }}</text>
            </view>
          </view>
        </view>
        <view v-else class="empty-wrap">
          <image class="empty-img" src="/static/empty-image.png" mode="aspectFit" />
          <text class="empty-text">你还没有浏览记录</text>
          <text class="empty-tip">去首页或发现页看看感兴趣的景点</text>
        </view>
      </template>

      <template v-else-if="activeTab === 'favorite'">
        <view v-if="favoriteList.length" class="card-list">
          <view
            class="favorite-card"
            v-for="spot in favoriteList"
            :key="spot.id"
            @click="goSpot(spot.id, 'favorite')"
          >
            <image class="favorite-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
            <view class="favorite-body">
              <text class="favorite-name">{{ spot.name }}</text>
              <text class="favorite-meta">{{ spot.regionName }} · {{ spot.categoryName }}</text>
              <view class="favorite-bottom">
                <text class="favorite-rating">★ {{ spot.avgRating }}</text>
                <text class="favorite-price">¥{{ spot.price }}</text>
              </view>
            </view>
            <view class="favorite-remove" @click.stop="removeFavoriteHandler(spot.id)">
              <text>取消</text>
            </view>
          </view>
        </view>
        <view v-else-if="!favoriteLoading" class="empty-wrap">
          <image class="empty-img" src="/static/empty-image.png" mode="aspectFit" />
          <text class="empty-text">暂无收藏</text>
          <text class="empty-tip">去发现喜欢的景点吧</text>
        </view>
      </template>

      <template v-else>
        <view v-if="reviewList.length" class="card-list">
          <view v-for="item in reviewList" :key="item.id" class="review-card">
            <image class="review-image" :src="getContentImageUrl(item.coverImageUrl)" mode="aspectFill" />
            <view class="review-body">
              <view class="review-top">
                <text class="review-name">{{ item.spotName || `景点 #${item.spotId}` }}</text>
                <text class="review-score">★ {{ item.score }}</text>
              </view>
              <text class="review-time">更新于 {{ item.updatedAt || item.createdAt || '-' }}</text>
              <text class="review-comment">{{ item.comment || '这条评价没有填写文字内容。' }}</text>
              <view class="review-actions">
                <button class="ghost-btn" @tap="openEdit(item)">编辑</button>
                <button class="danger-btn" @tap="handleDelete(item)">删除</button>
                <button class="link-btn" @tap="goSpot(item.spotId, 'review')">查看景点</button>
              </view>
            </view>
          </view>
        </view>
        <view v-else-if="!reviewLoading" class="empty-wrap">
          <image class="empty-img" src="/static/empty-image.png" mode="aspectFit" />
          <text class="empty-text">你还没有发布过评价</text>
          <text class="empty-tip">在景点详情里留下你的体验吧</text>
        </view>
      </template>

      <view v-if="showLoadMore" class="load-more">
        <button class="more-btn" @tap="loadMore" :disabled="currentLoading">
          {{ currentLoading ? '加载中...' : '加载更多' }}
        </button>
      </view>
    </scroll-view>

    <!-- 编辑评价弹层 -->
    <view v-if="editVisible" class="popup-mask" @tap="closeEdit">
      <view class="popup-panel" @tap.stop>
        <text class="popup-title">编辑评价</text>
        <text class="popup-spot">{{ currentReview?.spotName || '-' }}</text>
        <view class="star-row">
          <text
            v-for="i in 5"
            :key="i"
            class="star"
            :class="{ active: i <= editForm.score }"
            @tap="editForm.score = i"
          >★</text>
        </view>
        <textarea
          v-model="editForm.comment"
          class="comment-input"
          maxlength="300"
          placeholder="分享你的体验"
        />
        <view class="popup-actions">
          <button class="ghost-btn" @tap="closeEdit">取消</button>
          <button class="primary-btn" @tap="submitEdit">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import { getFavoriteList, removeFavorite } from '@/api/favorite'
import { deleteReview, getMyReviews, submitReview } from '@/api/review'
import { useUserStore } from '@/stores/user'
import { getContentImageUrl, getImageUrl } from '@/utils/request'

// 常量配置
const tabs = [
  { key: 'browse', label: '浏览' },
  { key: 'favorite', label: '收藏' },
  { key: 'review', label: '评价' }
]

// 基础依赖与用户状态
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

// 页面数据状态
const activeTab = ref('browse')
const footprintList = ref([])

const favoriteList = ref([])
const favoriteLoading = ref(false)
const favoritePage = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const reviewList = ref([])
const reviewLoading = ref(false)
const reviewPage = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const editVisible = ref(false)
const currentReview = ref(null)
const editForm = reactive({
  score: 5,
  comment: ''
})

// 计算属性
const currentLoading = computed(() => {
  if (activeTab.value === 'favorite') return favoriteLoading.value
  if (activeTab.value === 'review') return reviewLoading.value
  return false
})

const showLoadMore = computed(() => {
  if (activeTab.value === 'favorite') {
    return favoriteList.value.length > 0 && favoriteList.value.length < favoritePage.total
  }
  if (activeTab.value === 'review') {
    return reviewList.value.length > 0 && reviewList.value.length < reviewPage.total
  }
  return false
})

// 工具方法
const normalizeTab = (tab) => {
  return tabs.some(item => item.key === tab) ? tab : 'browse'
}

const ensureLogin = () => {
  if (isLoggedIn.value) return true
  uni.showToast({ title: '请先登录', icon: 'none' })
  setTimeout(() => {
    uni.navigateBack({ delta: 1 })
  }, 300)
  return false
}

const formatViewedTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const yyyy = date.getFullYear()
  const mm = `${date.getMonth() + 1}`.padStart(2, '0')
  const dd = `${date.getDate()}`.padStart(2, '0')
  const hh = `${date.getHours()}`.padStart(2, '0')
  const min = `${date.getMinutes()}`.padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${min}`
}

// 数据加载方法
const loadFootprints = () => {
  const history = uni.getStorageSync('spot_footprints')
  footprintList.value = Array.isArray(history) ? history : []
}

const fetchFavoritePage = async (reset = false) => {
  if (favoriteLoading.value) return
  favoriteLoading.value = true
  try {
    if (reset) {
      favoritePage.page = 1
      favoriteList.value = []
    }
    const res = await getFavoriteList(favoritePage.page, favoritePage.pageSize)
    const list = res.data?.list || []
    favoriteList.value = reset ? list : [...favoriteList.value, ...list]
    favoritePage.total = res.data?.total || 0
    favoritePage.page += 1
  } finally {
    favoriteLoading.value = false
  }
}

const fetchReviewPage = async (reset = false) => {
  if (reviewLoading.value) return
  reviewLoading.value = true
  try {
    if (reset) {
      reviewPage.page = 1
      reviewList.value = []
    }
    const res = await getMyReviews(reviewPage.page, reviewPage.pageSize)
    const list = res.data?.list || []
    reviewList.value = reset ? list : [...reviewList.value, ...list]
    reviewPage.total = res.data?.total || 0
    reviewPage.page += 1
  } finally {
    reviewLoading.value = false
  }
}

const loadActiveTab = async (reset = false) => {
  if (activeTab.value === 'browse') {
    loadFootprints()
    return
  }
  if (activeTab.value === 'favorite') {
    await fetchFavoritePage(reset)
    return
  }
  await fetchReviewPage(reset)
}

// 交互处理方法
const switchTab = async (tab) => {
  const nextTab = normalizeTab(tab)
  if (nextTab === activeTab.value) return
  activeTab.value = nextTab
  await loadActiveTab(true)
}

const loadMore = async () => {
  if (activeTab.value === 'favorite' && favoriteList.value.length < favoritePage.total) {
    await fetchFavoritePage()
  }
  if (activeTab.value === 'review' && reviewList.value.length < reviewPage.total) {
    await fetchReviewPage()
  }
}

const removeFavoriteHandler = async (spotId) => {
  try {
    await removeFavorite(spotId)
    favoriteList.value = favoriteList.value.filter(item => item.id !== spotId)
    favoritePage.total = Math.max(favoritePage.total - 1, 0)
    uni.showToast({ title: '已取消收藏', icon: 'none' })
  } catch (e) {
    uni.showToast({ title: '取消收藏失败', icon: 'none' })
  }
}

const openEdit = (item) => {
  currentReview.value = item
  editForm.score = item.score || 5
  editForm.comment = item.comment || ''
  editVisible.value = true
}

const closeEdit = () => {
  editVisible.value = false
}

const submitEdit = async () => {
  if (!currentReview.value) return
  if (editForm.score < 1) {
    uni.showToast({ title: '请先选择评分', icon: 'none' })
    return
  }
  try {
    await submitReview({
      spotId: currentReview.value.spotId,
      score: editForm.score,
      comment: editForm.comment
    })
    uni.showToast({ title: '评价已更新', icon: 'success' })
    editVisible.value = false
    await fetchReviewPage(true)
  } catch (e) {
    uni.showToast({ title: '更新失败', icon: 'none' })
  }
}

const handleDelete = (item) => {
  uni.showModal({
    title: '删除评价',
    content: '确认删除这条评价吗？删除后评分会一并撤销。',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteReview(item.id)
        uni.showToast({ title: '评价已删除', icon: 'success' })
        await fetchReviewPage(true)
      } catch (e) {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    }
  })
}

// 页面跳转方法
const goSpot = (spotId, source) => {
  uni.navigateTo({ url: `/pages/spot/detail?id=${spotId}&source=${source}` })
}

// 生命周期
onLoad((options) => {
  activeTab.value = normalizeTab(options?.tab)
})

onShow(async () => {
  if (!ensureLogin()) return
  await loadActiveTab(true)
})
</script>

<style scoped>
.activity-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f4f6fb 0%, #eef3f8 100%);
}

.tab-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  gap: 16rpx;
  padding: 20rpx 24rpx 16rpx;
  background: rgba(244, 246, 251, 0.85);
  backdrop-filter: blur(12px);
}

.tab-item {
  flex: 1;
  height: 80rpx;
  border-radius: 99rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.82);
  color: #64748b;
  transition: all 0.3s ease;
}

.tab-item.active {
  background: #2563eb;
  box-shadow: 0 8rpx 20rpx rgba(37, 99, 235, 0.25);
}

.tab-label {
  font-size: 28rpx;
  font-weight: 600;
}

.tab-item.active .tab-label {
  color: #fff;
}

.activity-scroll {
  height: calc(100vh - 112rpx);
  padding: 8rpx 24rpx 40rpx;
  box-sizing: border-box;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.browse-card,
.favorite-card,
.review-card {
  display: flex;
  gap: 24rpx;
  padding: 24rpx;
  border-radius: 36rpx;
  background: #fff;
  box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.04);
}

.browse-image,
.favorite-image,
.review-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 28rpx;
  flex-shrink: 0;
  background: #f1f5f9;
}

.browse-body,
.favorite-body,
.review-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.browse-name,
.favorite-name,
.review-name {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8rpx;
}

.browse-meta,
.favorite-meta,
.browse-time,
.review-time {
  display: block;
  font-size: 24rpx;
  color: #94a3b8;
  margin-bottom: 8rpx;
}

.favorite-bottom,
.review-top {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  align-items: flex-start;
}

.favorite-bottom {
  margin-top: 12rpx;
  align-items: baseline;
}

.favorite-rating,
.review-score {
  font-size: 26rpx;
  color: #f59e0b;
  font-weight: 700;
}

.favorite-price {
  font-size: 32rpx;
  color: #ef4444;
  font-weight: 800;
}

.favorite-remove {
  display: flex;
  align-items: center;
  color: #94a3b8;
  font-size: 26rpx;
  padding-left: 16rpx;
}

.review-comment {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  line-height: 1.6;
  color: #334155;
  background: #f8fafc;
  padding: 16rpx;
  border-radius: 16rpx;
}

.review-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 24rpx;
}

.ghost-btn,
.danger-btn,
.link-btn,
.primary-btn,
.more-btn {
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 99rpx;
  font-size: 26rpx;
  padding: 0 32rpx;
  margin: 0;
  font-weight: 600;
}

.ghost-btn {
  color: #2563eb;
  background: #eff6ff;
}

.danger-btn {
  color: #ef4444;
  background: #fef2f2;
}

.link-btn {
  color: #64748b;
  background: #f1f5f9;
}

.primary-btn {
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 4rpx 12rpx rgba(37, 99, 235, 0.3);
}

.load-more {
  margin-top: 32rpx;
  padding-bottom: 32rpx;
}

.more-btn {
  width: 100%;
  color: #64748b;
  background: #fff;
  box-shadow: 0 4rpx 12rpx rgba(17, 24, 39, 0.03);
  height: 88rpx;
  line-height: 88rpx;
  font-size: 28rpx;
}

.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 180rpx;
}

.empty-img {
  width: 240rpx;
  height: 240rpx;
  opacity: 0.8;
  margin-bottom: 24rpx;
}

.empty-text {
  font-size: 30rpx;
  color: #334155;
  font-weight: 600;
}

.empty-tip {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.popup-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.popup-panel {
  width: 100%;
  padding: 40rpx 40rpx 60rpx;
  border-radius: 40rpx 40rpx 0 0;
  background: #fff;
  box-sizing: border-box;
}

.popup-title {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 8rpx;
}

.popup-spot {
  display: block;
  font-size: 28rpx;
  color: #64748b;
}

.star-row {
  display: flex;
  gap: 16rpx;
  margin: 40rpx 0 32rpx;
}

.star {
  font-size: 56rpx;
  color: #e2e8f0;
  transition: color 0.2s ease;
}

.star.active {
  color: #f59e0b;
}

.comment-input {
  width: 100%;
  height: 240rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #f8fafc;
  font-size: 28rpx;
  box-sizing: border-box;
  color: #1e293b;
}

.popup-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 40rpx;
}

.popup-actions button {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
}
</style>
