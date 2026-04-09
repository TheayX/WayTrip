<!-- 穷游玩法页 -->
<template>
  <div class="page-container budget-page">
    <section class="hero-card premium-card">
      <div>
        <p class="hero-eyebrow">Budget Travel</p>
        <h1 class="hero-title">穷游玩法</h1>
        <p class="hero-desc">把预算作为第一筛选条件，先找更省钱的景点和攻略，再决定是否继续深入浏览。</p>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <strong>{{ budgetSpots.length }}</strong>
          <span>低预算景点</span>
        </div>
        <div class="hero-stat">
          <strong>{{ budgetGuides.length }}</strong>
          <span>低预算攻略</span>
        </div>
      </div>
    </section>

    <section class="control-card premium-card">
      <div class="control-head">
        <div>
          <p class="hero-eyebrow">Browse Mode</p>
          <h2>{{ budgetSummaryTitle }}</h2>
        </div>
        <p class="control-desc">{{ budgetSummaryText }}</p>
      </div>

      <div class="control-row">
        <el-segmented v-model="activeTab" :options="tabs" @change="handleTabChange" />
        <div class="filter-options">
          <button
            v-for="option in budgetModes"
            :key="option.value"
            type="button"
            class="filter-chip"
            :class="{ active: budgetMode === option.value }"
            @click="switchBudgetMode(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'spots'" class="content-section">
      <p class="section-tip">优先展示价格更低、同时热度更高的景点结果。</p>

      <div v-if="budgetSpots.length" class="spot-grid">
        <SpotCard
          v-for="item in budgetSpots"
          :key="item.id"
          :spot="item"
          @select="goSpotDetail(item.id)"
        />
      </div>
      <el-empty v-else-if="!loadingSpots" description="暂时没找到低预算景点">
        <template #description>
          <p>可以稍后再试，或切换预算口径继续看看。</p>
        </template>
      </el-empty>
    </section>

    <section v-else class="content-section">
      <p class="section-tip">根据攻略关联景点的价格，优先筛出更适合低预算出行的内容。</p>

      <div v-if="budgetGuides.length" class="guide-grid">
        <GuideCard
          v-for="item in budgetGuides"
          :key="item.id"
          :guide="item"
          @select="goGuideDetail(item.id)"
        />
      </div>
      <el-empty v-else-if="!loadingGuides" description="暂时没筛出低预算攻略">
        <template #description>
          <p>可以换个预算口径，或者稍后再试。</p>
        </template>
      </el-empty>
    </section>

    <section v-if="currentLoading" class="loading-row premium-card">
      <p>{{ currentLoadingText }}</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import GuideCard from '@/modules/guide/components/GuideCard.vue'
import {
  BUDGET_MAX_PRICE,
  BUDGET_MODE_FREE,
  BUDGET_MODE_OPTIONS,
  BUDGET_MODE_UNDER_50,
  fetchBudgetTravelGuides,
  fetchBudgetTravelSpots
} from '@/modules/budget-travel/api.js'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'

// 该页把“预算口径”和“内容类型”拆成两个维度，方便用户快速切换浏览方式。
const router = useRouter()

const tabs = [
  { label: '景点', value: 'spots' },
  { label: '攻略', value: 'guides' }
]
const budgetModes = BUDGET_MODE_OPTIONS
const activeTab = ref('spots')
const budgetMode = ref(BUDGET_MODE_UNDER_50)
const budgetSpots = ref([])
const budgetGuides = ref([])
const loadingSpots = ref(false)
const loadingGuides = ref(false)

