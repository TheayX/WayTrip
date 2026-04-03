<!-- 穷游玩法页 -->
<template>
  <div class="page-container budget-page">
    <section class="hero-card">
      <div>
        <p class="hero-eyebrow">Budget Travel</p>
        <h1 class="hero-title">穷游玩法</h1>
        <p class="hero-desc">先把预算压下来，再挑值得去的景点和攻略。</p>
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

    <section class="tab-bar card">
      <el-segmented v-model="activeTab" :options="tabs" @change="handleTabChange" />
    </section>

    <section class="filter-card card">
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
    </section>

    <section class="summary-card card">
      <h2>{{ budgetSummaryTitle }}</h2>
      <p>{{ budgetSummaryText }}</p>
    </section>

    <section v-if="activeTab === 'spots'" class="content-section">
      <p class="section-tip">优先展示低价且热度更高的景点。</p>

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
      <p class="section-tip">根据攻略关联景点的价格，先筛出更适合低预算出行的内容。</p>

      <div v-if="budgetGuides.length" class="guide-grid">
        <article v-for="item in budgetGuides" :key="item.id" class="guide-card card" @click="goGuideDetail(item.id)">
          <img :src="getImageUrl(item.coverImage)" class="guide-image" alt="" />
          <div class="guide-content">
            <div class="guide-head">
              <h3>{{ item.title }}</h3>
              <span class="guide-price">{{ item.priceLabel || '低预算' }}</span>
            </div>
            <p>{{ item.summary || '这篇攻略里提到的景点更适合低预算出行。' }}</p>
            <div class="guide-meta">
              <span class="tag">{{ item.category || '攻略' }}</span>
              <span class="tag">👁 {{ item.viewCount || 0 }}</span>
              <span class="tag">关联景点 {{ item.relatedCount || 0 }}</span>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else-if="!loadingGuides" description="暂时没筛出低预算攻略">
        <template #description>
          <p>可以换个预算口径，或者稍后再试。</p>
        </template>
      </el-empty>
    </section>

    <section v-if="currentLoading" class="loading-row card">
      <p>{{ currentLoadingText }}</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import {
  BUDGET_MAX_PRICE,
  BUDGET_MODE_FREE,
  BUDGET_MODE_OPTIONS,
  BUDGET_MODE_UNDER_50,
  fetchBudgetTravelGuides,
  fetchBudgetTravelSpots
} from '@/modules/budget-travel/api.js'
import { getImageUrl } from '@/shared/api/client.js'

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
  router.push(`/spots/${id}?source=budget-travel`)
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
  gap: 18px;
  padding-top: 8px;
  padding-bottom: 32px;
}

.hero-card,
.tab-bar,
.filter-card,
.summary-card,
.loading-row {
  padding: 24px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-end;
  background: linear-gradient(135deg, #fff7ed 0%, #ffffff 56%, #fffbeb 100%);
}

.hero-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.24em;
  color: #9a3412;
  text-transform: uppercase;
}

.hero-title {
  font-size: 36px;
  margin-bottom: 12px;
}

.hero-desc,
.summary-card p,
.section-tip,
.guide-content p,
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
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
}

.hero-stat strong,
.summary-card h2 {
  display: block;
  font-size: 28px;
  color: #111827;
}

.hero-stat span {
  display: block;
  margin-top: 8px;
  color: #64748b;
}

.filter-options {
  display: flex;
  gap: 12px;
}

.filter-chip {
  height: 44px;
  padding: 0 20px;
  border: none;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  cursor: pointer;
}

.filter-chip.active {
  background: rgba(249, 115, 22, 0.12);
  color: #ea580c;
  font-weight: 600;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.spot-grid,
.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.guide-card {
  cursor: pointer;
  overflow: hidden;
}

.guide-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.guide-content {
  padding: 18px;
}

.guide-head,
.guide-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.guide-head h3 {
  flex: 1;
  font-size: 18px;
}

.guide-price {
  color: #ea580c;
  font-weight: 700;
  white-space: nowrap;
}

.guide-content p {
  margin-top: 12px;
  margin-bottom: 14px;
}

.guide-meta {
  flex-wrap: wrap;
}

.tag {
  padding: 6px 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
}

@media (max-width: 992px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .spot-grid,
  .guide-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 30px;
  }

  .hero-stats,
  .filter-options {
    width: 100%;
    flex-direction: column;
  }
}
</style>
