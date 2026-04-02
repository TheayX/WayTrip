<!-- 推荐引擎配置页面 -->
<template>
  <div class="recommendation-page">
    <section class="page-hero">
      <div>
        <p class="page-kicker">Recommendation Workspace</p>
        <h1 class="page-title">推荐配置</h1>
        <p class="page-subtitle">左侧维护参数配置，右侧处理保存与矩阵执行，下方进行调试预览和结果验证。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="pageLoading || saving || updatingMatrix" @click="loadPageData">刷新状态</el-button>
      </div>
    </section>

    <div class="workspace-status-row">
      <div class="workspace-summary">
        <el-card shadow="hover" class="workspace-summary-card">
          <div class="summary-label">当前页面状态</div>
          <div class="summary-value">{{ pageStatusLabel }}</div>
          <div class="summary-desc">{{ pageStatusDesc }}</div>
        </el-card>
        <el-card shadow="hover" class="workspace-summary-card">
          <div class="summary-label">在线改动</div>
          <div class="summary-value">{{ immediateChangeSummary.count }}</div>
          <div class="summary-desc">保存后立即影响新推荐请求的参数项数量</div>
        </el-card>
        <el-card shadow="hover" class="workspace-summary-card">
          <div class="summary-label">离线矩阵改动</div>
          <div class="summary-value">{{ matrixChangeSummary.count }}</div>
          <div class="summary-desc">保存后仍需重建矩阵才能完全生效的参数项数量</div>
        </el-card>
      </div>

      <el-alert
        v-if="pageError"
        type="error"
        show-icon
        :closable="false"
        :title="pageError"
      />
      <el-alert
        v-else-if="pageWarning"
        type="warning"
        show-icon
        :closable="false"
        :title="pageWarning"
      />
    </div>

    <div v-if="pageError" class="error-state">
      <el-result icon="error" title="推荐配置加载失败" :sub-title="pageError">
        <template #extra>
          <el-button type="primary" :loading="pageLoading" @click="loadPageData">重新加载</el-button>
        </template>
      </el-result>
    </div>

    <template v-else>
      <RecommendationStatusCards v-loading="pageLoading" :status="status" />

    <el-row v-loading="pageLoading" :gutter="24" class="workspace-row">
      <el-col :xl="16" :lg="15" :md="24">
        <el-card shadow="hover" class="workspace-shell-card">
          <template #header>
            <div class="shell-card-header">
              <span>配置区</span>
              <el-tag size="small" effect="plain" type="primary" round>参数录入</el-tag>
            </div>
          </template>
          <RecommendationConfigCard
            :config="config"
            :impact-overview-cards="impactOverviewCards"
            :immediate-change-summary="immediateChangeSummary"
            :matrix-change-summary="matrixChangeSummary"
          />
        </el-card>
      </el-col>

      <el-col :xl="8" :lg="9" :md="24">
        <el-card shadow="hover" class="workspace-shell-card">
          <template #header>
            <div class="shell-card-header">
              <span>执行区</span>
              <el-tag size="small" effect="plain" type="success" round>保存与生效</el-tag>
            </div>
          </template>
          <RecommendationExecutionCard
            ref="executionCardRef"
            :status="status"
            :config="config"
            :matrix-change-summary="matrixChangeSummary"
            :saving="saving"
            :updating-matrix="updatingMatrix"
            @reset-config="handleResetConfig"
            @save-config="handleSaveConfig"
            @update-matrix="handleUpdateMatrix"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="workspace-shell-card debug-shell-card">
      <template #header>
        <div class="shell-card-header">
          <span>调试预览</span>
          <el-tag size="small" effect="plain" type="info" round>结果验证</el-tag>
        </div>
      </template>
      <RecommendationDebugCard
        ref="debugCardRef"
        :active-preview-tab="activePreviewTab"
        :debug-form="debugForm"
        :similarity-form="similarityForm"
        :previewing="previewing"
        :similarity-previewing="similarityPreviewing"
        :similarity-matrix-previewing="similarityMatrixPreviewing"
        :debug-result="debugResult"
        :similarity-result="similarityResult"
        :recommendation-type-meta="recommendationTypeMeta"
        :debug-items="debugItems"
        :debug-summary-cards="debugSummaryCards"
        :debug-info="debugInfo"
        :behavior-stats="behaviorStats"
        :behavior-details="behaviorDetails"
        :debug-insights="debugInsights"
        :compact-debug-insights="compactDebugInsights"
        :debug-notes="debugNotes"
        :debug-sections="debugSections"
        :result-contributions="resultContributions"
        :top-debug-items="topDebugItems"
        :debug-output="debugOutput"
        :debug-table-rows="debugTableRows"
        @update:active-preview-tab="activePreviewTab = $event"
        @preview-recommendations="handlePreviewRecommendations"
        @preview-similarity="handlePreviewSimilarity"
        @preview-similarity-update="handlePreviewSimilarityWithMatrixUpdate"
      />
    </el-card>

    <el-card shadow="hover" class="workspace-shell-card">
      <template #header>
        <div class="shell-card-header">
          <span>字段说明</span>
          <el-tag size="small" effect="plain" type="warning" round>配置参考</el-tag>
        </div>
      </template>
      <RecommendationHelpCard
        :active-collapse="activeCollapse"
        :weight-explanations="weightExplanations"
        :cf-data-field-references="cfDataFieldReferences"
        :cold-start-data-field-references="coldStartDataFieldReferences"
        @update:active-collapse="activeCollapse = $event"
      />
    </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, nextTick, ref } from 'vue'
