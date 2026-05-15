<!-- 发现页 -->
<template>
  <div class="page-container discover-page">
    <section class="discover-hero premium-card">
      <div>
        <p class="hero-eyebrow">Travel Discovery</p>
        <h2 class="page-title">今天想去哪里？</h2>
        <p class="page-subtitle">从推荐、热度和真实攻略里快速缩小选择范围。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="hero-search" @click="router.push(APP_ROUTE_PATHS.search)">
          <el-icon><Search /></el-icon>
          <span>搜索景点、城市或攻略</span>
        </button>
        <el-segmented v-model="viewMode" :options="viewModeOptions" @change="handleModeChange" />
      </div>
    </section>

    <section v-if="showSpotFilters || showGuideFilter" class="filter-panel premium-card">
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
      <div v-if="showGuideFilter" class="filter-group">
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
      <button v-if="hasFilters" type="button" class="section-link filter-reset" @click="resetFilters">重置筛选</button>
    </section>

    <section v-if="viewMode === 'all'" class="discover-board">
      <section class="content-block focus-block">
        <div class="section-head">
          <div>
            <h3>{{ userStore.isLoggedIn ? '为你挑选' : '先看这些景点' }}</h3>
            <p>先给出少量高价值选择，避免一上来就陷入长列表。</p>
          </div>
          <div class="section-actions">
            <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.recommendations)">查看全部</button>
            <button v-if="userStore.isLoggedIn" type="button" class="section-link" :disabled="refreshing" @click="handleRefresh">换一批</button>
          </div>
        </div>

        <div v-if="needPreference && userStore.isLoggedIn" class="hint-banner premium-card" @click="showPreferencePopup">
          <div>
            <strong>补充偏好后推荐会更准</strong>
            <p>选择感兴趣的景点分类，后续会优先展示相关内容。</p>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </div>

        <div v-if="recommendations.length" class="spot-grid spotlight-grid">
          <SpotCard
            v-for="spot in recommendations.slice(0, 3)"
            :key="spot.id"
            :spot="spot"
            @select="goSpot(spot.id, SPOT_DETAIL_SOURCE.RECOMMENDATION)"
          />
        </div>
        <el-empty v-else :description="userStore.isLoggedIn ? '当前暂无推荐景点' : '登录后查看推荐景点'">
          <el-button v-if="!userStore.isLoggedIn" type="primary" @click="router.push(AUTH_ROUTE_PATHS.login)">去登录</el-button>
        </el-empty>
      </section>

      <div class="discovery-columns">
        <section class="content-block compact-block">
          <div class="section-head">
            <div>
              <h3>热门景点</h3>
              <p>近期更容易被浏览和收藏的目的地。</p>
            </div>
            <button type="button" class="section-link" @click="router.push(`${APP_ROUTE_PATHS.spots}?sortBy=heat`)">查看全部</button>
          </div>

          <div v-if="hotSpots.length" class="spot-grid two-columns">
            <SpotCard
              v-for="spot in hotSpots.slice(0, 4)"
              :key="spot.id"
              :spot="spot"
              @select="goSpot(spot.id, SPOT_DETAIL_SOURCE.DISCOVER)"
            />
          </div>
          <el-empty v-else description="暂无热门景点" />
        </section>

        <div class="discovery-side">
          <section class="nearby-entry premium-card">
            <div class="section-head">
              <div>
                <p class="entry-kicker">Nearby</p>
                <h3>附近探索</h3>
                <p>{{ nearbySectionSummary }}</p>
              </div>
              <button type="button" class="section-link" @click="activateNearby">进入附近</button>
            </div>

            <div v-if="nearbySpots.length" class="nearby-entry-list">
              <article
                v-for="spot in nearbySpots.slice(0, 3)"
                :key="spot.id"
                class="nearby-entry-card"
                @click="goSpot(spot.id, SPOT_DETAIL_SOURCE.NEARBY)"
              >
                <img :src="getImageUrl(spot.coverImage)" class="nearby-entry-image" :alt="spot.name || '附近景点图片'" />
                <div>
                  <h4>{{ spot.name }}</h4>
                  <p>{{ spot.regionName || '附近区域' }} · {{ formatDistance(spot.distanceKm) }}</p>
                </div>
              </article>
            </div>
            <el-empty v-else :description="nearbyEmptyText" :image-size="72">
              <el-button v-if="userStore.isLoggedIn" size="small" type="primary" :loading="nearbyLoading" @click="handleLocate">
                {{ nearbyLoading ? '定位中' : '刷新附近' }}
              </el-button>
              <el-button v-else size="small" type="primary" @click="router.push(AUTH_ROUTE_PATHS.login)">登录后查看</el-button>
            </el-empty>
          </section>

          <section class="guide-entry premium-card">
            <div>
              <p class="entry-kicker">Guides</p>
              <h3>攻略灵感</h3>
              <p>用路线、预算和游玩主题反推目的地，适合还没确定去哪的时候。</p>
            </div>

            <div v-if="guideList.length" class="guide-entry-list">
              <button
                v-for="guide in guideList.slice(0, 3)"
                :key="guide.id"
                type="button"
                class="guide-entry-item"
                @click="goGuide(guide.id)"
              >
                <span>{{ resolveGuideCategory(guide.category) }}</span>
                <strong>{{ resolveGuideText(guide.title) }}</strong>
              </button>
            </div>
            <el-empty v-else description="暂无攻略" :image-size="72" />

            <div class="entry-actions">
              <el-button type="primary" @click="router.push(APP_ROUTE_PATHS.guides)">查看攻略</el-button>
              <button type="button" class="section-link" @click="activateGuides">按主题筛选</button>
            </div>
          </section>
        </div>
      </div>
    </section>

    <section v-else-if="viewMode === 'spots'" class="content-block">
      <div class="section-head">
        <h3>{{ hasSpotFilters ? '景点结果' : '景点发现' }}</h3>
        <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.spots)">查看全部</button>
      </div>

      <div v-if="spotModeItems.length" class="spot-grid three-columns">
        <SpotCard
          v-for="spot in spotModeItems"
          :key="spot.id"
          :spot="spot"
          @select="goSpot(spot.id, SPOT_DETAIL_SOURCE.DISCOVER)"
        />
      </div>
      <el-empty v-else description="当前条件暂无景点" />
    </section>

    <section v-else-if="viewMode === 'guides'" class="content-block">
      <div class="section-head">
        <h3>{{ selectedGuideCategory ? '攻略结果' : '攻略精选' }}</h3>
        <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.guides)">查看全部</button>
      </div>

      <div v-if="guideList.length" class="guide-featured-list">
        <article v-for="guide in guideList" :key="guide.id" class="guide-featured-card premium-card" @click="goGuide(guide.id)">
          <img :src="getImageUrl(guide.coverImage)" class="guide-featured-image" alt="" />
          <div class="guide-featured-content">
            <span>{{ resolveGuideCategory(guide.category) }}</span>
            <h3>{{ resolveGuideText(guide.title) }}</h3>
            <p>{{ resolveGuideSummary(guide.summary) }}</p>
            <div class="guide-meta">
              <span>浏览 {{ guide.viewCount || 0 }}</span>
              <span>{{ guide.createdAt || '时间待补充' }}</span>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前条件暂无攻略" />
    </section>

    <section v-else class="content-block">
      <div class="section-head">
        <h3>附近景点</h3>
        <div class="section-actions">
          <el-button v-if="userStore.isLoggedIn" :loading="nearbyLoading" @click="handleLocate">{{ nearbyLoading ? '定位中' : '重新定位' }}</el-button>
          <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.nearby)">附近页</button>
        </div>
      </div>

      <p class="nearby-note">{{ nearbySectionSummary }}</p>
      <div v-if="nearbySpots.length" class="spot-grid three-columns">
        <SpotCard
          v-for="spot in nearbySpots"
          :key="spot.id"
          :spot="spot"
          @select="goSpot(spot.id, SPOT_DETAIL_SOURCE.NEARBY)"
        />
      </div>
      <el-empty v-else :description="nearbyEmptyText">
        <el-button v-if="userStore.isLoggedIn" type="primary" @click="handleLocate">开启定位</el-button>
        <el-button v-else type="primary" @click="router.push(AUTH_ROUTE_PATHS.login)">去登录</el-button>
      </el-empty>
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
import { ArrowRight, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getGuideList, getCategories } from '@/modules/guide/api.js'
import { getHotSpots, getNearbySpots } from '@/modules/home/api.js'
import { useRecommendationFeed } from '@/modules/recommendation/composables/useRecommendationFeed.js'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import { getSpotList, getFilters } from '@/modules/spot/api.js'
import { useUserStore } from '@/modules/account/store/user.js'
import { getCurrentLocation, getLocationSnapshot } from '@/shared/lib/location.js'
import { getImageUrl } from '@/shared/api/client.js'
import { APP_ROUTE_PATHS, AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'
import { resolveWebGuideCategory, resolveWebGuideDisplayText } from '@/shared/constants/resource-display.js'

const DISCOVER_STATE_KEY = 'discover_state'
const DISCOVER_MODES = ['all', 'spots', 'guides', 'nearby']
const viewModeOptions = [
  { label: '综合', value: 'all' },
  { label: '景点', value: 'spots' },
  { label: '攻略', value: 'guides' },
  { label: '附近', value: 'nearby' }
]

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const viewMode = ref('all')
const regionTree = ref([])
const spotCategoryTree = ref([])
const guideCategories = ref([])
const selectedRegionPath = ref([])
const selectedSpotCategoryPath = ref([])
const selectedGuideCategory = ref('')
const spotList = ref([])
const hotSpots = ref([])
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
  fetchRecommendationList,
  rotateRecommendationList,
  openPreferenceDialog,
  savePreferences
} = useRecommendationFeed(8)

