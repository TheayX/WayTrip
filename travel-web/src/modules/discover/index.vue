<!-- 发现页 -->
<template>
  <div class="page-container discover-page">
    <section class="discover-hero premium-card">
      <div class="discover-hero-main">
        <p class="hero-eyebrow">Discover More</p>
        <h2 class="page-title">发现灵感</h2>
        <p class="page-subtitle">按来源和内容类型切换，再配合筛选条件缩小范围。</p>
      </div>
    </section>

    <section class="browse-panel premium-card">
      <div class="keyword-row">
        <span class="toolbar-label">热门搜索</span>
        <div class="keyword-list">
          <button
            v-for="item in hotKeywords"
            :key="item"
            type="button"
            class="keyword-chip"
            @click="handleKeywordSelect(item)"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <div class="browse-toolbar">
        <div class="toolbar-group">
          <span class="toolbar-label">来源</span>
          <el-segmented v-model="activeScene" :options="sceneOptions" @change="handleSceneChange" />
        </div>
        <div class="toolbar-group">
          <span class="toolbar-label">内容</span>
          <el-segmented v-model="activeTab" :options="contentOptions" @change="handleTabChange" />
        </div>
        <div class="toolbar-actions">
          <el-button v-if="activeScene === 'nearby' && userStore.isLoggedIn" :loading="nearbyLoading" @click="handleLocate">
            {{ nearbyLoading ? '定位中' : '重新定位' }}
          </el-button>
          <button v-if="hasBrowseChanges" type="button" class="section-link" @click="resetBrowseState">重置</button>
        </div>
      </div>

      <div v-if="activeFilterPills.length" class="active-filters">
        <span v-for="item in activeFilterPills" :key="item" class="filter-pill">{{ item }}</span>
      </div>

      <div class="filters-grid">
        <div v-if="showSpotFilters" class="filter-group">
          <span class="filter-label">地区</span>
          <el-cascader
            v-model="selectedRegionPath"
            :options="regionTree"
            :props="spotCascaderProps"
            clearable
            placeholder="全部地区"
            @change="handleSpotFilter"
          />
        </div>
        <div v-if="showSpotFilters" class="filter-group">
          <span class="filter-label">分类</span>
          <el-cascader
            v-model="selectedSpotCategoryPath"
            :options="spotCategoryTree"
            :props="spotCascaderProps"
            clearable
            placeholder="全部分类"
            @change="handleSpotFilter"
          />
        </div>
        <div v-if="showGuideFilters" class="filter-group">
          <span class="filter-label">主题</span>
          <el-cascader
            v-model="selectedGuideCategory"
            :options="guideCategoryOptions"
            :props="guideCascaderProps"
            clearable
            placeholder="全部主题"
            @change="handleGuideFilter"
          />
        </div>
      </div>

      <p v-if="activeScene === 'nearby'" class="browse-note">{{ nearbySectionSummary }}</p>
    </section>

    <section v-if="showRecommendationSection" class="content-section">
      <div class="section-head">
        <div>
          <h3>{{ userStore.isLoggedIn ? recommendType : '推荐景点' }}</h3>
        </div>
        <div class="section-actions">
          <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.recommendations)">查看全部</button>
          <button v-if="userStore.isLoggedIn" type="button" class="section-link" :disabled="refreshing" @click="handleRefresh">换一批</button>
        </div>
      </div>

      <div v-if="needPreference && userStore.isLoggedIn" class="hint-banner premium-card" @click="showPreferencePopup">
        <div>
          <strong>还没有设置偏好分类</strong>
          <p>先设置偏好，再看推荐。</p>
        </div>
        <el-icon><ArrowRight /></el-icon>
      </div>

      <div v-if="recommendations.length" class="spot-grid">
        <SpotCard
          v-for="spot in recommendations.slice(0, 6)"
          :key="spot.id"
          :spot="spot"
          @select="router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.RECOMMENDATION))"
        />
      </div>
      <el-empty v-else :description="userStore.isLoggedIn ? '当前暂无推荐景点' : '登录后查看推荐景点'">
        <el-button v-if="!userStore.isLoggedIn" type="primary" @click="router.push(AUTH_ROUTE_PATHS.login)">去登录</el-button>
      </el-empty>
    </section>

    <section v-if="showNearbySection" class="content-section">
      <div class="section-head">
        <div>
          <h3>附近景点</h3>
        </div>
        <div class="section-actions">
          <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.nearby)">附近页</button>
        </div>
      </div>

      <div v-if="nearbySpots.length" class="spot-grid">
        <SpotCard
          v-for="spot in nearbySpots"
          :key="spot.id"
          :spot="spot"
          @select="router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.NEARBY))"
        />
      </div>
      <el-empty v-else :description="nearbyEmptyText">
        <el-button v-if="userStore.isLoggedIn" type="primary" @click="handleLocate">开启定位</el-button>
        <el-button v-else type="primary" @click="router.push(AUTH_ROUTE_PATHS.login)">去登录</el-button>
      </el-empty>
    </section>

    <section v-if="showSpotSection" class="content-section">
      <div class="section-head">
        <div>
          <h3>{{ spotSectionTitle }}</h3>
        </div>
        <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.spots)">查看全部</button>
      </div>
      <div v-if="spotList.length" class="spot-grid">
        <SpotCard
          v-for="spot in spotList"
          :key="spot.id"
          :spot="spot"
          @select="router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.DISCOVER))"
        />
      </div>
      <el-empty v-else description="当前条件暂无景点" />
    </section>

    <section v-if="showGuideSection" class="content-section">
      <div class="section-head">
        <div>
          <h3>{{ guideSectionTitle }}</h3>
        </div>
        <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.guides)">查看全部</button>
      </div>
      <div v-if="guideList.length" class="guide-grid">
        <GuideCard
          v-for="guide in guideList"
          :key="guide.id"
          :guide="guide"
          @select="router.push(`/guides/${guide.id}`)"
        />
      </div>
      <el-empty v-else description="当前条件暂无攻略" />
    </section>

    <el-dialog v-model="preferenceVisible" title="选择你感兴趣的景点分类" width="520px" :close-on-click-modal="false">
      <div class="preference-tags">
        <el-check-tag
          v-for="category in categories"
          :key="category.id"
          :checked="selectedCategories.includes(category.id)"
          @change="toggleCategory(category.id)"
        >
          {{ category.name }}
        </el-check-tag>
      </div>
      <template #footer>
        <el-button @click="preferenceVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPref" @click="handleSavePreference">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import GuideCard from '@/modules/guide/components/GuideCard.vue'
