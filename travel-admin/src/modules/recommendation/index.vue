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
import { ref, onMounted, reactive, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import RecommendationStatusCards from '@/modules/recommendation/components/RecommendationStatusCards.vue'
import RecommendationExecutionCard from '@/modules/recommendation/components/RecommendationExecutionCard.vue'
import RecommendationDebugCard from '@/modules/recommendation/components/RecommendationDebugCard.vue'
import RecommendationHelpCard from '@/modules/recommendation/components/RecommendationHelpCard.vue'
import {
  getRecommendationConfig,
  updateRecommendationConfig,
  getRecommendationStatus,
  updateRecommendationMatrix,
  previewRecommendations,
  previewSimilarityNeighbors
} from '@/modules/recommendation/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'
import {
  DataLine, Setting, Clock
} from '@element-plus/icons-vue'

const route = useRoute()

// 默认推荐配置
const createDefaultConfig = () => ({
  algorithm: {
    weightView: 0.5,
    weightFavorite: 1.0,
    weightReviewFactor: 0.4,
    weightOrderPaid: 3.0,
    weightOrderCompleted: 4.0,
    viewSourceFactorHome: 0.9,
    viewSourceFactorSearch: 1.2,
    viewSourceFactorRecommendation: 1.1,
    viewSourceFactorGuide: 1.0,
    viewSourceFactorDetail: 1.0,
    viewDurationShortThresholdSeconds: 10,
    viewDurationMediumThresholdSeconds: 60,
    viewDurationLongThresholdSeconds: 180,
    viewDurationFactorShort: 0.6,
    viewDurationFactorMedium: 1.0,
    viewDurationFactorLong: 1.2,
    viewDurationFactorVeryLong: 1.35,
    minInteractionsForCF: 3,
    topKNeighbors: 20,
    candidateExpandFactor: 2,
    coldStartExpandFactor: 3
  },
  heat: {
    heatViewIncrement: 1,
    heatFavoriteIncrement: 3,
    heatReviewIncrement: 2,
    heatOrderPaidIncrement: 5,
    heatOrderCompletedIncrement: 8,
    heatRerankFactor: 0.05
  },
  cache: {
    similarityTTLHours: 24,
    userRecTTLMinutes: 60
  }
})

// 需要重建矩阵的字段
const matrixFieldPaths = [
  'algorithm.weightView',
  'algorithm.weightFavorite',
  'algorithm.weightReviewFactor',
  'algorithm.weightOrderPaid',
  'algorithm.weightOrderCompleted',
  'algorithm.viewSourceFactorHome',
  'algorithm.viewSourceFactorSearch',
  'algorithm.viewSourceFactorRecommendation',
  'algorithm.viewSourceFactorGuide',
  'algorithm.viewSourceFactorDetail',
  'algorithm.viewDurationShortThresholdSeconds',
  'algorithm.viewDurationMediumThresholdSeconds',
  'algorithm.viewDurationLongThresholdSeconds',
  'algorithm.viewDurationFactorShort',
  'algorithm.viewDurationFactorMedium',
  'algorithm.viewDurationFactorLong',
  'algorithm.viewDurationFactorVeryLong',
  'algorithm.topKNeighbors',
  'cache.similarityTTLHours'
]

// 保存后即时生效的字段
const immediateFieldPaths = [
  'algorithm.minInteractionsForCF',
  'algorithm.candidateExpandFactor',
  'algorithm.coldStartExpandFactor',
  'heat.heatViewIncrement',
  'heat.heatFavoriteIncrement',
  'heat.heatReviewIncrement',
  'heat.heatOrderPaidIncrement',
  'heat.heatOrderCompletedIncrement',
  'heat.heatRerankFactor',
  'cache.userRecTTLMinutes'
]

const cloneConfig = (value) => JSON.parse(JSON.stringify(value))

// 当前配置与上次保存的配置
const config = reactive(createDefaultConfig())
const savedConfig = ref(cloneConfig(createDefaultConfig()))

// 应用服务端配置并补齐默认值
const applyConfig = (nextConfig = {}) => {
  const defaults = createDefaultConfig()
  Object.assign(config.algorithm, defaults.algorithm, nextConfig.algorithm || {})
  Object.assign(config.heat, defaults.heat, nextConfig.heat || {})
  Object.assign(config.cache, defaults.cache, nextConfig.cache || {})
}

const getByPath = (target, path) => path.split('.').reduce((acc, key) => acc?.[key], target)
const getChangedPaths = (paths) => paths.filter(path => getByPath(config, path) !== getByPath(savedConfig.value, path))

const matrixChangedPaths = computed(() => getChangedPaths(matrixFieldPaths))
const immediateChangedPaths = computed(() => getChangedPaths(immediateFieldPaths))

// 推荐引擎运行状态
const status = reactive({
  lastUpdateTime: null,
  totalUsers: null,
  totalSpots: null,
  computing: false
})

// 顶部影响说明卡片
const impactOverviewCards = computed(() => [
  {
    title: '在线即时生效',
    desc: '热度累计、冷启动触发、用户缓存和热度重排在保存后会立刻影响新请求。',
    meta: `${immediateChangedPaths.value.length} 项待保存`,
    tag: '保存即生效',
    tagType: 'success',
    tone: 'tone-live'
  },
  {
    title: '离线相似度矩阵',
    desc: '交互权重、浏览因子、TopK 和矩阵 TTL 会影响离线相似邻居或其缓存节奏。',
    meta: `${matrixChangedPaths.value.length} 项待重建矩阵`,
    tag: '需重建矩阵',
    tagType: 'warning',
    tone: 'tone-matrix'
  },
  {
    title: '保存配置',
    desc: '保存只写入参数，不会自动触发相似度矩阵重建。',
    meta: '建议先保存，再按需重建矩阵',
    tag: '不自动重算',
    tagType: 'info',
    tone: 'tone-save'
  },
  {
    title: '当前矩阵版本',
    desc: '离线参数是否真正生效，要看最近一次矩阵重建时间。',
    meta: status.lastUpdateTime || '尚未生成',
    tag: '离线版本',
    tagType: 'primary',
    tone: 'tone-status'
  }
])

const matrixChangeSummary = computed(() => ({
  count: matrixChangedPaths.value.length,
  needsRebuild: matrixChangedPaths.value.length > 0,
  desc: matrixChangedPaths.value.length
    ? `已改动 ${matrixChangedPaths.value.length} 个离线矩阵相关字段`
    : '当前没有待重建的矩阵参数变更'
}))

const immediateChangeSummary = computed(() => ({
  count: immediateChangedPaths.value.length,
  desc: immediateChangedPaths.value.length
    ? `已改动 ${immediateChangedPaths.value.length} 个在线即时生效字段`
    : '当前没有待保存的即时生效字段变更'
}))

const saving = ref(false)
const updatingMatrix = ref(false)
const previewing = ref(false)
const similarityPreviewing = ref(false)
const similarityMatrixPreviewing = ref(false)
const activePreviewTab = ref('recommendation')
const activeCollapse = ref([])
const executionCardRef = ref()
const debugCardRef = ref()
const debugResult = ref(null)
const similarityResult = ref(null)
const debugForm = reactive({
  userId: 1,
  limit: 6,
  refresh: true,
  debug: true,
  stable: true
})
const similarityForm = reactive({
  spotId: 1,
  limit: 8
})
const debugOutput = computed(() => {
  if (!debugResult.value) return ''

  const lines = [
    `request.userId = ${debugForm.userId}`,
    `request.limit = ${debugForm.limit}`,
    `request.refresh = ${debugForm.refresh}`,
    `request.debug = ${debugForm.debug}`,
    `request.stable = ${debugForm.stable}`,
    `response.type = ${debugResult.value.type}`,
    `response.needPreference = ${debugResult.value.needPreference}`,
    `response.count = ${debugResult.value.list?.length || 0}`
  ]

  if (!debugResult.value.list?.length) {
    lines.push('response.items = []')
    lines.push('说明：当前请求已返回空推荐列表。若 type=preference，通常表示用户命中了偏好冷启动，但对应分类下没有可用景点。')
    return lines.join('\n')
  }

  debugResult.value.list.forEach((item, index) => {
    lines.push(
      `item[${index}] = { id: ${item.id}, name: ${item.name}, score: ${
        item.score == null ? 'null' : Number(item.score).toFixed(4)
      }, category: ${item.categoryName || '-'}, region: ${item.regionName || '-'} }`
    )
  })

  return lines.join('\n')
})

const debugItems = computed(() => debugResult.value?.list || [])
const debugInfo = computed(() => debugResult.value?.debugInfo || null)
const debugNotes = computed(() => debugInfo.value?.notes || [])
const behaviorStats = computed(() => debugInfo.value?.behaviorStats || [])
const behaviorDetails = computed(() => debugInfo.value?.behaviorDetails || [])
const resultContributions = computed(() => debugInfo.value?.resultContributions || [])

const debugSections = computed(() => {
  if (!debugInfo.value) return []
  const sections = [
    { key: 'interactions', title: '用户交互权重', items: debugInfo.value.userInteractions || [] },
    { key: 'candidates', title: '原始候选分数', items: debugInfo.value.candidateScores || [] },
    { key: 'filtered', title: '过滤后候选分数', items: debugInfo.value.filteredScores || [] },
    { key: 'reranked', title: '重排后候选分数', items: debugInfo.value.rerankedScores || [] },
    { key: 'removed', title: '被过滤景点', items: debugInfo.value.filteredOutItems || [] }
  ]
  return sections.filter(section => section.items.length)
})

const compactDebugInsights = computed(() => debugInsights.value.slice(0, 3))

const recommendationTypeMeta = computed(() => {
  const type = debugResult.value?.type
  if (type === 'personalized') {
    return {
      label: '个性化推荐',
      tagType: 'success',
      alertType: 'success',
      title: '当前结果来自协同过滤个性化推荐',
      description: '说明该用户已有足够交互行为，系统已基于 ItemCF、用户历史交互权重和相似景点关系完成推荐。'
    }
  }
  if (type === 'preference') {
    return {
      label: '偏好冷启动',
      tagType: 'warning',
      alertType: 'warning',
      title: '当前结果来自偏好冷启动推荐',
      description: '说明用户个性化交互不足，系统改为按用户偏好分类召回景点。'
    }
  }
  if (type === 'hot') {
    return {
      label: '热门兜底',
      tagType: 'info',
      alertType: 'info',
      title: '当前结果来自热门兜底推荐',
      description: '说明用户交互不足且偏好信息有限，系统回退到热门景点推荐。'
    }
  }
  return {
    label: '未知类型',
    tagType: 'info',
    alertType: 'info',
    title: '当前结果类型未识别',
    description: '请结合后端控制台调试日志进一步确认推荐链路。'
  }
})

const scoreStats = computed(() => {
  const scoredItems = debugItems.value.filter(item => item.score != null)
  if (!scoredItems.length) {
    return {
      count: 0,
      average: null,
      max: null,
      min: null
    }
  }
  const scores = scoredItems.map(item => Number(item.score))
  return {
    count: scoredItems.length,
    average: scores.reduce((sum, value) => sum + value, 0) / scores.length,
    max: Math.max(...scores),
    min: Math.min(...scores)
  }
})

const debugSummaryCards = computed(() => {
  const topItem = debugItems.value[0]
  return [
    {
      label: '推荐来源',
      value: recommendationTypeMeta.value.label,
      desc: debugInfo.value?.triggerReason || (debugResult.value?.needPreference ? '当前链路仍建议补充偏好' : '当前链路无需额外偏好引导')
    },
    {
      label: '返回结果数',
      value: String(debugItems.value.length),
      desc: `请求数量 ${debugForm.limit}，实际返回 ${debugItems.value.length}`
    },
    {
      label: '最高推荐分',
      value: topItem?.score != null ? Number(topItem.score).toFixed(4) : '无',
      desc: topItem ? `Top1：${topItem.name}` : '暂无返回结果'
    },
    {
      label: '平均推荐分',
      value: scoreStats.value.average != null ? scoreStats.value.average.toFixed(4) : '无',
      desc: scoreStats.value.count ? `有 ${scoreStats.value.count} 条结果带个性化分数` : '当前结果没有返回个性化分数'
    }
  ]
})

const debugInsights = computed(() => {
  if (!debugResult.value) return []
  const insights = []
  if (debugResult.value.type === 'personalized') {
    insights.push('当前用户已满足协同过滤触发条件，本次结果优先反映历史交互与相似景点关系。')
  }
  if (debugResult.value.type === 'preference') {
    insights.push('当前结果来自偏好冷启动，建议对比用户偏好分类是否和返回景点分类一致。')
  }
  if (debugResult.value.type === 'hot') {
    insights.push('当前结果来自热门兜底，此时推荐分数字段通常为空，重点看热度和上架数据是否合理。')
  }
  if (debugResult.value.needPreference) {
    insights.push('接口提示需要偏好引导，说明用户侧可以进一步补充偏好标签以改善冷启动效果。')
  }
  if (debugInfo.value?.filteredOutItems?.length) {
    insights.push(`后端共过滤掉 ${debugInfo.value.filteredOutItems.length} 个已交互景点，避免把用户已经看过/买过的景点继续推荐回来。`)
  }
  if (!debugItems.value.length) {
    insights.push('本次返回空列表。优先检查用户偏好命中的分类下是否有已上架且未删除的景点。')
  }
  if (scoreStats.value.count > 1 && scoreStats.value.max != null && scoreStats.value.min != null) {
    insights.push(`当前推荐分数区间为 ${scoreStats.value.min.toFixed(4)} ~ ${scoreStats.value.max.toFixed(4)}，可用于判断结果区分度是否足够。`)
  }
  if (debugForm.debug) {
    insights.push('本次已启用后端详细调试日志，可同步结合服务端控制台查看交互权重、候选分数、过滤与重排信息。')
  }
  return insights
})

const topDebugItems = computed(() =>
  debugItems.value.slice(0, 3).map((item, index) => ({
    ...item,
    rank: index + 1,
    scoreText: item.score == null ? '-' : Number(item.score).toFixed(4)
  }))
)

const debugTableRows = computed(() =>
  debugItems.value.map((item, index) => {
    const scoreText = item.score == null ? '-' : Number(item.score).toFixed(4)
    const priceText = item.price == null ? '-' : `¥${item.price}`
    const ratingText = item.avgRating == null
      ? '暂无评分'
      : `${Number(item.avgRating).toFixed(1)} 分 / ${item.ratingCount || 0} 条评价`
    let reason = '请结合后端详细日志查看交互权重、候选分数与过滤过程。'
    if (debugResult.value?.type === 'personalized') {
      reason = item.score == null
        ? '当前结果来自个性化链路，但该项未返回分数。请检查后端打分与响应填充。'
        : '该景点保留了个性化推荐分，适合继续比对候选分数与热度重排结果。'
    } else if (debugResult.value?.type === 'preference') {
      reason = '该景点来自偏好分类召回，重点确认用户偏好与景点分类是否匹配。'
    } else if (debugResult.value?.type === 'hot') {
      reason = '该景点来自热门兜底，重点确认热度、上架状态和冷启动逻辑。'
    }
    return {
      ...item,
      rank: index + 1,
      scoreText,
      priceText,
      ratingText,
      reason
    }
  })
)

const weightExplanations = [
  { behavior: '浏览', param: 'weightView', default: '0.5', description: '浏览基础权重；实际按 来源因子 × 停留时长因子 细化，来源和时长规则均可在管理端配置' },
  { behavior: '收藏', param: 'weightFavorite', default: '1.0', description: '用户主动收藏景点，表示明确兴趣' },
  { behavior: '评分', param: 'weightReviewFactor', default: '0.4', description: '实际权重 = 评分(1~5) × 因子，如5分评价 = 5×0.4 = 2.0' },
  { behavior: '已付款', param: 'weightOrderPaid', default: '3.0', description: '用户已下单付款但未完成行程，表示强烈意向' },
  { behavior: '已完成', param: 'weightOrderCompleted', default: '4.0', description: '用户已完成行程，最强信号' }
]

const cfDataFieldReferences = [
  {
    table: 'user_spot_view',
    fields: ['user_id', 'spot_id', 'view_source', 'view_duration'],
    usage: '浏览行为权重、来源因子、停留时长因子',
    phase: '交互权重构建'
  },
  {
    table: 'user_spot_favorite',
    fields: ['user_id', 'spot_id', 'is_deleted'],
    usage: '收藏行为权重与已交互过滤',
    phase: '交互权重构建'
  },
  {
    table: 'user_spot_review',
    fields: ['user_id', 'spot_id', 'score', 'is_deleted'],
    usage: '评分权重、评论分值信号',
    phase: '交互权重构建'
  },
  {
    table: 'order',
    fields: ['user_id', 'spot_id', 'status', 'is_deleted'],
    usage: '提取已支付/已完成订单行为权重',
    phase: '交互权重构建'
  }
]

const coldStartDataFieldReferences = [
  {
    table: 'user_preference',
    fields: ['user_id', 'tag', 'is_deleted'],
    usage: '冷启动偏好分类召回',
    phase: '冷启动'
  },
  {
    table: 'spot',
    fields: ['id', 'category_id', 'heat_score', 'is_published', 'is_deleted'],
    usage: '上架过滤、冷启动热门排序、热度重排',
    phase: '候选生成与重排'
  }
]

// 加载推荐配置
const fetchConfig = async () => {
  try {
    const res = await getRecommendationConfig()
    if (res.data) {
      applyConfig(res.data)
      savedConfig.value = cloneConfig({
        algorithm: { ...config.algorithm },
        heat: { ...config.heat },
        cache: { ...config.cache }
      })
    }
  } catch (e) {
    console.error('获取配置失败', e)
  }
}

// 加载引擎状态
const fetchStatus = async () => {
  try {
    const res = await getRecommendationStatus()
    if (res.data) {
      Object.assign(status, res.data)
    }
  } catch (e) {
    console.error('获取状态失败', e)
  }
}

// 保存推荐配置
const handleSaveConfig = async () => {
  try {
    saving.value = true
    const payload = {
      algorithm: { ...config.algorithm },
      heat: { ...config.heat },
      cache: { ...config.cache }
    }
    const matrixChangedCount = matrixChangedPaths.value.length
    const immediateChangedCount = immediateChangedPaths.value.length
    await updateRecommendationConfig(payload)
    savedConfig.value = cloneConfig(payload)
    if (matrixChangedCount > 0 && immediateChangedCount > 0) {
      ElMessage.success('配置已保存：在线参数立即生效；离线矩阵参数需重建相似度矩阵后完全生效')
      return
    }
    if (matrixChangedCount > 0) {
      ElMessage.success('配置已保存：当前改动属于离线矩阵参数，请继续执行“重建相似度矩阵”')
      return
    }
    ElMessage.success('配置已保存：当前改动会在新的推荐请求中立即生效')
    return
  } catch (e) {
    ElMessage.error('保存配置失败')
  } finally {
    saving.value = false
  }
}

// 恢复默认配置
const handleResetConfig = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要将所有参数恢复为默认值吗？这将覆盖当前配置。',
      '恢复默认配置',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const defaults = createDefaultConfig()
    applyConfig(defaults)
    await updateRecommendationConfig(defaults)
    savedConfig.value = cloneConfig(defaults)
    ElMessage.success('已恢复为默认配置')
  } catch (e) {
    if (!isMessageBoxDismissed(e)) {
      ElMessage.error('恢复默认失败')
    }
  }
}

