<!-- 发现页 -->
<template>
  <view class="discover-page">
    <!-- 吸顶宽幅导航栏带搜索 -->
    <view class="ios-header">
      <view class="header-top">
        <text class="large-title">发现</text>
      </view>
      <view class="search-bar" @click="goSearch">
        <uni-search-bar
          :modelValue="''"
          placeholder="搜索景点、攻略..."
          :clearButton="'none'"
          :cancelButton="'none'"
          :radius="100"
          :readonly="true"
          bgColor="rgba(255, 255, 255, 0.6)"
        />
      </view>
    </view>

    <!-- 顶部状态栏占位下面是内容 -->
    <view class="page-content">
      <!-- 快捷入口 (简约化) -->
      <view class="quick-panel">
        <view class="quick-card" @click="goSpotList">
          <view class="icon-orb bg-blue"><uni-icons type="location-filled" size="24" color="#2563eb" /></view>
          <text class="quick-title">景点大全</text>
        </view>
        <view class="quick-card" @click="goGuideList">
          <view class="icon-orb bg-orange"><uni-icons type="paperplane-filled" size="24" color="#ea580c" /></view>
          <text class="quick-title">出游攻略</text>
        </view>
        <view class="quick-card" @click="goRecommendationSpots">
          <view class="icon-orb bg-amber"><uni-icons type="star-filled" size="24" color="#d97706" /></view>
          <text class="quick-title">个性推荐</text>
        </view>
      </view>

      <!-- 模式切换：胶囊切页 -->
      <view class="mode-tabs">
        <view
          v-for="tab in contentTabs"
          :key="tab.value"
          class="mode-tab"
          :class="{ active: activeTab === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </view>
      </view>

      <!-- 扁平化筛选区域 -->
      <view class="filter-card" v-if="showSpotFilters || showGuideFilters">
        <view class="filter-group" v-if="showSpotFilters">
          <text class="filter-group-title">地区</text>
          <scroll-view class="filter-scroll" scroll-x :show-scrollbar="false">
            <view class="chip-row">
              <view class="chip" :class="{ active: selectedRegionId === '' }" @click="selectRegion('')">全部</view>
              <view
                v-for="item in regions"
                :key="`region-${item.id}`"
                class="chip"
                :class="{ active: selectedRegionId === item.id }"
                @click="selectRegion(item.id)"
              >
                {{ item.name }}
              </view>
            </view>
          </scroll-view>
        </view>

        <view class="filter-group" v-if="showSpotFilters">
          <text class="filter-group-title">分类</text>
          <scroll-view class="filter-scroll" scroll-x :show-scrollbar="false">
            <view class="chip-row">
              <view class="chip" :class="{ active: selectedSpotCategoryId === '' }" @click="selectSpotCategory('')">全部</view>
              <view
                v-for="item in spotCategories"
                :key="`spot-${item.id}`"
                class="chip"
                :class="{ active: selectedSpotCategoryId === item.id }"
                @click="selectSpotCategory(item.id)"
              >
                {{ item.name }}
              </view>
            </view>
          </scroll-view>
        </view>

        <view class="filter-group" v-if="showGuideFilters">
          <text class="filter-group-title">主题</text>
          <scroll-view class="filter-scroll" scroll-x :show-scrollbar="false">
            <view class="chip-row">
              <view class="chip" :class="{ active: selectedGuideCategory === '' }" @click="selectGuideCategory('')">全部</view>
              <view
                v-for="item in guideCategories"
                :key="`guide-${item}`"
                class="chip"
                :class="{ active: selectedGuideCategory === item }"
                @click="selectGuideCategory(item)"
              >
                {{ item }}
              </view>
            </view>
          </scroll-view>
        </view>
      </view>

      <!-- 景点列表 -->
      <view class="section" v-if="showSpotSection">
        <view class="section-header">
          <text class="section-title">探索景点</text>
          <text class="section-link" @click="goSpotList">查看全部</text>
        </view>

        <view class="card-list" v-if="spotList.length">
          <view class="spot-card" v-for="spot in spotList" :key="spot.id" @click="goSpotDetail(spot.id)">
            <image class="spot-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
            <view class="spot-content">
              <view class="spot-title-row">
                <text class="spot-title">{{ spot.name }}</text>
                <text class="spot-price">¥{{ spot.price }}</text>
              </view>
              <text v-if="spot.intro" class="spot-desc">{{ spot.intro }}</text>
              <view class="spot-meta">
                <text class="meta-tag">{{ spot.regionName || '地区待补充' }}</text>
                <text class="meta-tag">{{ spot.categoryName || '分类待补充' }}</text>
              </view>
            </view>
          </view>
        </view>
        <view class="empty-tip" v-else>
          <uni-icons type="info" size="32" color="#cbd5e1" />
          <text class="empty-text">当前条件暂无景点，换个筛选试试吧</text>
        </view>
      </view>

      <!-- 攻略列表 -->
      <view class="section" v-if="showGuideSection">
        <view class="section-header">
          <text class="section-title">精华攻略</text>
          <text class="section-link" @click="goGuideList">查看全部</text>
        </view>

        <view class="card-list" v-if="guideList.length">
          <view class="guide-card" v-for="guide in guideList" :key="guide.id" @click="goGuideDetail(guide.id)">
            <image class="guide-image" :src="getImageUrl(guide.coverImage)" mode="aspectFill" />
            <view class="guide-content">
              <text class="guide-title">{{ guide.title }}</text>
              <text class="guide-desc">{{ guide.summary || '带上好心情，发现更多旅行灵感。' }}</text>
              <view class="guide-meta">
                <text class="meta-tag">{{ guide.category || '攻略' }}</text>
                <view class="meta-view"><uni-icons type="eye" size="14" color="#9ca3af"/> {{ guide.viewCount || 0 }}</view>
              </view>
            </view>
          </view>
        </view>
        <view class="empty-tip" v-else>
          <uni-icons type="info" size="32" color="#cbd5e1" />
          <text class="empty-text">当前条件暂无攻略，换个筛选试试吧</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getGuideList, getCategories } from '@/api/guide'
