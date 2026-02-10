<template>
  <view class="ios-page">
    <!-- iOS 风格头部 -->
    <view class="ios-header">
      <text class="large-title">景点</text>
      <view class="search-bar" @click="goSearch">
        <image class="search-icon" src="/static/搜索.png" />
        <text class="search-placeholder">搜索景点</text>
      </view>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-bar">
      <view 
        class="filter-item" 
        :class="{ active: currentFilter === 'region' }"
        @click="toggleFilter('region')"
      >
        <text>{{ currentRegion?.name || '地区' }}</text>
        <text class="filter-arrow">▼</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: currentFilter === 'category' }"
        @click="toggleFilter('category')"
      >
        <text>{{ currentCategory?.rawName || currentCategory?.name || '分类' }}</text>
        <text class="filter-arrow">▼</text>
      </view>
      <view 
        class="filter-item" 
        :class="{ active: currentFilter === 'sort' }"
        @click="toggleFilter('sort')"
      >
        <text>{{ sortOptions.find(s => s.value === sortBy)?.label || '排序' }}</text>
        <text class="filter-arrow">▼</text>
      </view>
    </view>

    <!-- 筛选下拉面板 -->
    <view class="filter-panel" v-if="showFilterPanel">
      <view class="filter-options">
        <view 
          class="filter-option" 
          v-for="option in filterOptions" 
          :key="option.id || option.value"
          :class="{ selected: isOptionSelected(option) }"
          @click="selectOption(option)"
        >
          {{ option.name || option.label }}
        </view>
      </view>
    </view>

    <!-- 景点列表 -->
    <scroll-view 
      class="spot-list" 
      scroll-y 
      @scrolltolower="loadMore"
    >
      <view 
        class="spot-card" 
        v-for="spot in spotList" 
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
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      <view class="no-more" v-if="!hasMore && spotList.length > 0">
        <text>没有更多了</text>
      </view>

      <!-- 空状态 -->
      <view class="empty" v-if="!loading && spotList.length === 0">
        <text class="empty-text">暂无景点</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSpotList, getFilters } from '@/api/spot'
import { getImageUrl } from '@/utils/request'

// 筛选数据
const regions = ref([])
const categories = ref([])
const categoryTree = ref([])
const sortOptions = [
  { label: '热度排序', value: 'heat' },
  { label: '评分排序', value: 'rating' },
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' }
]

// 当前筛选状态
const currentFilter = ref('')
const showFilterPanel = ref(false)
const currentRegion = ref(null)
const currentCategory = ref(null)
const sortBy = ref('heat')

// 列表数据
const spotList = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const hasMore = computed(() => spotList.value.length < total.value)

// 筛选选项
const filterOptions = computed(() => {
  if (currentFilter.value === 'region') {
    return [{ id: null, name: '全部地区' }, ...regions.value]
  } else if (currentFilter.value === 'category') {
    return [{ id: null, name: '全部分类' }, ...flattenCategoryTree(categoryTree.value)]
  } else if (currentFilter.value === 'sort') {
    return sortOptions
  }
  return []
})

const flattenCategoryTree = (nodes = [], level = 0) => {
  return nodes.reduce((acc, node) => {
    const hasChildren = Array.isArray(node.children) && node.children.length > 0
    acc.push({
      id: node.id,
      name: `${'　'.repeat(level)}${level > 0 ? '└ ' : ''}${node.name}`,
      rawName: node.name,
      hasChildren
    })

    if (hasChildren) {
      acc.push(...flattenCategoryTree(node.children, level + 1))
    }

    return acc
  }, [])
}

// 切换筛选面板
const toggleFilter = (type) => {
  if (currentFilter.value === type) {
    showFilterPanel.value = !showFilterPanel.value
  } else {
    currentFilter.value = type
    showFilterPanel.value = true
  }
}

