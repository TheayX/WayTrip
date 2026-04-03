<!-- 景点列表页 -->
<template>
  <view class="page-container">
    <!-- 顶部筛选区域 -->
    <view class="sticky-header">
      <view class="search-section" @click="goSearch">
        <view class="search-box">
          <uni-icons type="search" size="18" color="#6B7280"></uni-icons>
          <text class="placeholder">搜索景点、城市或旅行灵感</text>
        </view>
      </view>

      <view class="filter-bar">
        <view class="filter-item" :class="{ active: activeTab === 'region' }" @click="toggleTab('region')">
          <text class="text">{{ currentRegionDisplay }}</text>
          <uni-icons :type="activeTab === 'region' ? 'top' : 'bottom'" size="12" :color="activeTab === 'region' ? '#111827' : '#6B7280'"></uni-icons>
        </view>
        <view class="filter-item" :class="{ active: activeTab === 'category' }" @click="toggleTab('category')">
          <text class="text">{{ currentCategoryDisplay }}</text>
          <uni-icons :type="activeTab === 'category' ? 'top' : 'bottom'" size="12" :color="activeTab === 'category' ? '#111827' : '#6B7280'"></uni-icons>
        </view>

        <view class="filter-item" :class="{ active: activeTab === 'sort' }" @click="toggleTab('sort')">
          <text class="text">{{ currentSortLabel }}</text>
          <uni-icons :type="activeTab === 'sort' ? 'top' : 'bottom'" size="12" :color="activeTab === 'sort' ? '#111827' : '#6B7280'"></uni-icons>
        </view>
      </view>

      <view class="dropdown-mask" v-if="activeTab" @click="closeTab" @touchmove.stop.prevent></view>
      
      <view class="dropdown-content region-panel" v-if="activeTab === 'region'">
        <view class="double-column">
          <scroll-view scroll-y class="col-left">
            <view
              class="menu-item"
              :class="{ active: tempProvinceId === null }"
              @click="handleProvinceClick(null)"
            >全部地区</view>
            <view
              class="menu-item"
              v-for="item in regionTree"
              :key="item.id"
              :class="{ active: tempProvinceId === item.id }"
              @click="handleProvinceClick(item.id)"
            >
              {{ item.name }}
            </view>
          </scroll-view>

          <scroll-view scroll-y class="col-right">
            <view
              class="sub-item"
              :class="{ active: !tempCityId && tempProvinceId === activeProvinceId }"
              @click="handleRegionConfirm(null)"
            >
              <text>全部</text>
              <uni-icons v-if="!tempCityId && tempProvinceId === activeProvinceId" type="checkmarkempty" color="#111827" size="16"></uni-icons>
            </view>
            <view
              class="sub-item"
              v-for="city in currentSubRegions"
              :key="city.id"
              :class="{ active: tempCityId === city.id }"
              @click="handleRegionConfirm(city)"
            >
              <text>{{ city.name }}</text>
              <uni-icons v-if="tempCityId === city.id" type="checkmarkempty" color="#111827" size="16"></uni-icons>
            </view>
          </scroll-view>
        </view>
      </view>
      <view class="dropdown-content category-panel" v-if="activeTab === 'category'">
        <view class="double-column">
          <scroll-view scroll-y class="col-left">
            <view 
              class="menu-item" 
              :class="{ active: tempParentId === null }"
              @click="handleParentClick(null)"
            >全部分类</view>
            <view 
              class="menu-item" 
              v-for="item in categoryTree" 
              :key="item.id"
              :class="{ active: tempParentId === item.id }"
              @click="handleParentClick(item.id)"
            >
              {{ item.name }}
            </view>
          </scroll-view>

          <scroll-view scroll-y class="col-right">
            <view 
              class="sub-item" 
              :class="{ active: !tempCategoryId && tempParentId === activeParentId }"
              @click="handleCategoryConfirm(null)"
            >
              <text>全部</text>
              <uni-icons v-if="!tempCategoryId && tempParentId === activeParentId" type="checkmarkempty" color="#111827" size="16"></uni-icons>
            </view>
            <view 
              class="sub-item" 
              v-for="sub in currentSubCategories" 
              :key="sub.id"
              :class="{ active: tempCategoryId === sub.id }"
              @click="handleCategoryConfirm(sub)"
            >
              <text>{{ sub.name }}</text>
              <uni-icons v-if="tempCategoryId === sub.id" type="checkmarkempty" color="#111827" size="16"></uni-icons>
            </view>
          </scroll-view>
        </view>
      </view>

      <view class="dropdown-content sort-panel" v-if="activeTab === 'sort'">
        <view 
          class="list-cell" 
          v-for="opt in sortOptions" 
          :key="opt.value"
          @click="handleSelectSort(opt)"
        >
          <text :class="{ 'text-blue': sortBy === opt.value }">{{ opt.label }}</text>
          <uni-icons v-if="sortBy === opt.value" type="checkmarkempty" color="#111827" size="16"></uni-icons>
        </view>
      </view>
    </view>

    <!-- 列表区域 -->
    <scroll-view 
      class="scroll-container" 
      scroll-y 
      @scrolltolower="loadMore"
      :enable-back-to-top="true"
    >
      <view class="header-placeholder"></view>

      <view class="list-padding">
        <view class="hero-panel">
          <view class="hero-copy">
            <text class="hero-eyebrow">目的地探索</text>
            <text class="hero-title">找到下一处值得出发的风景</text>
            <text class="hero-subtitle">按地区、分类与偏好筛选，快速浏览适合当前行程的景点。</text>
          </view>
        </view>

        <view class="current-state-card" v-if="activeFilterTags.length || total">
          <view class="state-header">
            <view class="state-copy">
              <text class="state-title">筛选结果</text>
              <text class="state-subtitle">已为你整理当前条件下更值得先看的景点</text>
            </view>
            <text class="state-count">{{ total }} 个结果</text>
          </view>
          <view class="state-tags" v-if="activeFilterTags.length">
            <text v-for="tag in activeFilterTags" :key="tag" class="state-tag">{{ tag }}</text>
          </view>
        </view>

        <view 
          class="spot-card" 
          v-for="spot in spotList" 
          :key="spot.id"
          @click="goDetail(spot.id)"
        >
          <view class="card-image-box">
            <image class="card-img" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
            <view class="rating-badge" v-if="spot.avgRating > 0">
              <text class="score">{{ spot.avgRating }}</text>
              <text class="unit">分</text>
            </view>
          </view>
          
          <view class="card-info">
            <view class="card-header">
              <text class="title">{{ spot.name }}</text>
            </view>
            
            <view class="tags-row">
              <view class="tag location" v-if="spot.regionName">{{ spot.regionName }}</view>
              <view class="tag category" v-if="spot.categoryName">{{ spot.categoryName }}</view>
            </view>

            <view class="card-footer">
              <view class="price-box">
                <text class="symbol">¥</text>
                <text class="num">{{ spot.price }}</text>
                <text class="label">起</text>
              </view>
              <view class="heat-box">
                <text class="heat-text">热度 {{ spot.heatScore || 0 }}</text>
              </view>
            </view>
          </view>
        </view>

        <view class="loading-state" v-if="loading">
          <text class="loading-text">正在加载精彩内容...</text>
        </view>
        <view class="empty-state" v-else-if="spotList.length === 0">
