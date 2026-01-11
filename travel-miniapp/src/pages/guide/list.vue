<template>
  <view class="guide-list-page">
    <!-- 分类筛选 -->
    <scroll-view class="category-bar" scroll-x>
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
    <scroll-view 
      class="guide-list" 
      scroll-y 
      @scrolltolower="loadMore"
    >
      <view 
        class="guide-item card" 
        v-for="guide in guideList" 
        :key="guide.id"
        @click="goDetail(guide.id)"
      >
        <image class="guide-cover" :src="guide.coverImage" mode="aspectFill" />
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
        <text class="empty-icon">📖</text>
        <text class="empty-text">暂无攻略</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getGuideList, getCategories } from '@/api/guide'

// 分类数据
const categories = ref([])
const currentCategory = ref('')

// 列表数据
const guideList = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const hasMore = computed(() => guideList.value.length < total.value)

// 获取分类
const fetchCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

// 获取攻略列表
const fetchGuideList = async (isRefresh = false) => {
  if (loading.value) return
  loading.value = true

  try {
    const params = {
      page: isRefresh ? 1 : page.value,
      pageSize: pageSize.value,
      sortBy: 'time'
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

// 选择分类
const selectCategory = (cat) => {
  currentCategory.value = cat
  fetchGuideList(true)
}

// 加载更多
const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchGuideList()
  }
}

// 跳转详情
const goDetail = (id) => {
  uni.navigateTo({
    url: `/pages/guide/detail?id=${id}`
  })
}

// 初始化
onMounted(() => {
  fetchCategories()
  fetchGuideList(true)
})
</script>

<style scoped>
.guide-list-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

/* 分类栏 */
.category-bar {
  white-space: nowrap;
  background: #fff;
  padding: 20rpx;
  border-bottom: 1rpx solid #eee;
}

.category-item {
  display: inline-block;
  padding: 12rpx 32rpx;
  margin-right: 20rpx;
  background: #f5f5f5;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;
}

.category-item.active {
  background: #409EFF;
  color: #fff;
}

/* 攻略列表 */
.guide-list {
  flex: 1;
  padding: 20rpx;
}

.guide-item {
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.guide-cover {
  width: 100%;
  height: 300rpx;
}

.guide-info {
  padding: 24rpx;
}

.guide-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 12rpx;
}

.guide-summary {
  font-size: 26rpx;
  color: #666;
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
  color: #409EFF;
  background: rgba(64, 158, 255, 0.1);
  padding: 6rpx 16rpx;
  border-radius: 4rpx;
}

.guide-views {
  font-size: 24rpx;
  color: #999;
}

/* 加载状态 */
.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  color: #999;
  font-size: 26rpx;
}
</style>
