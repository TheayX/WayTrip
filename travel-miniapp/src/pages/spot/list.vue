<template>
  <view class="page-container">
    <view class="sticky-header">
      <view class="search-section" @click="goSearch">
        <view class="search-box">
          <uni-icons type="search" size="18" color="#8E8E93"></uni-icons>
          <text class="placeholder">搜索景点名称 / 城市</text>
        </view>
      </view>

      <view class="filter-bar">
        <view class="filter-item" :class="{ active: activeTab === 'region' }" @click="toggleTab('region')">
          <text class="text">{{ currentRegion?.name || '全地区' }}</text>
          <uni-icons :type="activeTab === 'region' ? 'top' : 'bottom'" size="12" :color="activeTab === 'region' ? '#007AFF' : '#333'"></uni-icons>
        </view>

        <view class="filter-item" :class="{ active: activeTab === 'category' }" @click="toggleTab('category')">
          <text class="text">{{ currentCategoryDisplay }}</text>
          <uni-icons :type="activeTab === 'category' ? 'top' : 'bottom'" size="12" :color="activeTab === 'category' ? '#007AFF' : '#333'"></uni-icons>
        </view>

        <view class="filter-item" :class="{ active: activeTab === 'sort' }" @click="toggleTab('sort')">
          <text class="text">{{ currentSortLabel }}</text>
          <uni-icons :type="activeTab === 'sort' ? 'top' : 'bottom'" size="12" :color="activeTab === 'sort' ? '#007AFF' : '#333'"></uni-icons>
        </view>
      </view>

      <view class="dropdown-mask" v-if="activeTab" @click="closeTab" @touchmove.stop.prevent></view>
      
      <view class="dropdown-content region-panel" v-if="activeTab === 'region'">
        <view class="panel-body">
          <view class="grid-container">
            <view 
              class="grid-item" 
              :class="{ active: !currentRegion }"
              @click="handleSelectRegion(null)"
            >全部</view>
            <view 
              class="grid-item" 
              v-for="item in regions" 
              :key="item.id"
              :class="{ active: currentRegion?.id === item.id }"
              @click="handleSelectRegion(item)"
            >
              {{ item.name }}
            </view>
          </view>
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
              <uni-icons v-if="!tempCategoryId && tempParentId === activeParentId" type="checkmarkempty" color="#007AFF" size="16"></uni-icons>
            </view>
            <view 
              class="sub-item" 
              v-for="sub in currentSubCategories" 
              :key="sub.id"
              :class="{ active: tempCategoryId === sub.id }"
              @click="handleCategoryConfirm(sub)"
            >
              <text>{{ sub.name }}</text>
              <uni-icons v-if="tempCategoryId === sub.id" type="checkmarkempty" color="#007AFF" size="16"></uni-icons>
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
          <uni-icons v-if="sortBy === opt.value" type="checkmarkempty" color="#007AFF" size="16"></uni-icons>
        </view>
      </view>
    </view>

    <scroll-view 
      class="scroll-container" 
      scroll-y 
      @scrolltolower="loadMore"
      :enable-back-to-top="true"
    >
      <view class="header-placeholder"></view>

      <view class="list-padding">
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
              <view class="tag location">{{ spot.regionName }}</view>
              <view class="tag category">{{ spot.categoryName }}</view>
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
          <image class="empty-img" src="/static/empty.png" mode="widthFix" />
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
import { getSpotList, getFilters } from '@/api/spot'
import { getImageUrl } from '@/utils/request'
import UniIcons from '@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue'

// --- 基础数据 ---
const regions = ref([])
const categories = ref([])
const categoryTree = ref([])
const sortOptions = [
  { label: '综合热度', value: 'heat' },
  { label: '评分最高', value: 'rating' },
  { label: '价格最低', value: 'price_asc' },
  { label: '价格最高', value: 'price_desc' }
]

// --- 筛选状态 ---
const activeTab = ref(null) // 当前展开的面板：'region' | 'category' | 'sort' | null
const currentRegion = ref(null)
const sortBy = ref('heat')

// --- 分类双栏逻辑专用状态 ---
const activeParentId = ref(null) // 实际选中的父类ID
const activeCategoryId = ref(null) // 实际选中的子类ID
const activeCategoryName = ref('') // 用于展示的名字

const tempParentId = ref(null) // 临时选中的父类ID（在面板未关闭前）
const tempCategoryId = ref(null) // 临时选中的子类ID

// --- 列表数据 ---
const spotList = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const hasMore = computed(() => spotList.value.length < total.value)

// --- 计算属性 ---
const currentSortLabel = computed(() => {
  return sortOptions.find(s => s.value === sortBy.value)?.label || '排序'
})

const currentCategoryDisplay = computed(() => {
  return activeCategoryName.value || '全部分类'
})

// 计算当前右侧应该显示的子分类列表
const currentSubCategories = computed(() => {
  if (!tempParentId.value) return []
  const parent = categoryTree.value.find(item => item.id === tempParentId.value)
  return parent ? parent.children : []
})