// 重建推荐矩阵
const handleUpdateMatrix = async () => {
  try {
    await ElMessageBox.confirm(
      '更新矩阵将使用当前保存的参数重新计算所有景点的相似度。此操作可能耗时较长，确定继续？',
      '手动更新矩阵',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    updatingMatrix.value = true
    status.computing = true
    await updateRecommendationMatrix()
    ElMessage.success('相似度矩阵更新完成！')
    await fetchStatus()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) {
      ElMessage.error('矩阵更新失败')
    }
  } finally {
    updatingMatrix.value = false
    status.computing = false
  }
}

const handlePreviewRecommendations = async () => {
  if (!debugForm.userId) {
    ElMessage.warning('请输入用户 ID')
    return
  }
  try {
    previewing.value = true
    const res = await previewRecommendations({ ...debugForm })
    debugResult.value = res.data || null
    ElMessage.success('调试预览完成')
  } catch (e) {
    ElMessage.error('调试预览失败')
  } finally {
    previewing.value = false
  }
}

const handlePreviewSimilarity = async () => {
  if (!similarityForm.spotId) {
    ElMessage.warning('请输入景点 ID')
    return
  }
  try {
    similarityPreviewing.value = true
    const res = await previewSimilarityNeighbors({ ...similarityForm })
    similarityResult.value = res.data || null
    ElMessage.success('相似邻居预览完成')
  } catch (e) {
    ElMessage.error('相似邻居预览失败')
  } finally {
    similarityPreviewing.value = false
  }
}

const handlePreviewSimilarityWithMatrixUpdate = async () => {
  if (!similarityForm.spotId) {
    ElMessage.warning('请输入景点 ID')
    return
  }
  try {
    similarityMatrixPreviewing.value = true
    status.computing = true
    await updateRecommendationMatrix()
    const res = await previewSimilarityNeighbors({ ...similarityForm })
    similarityResult.value = res.data || null
    ElMessage.success('矩阵更新完成，已加载最新相似邻居')
    await fetchStatus()
  } catch (e) {
    ElMessage.error('更新预览失败')
  } finally {
    similarityMatrixPreviewing.value = false
    status.computing = false
  }
}

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