const selectedRegionId = computed(() => selectedRegionPath.value[selectedRegionPath.value.length - 1] || '')
const selectedSpotCategoryId = computed(() => selectedSpotCategoryPath.value[selectedSpotCategoryPath.value.length - 1] || '')
const showSpotFilters = computed(() => viewMode.value === 'spots')
const showGuideFilter = computed(() => viewMode.value === 'guides')
const hasSpotFilters = computed(() => !!selectedRegionId.value || !!selectedSpotCategoryId.value)
const hasFilters = computed(() => hasSpotFilters.value || !!selectedGuideCategory.value)
const spotModeItems = computed(() => (hasSpotFilters.value ? spotList.value : [
  ...recommendations.value.slice(0, 3),
  ...hotSpots.value.slice(0, 6)
]))
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
  return '允许定位后查看附近景点。'
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

const resolveGuideText = (value) => resolveWebGuideDisplayText(value)
const resolveGuideCategory = (value) => resolveWebGuideCategory(value)
const resolveGuideSummary = (value) => value || '暂无摘要'

const findPathById = (tree, targetId) => {
  if (!Array.isArray(tree) || !tree.length || targetId === '' || targetId == null) return []

  const normalizedTargetId = String(targetId)
  const stack = tree.map((node) => ({ node, path: [node.id] }))
  while (stack.length) {
    const current = stack.pop()
    if (!current) continue
    if (String(current.node.id) === normalizedTargetId) return current.path
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
    mode: viewMode.value,
    selectedRegionId: selectedRegionId.value,
    selectedSpotCategoryId: selectedSpotCategoryId.value,
    selectedGuideCategory: selectedGuideCategory.value
  }))
}