// 当前加载态按页签切换，避免景点和攻略互相覆盖 loading 反馈。
const currentLoading = computed(() => (activeTab.value === 'spots' ? loadingSpots.value : loadingGuides.value))
const currentLoadingText = computed(() => (activeTab.value === 'spots' ? '正在筛选低预算景点...' : '正在整理低预算攻略...'))
const currentBudgetModeLabel = computed(() => (budgetMode.value === BUDGET_MODE_FREE ? '免费' : '50 元以内'))
const budgetSummaryTitle = computed(() => `${currentBudgetModeLabel.value} · ${activeTab.value === 'spots' ? '景点' : '攻略'}`)
const budgetSummaryText = computed(() => {
  const currentCount = activeTab.value === 'spots' ? budgetSpots.value.length : budgetGuides.value.length
  if (currentLoading.value) {
    return `正在整理${currentBudgetModeLabel.value}内容，请稍候。`
  }
  return `当前已筛出 ${currentCount} 条${activeTab.value === 'spots' ? '景点' : '攻略'}结果。`
})

const loadBudgetSpots = async () => {
  if (loadingSpots.value) return
  loadingSpots.value = true

  try {
    budgetSpots.value = await fetchBudgetTravelSpots({
      budgetMode: budgetMode.value,
      maxPrice: BUDGET_MAX_PRICE
    })
  } catch (error) {
    console.error('加载穷游玩法景点失败', error)
    ElMessage.error('加载景点失败，请稍后重试')
  } finally {
    loadingSpots.value = false
  }
}

const loadBudgetGuides = async () => {
  if (loadingGuides.value) return
  loadingGuides.value = true

  try {
    budgetGuides.value = await fetchBudgetTravelGuides({
      budgetMode: budgetMode.value,
      maxPrice: BUDGET_MAX_PRICE
    })
  } catch (error) {
    console.error('加载穷游玩法攻略失败', error)
    ElMessage.error('加载攻略失败，请稍后重试')
  } finally {
    loadingGuides.value = false
  }
}

const handleTabChange = (value) => {
  activeTab.value = value
  if (value === 'spots') {
    loadBudgetSpots()
    return
  }
  loadBudgetGuides()
}

const switchBudgetMode = (value) => {
  if (budgetMode.value === value) return
  budgetMode.value = value
  budgetSpots.value = []
  budgetGuides.value = []
  if (activeTab.value === 'spots') {
    loadBudgetSpots()
    return
  }
  loadBudgetGuides()
}

const goSpotDetail = (id) => {
  router.push(buildSpotDetailRoute(id, SPOT_DETAIL_SOURCE.BUDGET_TRAVEL))
}

const goGuideDetail = (id) => {
  router.push(`/guides/${id}`)
}

onMounted(() => {
  loadBudgetSpots()
})
</script>

<style lang="scss" scoped>
.budget-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 4px;
  padding-bottom: 32px;
}

.hero-card,
.control-card,
.loading-row {
  padding: 26px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-end;
  background:
    radial-gradient(circle at top right, rgba(249, 115, 22, 0.12), transparent 28%),
    linear-gradient(135deg, #fff9f4 0%, #ffffff 58%, #fff7ed 100%);
}

.hero-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 700;
}

.hero-title,
.control-head h2 {
  color: #0f172a;
  letter-spacing: -0.03em;
}

.hero-title {
  font-size: 36px;
}

.hero-desc,
.control-desc,
.section-tip,
.loading-row p {
  color: #64748b;
  line-height: 1.8;
}

.hero-stats {
  display: flex;
  gap: 14px;
}

.hero-stat {
  min-width: 140px;
  padding: 18px 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.8);
}

.hero-stat strong {
  display: block;
  font-size: 28px;
  color: #111827;
}

.hero-stat span {
  display: block;
  margin-top: 8px;
  color: #64748b;
}

.control-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.control-row {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.filter-options {
  display: flex;
  gap: 12px;
}

.filter-chip {
  height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  cursor: pointer;
  font-weight: 600;
}

.filter-chip.active {
  background: rgba(249, 115, 22, 0.12);
  color: #ea580c;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.spot-grid,
.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 992px) {
  .hero-card,
  .control-head,
  .control-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .spot-grid,
  .guide-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-stats,
  .filter-options {
    width: 100%;
    flex-direction: column;
  }
}
</style>