import { getSpotList, getFilters } from '@/api/spot'
import { promptLogin } from '@/utils/auth'
import { getImageUrl } from '@/utils/request'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'

// 常量配置
const DISCOVER_STATE_KEY = 'discover_state'

const contentTabs = [
  { label: '综合', value: 'all' },
  { label: '景点', value: 'spot' },
  { label: '攻略', value: 'guide' }
]

// 页面数据状态
const activeTab = ref('all')
const regions = ref([])
const spotCategories = ref([])
const guideCategories = ref([])
const selectedRegionId = ref('')
const selectedSpotCategoryId = ref('')
const selectedGuideCategory = ref('')

const spotList = ref([])
const guideList = ref([])

// 计算属性
const showSpotFilters = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideFilters = computed(() => activeTab.value === 'all' || activeTab.value === 'guide')
const showSpotSection = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideSection = computed(() => activeTab.value === 'all' || activeTab.value === 'guide')

// 数据加载方法
const fetchSpotFilters = async () => {
  try {
    const res = await getFilters()
    regions.value = res.data?.regions || []
    spotCategories.value = res.data?.categories || []
  } catch (error) {
    console.error('获取景点筛选项失败', error)
  }
}

const fetchGuideCategories = async () => {
  try {
    const res = await getCategories()
    guideCategories.value = res.data || []
  } catch (error) {
    console.error('获取攻略分类失败', error)
  }
}

const fetchSpotPreview = async () => {
  try {
    const params = { page: 1, pageSize: 6, sortBy: 'heat' }
    if (selectedRegionId.value) params.regionId = selectedRegionId.value
    if (selectedSpotCategoryId.value) params.categoryId = selectedSpotCategoryId.value
    const res = await getSpotList(params)
    spotList.value = res.data?.list || []
  } catch (error) {
    console.error('获取景点数据失败', error)
  }
}

const fetchGuidePreview = async () => {
  try {
    const params = { page: 1, pageSize: 6, sortBy: 'time' }
    if (selectedGuideCategory.value) params.category = selectedGuideCategory.value
    const res = await getGuideList(params)
    guideList.value = res.data?.list || []
  } catch (error) {
    console.error('获取攻略数据失败', error)
  }
}

const refreshDiscover = async () => {
  await Promise.all([fetchSpotPreview(), fetchGuidePreview()])
}

// 工具方法
const persistDiscoverState = () => {
  uni.setStorageSync(DISCOVER_STATE_KEY, {
    tab: activeTab.value,
    selectedRegionId: selectedRegionId.value,
    selectedSpotCategoryId: selectedSpotCategoryId.value,
    selectedGuideCategory: selectedGuideCategory.value
  })
}