<image class="empty-img" src="/static/empty-image.png" mode="widthFix" />
          <text>暂无相关景点</text>
          <view class="reset-btn" @click="resetAll">清除筛选</view>
        </view>
        <view class="no-more" v-else-if="!hasMore">
          <text>—— 到底啦 ——</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getSpotList, getFilters } from '@/api/spot'
import { promptLogin } from '@/utils/auth'
import { getImageUrl } from '@/utils/request'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'
import UniIcons from '@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue'

// 基础数据状态
const regionTree = ref([])
const categories = ref([])
const categoryTree = ref([])
const sortOptions = [
  { label: '综合热度', value: 'heat' },
  { label: '评分最高', value: 'rating' },
  { label: '价格最低', value: 'price_asc' },
  { label: '价格最高', value: 'price_desc' }
]

// 筛选状态
const activeTab = ref(null) // 当前展开的面板：'region' | 'category' | 'sort' | null
const currentRegion = ref(null)
const sortBy = ref('heat')
const activeProvinceId = ref(null)
const activeCityId = ref(null)
const tempProvinceId = ref(null)
const tempCityId = ref(null)

// 分类筛选状态
const activeParentId = ref(null) // 实际选中的父类ID
const activeCategoryId = ref(null) // 实际选中的子类ID
const activeCategoryName = ref('') // 用于展示的名字

