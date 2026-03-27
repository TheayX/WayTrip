<template>
  <view class="discover-page">
    <view class="page-intro">
      <text class="intro-title">发现</text>
      <text class="intro-desc">把景点和攻略放在同一页里浏览，减少来回切换。</text>
    </view>

    <view class="search-card" @click="goSearch">
      <uni-icons type="search" size="18" color="#6b7280" />
      <text class="search-text">搜索景点、攻略</text>
    </view>

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

    <view class="quick-panel">
      <view class="quick-card" @click="goSpotList">
        <text class="quick-title">景点列表</text>
        <text class="quick-desc">完整筛选和排序</text>
      </view>
      <view class="quick-card" @click="goGuideList">
        <text class="quick-title">攻略列表</text>
        <text class="quick-desc">查看全部攻略内容</text>
      </view>
      <view class="quick-card" @click="goRecommendationList">
        <text class="quick-title">推荐列表</text>
        <text class="quick-desc">查看更多推荐结果</text>
      </view>
    </view>

    <view class="filter-card" v-if="showSpotFilters">
      <view class="filter-header">
        <text class="filter-title">景点筛选</text>
        <view class="filter-mode-tabs">
          <text
            class="filter-mode"
            :class="{ active: spotFilterMode === 'region' }"
            @click="changeSpotFilterMode('region')"
          >
            地区
          </text>
          <text
            class="filter-mode"
            :class="{ active: spotFilterMode === 'category' }"
            @click="changeSpotFilterMode('category')"
          >
            分类
          </text>
        </view>
      </view>

      <scroll-view class="filter-scroll" scroll-x :show-scrollbar="false">
        <view class="chip-row">
          <view
            class="chip"
            :class="{ active: currentSpotFilterValue === '' }"
            @click="selectSpotFilter('')"
          >
            全部
          </view>
          <view
            v-for="item in currentSpotFilters"
            :key="item.id"
            class="chip"
            :class="{ active: currentSpotFilterValue === item.id }"
            @click="selectSpotFilter(item.id)"
          >
            {{ item.name }}
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="filter-card" v-if="showGuideFilters">
      <view class="filter-header">
        <text class="filter-title">攻略分类</text>
      </view>

      <scroll-view class="filter-scroll" scroll-x :show-scrollbar="false">
        <view class="chip-row">
          <view
            class="chip"
            :class="{ active: selectedGuideCategory === '' }"
            @click="selectGuideCategory('')"
          >
            全部
          </view>
          <view
            v-for="item in guideCategories"
            :key="item"
            class="chip"
            :class="{ active: selectedGuideCategory === item }"
            @click="selectGuideCategory(item)"
          >
            {{ item }}
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="section" v-if="showSpotSection">
      <view class="section-header">
        <view>
          <text class="section-title">景点</text>
          <text class="section-subtitle">{{ spotSectionSubtitle }}</text>
        </view>
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
            <text class="spot-desc">{{ spot.intro || '点击查看景点详情' }}</text>
            <view class="spot-meta">
              <text class="meta-tag">{{ spot.regionName || '地区待补充' }}</text>
              <text class="meta-tag">{{ spot.categoryName || '分类待补充' }}</text>
            </view>
          </view>
        </view>
      </view>
      <view class="empty-block" v-else>
        <text>当前条件下暂无景点</text>
      </view>
    </view>

    <view class="section" v-if="showGuideSection">
      <view class="section-header">
        <view>
          <text class="section-title">攻略</text>
          <text class="section-subtitle">{{ guideSectionSubtitle }}</text>
        </view>
        <text class="section-link" @click="goGuideList">查看全部</text>
      </view>

      <view class="card-list" v-if="guideList.length">
        <view class="guide-card" v-for="guide in guideList" :key="guide.id" @click="goGuideDetail(guide.id)">
          <image class="guide-image" :src="getImageUrl(guide.coverImage)" mode="aspectFill" />
          <view class="guide-content">
            <text class="guide-title">{{ guide.title }}</text>
            <text class="guide-desc">{{ guide.summary || '点击查看攻略详情' }}</text>
            <view class="guide-meta">
              <text class="meta-tag">{{ guide.category || '攻略' }}</text>
              <text class="meta-view">浏览 {{ guide.viewCount || 0 }}</text>
            </view>
          </view>
        </view>
      </view>
      <view class="empty-block" v-else>
        <text>当前条件下暂无攻略</text>
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
import UniIcons from '@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue'