// 判断选项是否选中
const isOptionSelected = (option) => {
  if (currentFilter.value === 'region') {
    return currentRegion.value?.id === option.id
  } else if (currentFilter.value === 'category') {
    return currentCategory.value?.id === option.id
  } else if (currentFilter.value === 'sort') {
    return sortBy.value === option.value
  }
  return false
}

// 选择筛选选项
const selectOption = (option) => {
  if (currentFilter.value === 'region') {
    currentRegion.value = option.id ? option : null
  } else if (currentFilter.value === 'category') {
    if (option.hasChildren) {
      return
    }
    currentCategory.value = option.id ? option : null
  } else if (currentFilter.value === 'sort') {
    sortBy.value = option.value
  }
  showFilterPanel.value = false
  refreshList()
}

// 获取筛选选项
const fetchFilters = async () => {
  try {
    const res = await getFilters()
    regions.value = res.data.regions || []
    categories.value = res.data.categories || []
    categoryTree.value = res.data.categoryTree?.length ? res.data.categoryTree : categories.value
  } catch (e) {
    console.error('获取筛选选项失败', e)
  }
}

// 获取景点列表
const fetchSpotList = async (isRefresh = false) => {
  if (loading.value) return
  loading.value = true

  try {
    const params = {
      page: isRefresh ? 1 : page.value,
      pageSize: pageSize.value,
      sortBy: sortBy.value
    }
    if (currentRegion.value?.id) {
      params.regionId = currentRegion.value.id
    }
    if (currentCategory.value?.id) {
      params.categoryId = currentCategory.value.id
    }

    const res = await getSpotList(params)
    const list = res.data.list || []
    
    if (isRefresh) {
      spotList.value = list
      page.value = 1
    } else {
      spotList.value = [...spotList.value, ...list]
    }
    total.value = res.data.total || 0
    page.value++
  } catch (e) {
    console.error('获取景点列表失败', e)
  } finally {
    loading.value = false
  }
}

// 刷新列表
const refreshList = () => {
  fetchSpotList(true)
}

// 加载更多
const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchSpotList()
  }
}

// 跳转搜索
const goSearch = () => {
  uni.navigateTo({ url: '/pages/spot/search' })
}

// 跳转详情
const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/spot/detail?id=${id}` })
}

// 初始化
onMounted(() => {
  fetchFilters()
  fetchSpotList(true)
})
</script>

<style scoped>
.ios-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #F2F2F7;
}

/* iOS 头部 */
.ios-header {
  padding: 88rpx 32rpx 20rpx;
  background: #fff;
}

.large-title {
  font-size: 60rpx;
  font-weight: 800;
  color: #000;
  display: block;
  margin-bottom: 24rpx;
}

.search-bar {
  background: #E3E3E8;
  height: 72rpx;
  border-radius: 16rpx;
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

/* 筛选栏 */
.filter-bar {
  display: flex;
  background: #fff;
  padding: 20rpx 32rpx;
  border-top: 1px solid #F2F2F7;
}

.filter-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: #8E8E93;
}

.filter-item.active {
  color: #007AFF;
}

.filter-arrow {
  font-size: 18rpx;
  margin-left: 8rpx;
}

/* 筛选面板 */
.filter-panel {
  background: #fff;
  padding: 24rpx 32rpx;
  border-top: 1px solid #F2F2F7;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.filter-option {
  padding: 16rpx 28rpx;
  background: #F2F2F7;
  border-radius: 100rpx;
  font-size: 26rpx;
  color: #666;
}

.filter-option.selected {
  background: #007AFF;
  color: #fff;
}

/* 景点列表 */
.spot-list {
  flex: 1;
  padding: 24rpx 32rpx;
}

.spot-card {
  display: flex;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.spot-image {
  width: 240rpx;
  height: 180rpx;
  flex-shrink: 0;
}

.spot-info {
  flex: 1;
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
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
}

.spot-rating {
  font-size: 24rpx;
  color: #FF9500;
  font-weight: 600;
}

.spot-price {
  font-size: 32rpx;
  color: #FF3B30;
  font-weight: 600;
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
</style>