const applySavedState = () => {
  const savedState = uni.getStorageSync(DISCOVER_STATE_KEY)
  if (!savedState || typeof savedState !== 'object' || Array.isArray(savedState)) return false
  if (savedState.tab && ['all', 'spot', 'guide'].includes(savedState.tab)) activeTab.value = savedState.tab
  selectedRegionId.value = savedState.selectedRegionId || ''
  selectedSpotCategoryId.value = savedState.selectedSpotCategoryId || ''
  selectedGuideCategory.value = savedState.selectedGuideCategory || ''
  return true
}

const applyPreset = () => {
  const preset = uni.getStorageSync('discover_preset')
  if (!preset || typeof preset !== 'object' || Array.isArray(preset)) return false
  if (preset.tab && ['all', 'spot', 'guide'].includes(preset.tab)) activeTab.value = preset.tab
  if (preset.spotFilterMode === 'region') selectedSpotCategoryId.value = ''
  if (preset.spotFilterMode === 'category') selectedRegionId.value = ''
  uni.removeStorageSync('discover_preset')
  persistDiscoverState()
  return true
}

// 交互处理方法
const switchTab = (value) => {
  activeTab.value = value
  persistDiscoverState()
  refreshDiscover()
}

const selectRegion = (value) => {
  selectedRegionId.value = value
  persistDiscoverState()
  fetchSpotPreview()
}

const selectSpotCategory = (value) => {
  selectedSpotCategoryId.value = value
  persistDiscoverState()
  fetchSpotPreview()
}

const selectGuideCategory = (value) => {
  selectedGuideCategory.value = value
  persistDiscoverState()
  fetchGuidePreview()
}

// 页面跳转方法
const goSearch = () => {
  uni.navigateTo({ url: '/pages/spot/search' })
}

const goSpotList = () => {
  const query = ['sortBy=heat']
  if (selectedRegionId.value) query.push(`regionId=${selectedRegionId.value}`)
  if (selectedSpotCategoryId.value) query.push(`categoryId=${selectedSpotCategoryId.value}`)
  uni.navigateTo({ url: `/pages/spot/list?${query.join('&')}` })
}

const goGuideList = () => {
  const query = ['sortBy=time']
  if (selectedGuideCategory.value) query.push(`category=${encodeURIComponent(selectedGuideCategory.value)}`)
  uni.navigateTo({ url: `/pages/guide/list?${query.join('&')}` })
}

const goRecommendationSpots = () => {
  if (!promptLogin('登录后可查看个性推荐，是否现在去登录？')) return
  uni.navigateTo({ url: '/pages/recommendation/index' })
}

const goSpotDetail = (id) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) return
  uni.navigateTo({ url: buildSpotDetailUrl(id, SPOT_DETAIL_SOURCE.DISCOVER) })
}

const goGuideDetail = (id) => {
  if (!promptLogin('登录后可查看攻略详情，是否现在去登录？')) return
  uni.navigateTo({ url: `/pages/guide/detail?id=${id}` })
}

// 生命周期
onShow(async () => {
  const updatedSpot = uni.getStorageSync('spot_detail_updated')
  if (updatedSpot?.id) {
    const index = spotList.value.findIndex(item => item.id === updatedSpot.id)
    if (index !== -1) {
      spotList.value.splice(index, 1, { ...spotList.value[index], ...updatedSpot })
    }
    uni.removeStorageSync('spot_detail_updated')
  }

  const updatedGuide = uni.getStorageSync('guide_detail_updated')
  if (updatedGuide?.id) {
    const index = guideList.value.findIndex(item => item.id === updatedGuide.id)
    if (index !== -1) {
      guideList.value.splice(index, 1, { ...guideList.value[index], ...updatedGuide })
    }
    uni.removeStorageSync('guide_detail_updated')
  }

  if (!regions.value.length || !spotCategories.value.length) await fetchSpotFilters()
  if (!guideCategories.value.length) await fetchGuideCategories()

  const usedPreset = applyPreset()
  if (!usedPreset) applySavedState()

  await refreshDiscover()
})
</script>

<style scoped>
.discover-page {
  min-height: 100vh;
  background: #f4f6fb;
  padding-bottom: 48rpx;
}