import { getGuideList, getCategories } from '@/modules/guide/api.js'
import { getNearbySpots } from '@/modules/home/api.js'
import { useRecommendationFeed } from '@/modules/recommendation/composables/useRecommendationFeed.js'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import { getSpotList, getFilters } from '@/modules/spot/api.js'
import { useUserStore } from '@/modules/account/store/user.js'
import { getCurrentLocation, getLocationSnapshot } from '@/shared/lib/location.js'
import { SEARCH_HOT_KEYWORDS } from '@/shared/constants/search.js'
import { APP_ROUTE_PATHS, AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'

const DISCOVER_STATE_KEY = 'discover_state'
const DISCOVER_TABS = ['all', 'spot', 'guide']
const DISCOVER_SCENES = ['all', 'recommend', 'nearby']
const hotKeywords = SEARCH_HOT_KEYWORDS
const tabOptions = [
  { label: '全部', value: 'all' },
  { label: '景点', value: 'spot' },
  { label: '攻略', value: 'guide' }
]
const sceneOptions = [
  { label: '全部来源', value: 'all' },
  { label: '推荐', value: 'recommend' },
  { label: '附近', value: 'nearby' }
]

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const activeTab = ref('all')
const activeScene = ref('all')
const regionTree = ref([])
const spotCategoryTree = ref([])
const guideCategories = ref([])
const selectedRegionPath = ref([])
const selectedSpotCategoryPath = ref([])
const selectedGuideCategory = ref('')
const spotList = ref([])
const guideList = ref([])
const nearbySpots = ref([])
const nearbyLoading = ref(false)
const nearbyStatus = ref('idle')
const refreshing = ref(false)
const savingPref = ref(false)
const {
  recommendations,
  needPreference,
  categories,
  selectedCategories,
  preferenceVisible,
  recommendType,
  fetchRecommendationList,
  rotateRecommendationList,
  openPreferenceDialog,
  savePreferences
} = useRecommendationFeed(12)

const selectedRegionId = computed(() => selectedRegionPath.value[selectedRegionPath.value.length - 1] || '')
const selectedSpotCategoryId = computed(() => selectedSpotCategoryPath.value[selectedSpotCategoryPath.value.length - 1] || '')
const contentOptions = computed(() => {
  const isSourceLimited = activeScene.value === 'recommend' || activeScene.value === 'nearby'

  return [
    { label: '全部', value: 'all', disabled: isSourceLimited },
    { label: '景点', value: 'spot', disabled: false },
    { label: '攻略', value: 'guide', disabled: isSourceLimited }
  ]
})
const showSpotFilters = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideFilters = computed(() => activeScene.value === 'all' && (activeTab.value === 'all' || activeTab.value === 'guide'))
const showRecommendationSection = computed(() => (
  activeScene.value === 'recommend'
  || (activeScene.value === 'all' && (activeTab.value === 'all' || activeTab.value === 'spot'))
))
const showNearbySection = computed(() => (
  activeScene.value === 'nearby'
  || (activeScene.value === 'all' && (activeTab.value === 'all' || activeTab.value === 'spot'))
))
const showSpotSection = computed(() => (
  activeScene.value === 'all' && (activeTab.value === 'all' || activeTab.value === 'spot')
))
const showGuideSection = computed(() => (
  activeScene.value === 'all' && (activeTab.value === 'all' || activeTab.value === 'guide')
))
const hasBrowseChanges = computed(() => (
  activeScene.value !== 'all'
  || activeTab.value !== 'all'
  || !!selectedRegionId.value
  || !!selectedSpotCategoryId.value
  || !!selectedGuideCategory.value
))
const hasSpotFilters = computed(() => !!selectedRegionId.value || !!selectedSpotCategoryId.value)
const hasGuideFilter = computed(() => !!selectedGuideCategory.value)
const spotSectionTitle = computed(() => {
  if (hasSpotFilters.value) return '景点结果'
  return '热门景点'
})
const guideSectionTitle = computed(() => {
  if (hasGuideFilter.value) return '攻略结果'
  return '最新攻略'
})
const nearbyEmptyText = computed(() => {
  if (!userStore.isLoggedIn) return '登录后查看附近景点'
  if (nearbyStatus.value === 'empty') return '附近暂时没有景点'
  return '还没有加载附近景点'
})
const nearbySectionSummary = computed(() => {
  if (!userStore.isLoggedIn) return '登录后可以按当前位置加载附近景点。'
  if (nearbyLoading.value) return '正在获取当前位置附近的景点。'
  if (nearbyStatus.value === 'ready' && nearbySpots.value.length) {
    return `已找到 ${nearbySpots.value.length} 个附近景点，最近约 ${formatDistance(nearbySpots.value[0].distanceKm)}。`
  }
  if (nearbyStatus.value === 'empty') return '当前位置附近暂时没有景点。'
  return '切换到附近后可以重新定位。'
})
const activeFilterPills = computed(() => {
  const pills = []

  if (activeScene.value === 'recommend') pills.push('来源：推荐')
  if (activeScene.value === 'nearby') pills.push('来源：附近')
  if (activeTab.value === 'spot') pills.push('内容：景点')
  if (activeTab.value === 'guide') pills.push('内容：攻略')

  if (selectedRegionId.value) {
    const region = findTreeNodeById(regionTree.value, selectedRegionId.value)
    pills.push(`地区：${region?.name || '已选择'}`)
  }
  if (selectedSpotCategoryId.value) {
    const category = findTreeNodeById(spotCategoryTree.value, selectedSpotCategoryId.value)
    pills.push(`分类：${category?.name || '已选择'}`)
  }
  if (selectedGuideCategory.value) {
    pills.push(`主题：${selectedGuideCategory.value}`)
  }

  return pills
})
const guideCategoryOptions = computed(() => guideCategories.value.map((item) => ({
  value: item,
  label: item
})))

const spotCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: true
}
const guideCascaderProps = {
  value: 'value',
  label: 'label',
  checkStrictly: true,
  emitPath: false
}