const tempParentId = ref(null) // 临时选中的父类ID（在面板未关闭前）
const tempCategoryId = ref(null) // 临时选中的子类ID

// 列表数据状态
const spotList = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const hasMore = computed(() => spotList.value.length < total.value)

// 计算属性
const currentSortLabel = computed(() => {
  return sortOptions.find(s => s.value === sortBy.value)?.label || '排序'
})
const currentRegionDisplay = computed(() => currentRegion.value?.name || '全地区')

const currentCategoryDisplay = computed(() => {
  return activeCategoryName.value || '全部分类'
})
const activeFilterTags = computed(() => {
  const tags = []
  if (currentRegion.value?.name) {
    tags.push(currentRegion.value.name)
  }
  if (activeCategoryName.value) {
    tags.push(activeCategoryName.value)
  }
  if (sortBy.value !== 'heat') {
    tags.push(currentSortLabel.value)
  }
  return tags
})

// 计算当前右侧应该显示的子分类列表
const currentSubCategories = computed(() => {
  if (!tempParentId.value) return []
  const parent = categoryTree.value.find(item => item.id === tempParentId.value)
  return parent ? parent.children : []
})
const currentSubRegions = computed(() => {
  if (!tempProvinceId.value) return []
  const parent = regionTree.value.find(item => item.id === tempProvinceId.value)
  return parent?.children || []
})

// 工具方法
const parseNumberOption = (value) => {
  if (value === undefined || value === null || value === '') {
    return null
  }

  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

const syncRegionDisplay = () => {
  if (!activeProvinceId.value && !activeCityId.value) {
    currentRegion.value = null
    return
  }

  if (activeCityId.value) {
    for (const province of regionTree.value) {
      const city = province.children?.find(item => item.id === activeCityId.value)
      if (city) {
        currentRegion.value = { id: city.id, name: city.name }
        return
      }
    }
  }

  const province = regionTree.value.find(item => item.id === activeProvinceId.value)
  currentRegion.value = province ? { id: province.id, name: province.name } : null
}

const syncCategoryDisplay = () => {
  if (activeCategoryId.value) {
    for (const parent of categoryTree.value) {
      const category = parent.children?.find(item => item.id === activeCategoryId.value)
      if (category) {
        activeParentId.value = parent.id
        activeCategoryName.value = category.name
        return
      }
    }
  }

  if (activeParentId.value) {
    const parent = categoryTree.value.find(item => item.id === activeParentId.value)
    activeCategoryName.value = parent?.name || ''
    return
  }

  activeCategoryName.value = ''
}

// 交互处理方法
const toggleTab = (tab) => {
  if (activeTab.value === tab) {
    closeTab()
  } else {
    activeTab.value = tab
    if (tab === 'region') {
      tempProvinceId.value = activeProvinceId.value
      tempCityId.value = activeCityId.value
    }
    // 打开分类面板时，初始化临时状态
    if (tab === 'category') {
      tempParentId.value = activeParentId.value
      tempCategoryId.value = activeCategoryId.value
    }
  }
}

const closeTab = () => {
  activeTab.value = null
}

const handleProvinceClick = (provinceId) => {
  tempProvinceId.value = provinceId
  tempCityId.value = null
  if (provinceId === null) {
    handleRegionConfirm(null)
  }
}

const handleRegionConfirm = (city) => {
  activeProvinceId.value = tempProvinceId.value
  if (tempProvinceId.value === null) {
    activeCityId.value = null
    currentRegion.value = null
  } else if (city === null) {
    activeCityId.value = null
    const province = regionTree.value.find(item => item.id === tempProvinceId.value)
    currentRegion.value = province ? { id: province.id, name: province.name } : null
  } else {
    activeCityId.value = city.id
    currentRegion.value = { id: city.id, name: city.name }
  }
  closeTab()
  refreshList()
}

const handleSelectSort = (option) => {
  sortBy.value = option.value
  closeTab()
  refreshList()
}

// 分类左侧点击
const handleParentClick = (parentId) => {
  tempParentId.value = parentId
  // 如果点击的是“全部分类”，直接确认
  if (parentId === null) {
    handleCategoryConfirm(null)
  }
}

// 分类右侧（或左侧全部）确认
const handleCategoryConfirm = (subCategory) => {
  // 更新实际状态
  activeParentId.value = tempParentId.value
  
  if (tempParentId.value === null) {
    // 选了“全部分类”
    activeCategoryId.value = null
    activeCategoryName.value = ''
  } else if (subCategory === null) {
    // 选了某个父类下的“全部”
    activeCategoryId.value = null // 传参时通常只传父类ID即可，或者看后端逻辑
    // 找到父类名字
    const parent = categoryTree.value.find(p => p.id === activeParentId.value)
    activeCategoryName.value = parent?.name || ''
  } else {
    // 选了具体子类
    activeCategoryId.value = subCategory.id
    activeCategoryName.value = subCategory.name
  }

  closeTab()
  refreshList()
}

const resetAll = () => {
  currentRegion.value = null
  activeProvinceId.value = null
  activeCityId.value = null
  tempProvinceId.value = null
  tempCityId.value = null
  activeParentId.value = null
  activeCategoryId.value = null
  activeCategoryName.value = ''
  sortBy.value = 'heat'
  refreshList()
}

// 数据加载方法
const fetchFilters = async () => {
  try {
    const res = await getFilters()
    const fallbackRegions = (res.data.regions || []).map(item => ({ ...item, children: [] }))
    regionTree.value = res.data.regionTree?.length ? res.data.regionTree : fallbackRegions
    categories.value = res.data.categories || []
    categoryTree.value = res.data.categoryTree?.length ? res.data.categoryTree : []
    syncRegionDisplay()
    syncCategoryDisplay()
  } catch (e) {
    console.error(e)
  }
}

const fetchSpotList = async (isRefresh = false) => {
  if (loading.value) return
  loading.value = true

  try {
    const params = {
      page: isRefresh ? 1 : page.value,
      pageSize: pageSize.value,
      sortBy: sortBy.value
    }
    if (currentRegion.value?.id) params.regionId = currentRegion.value.id
    
    // 优先传子分类，如果没有子分类但有父分类，则传父分类（根据你后端逻辑调整）
    // 假设后端支持 categoryId 查子类，或者有单独字段。
    // 这里假设传 categoryId 即可，如果只选了父类，可能需要后端支持父类ID查询
    // 如果你的后端只接收 categoryId 且必须是叶子节点，这里需要注意。
    // 通常做法：如果有子类ID传子类ID，否则传父类ID。
    if (activeCategoryId.value) {
      params.categoryId = activeCategoryId.value
    } else if (activeParentId.value) {
      params.categoryId = activeParentId.value 
      // 注意：如果你的后端区分 parentCategoryId 和 categoryId，请在此处修改参数名
      // 例如：params.parentCategoryId = activeParentId.value
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
    console.error(e)
  } finally {
    loading.value = false
  }
}

const refreshList = () => {
  fetchSpotList(true)
}

const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchSpotList()
  }
}