const restoreState = () => {
  const raw = localStorage.getItem(DISCOVER_STATE_KEY)
  if (!raw) return

  const state = JSON.parse(raw)
  viewMode.value = DISCOVER_MODES.includes(state.mode) ? state.mode : 'all'
  selectedRegionPath.value = state.selectedRegionId ? [state.selectedRegionId] : []
  selectedSpotCategoryPath.value = state.selectedSpotCategoryId ? [state.selectedSpotCategoryId] : []
  selectedGuideCategory.value = state.selectedGuideCategory || ''
}

const syncRouteQuery = () => {
  router.replace({
    path: APP_ROUTE_PATHS.discover,
    query: {
      ...(viewMode.value !== 'all' ? { mode: viewMode.value } : {}),
      ...(selectedRegionId.value ? { regionId: selectedRegionId.value } : {}),
      ...(selectedSpotCategoryId.value ? { categoryId: selectedSpotCategoryId.value } : {}),
      ...(selectedGuideCategory.value ? { guideCategory: selectedGuideCategory.value } : {})
    }
  })
}

const applyRoutePreset = () => {
  const legacyScene = typeof route.query.scene === 'string' ? route.query.scene : ''
  const legacyTab = typeof route.query.tab === 'string' ? route.query.tab : ''
  const mode = typeof route.query.mode === 'string' ? route.query.mode : ''

  if (DISCOVER_MODES.includes(mode)) viewMode.value = mode
  if (legacyScene === 'nearby') viewMode.value = 'nearby'
  if (legacyScene === 'recommend') viewMode.value = 'spots'
  if (legacyTab === 'guide') viewMode.value = 'guides'
  if (legacyTab === 'spot') viewMode.value = 'spots'
  if (typeof route.query.regionId === 'string') selectedRegionPath.value = [route.query.regionId]
  if (typeof route.query.categoryId === 'string') selectedSpotCategoryPath.value = [route.query.categoryId]
  if (typeof route.query.guideCategory === 'string') selectedGuideCategory.value = route.query.guideCategory
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
  const params = { page: 1, pageSize: 6, sortBy: hasSpotFilters.value ? 'heat' : 'heat' }
  if (selectedRegionId.value) params.regionId = selectedRegionId.value
  if (selectedSpotCategoryId.value) params.categoryId = selectedSpotCategoryId.value
  const res = await getSpotList(params)
  spotList.value = res.data?.list || []
}

