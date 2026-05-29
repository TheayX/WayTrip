<!-- 攻略列表页 -->
<template>
  <view class="ios-page">
    <view class="hero-card">
      <view class="hero-copy">
        <text class="hero-eyebrow">旅行灵感</text>
        <text class="hero-title">从真实经验里挑一份更适合当下的攻略</text>
        <text class="hero-subtitle">{{ currentStateText }}</text>
      </view>
      <view class="state-actions">
        <text
          class="sort-chip"
          :class="{ active: sortBy === 'view_count' }"
          @click="changeSort('view_count')"
        >
          浏览量
        </text>
        <text
          class="sort-chip"
          :class="{ active: sortBy === 'time' }"
          @click="changeSort('time')"
        >
          最新发布
        </text>
        <text
          class="sort-chip"
          :class="{ active: sortBy === 'category' }"
          @click="changeSort('category')"
        >
          按主题看
        </text>
        <text class="reset-link" @click="resetFilters">清空</text>
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
          <view class="guide-label-row">
            <text class="guide-category">{{ resolveGuideCategory(guide.category) }}</text>
            <text class="guide-views">{{ guide.viewCount || 0 }} 次浏览</text>
          </view>
          <text class="guide-title">{{ resolveGuideText(guide.title) }}</text>
          <text class="guide-summary">{{ resolveGuideSummary(guide.summary) }}</text>
          <view class="guide-meta">
            <text class="guide-meta-text">打开查看完整路线与行程建议</text>
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
import { resolveMiniappGuideCategory, resolveMiniappGuideDisplayText } from '@/utils/resource-display'

// 统一解析攻略标题展示文案。
const resolveGuideText = (value) => resolveMiniappGuideDisplayText(value)

// 统一解析攻略分类展示文案。
const resolveGuideCategory = (value) => resolveMiniappGuideCategory(value)

// 列表摘要缺失时回退为默认介绍文案。
const resolveGuideSummary = (value) => value || '整理路线、玩法与出行经验，帮助你更快形成这次旅程的安排。'

// 页面数据状态
const categories = ref([])
const currentCategory = ref('')
const sortBy = ref('view_count')

const guideList = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const hasMore = computed(() => guideList.value.length < total.value)

// 计算属性
const currentStateText = computed(() => {
  const categoryText = currentCategory.value ? currentCategory.value : '全部主题'
  const sortTextMap = {
    view_count: '按浏览量整理',
    time: '按发布时间整理',
    category: '按主题整理'
  }
  const sortText = sortTextMap[sortBy.value] || '按浏览量整理'
  return `${categoryText} · ${total.value} 篇攻略 · ${sortText}`
})

// 只消费结构明确的详情页回写缓存，避免旧数据污染列表。
const isValidGuidePreview = (value) => {
  return value && typeof value === 'object' && !Array.isArray(value) && Number.isFinite(value.id)
}

// 浏览量回写缓存必须同时包含有效 ID 和数值型浏览量。
const isValidGuideViewCache = (value) => {
  return isValidGuidePreview(value) && typeof value.viewCount === 'number'
}

// 加载攻略分类筛选项。
const fetchCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

// 按当前筛选条件加载攻略列表，并兼容刷新与追加两种模式。
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

// 切换分类后重置为当前条件下的第一页结果。
const selectCategory = (cat) => {
  currentCategory.value = cat
  fetchGuideList(true)
}

// 切换排序方式后刷新列表。
const changeSort = (value) => {
  if (sortBy.value === value) return
  sortBy.value = value
  fetchGuideList(true)
}

// 恢复默认筛选条件并重新加载列表。
const resetFilters = () => {
  currentCategory.value = ''
  sortBy.value = 'view_count'
  fetchGuideList(true)
}

// 仅在存在下一页时继续加载更多攻略。
const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchGuideList()
  }
}

