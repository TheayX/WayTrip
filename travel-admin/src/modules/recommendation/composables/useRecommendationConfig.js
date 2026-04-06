import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRecommendationConfig,
  getRecommendationStatus,
  updateRecommendationConfig,
  updateRecommendationMatrix
} from '@/modules/recommendation/api/recommendation.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'

// 统一维护推荐配置默认值，避免页面内散落默认参数。
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

// 管理推荐配置、矩阵状态和保存动作，避免配置页继续堆积状态型逻辑。
export function useRecommendationConfig() {
  const config = reactive(createDefaultConfig())
  const savedConfig = ref(cloneConfig(createDefaultConfig()))
  const status = reactive({
    lastUpdateTime: null,
    totalUsers: null,
    totalSpots: null,
    computing: false
  })
  const saving = ref(false)
  const updatingMatrix = ref(false)

  const applyConfig = (nextConfig = {}) => {
    const defaults = createDefaultConfig()
    Object.assign(config.algorithm, defaults.algorithm, nextConfig.algorithm || {})
    Object.assign(config.heat, defaults.heat, nextConfig.heat || {})
    Object.assign(config.cache, defaults.cache, nextConfig.cache || {})
  }

  // 用字段路径比对配置差异，便于区分“即时生效”和“需重建矩阵”的改动。
  const getByPath = (target, path) => path.split('.').reduce((acc, key) => acc?.[key], target)
  const getChangedPaths = (paths) => paths.filter(path => getByPath(config, path) !== getByPath(savedConfig.value, path))

  const matrixChangedPaths = computed(() => getChangedPaths(matrixFieldPaths))
  const immediateChangedPaths = computed(() => getChangedPaths(immediateFieldPaths))

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

  const fetchConfig = async () => {
    try {
      const res = await getRecommendationConfig()
      if (res.data) {
        // 服务端配置可能缺部分字段，这里始终回填默认值，避免表单出现 undefined。
        applyConfig(res.data)
        savedConfig.value = cloneConfig({
          algorithm: { ...config.algorithm },
          heat: { ...config.heat },
          cache: { ...config.cache }
        })
      }
      return res
    } catch (e) {
      console.error('获取配置失败', e)
      throw e
    }
  }

  const fetchStatus = async () => {
    try {
      const res = await getRecommendationStatus()
      if (res.data) {
        Object.assign(status, res.data)
      }
      return res
    } catch (e) {
      console.error('获取状态失败', e)
      throw e
    }
  }

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
      // 保存成功后立刻刷新已保存快照，确保“待保存项”统计与页面状态一致。
      if (matrixChangedCount > 0 && immediateChangedCount > 0) {
        ElMessage.success('配置已保存：在线参数立即生效；离线矩阵参数需重建相似度矩阵后完全生效')
        return
      }
      if (matrixChangedCount > 0) {
        ElMessage.success('配置已保存：当前改动属于离线矩阵参数，请继续执行“重建相似度矩阵”')
        return
      }
      ElMessage.success('配置已保存：当前改动会在新的推荐请求中立即生效')
    } catch {
      ElMessage.error('保存配置失败')
    } finally {
      saving.value = false
    }
  }

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
      // 重建完成后立即刷新状态，保证总览时间和覆盖数量同步更新。
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

  return {
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
  }
}
