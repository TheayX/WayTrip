<template>
  <view class="spot-list-page">
    <!-- 搜索栏 -->
    <view class="search-bar" @click="goSearch">
      <text class="search-icon">🔍</text>
      <text class="search-placeholder">搜索景点</text>
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
        <text>{{ currentCategory?.name || '分类' }}</text>
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
      :style="{ height: listHeight }"
    >
      <view 
        class="spot-item card" 
        v-for="spot in spotList" 
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
        <text class="empty-icon">📭</text>
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

// 列表高度计算
const listHeight = ref('calc(100vh - 200rpx)')

// 筛选选项
const filterOptions = computed(() => {
  if (currentFilter.value === 'region') {
    return [{ id: null, name: '全部地区' }, ...regions.value]
  } else if (currentFilter.value === 'category') {
    return [{ id: null, name: '全部分类' }, ...categories.value]
  } else if (currentFilter.value === 'sort') {
    return sortOptions
  }
  return []
})

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
  uni.navigateTo({
    url: '/pages/spot/search'
  })
}

// 跳转详情
const goDetail = (id) => {
  uni.navigateTo({
    url: `/pages/spot/detail?id=${id}`
  })
}

// 初始化
onMounted(() => {
  fetchFilters()
  fetchSpotList(true)
})
</script>

<style scoped>
.spot-list-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  margin: 20rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  border-radius: 40rpx;
}

.search-icon {
  margin-right: 16rpx;
}

.search-placeholder {
  color: #999;
  font-size: 28rpx;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  background: #fff;
  padding: 20rpx;
  border-bottom: 1rpx solid #eee;
}

.filter-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: #666;
}

.filter-item.active {
  color: #409EFF;
}

.filter-arrow {
  font-size: 20rpx;
  margin-left: 8rpx;
}

/* 筛选面板 */
.filter-panel {
  background: #fff;
  padding: 20rpx;
  border-bottom: 1rpx solid #eee;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.filter-option {
  padding: 16rpx 32rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
  font-size: 26rpx;
  color: #666;
}

.filter-option.selected {
  background: #409EFF;
  color: #fff;
}

/* 景点列表 */
.spot-list {
  flex: 1;
  padding: 20rpx;
}

.spot-item {
  display: flex;
  padding: 0;
  overflow: hidden;
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
}

.spot-rating {
  font-size: 24rpx;
  color: #ff9500;
}

.spot-price {
  font-size: 32rpx;
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
