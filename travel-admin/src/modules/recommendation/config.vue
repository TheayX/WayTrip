<!-- 推荐引擎配置页面 -->
<template>
  <div class="recommendation-page">
    <RecommendationStatusCards :status="status" />

    <el-row :gutter="24" class="workspace-row">
      <el-col :xl="16" :lg="15" :md="24">
        <RecommendationConfigCard
          :config="config"
          :impact-overview-cards="impactOverviewCards"
          :immediate-change-summary="immediateChangeSummary"
          :matrix-change-summary="matrixChangeSummary"
        />
      </el-col>

      <el-col :xl="8" :lg="9" :md="24">
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
      </el-col>
    </el-row>

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

    <RecommendationHelpCard
      :active-collapse="activeCollapse"
      :weight-explanations="weightExplanations"
      :cf-data-field-references="cfDataFieldReferences"
      :cold-start-data-field-references="coldStartDataFieldReferences"
      @update:active-collapse="activeCollapse = $event"
    />
  </div>
</template>

<script setup>
import { onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import RecommendationStatusCards from '@/modules/recommendation/components/RecommendationStatusCards.vue'
import RecommendationConfigCard from '@/modules/recommendation/components/RecommendationConfigCard.vue'
import RecommendationExecutionCard from '@/modules/recommendation/components/RecommendationExecutionCard.vue'
import RecommendationDebugCard from '@/modules/recommendation/components/RecommendationDebugCard.vue'
import RecommendationHelpCard from '@/modules/recommendation/components/RecommendationHelpCard.vue'
import { useRecommendationConfig } from '@/modules/recommendation/composables/useRecommendationConfig.js'
import { useRecommendationPreview } from '@/modules/recommendation/composables/useRecommendationPreview.js'

const route = useRoute()

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

// 页面初始化
onMounted(async () => {
  fetchConfig()
  fetchStatus()
  applyRouteFocus()
})
</script>

<style lang="scss" scoped>
.recommendation-page {
  .workspace-row {
    margin-bottom: 24px;
  }

  @media (max-width: 768px) {
    .workspace-row {
      margin-bottom: 16px;
    }
  }
}
</style>