const contentTabs = [
  { label: '综合', value: 'all' },
  { label: '景点', value: 'spot' },
  { label: '攻略', value: 'guide' }
]

const activeTab = ref('all')

const spotFilterMode = ref('region')
const regions = ref([])
const spotCategories = ref([])
const selectedRegionId = ref('')
const selectedSpotCategoryId = ref('')

const guideCategories = ref([])
const selectedGuideCategory = ref('')

const spotList = ref([])
const guideList = ref([])

const showSpotFilters = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideFilters = computed(() => activeTab.value === 'all' || activeTab.value === 'guide')
const showSpotSection = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideSection = computed(() => activeTab.value === 'all' || activeTab.value === 'guide')

const currentSpotFilters = computed(() => {
  return spotFilterMode.value === 'region' ? regions.value : spotCategories.value
})

const currentSpotFilterValue = computed(() => {
  return spotFilterMode.value === 'region' ? selectedRegionId.value : selectedSpotCategoryId.value
})

const spotSectionSubtitle = computed(() => {
  if (spotFilterMode.value === 'region' && selectedRegionId.value) {
    return '按地区筛选的景点结果'
  }
  if (spotFilterMode.value === 'category' && selectedSpotCategoryId.value) {
    return '按分类筛选的景点结果'
  }
  return '当前推荐浏览的景点内容'
})

const guideSectionSubtitle = computed(() => {
  if (selectedGuideCategory.value) {
    return `当前分类：${selectedGuideCategory.value}`
  }
  return '当前推荐浏览的攻略内容'
})

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
    const params = {
      page: 1,
      pageSize: 6,
      sortBy: 'heat'
    }

    if (selectedRegionId.value) {
      params.regionId = selectedRegionId.value
    }

    if (selectedSpotCategoryId.value) {
      params.categoryId = selectedSpotCategoryId.value
    }

    const res = await getSpotList(params)
    spotList.value = res.data?.list || []
  } catch (error) {
    console.error('获取景点数据失败', error)
  }
}

const fetchGuidePreview = async () => {
  try {
    const params = {
      page: 1,
      pageSize: 6,
      sortBy: 'time'
    }

    if (selectedGuideCategory.value) {
      params.category = selectedGuideCategory.value
    }

    const res = await getGuideList(params)
    guideList.value = res.data?.list || []
  } catch (error) {
    console.error('获取攻略数据失败', error)
  }
}

const refreshDiscover = async () => {
  await Promise.all([
    fetchSpotPreview(),
    fetchGuidePreview()
  ])
}

const applyPreset = () => {
  const preset = uni.getStorageSync('discover_preset')
  if (!preset || typeof preset !== 'object' || Array.isArray(preset)) {
    return
  }

  if (preset.tab && ['all', 'spot', 'guide'].includes(preset.tab)) {
    activeTab.value = preset.tab
  }

  if (preset.spotFilterMode && ['region', 'category'].includes(preset.spotFilterMode)) {
    spotFilterMode.value = preset.spotFilterMode
  }

  uni.removeStorageSync('discover_preset')
}

const switchTab = (value) => {
  activeTab.value = value
  refreshDiscover()
}

const changeSpotFilterMode = (value) => {
  spotFilterMode.value = value
}

const selectSpotFilter = (value) => {
  if (spotFilterMode.value === 'region') {
    selectedRegionId.value = value
  } else {
    selectedSpotCategoryId.value = value
  }
  fetchSpotPreview()
}

const selectGuideCategory = (value) => {
  selectedGuideCategory.value = value
  fetchGuidePreview()
}

const goSearch = () => {
  uni.navigateTo({ url: '/pages/spot/search' })
}