const fetchHotPreview = async () => {
  const res = await getHotSpots(6)
  hotSpots.value = res.data?.list || []
}

const fetchGuidePreview = async () => {
  const params = { page: 1, pageSize: viewMode.value === 'guides' ? 10 : 4, sortBy: 'time' }
  if (selectedGuideCategory.value) params.category = selectedGuideCategory.value
  const res = await getGuideList(params)
  guideList.value = res.data?.list || []
}

const fetchNearbyPreview = async (location) => {
  if (!location) return
  const res = await getNearbySpots(location.latitude, location.longitude, viewMode.value === 'nearby' ? 9 : 3)
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
    fetchRecommendationList(),
    fetchHotPreview(),
    fetchSpotPreview(),
    fetchGuidePreview(),
    tryLoadNearbyAutomatically()
  ])
}

const handleModeChange = async () => {
  syncRouteQuery()
  if (viewMode.value === 'nearby') {
    await handleLocate()
    return
  }
  if (viewMode.value === 'guides') {
    await fetchGuidePreview()
    return
  }
  if (viewMode.value === 'spots') {
    await Promise.all([fetchRecommendationList(), fetchHotPreview(), fetchSpotPreview()])
  }
}

const handleSpotFilter = async () => {
  syncRouteQuery()
  await fetchSpotPreview()
}

const handleGuideFilter = async () => {
  syncRouteQuery()
  await fetchGuidePreview()
}