// --- 交互逻辑 ---
const toggleTab = (tab) => {
  if (activeTab.value === tab) {
    closeTab()
  } else {
    activeTab.value = tab
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

const handleSelectRegion = (region) => {
  currentRegion.value = region
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
  activeParentId.value = null
  activeCategoryId.value = null
  activeCategoryName.value = ''
  sortBy.value = 'heat'
  refreshList()
}

// --- API 请求 ---
const fetchFilters = async () => {
  try {
    const res = await getFilters()
    regions.value = res.data.regions || []
    categories.value = res.data.categories || []
    categoryTree.value = res.data.categoryTree?.length ? res.data.categoryTree : []
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

const goSearch = () => uni.navigateTo({ url: '/pages/spot/search' })
const goDetail = (id) => uni.navigateTo({ url: `/pages/spot/detail?id=${id}` })

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
  background-color: #F5F7FA;
}

/* 吸顶容器 */
.sticky-header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 99;
  background-color: #fff;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}

/* 搜索栏 */
.search-section {
  padding: 20rpx 32rpx;
  background: #fff;
  
  .search-box {
    height: 72rpx;
    background: #F5F7FA;
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    padding: 0 24rpx;
    
    .placeholder {
      font-size: 28rpx;
      color: #909399;
      margin-left: 12rpx;
    }
  }
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  height: 88rpx;
  border-top: 1rpx solid #EBEEF5;
  background: #fff;
  
  .filter-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    color: #333;
    
    .text {
      max-width: 140rpx;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
      margin-right: 8rpx;
      font-weight: 500;
    }
    
    &.active {
      color: #007AFF;
      .text {
        color: #007AFF;
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
  background: rgba(0,0,0,0.4);
  z-index: 90;
}

.dropdown-content {
  position: absolute;
  top: 100%; /* 相对于 sticky-header */
  left: 0;
  width: 100%;
  background: #fff;
  z-index: 99;
  border-radius: 0 0 24rpx 24rpx;
  overflow: hidden;
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
      background: #F5F7FA;
      border-radius: 8rpx;
      font-size: 26rpx;
      color: #606266;
      
      &.active {
        background: #E1F0FF;
        color: #007AFF;
        font-weight: 500;
      }
    }
  }
}

/* 分类 - 双栏 */
.double-column {
  display: flex;
  height: 600rpx;
  
  .col-left {
    width: 200rpx;
    background: #F5F7FA;
    
    .menu-item {
      height: 90rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28rpx;
      color: #606266;
      position: relative;
      
      &.active {
        background: #fff;
        color: #007AFF;
        font-weight: 600;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 30rpx;
          bottom: 30rpx;
          width: 6rpx;
          background: #007AFF;
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
      color: #333;
      border-bottom: 1rpx solid #F5F7FA;
      
      &.active {
        color: #007AFF;
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
    color: #333;
    border-bottom: 1rpx solid #F5F7FA;
    
    .text-blue {
      color: #007AFF;
      font-weight: 500;
    }
  }
}

/* 内容区 */
.scroll-container {
  flex: 1;
  height: 0; // 必须设置高度为0以触发 flex 滚动
}

.header-placeholder {
  // 搜索栏(72+20+20) + 筛选栏(88) + 额外间距
  height: calc(112rpx + 88rpx + 20rpx);
}

.list-padding {
  padding: 0 24rpx 40rpx;
}

/* 景点卡片优化 */
.spot-card {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03);
  
  .card-image-box {
    position: relative;
    width: 100%;
    height: 320rpx; // 加大图片展示
    
    .card-img {
      width: 100%;
      height: 100%;
    }
    
    .rating-badge {
      position: absolute;
      left: 20rpx;
      bottom: 20rpx;
      background: rgba(255, 255, 255, 0.95);
      padding: 6rpx 16rpx;
      border-radius: 30rpx;
      display: flex;
      align-items: baseline;
      box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.1);
      
      .score {
        font-size: 28rpx;
        font-weight: 700;
        color: #FF9500;
        margin-right: 4rpx;
      }
      .unit {
        font-size: 20rpx;
        color: #606266;
      }
    }
  }
  
  .card-info {
    padding: 24rpx;
    
    .card-header {
      margin-bottom: 12rpx;
      .title {
        font-size: 32rpx;
        font-weight: 600;
        color: #303133;
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
        padding: 4rpx 12rpx;
        border-radius: 6rpx;
        
        &.location {
          background: #F2F6FC;
          color: #606266;
        }
        &.category {
          background: #ECF5FF;
          color: #409EFF;
        }
      }
    }
    
    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .price-box {
        color: #FF3B30;
        display: flex;
        align-items: baseline;
        
        .symbol { font-size: 24rpx; margin-right: 2rpx; }
        .num { font-size: 40rpx; font-weight: 700; }
        .label { font-size: 22rpx; color: #909399; margin-left: 4rpx; font-weight: normal;}
      }
      
      .heat-box {
        .heat-text {
          font-size: 22rpx;
          color: #909399;
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
    color: #909399;
    font-size: 28rpx;
    margin-bottom: 40rpx;
  }
  
  .reset-btn {
    padding: 16rpx 48rpx;
    border: 1rpx solid #DCDFE6;
    border-radius: 36rpx;
    color: #606266;
    font-size: 26rpx;
  }
}

.loading-state, .no-more {
  text-align: center;
  padding: 30rpx 0;
  color: #C0C4CC;
  font-size: 24rpx;
}
</style>
