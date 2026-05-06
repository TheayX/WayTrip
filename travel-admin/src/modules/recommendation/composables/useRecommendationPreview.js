import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { resolveCategoryDisplayName, resolveRegionDisplayName } from '@/shared/lib/resource-display.js'
import {
  previewRecommendations,
  previewSimilarityNeighbors,
  updateRecommendationMatrix
} from '@/modules/recommendation/api/recommendation.js'

// 管理调试预览与相似邻居预览，避免页面层混入大段结果推导逻辑。
export function useRecommendationPreview({ status, fetchStatus }) {
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
    mode: 'recompute',
    debug: true
  })
  const similarityForm = reactive({
    spotId: 1,
    limit: 8
  })

  // 统一生成可读调试文本，方便页面展示和后续对比不同请求结果。
  const debugOutput = computed(() => {
    if (!debugResult.value) return ''

    const lines = [
      `request.userId = ${debugForm.userId}`,
      `request.userNickname = ${debugResult.value?.debugInfo?.userNickname || '未返回'}`,
      `request.limit = ${debugForm.limit}`,
      `request.mode = ${debugForm.mode}`,
      `request.debug = ${debugForm.debug}`,
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
        }, category: ${resolveCategoryDisplayName(item.categoryName)}, region: ${resolveRegionDisplayName(item.regionName)} }`
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
    // 只保留后端真正返回了数据的调试区块，避免页面出现空表格。
    const sections = [
      { key: 'interactions', title: '用户交互权重', items: debugInfo.value.userInteractions || [] },
      { key: 'candidates', title: '原始候选分数', items: debugInfo.value.candidateScores || [] },
      { key: 'filtered', title: '过滤后候选分数', items: debugInfo.value.filteredScores || [] },
      { key: 'reranked', title: '重排后候选分数', items: debugInfo.value.rerankedScores || [] },
      { key: 'removed', title: '被过滤景点', items: debugInfo.value.filteredOutItems || [] }
    ]
    return sections.filter(section => section.items.length)
  })

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
      { label: '返回结果数', value: String(debugItems.value.length), desc: `请求数量 ${debugForm.limit}，实际返回 ${debugItems.value.length}` },
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
    if (debugForm.mode === 'cache') {
      insights.push('缓存模式不会主动重算推荐链路，更适合确认当前用户端正在消费哪一份结果。')
    } else if (debugForm.mode === 'recompute_rotate') {
      insights.push('当前模式会在重算完成后模拟用户端“换一批”，便于观察轮换后的展示结果。')
    }
    return insights
  })

  const compactDebugInsights = computed(() => debugInsights.value.slice(0, 3))

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
    } catch {
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
    } catch {
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
      // 先重建矩阵再读邻居，确保本次预览看到的是最新离线结果。
      await updateRecommendationMatrix()
      const res = await previewSimilarityNeighbors({ ...similarityForm })
      similarityResult.value = res.data || null
      ElMessage.success('矩阵更新完成，已加载最新相似邻居')
      await fetchStatus()
    } catch {
      ElMessage.error('更新预览失败')
    } finally {
      similarityMatrixPreviewing.value = false
      status.computing = false
    }
  }

  return {
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
    debugSummaryCards,
    recommendationTypeMeta,
    handlePreviewRecommendations,
    handlePreviewSimilarity,
    handlePreviewSimilarityWithMatrixUpdate
  }
}