const resetFilters = async () => {
  selectedRegionPath.value = []
  selectedSpotCategoryPath.value = []
  selectedGuideCategory.value = ''
  syncRouteQuery()
  await Promise.all([fetchSpotPreview(), fetchGuidePreview()])
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

const activateNearby = async () => {
  viewMode.value = 'nearby'
  await handleModeChange()
}

const activateGuides = async () => {
  viewMode.value = 'guides'
  await handleModeChange()
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

const goSpot = (id, source) => {
  router.push(buildSpotDetailRoute(id, source))
}

const goGuide = (id) => {
  router.push(`/guides/${id}`)
}

watch([viewMode, selectedRegionId, selectedSpotCategoryId, selectedGuideCategory], persistState)

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
  padding: 26px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.12), transparent 28%),
    linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
}

.hero-actions {
  width: min(100%, 420px);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-search {
  width: 100%;
  min-height: 48px;
  padding: 0 16px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #ffffff;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-weight: 700;
  box-shadow: 0 18px 36px -28px rgba(37, 99, 235, 0.58);
  transition: border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.hero-search:hover {
  border-color: #93c5fd;
  color: #1d4ed8;
  box-shadow: 0 22px 40px -28px rgba(37, 99, 235, 0.72);
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
.nearby-note,
.hint-banner p,
.section-head p,
.guide-featured-card p,
.nearby-entry-card p {
  color: #64748b;
  line-height: 1.75;
}

.filter-panel {
  padding: 18px 20px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  gap: 14px;
  align-items: end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.filter-label {
  color: #475569;
  font-weight: 600;
  font-size: 13px;
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

.section-link:disabled {
  color: #94a3b8;
  cursor: default;
}

.filter-reset {
  min-height: 32px;
}

.discover-board {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.discovery-columns,
.discovery-side,
.content-block {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.discovery-columns {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.9fr);
  align-items: stretch;
}

.focus-block,
.compact-block {
  height: 100%;
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
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

.section-head p {
  margin-top: 6px;
  font-size: 14px;
}

.hint-banner {
  padding: 16px 18px;
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

.spot-grid {
  display: grid;
  gap: 16px;
}

.two-columns {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.three-columns {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.spotlight-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.nearby-entry,
.guide-entry {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.entry-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.nearby-entry h3,
.guide-entry h3 {
  color: #0f172a;
  font-size: 24px;
}

.nearby-entry p,
.guide-entry p {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.75;
}

.nearby-entry-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.nearby-entry-card {
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.nearby-entry-card:hover {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.nearby-entry-image {
  width: 96px;
  height: 76px;
  border-radius: 10px;
  object-fit: cover;
}

.nearby-entry-card h4 {
  color: #0f172a;
  line-height: 1.35;
}

.nearby-entry-card p {
  margin-top: 6px;
  font-size: 13px;
}

.guide-entry-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.guide-entry-item {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  color: #0f172a;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.guide-entry-item:hover {
  border-color: #fed7aa;
  background: #fff7ed;
}

.guide-entry-item span {
  color: #9a3412;
  font-size: 12px;
  font-weight: 700;
}

.guide-entry-item strong {
  line-height: 1.4;
  font-size: 15px;
}

.entry-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.guide-featured-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.guide-featured-card:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
}

.guide-featured-card span {
  color: #8a6a2f;
  font-size: 12px;
  font-weight: 700;
}

.guide-featured-card {
  overflow: hidden;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.guide-featured-image {
  width: 100%;
  height: 100%;
  min-height: 220px;
  object-fit: cover;
}

.guide-featured-content {
  padding: 22px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.guide-featured-content h3 {
  margin-top: 10px;
  font-size: 24px;
  color: #0f172a;
  line-height: 1.25;
}

.guide-featured-content p {
  margin-top: 12px;
}

.guide-meta {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.nearby-note {
  margin-top: -4px;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 1200px) {
  .discovery-columns {
    grid-template-columns: 1fr;
  }

  .three-columns {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .discover-hero,
  .section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-panel,
  .two-columns,
  .three-columns,
  .spotlight-grid,
  .guide-featured-card {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: 28px;
  }

  .guide-featured-image {
    min-height: 200px;
  }
}
</style>