// 从列表进入攻略详情页。
const goDetail = (id) => {
  if (!promptLogin('登录后可查看攻略详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/guide/detail?id=${id}` })
}

// 首次进入时接收外部传入的分类和排序参数。
onLoad((options) => {
  if (typeof options?.category === 'string' && options.category) {
    currentCategory.value = decodeURIComponent(options.category)
  }

  if (options?.sortBy === 'view_count' || options?.sortBy === 'time' || options?.sortBy === 'category') {
    sortBy.value = options.sortBy
  }
})

// 页面挂载后初始化分类和首屏列表。
onMounted(() => {
  fetchCategories()
  fetchGuideList(true)
})

// 从详情页返回时回写最新攻略内容和浏览量。
onShow(() => {
  const updatedGuide = uni.getStorageSync('guide_detail_updated')
  if (isValidGuidePreview(updatedGuide)) {
    const guideIndex = guideList.value.findIndex(item => item.id === updatedGuide.id)
    if (guideIndex !== -1) {
      guideList.value[guideIndex] = {
        ...guideList.value[guideIndex],
        ...updatedGuide
      }
    }
  }
  if (updatedGuide) uni.removeStorageSync('guide_detail_updated')

  const updated = uni.getStorageSync('guide_view_updated')
  if (isValidGuideViewCache(updated)) {
    const idx = guideList.value.findIndex(item => item.id === updated.id)
    if (idx !== -1) {
      guideList.value[idx] = {
        ...guideList.value[idx],
        viewCount: updated.viewCount
      }
    }
  }
  if (updated) uni.removeStorageSync('guide_view_updated')
})
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
  margin: 24rpx 24rpx 18rpx;
  padding: 30rpx 28rpx 26rpx;
  border-radius: 32rpx;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.58) 100%);
  border: 1rpx solid rgba(255, 255, 255, 0.86);
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.hero-eyebrow {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: #71717a;
}

.hero-title {
  font-size: 42rpx;
  line-height: 1.3;
  font-weight: 600;
  color: #18181b;
}

.hero-subtitle {
  font-size: 24rpx;
  line-height: 1.6;
  color: #52525b;
}

.state-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16rpx;
  margin-top: 22rpx;
}

.sort-chip {
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  border: 1rpx solid rgba(15, 23, 42, 0.08);
  font-size: 24rpx;
  color: #52525b;
}

.sort-chip.active {
  background: rgba(24, 24, 27, 0.92);
  border-color: rgba(24, 24, 27, 0.92);
  color: #fafafa;
}

.reset-link {
  margin-left: auto;
  font-size: 24rpx;
  color: #a16207;
}

/* 分类栏 */
.category-bar {
  white-space: nowrap;
  padding: 0 24rpx 12rpx;
  box-sizing: border-box;
}

.category-item {
  display: inline-block;
  padding: 14rpx 28rpx;
  margin-right: 16rpx;
  background: rgba(255, 255, 255, 0.76);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 100rpx;
  font-size: 26rpx;
  color: #52525b;
  box-shadow:
    0 12rpx 30rpx rgba(15, 23, 42, 0.05),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.8);
}

.category-item.active {
  background: rgba(24, 24, 27, 0.92);
  border-color: rgba(24, 24, 27, 0.92);
  color: #fafafa;
}

/* 攻略列表 */
.guide-list {
  flex: 1;
  padding: 12rpx 24rpx 36rpx;
  box-sizing: border-box;
}

.guide-card {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.76);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 36rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
  width: 100%;
}

.guide-cover {
  width: 100%;
  height: 320rpx;
  display: block;
}

.guide-info {
  padding: 24rpx 24rpx 26rpx;
}

.guide-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 14rpx;
}

.guide-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #18181b;
  display: block;
  margin-bottom: 12rpx;
  line-height: 1.4;
}

.guide-summary {
  font-size: 26rpx;
  color: #52525b;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.guide-meta {
  margin-top: 16rpx;
}

.guide-category {
  font-size: 24rpx;
  color: #18181b;
  background: rgba(24, 24, 27, 0.06);
  padding: 8rpx 18rpx;
  border-radius: 100rpx;
}

.guide-views {
  font-size: 24rpx;
  color: #71717a;
}

.guide-meta-text {
  font-size: 23rpx;
  color: #71717a;
}

/* 加载状态 */
.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  color: #71717a;
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
  color: #71717a;
}

.empty-reset {
  margin-top: 28rpx;
  padding: 14rpx 36rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(15, 23, 42, 0.12);
  font-size: 24rpx;
  color: #3f3f46;
}
</style>
