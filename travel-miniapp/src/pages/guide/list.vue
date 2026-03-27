<template>
  <view class="ios-page">
    <view class="list-state-card">
      <view class="state-main">
        <text class="state-title">攻略列表</text>
        <text class="state-desc">{{ currentStateText }}</text>
      </view>
      <view class="state-actions">
        <text
          class="sort-chip"
          :class="{ active: sortBy === 'time' }"
          @click="changeSort('time')"
        >
          最新优先
        </text>
        <text
          class="sort-chip"
          :class="{ active: sortBy === 'category' }"
          @click="changeSort('category')"
        >
          分类排序
        </text>
        <text class="reset-link" @click="resetFilters">重置</text>
      </view>
    </view>

    <!-- 分类筛选 -->
    <scroll-view class="category-bar" scroll-x :show-scrollbar="false">
      <view 
        class="category-item" 
        :class="{ active: currentCategory === '' }"
        @click="selectCategory('')"
      >
        全部
      </view>
      <view 
        class="category-item" 
        v-for="cat in categories" 
        :key="cat"
        :class="{ active: currentCategory === cat }"
        @click="selectCategory(cat)"
      >
        {{ cat }}
      </view>
    </scroll-view>

    <!-- 攻略列表 -->
    <scroll-view class="guide-list" scroll-y @scrolltolower="loadMore">
      <view 
        class="guide-card" 
        v-for="guide in guideList" 
        :key="guide.id"
        @click="goDetail(guide.id)"
      >
        <image class="guide-cover" :src="getImageUrl(guide.coverImage)" mode="aspectFill" />
        <view class="guide-info">
          <text class="guide-title">{{ guide.title }}</text>
          <text class="guide-summary">{{ guide.summary }}</text>
          <view class="guide-meta">
            <text class="guide-category">{{ guide.category }}</text>
            <text class="guide-views">👁 {{ guide.viewCount }}</text>
          </view>
        </view>
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      <view class="no-more" v-if="!hasMore && guideList.length > 0">
        <text>没有更多了</text>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-if="!loading && guideList.length === 0">
        <text class="empty-text">暂无攻略</text>
        <view class="empty-reset" @click="resetFilters">清空筛选</view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getGuideList, getCategories } from '@/api/guide'
import { promptLogin } from '@/utils/auth'
import { getImageUrl } from '@/utils/request'


const categories = ref([])
const currentCategory = ref('')
const sortBy = ref('time')

const guideList = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const hasMore = computed(() => guideList.value.length < total.value)
const currentStateText = computed(() => {
  const categoryText = currentCategory.value ? `分类：${currentCategory.value}` : '全部分类'
  const sortText = sortBy.value === 'category' ? '分类排序' : '最新优先'
  return `${categoryText} · 共 ${total.value} 条 · ${sortText}`
})

const fetchCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

const fetchGuideList = async (isRefresh = false) => {
  if (loading.value) return
  loading.value = true

  try {
    const params = {
      page: isRefresh ? 1 : page.value,
      pageSize: pageSize.value,
      sortBy: sortBy.value
    }
    if (currentCategory.value) {
      params.category = currentCategory.value
    }

    const res = await getGuideList(params)
    const list = res.data.list || []
    
    if (isRefresh) {
      guideList.value = list
      page.value = 1
    } else {
      guideList.value = [...guideList.value, ...list]
    }
    total.value = res.data.total || 0
    page.value++
  } catch (e) {
    console.error('获取攻略列表失败', e)
  } finally {
    loading.value = false
  }
}

const selectCategory = (cat) => {
  currentCategory.value = cat
  fetchGuideList(true)
}

const changeSort = (value) => {
  if (sortBy.value === value) return
  sortBy.value = value
  fetchGuideList(true)
}

const resetFilters = () => {
  currentCategory.value = ''
  sortBy.value = 'time'
  fetchGuideList(true)
}

const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchGuideList()
  }
}

const goDetail = (id) => {
  if (!promptLogin('登录后可查看攻略详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/guide/detail?id=${id}` })
}

onLoad((options) => {
  if (typeof options?.category === 'string' && options.category) {
    currentCategory.value = decodeURIComponent(options.category)
  }

  if (options?.sortBy === 'time' || options?.sortBy === 'category') {
    sortBy.value = options.sortBy
  }
})

onMounted(() => {
  fetchCategories()
  fetchGuideList(true)
})

onShow(() => {
  const updatedGuide = uni.getStorageSync('guide_detail_updated')
  if (updatedGuide?.id) {
    const guideIndex = guideList.value.findIndex(item => item.id === updatedGuide.id)
    if (guideIndex !== -1) {
      guideList.value[guideIndex] = {
        ...guideList.value[guideIndex],
        ...updatedGuide
      }
    }
    uni.removeStorageSync('guide_detail_updated')
  }

  const updated = uni.getStorageSync('guide_view_updated')
  if (!updated || !updated.id) return
  const idx = guideList.value.findIndex(item => item.id === updated.id)
  if (idx !== -1) {
    guideList.value[idx] = {
      ...guideList.value[idx],
      viewCount: updated.viewCount
    }
  }
  uni.removeStorageSync('guide_view_updated')
})
</script>

<style scoped>
.ios-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #F2F2F7;
}

.list-state-card {
  padding: 24rpx 32rpx 20rpx;
  background: #fff;
  border-bottom: 1px solid #F2F2F7;
}

.state-main {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.state-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1C1C1E;
}

.state-desc {
  font-size: 24rpx;
  color: #8E8E93;
}

.state-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 18rpx;
}

.sort-chip {
  padding: 10rpx 22rpx;
  border-radius: 999rpx;
  background: #F2F2F7;
  font-size: 24rpx;
  color: #8E8E93;
}

.sort-chip.active {
  background: rgba(0, 122, 255, 0.1);
  color: #007AFF;
}

.reset-link {
  margin-left: auto;
  font-size: 24rpx;
  color: #007AFF;
}

/* 分类栏 */
.category-bar {
  white-space: nowrap;
  background: #fff;
  padding: 20rpx 32rpx;
  border-bottom: 1px solid #F2F2F7;
}

.category-item {
  display: inline-block;
  padding: 14rpx 32rpx;
  margin-right: 16rpx;
  background: #F2F2F7;
  border-radius: 100rpx;
  font-size: 26rpx;
  color: #8E8E93;
}

.category-item.active {
  background: #007AFF;
  color: #fff;
}

/* 攻略列表 */
.guide-list {
  flex: 1;
  padding: 24rpx 32rpx;
  box-sizing: border-box;
}

.guide-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  width: 100%;
}

.guide-cover {
  width: 100%;
  height: 300rpx;
  display: block;
}

.guide-info {
  padding: 24rpx;
}

.guide-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1C1C1E;
  display: block;
  margin-bottom: 12rpx;
}

.guide-summary {
  font-size: 26rpx;
  color: #8E8E93;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.guide-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
}

.guide-category {
  font-size: 24rpx;
  color: #007AFF;
  background: rgba(0, 122, 255, 0.1);
  padding: 6rpx 16rpx;
  border-radius: 100rpx;
}

.guide-views {
  font-size: 24rpx;
  color: #8E8E93;
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
  padding: 100rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #8E8E93;
}

.empty-reset {
  margin-top: 28rpx;
  padding: 14rpx 36rpx;
  border-radius: 999rpx;
  background: #fff;
  border: 1px solid #d1d5db;
  font-size: 24rpx;
  color: #4B5563;
}
</style>