const findTreeNodeById = (tree, targetId) => {
  if (!Array.isArray(tree) || !tree.length || targetId === '' || targetId == null) {
    return null
  }

  const normalizedTargetId = String(targetId)
  const stack = [...tree]
  while (stack.length) {
    const current = stack.pop()
    if (!current) continue
    if (String(current.id) === normalizedTargetId) return current
    if (Array.isArray(current.children) && current.children.length) {
      stack.push(...current.children)
    }
  }

  return null
}

const findPathById = (tree, targetId) => {
  if (!Array.isArray(tree) || !tree.length || targetId === '' || targetId == null) {
    return []
  }

  const normalizedTargetId = String(targetId)
  const stack = tree.map((node) => ({ node, path: [node.id] }))
  while (stack.length) {
    const current = stack.pop()
    if (!current) continue
    if (String(current.node.id) === normalizedTargetId) {
      return current.path
    }
    if (Array.isArray(current.node.children) && current.node.children.length) {
      for (const child of current.node.children) {
        stack.push({ node: child, path: [...current.path, child.id] })
      }
    }
  }

  return []
}

const formatDistance = (value) => {
  const distance = Number(value)
  if (!Number.isFinite(distance)) return '-- km'
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))} m` : `${distance.toFixed(1)} km`
}

const persistState = () => {
  localStorage.setItem(DISCOVER_STATE_KEY, JSON.stringify({
    tab: activeTab.value,
    scene: activeScene.value,
    selectedRegionId: selectedRegionId.value,
    selectedSpotCategoryId: selectedSpotCategoryId.value,
    selectedGuideCategory: selectedGuideCategory.value
  }))
}

const syncRouteQuery = () => {
  router.replace({
    path: APP_ROUTE_PATHS.discover,
    query: {
      ...(activeTab.value !== 'all' ? { tab: activeTab.value } : {}),
      ...(activeScene.value !== 'all' ? { scene: activeScene.value } : {}),
      ...(selectedRegionId.value ? { regionId: selectedRegionId.value } : {}),
      ...(selectedSpotCategoryId.value ? { categoryId: selectedSpotCategoryId.value } : {}),
      ...(selectedGuideCategory.value ? { guideCategory: selectedGuideCategory.value } : {})
    }
  })
}

const restoreState = () => {
  const raw = localStorage.getItem(DISCOVER_STATE_KEY)
  if (!raw) return

  const state = JSON.parse(raw)
  activeTab.value = DISCOVER_TABS.includes(state.tab) ? state.tab : 'all'
  activeScene.value = DISCOVER_SCENES.includes(state.scene) ? state.scene : 'all'
  selectedRegionPath.value = state.selectedRegionId ? [state.selectedRegionId] : []
  selectedSpotCategoryPath.value = state.selectedSpotCategoryId ? [state.selectedSpotCategoryId] : []
  selectedGuideCategory.value = state.selectedGuideCategory || ''
}

const applyRoutePreset = () => {
  const tab = typeof route.query.tab === 'string' ? route.query.tab : ''
  const scene = typeof route.query.scene === 'string' ? route.query.scene : ''
  if (DISCOVER_TABS.includes(tab)) activeTab.value = tab
  if (DISCOVER_SCENES.includes(scene)) activeScene.value = scene
  if (typeof route.query.regionId === 'string') selectedRegionPath.value = [route.query.regionId]
  if (typeof route.query.categoryId === 'string') selectedSpotCategoryPath.value = [route.query.categoryId]
  if (typeof route.query.guideCategory === 'string') selectedGuideCategory.value = route.query.guideCategory
}

const toggleCategory = (id) => {
  const index = selectedCategories.value.indexOf(id)
  if (index > -1) {
    selectedCategories.value.splice(index, 1)
  } else {
    selectedCategories.value.push(id)
  }
}

const showPreferencePopup = async () => {
  await openPreferenceDialog()
}

const handleKeywordSelect = (value) => {
  router.push({ path: APP_ROUTE_PATHS.search, query: { keyword: value } })
}

const fetchSpotFilters = async () => {
  const res = await getFilters()
  regionTree.value = res.data?.regionTree?.length ? res.data.regionTree : (res.data?.regions || [])
  spotCategoryTree.value = res.data?.categoryTree?.length ? res.data.categoryTree : (res.data?.categories || [])

  if (selectedRegionId.value) {
    selectedRegionPath.value = findPathById(regionTree.value, selectedRegionId.value)
  }
  if (selectedSpotCategoryId.value) {
    selectedSpotCategoryPath.value = findPathById(spotCategoryTree.value, selectedSpotCategoryId.value)
  }
}

const fetchGuideCategories = async () => {
  const res = await getCategories()
  guideCategories.value = res.data || []
}

const fetchSpotPreview = async () => {
  const params = { page: 1, pageSize: 6, sortBy: 'heat' }
  if (selectedRegionId.value) params.regionId = selectedRegionId.value
  if (selectedSpotCategoryId.value) params.categoryId = selectedSpotCategoryId.value
  const res = await getSpotList(params)
  spotList.value = res.data?.list || []
}

const fetchGuidePreview = async () => {
  const params = { page: 1, pageSize: 6, sortBy: 'time' }
  if (selectedGuideCategory.value) params.category = selectedGuideCategory.value
  const res = await getGuideList(params)
  guideList.value = res.data?.list || []
}

const fetchNearbyPreview = async (location) => {
  if (!location) return
  const res = await getNearbySpots(location.latitude, location.longitude, 6)
  nearbySpots.value = res.data?.list || []
  nearbyStatus.value = nearbySpots.value.length ? 'ready' : 'empty'
}

const tryLoadNearbyAutomatically = async () => {
  if (!userStore.isLoggedIn) {
    nearbySpots.value = []
    nearbyStatus.value = 'idle'
    return
  }

  const snapshot = await getLocationSnapshot()
  if (snapshot.current) {
    await fetchNearbyPreview(snapshot.current)
  }
}

const refreshDiscover = async () => {
  await Promise.all([
    fetchSpotPreview(),
    fetchGuidePreview(),
    fetchRecommendationList(),
    tryLoadNearbyAutomatically()
  ])
}

const handleTabChange = async () => {
  if (activeTab.value === 'guide' && activeScene.value !== 'all') {
    activeScene.value = 'all'
  }
  syncRouteQuery()
  await Promise.all([
    fetchSpotPreview(),
    fetchGuidePreview()
  ])
}

const handleSpotFilter = async () => {
  syncRouteQuery()
  await fetchSpotPreview()
}

const handleGuideFilter = async () => {
  syncRouteQuery()
  await fetchGuidePreview()
}

const handleSceneChange = async () => {
  if ((activeScene.value === 'recommend' || activeScene.value === 'nearby') && activeTab.value !== 'spot') {
    activeTab.value = 'spot'
  }
  syncRouteQuery()
  if (activeScene.value === 'recommend') {
    await fetchRecommendationList()
    return
  }
  if (activeScene.value === 'nearby' && userStore.isLoggedIn && nearbyStatus.value === 'idle') {
    await tryLoadNearbyAutomatically()
  }
}

const resetBrowseState = async () => {
  activeTab.value = 'all'
  activeScene.value = 'all'
  selectedRegionPath.value = []
  selectedSpotCategoryPath.value = []
  selectedGuideCategory.value = ''
  syncRouteQuery()
  await refreshDiscover()
}

const handleRefresh = async () => {
  refreshing.value = true
  try {
    await rotateRecommendationList()
    ElMessage.success('已换一批推荐')
  } catch {
    ElMessage.error('换一批失败，请稍后重试')
  } finally {
    refreshing.value = false
  }
}

const handleSavePreference = async () => {
  savingPref.value = true
  try {
    await savePreferences()
    ElMessage.success(selectedCategories.value.length ? '偏好设置成功' : '已清空偏好')
    await handleRefresh()
  } finally {
    savingPref.value = false
  }
}

const handleLocate = async () => {
  if (!userStore.isLoggedIn) {
    nearbyStatus.value = 'idle'
    return
  }

  nearbyLoading.value = true
  try {
    const location = await getCurrentLocation()
    await fetchNearbyPreview(location)
  } catch (_error) {
    nearbyStatus.value = 'empty'
    ElMessage.warning('请先允许浏览器定位，再查看附近景点')
  } finally {
    nearbyLoading.value = false
  }
}

watch([activeTab, activeScene, selectedRegionId, selectedSpotCategoryId, selectedGuideCategory], persistState)

onMounted(async () => {
  restoreState()
  applyRoutePreset()
  await Promise.all([fetchSpotFilters(), fetchGuideCategories()])
  await refreshDiscover()
})
</script>

<style lang="scss" scoped>
.discover-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 2px;
}

.discover-hero {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  background:
    radial-gradient(circle at top right, rgba(202, 138, 4, 0.12), transparent 28%),
    linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
}

.hero-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 700;
}

.page-title {
  font-size: 32px;
  line-height: 1.12;
  font-weight: 700;
  letter-spacing: 0;
  color: #0f172a;
}

.page-subtitle,
.browse-note,
.hint-banner p {
  color: #64748b;
  line-height: 1.8;
}

.section-link {
  border: none;
  padding: 0;
  background: transparent;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease;
}

.section-link:hover:not(:disabled) {
  color: #0f172a;
}

.browse-panel,
.hint-banner {
  padding: 18px 20px;
}

.keyword-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid #eef2f7;
}

.keyword-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.browse-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}

.toolbar-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.toolbar-label,
.filter-label {
  color: #475569;
  font-weight: 600;
  font-size: 13px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.active-filters {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.keyword-chip {
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
}

.keyword-chip:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #0f172a;
}

.filters-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.browse-note {
  margin-top: 16px;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-head,
.section-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-head h3 {
  font-size: 24px;
  color: #0f172a;
  letter-spacing: 0;
}

.section-link:disabled {
  color: #94a3b8;
  cursor: default;
}

.hint-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  cursor: pointer;
}

.hint-banner strong {
  color: #0f172a;
}

.hint-banner p {
  margin-top: 6px;
}

.spot-grid,
.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 1100px) {
  .filters-grid,
  .spot-grid,
  .guide-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .discover-hero,
  .browse-toolbar,
  .section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .page-title {
    font-size: 28px;
  }

  .toolbar-actions {
    justify-content: flex-start;
    margin-left: 0;
  }
}
</style>