const goSpotList = () => {
  const query = [`sortBy=heat`]

  if (selectedRegionId.value) {
    query.push(`regionId=${selectedRegionId.value}`)
  }

  if (selectedSpotCategoryId.value) {
    query.push(`categoryId=${selectedSpotCategoryId.value}`)
  }

  uni.navigateTo({ url: `/pages/spot/list?${query.join('&')}` })
}

const goGuideList = () => {
  const query = ['sortBy=time']

  if (selectedGuideCategory.value) {
    query.push(`category=${encodeURIComponent(selectedGuideCategory.value)}`)
  }

  uni.navigateTo({ url: `/pages/guide/list?${query.join('&')}` })
}

const goRecommendationList = () => {
  uni.navigateTo({ url: '/pages/recommendation/index' })
}

const goSpotDetail = (id) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/spot/detail?id=${id}&source=discover` })
}

const goGuideDetail = (id) => {
  if (!promptLogin('登录后可查看攻略详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/guide/detail?id=${id}` })
}

onShow(async () => {
  if (!regions.value.length || !spotCategories.value.length) {
    await fetchSpotFilters()
  }
  if (!guideCategories.value.length) {
    await fetchGuideCategories()
  }
  applyPreset()
  await refreshDiscover()
})
</script>

<style scoped>
.discover-page {
  min-height: 100vh;
  padding: 24rpx 24rpx 40rpx;
  background: #f4f6fb;
}

.page-intro {
  margin-bottom: 20rpx;
}

.intro-title {
  display: block;
  font-size: 42rpx;
  font-weight: 700;
  color: #111827;
}

.intro-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.search-card,
.quick-card,
.filter-card,
.spot-card,
.guide-card,
.empty-block {
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.05);
}

.search-card {
  padding: 22rpx 24rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.search-text {
  font-size: 28rpx;
  color: #6b7280;
}

.mode-tabs {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.mode-tab {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 18rpx;
  text-align: center;
  background: #e8edf5;
  color: #4b5563;
  font-size: 28rpx;
  font-weight: 600;
}

.mode-tab.active {
  background: #2563eb;
  color: #ffffff;
}

.quick-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 24rpx;
}

.quick-card {
  padding: 24rpx 20rpx;
}

.quick-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #111827;
}

.quick-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: #6b7280;
}

.filter-card,
.section {
  margin-top: 24rpx;
}

.filter-card {
  padding: 24rpx 0 18rpx;
}

.filter-header,
.section-header {
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.filter-title,
.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.filter-mode-tabs {
  display: flex;
  gap: 12rpx;
}

.filter-mode {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: #eef2f7;
  color: #4b5563;
  font-size: 22rpx;
}

.filter-mode.active {
  background: #dbeafe;
  color: #2563eb;
}

.filter-scroll {
  margin-top: 18rpx;
  white-space: nowrap;
}

.chip-row {
  padding: 0 24rpx;
}

.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  padding: 0 24rpx;
  margin-right: 16rpx;
  border-radius: 999rpx;
  background: #eef2f7;
  color: #4b5563;
  font-size: 24rpx;
}

.chip.active {
  background: #2563eb;
  color: #ffffff;
}

.section-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #6b7280;
}

.section-link {
  font-size: 24rpx;
  color: #2563eb;
}

.card-list {
  margin-top: 16rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.spot-card,
.guide-card {
  overflow: hidden;
}

.spot-image,
.guide-image {
  width: 100%;
  height: 260rpx;
}

.spot-content,
.guide-content {
  padding: 22rpx 24rpx;
}

.spot-title-row,
.guide-meta,
.spot-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.spot-title,
.guide-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.spot-price {
  font-size: 28rpx;
  font-weight: 700;
  color: #ef4444;
}

.spot-desc,
.guide-desc {
  display: -webkit-box;
  margin-top: 12rpx;
  margin-bottom: 16rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #6b7280;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.meta-tag,
.meta-view {
  font-size: 22rpx;
  color: #4b5563;
}

.meta-tag {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #eef2f7;
}

.spot-meta {
  justify-content: flex-start;
  flex-wrap: wrap;
}

.empty-block {
  margin-top: 16rpx;
  padding: 48rpx 24rpx;
  text-align: center;
  font-size: 26rpx;
  color: #6b7280;
}
</style>