// 页面跳转方法
const goSearch = () => uni.navigateTo({ url: '/pages/spot/search' })
const goDetail = (id) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: buildSpotDetailUrl(id, SPOT_DETAIL_SOURCE.LIST) })
}

// 生命周期
onShow(() => {
  const updated = uni.getStorageSync('spot_detail_updated')
  if (!updated || !updated.id) return

  const index = spotList.value.findIndex(item => item.id === updated.id)
  if (index !== -1) {
    spotList.value[index] = {
      ...spotList.value[index],
      ...updated
    }
  }

  uni.removeStorageSync('spot_detail_updated')
})

onLoad((options) => {
  const provinceId = parseNumberOption(options?.provinceId)
  const regionId = parseNumberOption(options?.regionId)
  const parentCategoryId = parseNumberOption(options?.parentCategoryId)
  const categoryId = parseNumberOption(options?.categoryId)

  if (provinceId) {
    activeProvinceId.value = provinceId
    tempProvinceId.value = provinceId
  }

  if (regionId) {
    activeCityId.value = regionId
    tempCityId.value = regionId
  }

  if (parentCategoryId) {
    activeParentId.value = parentCategoryId
    tempParentId.value = parentCategoryId
  }

  if (categoryId) {
    activeCategoryId.value = categoryId
    tempCategoryId.value = categoryId
  }

  if (!parentCategoryId && categoryId) {
    activeParentId.value = categoryId
    tempParentId.value = categoryId
  }

  if (options?.sortBy && sortOptions.some(item => item.value === options.sortBy)) {
    sortBy.value = options.sortBy
  }
})