import { useRoute } from 'vue-router'
import RecommendationStatusCards from '@/modules/recommendation/components/RecommendationStatusCards.vue'
import RecommendationConfigCard from '@/modules/recommendation/components/RecommendationConfigCard.vue'
import RecommendationExecutionCard from '@/modules/recommendation/components/RecommendationExecutionCard.vue'
import RecommendationDebugCard from '@/modules/recommendation/components/RecommendationDebugCard.vue'
import RecommendationHelpCard from '@/modules/recommendation/components/RecommendationHelpCard.vue'
import { useRecommendationConfig } from '@/modules/recommendation/composables/useRecommendationConfig.js'
import { useRecommendationPreview } from '@/modules/recommendation/composables/useRecommendationPreview.js'

const route = useRoute()
const pageLoading = ref(false)
const pageError = ref('')
const pageWarning = ref('')

const {
  config,
  status,
  impactOverviewCards,
  immediateChangeSummary,
  matrixChangeSummary,
  saving,
  updatingMatrix,
  weightExplanations,
  cfDataFieldReferences,
  coldStartDataFieldReferences,
  fetchConfig,
  fetchStatus,
  handleSaveConfig,
  handleResetConfig,
  handleUpdateMatrix
} = useRecommendationConfig()

const {
  previewing,
  similarityPreviewing,
  similarityMatrixPreviewing,
  activePreviewTab,
  activeCollapse,
  executionCardRef,
  debugCardRef,
  debugResult,
  similarityResult,
  debugForm,
  similarityForm,
  debugOutput,
  debugItems,
  debugSummaryCards,
  debugInfo,
  behaviorStats,
  behaviorDetails,
  debugInsights,
  compactDebugInsights,
  debugNotes,
  debugSections,
  resultContributions,
  topDebugItems,
  debugTableRows,
  recommendationTypeMeta,
  handlePreviewRecommendations,
  handlePreviewSimilarity,
  handlePreviewSimilarityWithMatrixUpdate
} = useRecommendationPreview({ status, fetchStatus })

const pageStatusLabel = computed(() => {
  if (pageLoading.value) return '加载中'
  if (pageError.value) return '加载失败'
  if (pageWarning.value) return '部分完成'
  if (status.computing) return '矩阵计算中'
  return '就绪'
})

const pageStatusDesc = computed(() => {
  if (pageError.value) return '配置区和状态区未能完整加载，请重新拉取当前工作台数据。'
  if (pageWarning.value) return pageWarning.value
  if (status.computing) return '当前推荐引擎正在计算矩阵，离线结果会在完成后刷新。'
  return '当前工作台状态正常，可以继续保存配置、更新矩阵或执行调试预览。'
})

// 根据路由定位区域
const applyRouteFocus = async () => {
  await nextTick()
  if (route.query.focus === 'execution') {
    executionCardRef.value?.$el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  if (route.query.focus === 'debug') {
    activePreviewTab.value = 'recommendation'
    await nextTick()
    debugCardRef.value?.$el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const loadPageData = async () => {
  pageLoading.value = true
  pageError.value = ''
  pageWarning.value = ''

  const results = await Promise.allSettled([
    fetchConfig(),
    fetchStatus()
  ])

  const failedSections = []
  if (results[0].status === 'rejected') failedSections.push('参数配置')
  if (results[1].status === 'rejected') failedSections.push('运行状态')

  if (failedSections.length === results.length) {
    const firstError = results.find(item => item.status === 'rejected')
    pageError.value = firstError?.reason?.response?.data?.message || firstError?.reason?.message || '推荐工作台初始化失败，请稍后重试。'
  } else if (failedSections.length) {
    pageWarning.value = `部分区块未成功刷新：${failedSections.join('、')}。当前页面已保留可用数据。`
  }

  pageLoading.value = false
}

// 页面初始化
onMounted(async () => {
  await loadPageData()
  await applyRouteFocus()
})
</script>

<style lang="scss" scoped>
.recommendation-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .page-hero {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    padding: 4px 2px;
  }

  .page-kicker {
    margin: 0 0 6px;
    color: #64748b;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  .page-title {
    margin: 0;
    color: #0f172a;
    font-size: 30px;
    line-height: 1.2;
  }

  .page-subtitle {
    margin: 8px 0 0;
    color: #64748b;
  }

  .workspace-status-row {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .workspace-summary {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }

  .workspace-summary-card {
    border-radius: 14px;
    border: 1px solid #e7edf7;
  }

  .summary-label {
    font-size: 12px;
    color: #64748b;
  }

  .summary-value {
    margin-top: 10px;
    font-size: 24px;
    font-weight: 700;
    color: #0f172a;
  }

  .summary-desc {
    margin-top: 10px;
    font-size: 12px;
    line-height: 1.7;
    color: #607086;
  }

  .error-state {
    padding: 8px 0 4px;
  }

  .workspace-shell-card {
    border: none;
  }

  .shell-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }

  .debug-shell-card {
    margin-top: 4px;
  }

  .workspace-row {
    margin-bottom: 4px;
  }

  @media (max-width: 1200px) {
    .workspace-summary {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 768px) {
    .page-hero {
      flex-direction: column;
    }

    .hero-actions {
      width: 100%;
    }

    .hero-actions :deep(.el-button) {
      width: 100%;
    }

    .workspace-row {
      margin-bottom: 16px;
    }
  }
}
</style>