/* Glassmorphism Header */
.ios-header {
  padding: 88rpx 32rpx 24rpx;
  background: linear-gradient(180deg, #f4f6fb 0%, rgba(244, 246, 251, 0.9) 100%);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
}
.header-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
.large-title { font-size: 48rpx; font-weight: 800; color: #111827; letter-spacing: 1rpx; }
.search-bar :deep(.uni-searchbar) { padding: 0; background: transparent; }
.search-bar :deep(.uni-searchbar__box) { border: 2rpx solid #e5e7eb; height: 80rpx; border-radius: 40rpx; backdrop-filter: blur(10px); box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.02); }

.page-content {
  padding: 0 32rpx;
}

/* Quick Nav Redesign */
.quick-panel {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
  margin-bottom: 32rpx;
}

.quick-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.icon-orb {
  width: 96rpx;
  height: 96rpx;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 16rpx rgba(0,0,0,0.03);
}
.bg-blue { background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); }
.bg-orange { background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%); }
.bg-amber { background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); }

.quick-title {
  font-size: 26rpx;
  font-weight: 500;
  color: #374151;
}

/* Tab Redesign */
.mode-tabs {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
  background: #ffffff;
  padding: 10rpx;
  border-radius: 36rpx;
  box-shadow: 0 4rpx 16rpx rgba(17, 24, 39, 0.03);
}

.mode-tab {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 28rpx;
  text-align: center;
  color: #6b7280;
  font-size: 28rpx;
  font-weight: 600;
  transition: all 0.3s ease;
}

.mode-tab.active {
  background: #2563eb;
  color: #ffffff;
  box-shadow: 0 6rpx 12rpx rgba(37, 99, 235, 0.2);
}

/* Filters */
.filter-card {
  margin-top: 24rpx;
  background: #ffffff;
  border-radius: 36rpx;
  padding: 20rpx 0;
  box-shadow: 0 4rpx 16rpx rgba(17, 24, 39, 0.03);
  overflow: hidden;
}

.filter-group {
  display: flex;
  align-items: center;
  flex-direction: row;
  min-height: 72rpx;
  white-space: nowrap;
}

.filter-group + .filter-group {
  margin-top: 12rpx;
}

.filter-group-title {
  display: block;
  width: 90rpx;
  padding-left: 28rpx;
  flex-shrink: 0;
  font-size: 24rpx;
  font-weight: 600;
  color: #9ca3af;
  line-height: 56rpx;
}

.filter-scroll {
  flex: 1;
  width: 0;
  min-width: 0;
  display: block;
  white-space: nowrap;
}

.chip-row {
  display: inline-flex;
  align-items: center;
  padding: 0 24rpx 0 0;
}

.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 56rpx;
  padding: 0 24rpx;
  margin-right: 16rpx;
  border-radius: 99rpx;
  background: #f1f5f9;
  color: #64748b;
  font-size: 24rpx;
  font-weight: 500;
  transition: all 0.2s ease;
}

.chip.active {
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  font-weight: 600;
}

/* Section Shared */
.section { margin-top: 40rpx; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
.section-title { font-size: 38rpx; font-weight: 700; color: #111827; }
.section-link { font-size: 26rpx; color: #6b7280; font-weight: 500;}

/* Lists */
.card-list { display: flex; flex-direction: column; gap: 24rpx; }
.spot-card, .guide-card {
  background: #ffffff;
  border-radius: 36rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.04);
}
.spot-image, .guide-image { width: 100%; height: 280rpx; }
.spot-content, .guide-content { padding: 24rpx 28rpx; }

.spot-title-row { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; margin-bottom: 12rpx; }
.spot-title, .guide-title { font-size: 34rpx; font-weight: 700; color: #111827; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.spot-price { font-size: 34rpx; font-weight: 800; color: #ef4444; }

.spot-desc, .guide-desc {
  display: -webkit-box; margin-bottom: 20rpx; font-size: 26rpx; line-height: 1.5;
  color: #6b7280; overflow: hidden; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}

.guide-title { margin-bottom: 12rpx;}

.spot-meta, .guide-meta { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12rpx;}
.spot-meta { justify-content: flex-start; }
.meta-tag { padding: 8rpx 20rpx; border-radius: 12rpx; background: #f3f4f6; color: #4b5563; font-size: 22rpx; font-weight: 500; }
.meta-view { display: flex; align-items: center; gap: 6rpx; font-size: 24rpx; color: #9ca3af; font-weight: 500;}

.empty-tip { display: flex; flex-direction: column; align-items: center; padding: 64rpx 0; gap: 16rpx; }
.empty-text { font-size: 26rpx; color: #94a3b8; }
</style>