onMounted(() => {
  fetchFilters()
  fetchSpotList(true)
})

</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.92), rgba(244, 246, 251, 0.88) 48%, rgba(238, 242, 247, 1) 100%),
    linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
}

/* 吸顶容器 */
.sticky-header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 99;
  padding: 20rpx 20rpx 16rpx;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96) 0%, rgba(248, 250, 252, 0.88) 100%);
  backdrop-filter: blur(24rpx);
  box-sizing: border-box;
}

.hero-panel {
  padding: 0 0 24rpx;
}

.hero-copy {
  padding: 28rpx 28rpx 22rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  border-radius: 28rpx;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(255, 255, 255, 0.54) 100%);
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.hero-eyebrow {
  display: block;
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: #6b7280;
}

.hero-title {
  display: block;
  margin-top: 14rpx;
  font-size: 42rpx;
  line-height: 1.28;
  font-weight: 600;
  color: #111827;
}

.hero-subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #4b5563;
}

/* 搜索栏 */
.search-section {
  padding: 0 12rpx 12rpx;
  
  .search-box {
    height: 84rpx;
    background: rgba(255, 255, 255, 0.72);
    border: 1rpx solid rgba(255, 255, 255, 0.76);
    border-radius: 42rpx;
    display: flex;
    align-items: center;
    padding: 0 26rpx;
    box-shadow:
      0 14rpx 34rpx rgba(15, 23, 42, 0.06),
      inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
    
    .placeholder {
      font-size: 28rpx;
      color: #6b7280;
      margin-left: 14rpx;
    }
  }
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  height: 84rpx;
  padding: 0 12rpx;
  gap: 12rpx;
  box-sizing: border-box;
  
  .filter-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 999rpx;
    background: rgba(255, 255, 255, 0.74);
    border: 1rpx solid rgba(255, 255, 255, 0.76);
    font-size: 28rpx;
    color: #4b5563;
    box-shadow:
      0 12rpx 30rpx rgba(15, 23, 42, 0.05),
      inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
    
    .text {
      max-width: 140rpx;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
      margin-right: 8rpx;
      font-weight: 500;
    }
    
    &.active {
      color: #111827;
      background: rgba(255, 255, 255, 0.94);
      border-color: rgba(255, 255, 255, 0.96);
      .text {
        color: #111827;
      }
    }
  }
}

/* 下拉菜单 */
.dropdown-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(15, 23, 42, 0.18);
  z-index: 90;
}

.dropdown-content {
  position: absolute;
  top: calc(100% + 12rpx);
  left: 20rpx;
  width: calc(100% - 40rpx);
  background: rgba(255, 255, 255, 0.96);
  z-index: 99;
  border: 1rpx solid rgba(255, 255, 255, 0.88);
  border-radius: 30rpx;
  overflow: hidden;
  backdrop-filter: blur(24rpx);
  box-shadow: 0 24rpx 60rpx rgba(15, 23, 42, 0.12);
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from { transform: translateY(-10%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* 地区 - 网格 */
.region-panel {
  padding: 30rpx;
  .grid-container {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;
    
    .grid-item {
      width: calc((100% - 60rpx) / 4);
      height: 64rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f4f6fb;
      border-radius: 8rpx;
      font-size: 26rpx;
      color: #606266;
      
      &.active {
        background: #E1F0FF;
        color: #2563eb;
        font-weight: 500;
      }
    }
  }
}

/* 分类 - 双栏 */
.double-column {
  display: flex;
  height: 560rpx;
  
  .col-left {
    width: 200rpx;
    background: rgba(248, 250, 252, 0.94);
    
    .menu-item {
      height: 90rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28rpx;
      color: #6b7280;
      position: relative;
      
      &.active {
        background: rgba(255, 255, 255, 0.96);
        color: #111827;
        font-weight: 600;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 30rpx;
          bottom: 30rpx;
          width: 6rpx;
          background: #111827;
          border-radius: 0 4rpx 4rpx 0;
        }
      }
    }
  }
  
  .col-right {
    flex: 1;
    background: #fff;
    
    .sub-item {
      height: 90rpx;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 32rpx;
      font-size: 28rpx;
      color: #374151;
      border-bottom: 1rpx solid rgba(229, 231, 235, 0.64);
      
      &.active {
        color: #111827;
        font-weight: 600;
      }
    }
  }
}

/* 排序 - 列表 */
.sort-panel {
  .list-cell {
    height: 100rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 40rpx;
    font-size: 28rpx;
    color: #374151;
    border-bottom: 1rpx solid rgba(229, 231, 235, 0.64);
    
    .text-blue {
      color: #111827;
      font-weight: 600;
    }
  }
}

/* 内容区 */
.scroll-container {
  flex: 1;
  height: 0; // 必须设置高度为0以触发 flex 滚动
}

.header-placeholder {
  // 搜索框 + 筛选栏 + 下方间距
  height: 148rpx;
}

.list-padding {
  padding: 0 24rpx 44rpx;
}

.current-state-card {
  margin-bottom: 24rpx;
  padding: 24rpx 24rpx 22rpx;
  background: rgba(255, 255, 255, 0.72);
  border: 1rpx solid rgba(255, 255, 255, 0.82);
  border-radius: 28rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);

  .state-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16rpx;
  }

  .state-copy {
    flex: 1;
  }

  .state-title {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: #111827;
  }

  .state-subtitle {
    display: block;
    margin-top: 8rpx;
    font-size: 22rpx;
    line-height: 1.5;
    color: #6b7280;
  }

  .state-count {
    font-size: 24rpx;
    color: #4b5563;
  }

  .state-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 16rpx;
  }

  .state-tag {
    padding: 8rpx 18rpx;
    border-radius: 999rpx;
    background: rgba(17, 24, 39, 0.06);
    color: #374151;
    font-size: 22rpx;
  }
}

