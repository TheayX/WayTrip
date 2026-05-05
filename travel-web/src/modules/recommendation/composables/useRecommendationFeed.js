import { computed, ref } from 'vue'
import { getFilters } from '@/modules/spot/api.js'
import { getRecommendations, rotateRecommendations } from '@/modules/home/api.js'
import { setPreferences } from '@/modules/account/api.js'
import { markColdStartGuideCompleted } from '@/shared/lib/cold-start-guide.js'
import { useUserStore } from '@/modules/account/store/user.js'

// 推荐能力复用组合式函数

// 推荐类型文案统一在这里映射，避免页面层重复判断后端 type。
const RECOMMENDATION_TYPE_MAP = {
  personalized: '个性推荐',
  preference: '偏好推荐',
  hot: '热门推荐'
}

/**
 * 统一管理推荐列表与偏好设置逻辑
 * @param {number} limit
 * @returns {Object}
 */
export const useRecommendationFeed = (limit = 10) => {
  const userStore = useUserStore()
  // 推荐列表、推荐类型和偏好弹窗状态统一收口在组合函数内部管理。
  const recommendations = ref([])
  const recommendationType = ref('hot')
  const needPreference = ref(false)
  const categories = ref([])
  const selectedCategories = ref([])
  const preferenceVisible = ref(false)

  const recommendType = computed(() => RECOMMENDATION_TYPE_MAP[recommendationType.value] || '个性推荐')

  // 后端推荐接口和刷新接口返回结构一致，这里统一做一次状态回填。
  const applyRecommendationResponse = (data) => {
    recommendations.value = data?.list || []
    recommendationType.value = data?.type || 'hot'
    needPreference.value = data?.needPreference || false
  }

  // 未登录或切换账号时重置推荐状态，避免残留上一位用户的结果。
  const resetRecommendationState = () => {
    recommendations.value = []
    recommendationType.value = 'hot'
    needPreference.value = false
  }

  const fetchCategories = async () => {
    // 偏好分类只对登录用户有意义，未登录时直接清空避免误展示旧数据。
    if (!userStore.token) {
      categories.value = []
      return
    }

    const res = await getFilters()
    categories.value = res.data?.categories || []
  }

  const ensureCategoriesLoaded = async () => {
    if (!categories.value.length) {
      await fetchCategories()
    }
  }

  const fetchRecommendationList = async () => {
    // 未登录时不请求推荐接口，页面层可据此直接展示登录引导或空状态。
    if (!userStore.token) {
      resetRecommendationState()
      return null
    }

    const res = await getRecommendations(limit)
    applyRecommendationResponse(res.data)
    return res.data
  }

  const rotateRecommendationList = async () => {
    const res = await rotateRecommendations(limit)
    applyRecommendationResponse(res.data)
    return res.data
  }

  const openPreferenceDialog = async () => {
    // 打开弹窗前先补齐分类选项，并回填用户当前偏好，避免弹窗出现空白状态。
    await ensureCategoriesLoaded()
    selectedCategories.value = [...(userStore.userInfo?.preferenceCategoryIds || [])]
    preferenceVisible.value = true
  }

  const savePreferences = async () => {
    // 保存时同步回写 store，保证推荐页、个人中心等位置立即拿到最新偏好。
    const categoryIds = [...selectedCategories.value]
    const categoryNames = categoryIds
      .map((id) => categories.value.find((cat) => cat.id === id)?.name)
      .filter(Boolean)

    await setPreferences(categoryIds)
    userStore.updatePreferences({
      preferences: categoryNames,
      preferenceCategoryIds: categoryIds,
      preferenceCategoryNames: categoryNames
    })
    // 偏好一旦完成，冷启动引导就不需要再次对同一用户重复弹出。
    markColdStartGuideCompleted(userStore.userInfo?.id)
    preferenceVisible.value = false
    return categoryNames
  }

  return {
    recommendations,
    recommendationType,
    needPreference,
    categories,
    selectedCategories,
    preferenceVisible,
    recommendType,
    fetchCategories,
    ensureCategoriesLoaded,
    fetchRecommendationList,
    rotateRecommendationList,
    openPreferenceDialog,
    savePreferences,
    resetRecommendationState
  }
}