/* 景点卡片优化 */
.spot-card {
  background: rgba(255, 255, 255, 0.74);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 32rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
  
  .card-image-box {
    position: relative;
    width: 100%;
    height: 332rpx;
    
    .card-img {
      width: 100%;
      height: 100%;
    }
    
    .rating-badge {
      position: absolute;
      left: 20rpx;
      bottom: 20rpx;
      background: rgba(255, 255, 255, 0.94);
      padding: 8rpx 18rpx;
      border-radius: 30rpx;
      display: flex;
      align-items: baseline;
      box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.12);
      
      .score {
        font-size: 28rpx;
        font-weight: 700;
        color: #b45309;
        margin-right: 4rpx;
      }
      .unit {
        font-size: 20rpx;
        color: #6b7280;
      }
    }
  }
  
  .card-info {
    padding: 24rpx 24rpx 26rpx;
    
    .card-header {
      margin-bottom: 12rpx;
      .title {
        font-size: 32rpx;
        font-weight: 600;
        color: #111827;
        line-height: 1.4;
      }
    }
    
    .tags-row {
      display: flex;
      flex-wrap: wrap;
      gap: 12rpx;
      margin-bottom: 20rpx;
      
      .tag {
        font-size: 22rpx;
        padding: 8rpx 16rpx;
        border-radius: 999rpx;
        
        &.location {
          background: rgba(15, 23, 42, 0.06);
          color: #4b5563;
        }
        &.category {
          background: rgba(255, 255, 255, 0.72);
          color: #111827;
          border: 1rpx solid rgba(15, 23, 42, 0.08);
        }
      }
    }
    
    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .price-box {
        color: #9f1239;
        display: flex;
        align-items: baseline;
        
        .symbol { font-size: 24rpx; margin-right: 2rpx; }
        .num { font-size: 40rpx; font-weight: 700; }
        .label { font-size: 22rpx; color: #6b7280; margin-left: 4rpx; font-weight: normal;}
      }
      
      .heat-box {
        .heat-text {
          font-size: 22rpx;
          color: #6b7280;
        }
      }
    }
  }
}

/* 状态页 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100rpx;
  
  .empty-img {
    width: 300rpx;
    margin-bottom: 30rpx;
  }
  
  text {
    color: #6b7280;
    font-size: 28rpx;
    margin-bottom: 40rpx;
  }
  
  .reset-btn {
    padding: 16rpx 48rpx;
    border: 1rpx solid rgba(15, 23, 42, 0.12);
    border-radius: 36rpx;
    color: #374151;
    background: rgba(255, 255, 255, 0.72);
    font-size: 26rpx;
  }
}

.loading-state, .no-more {
  text-align: center;
  padding: 30rpx 0;
  color: #9ca3af;
  font-size: 24rpx;
}
</style>
