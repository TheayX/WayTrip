import { computed, ref } from 'vue'
import { getFilters } from '@/modules/spot/api.js'
import { getRecommendations, refreshRecommendations } from '@/modules/home/api.js'
import { setPreferences } from '@/modules/account/api.js'
import { markColdStartGuideCompleted } from '@/shared/lib/cold-start-guide.js'
import { useUserStore } from '@/modules/account/store/user.js'

// 推荐能力复用组合式函数

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
  const recommendations = ref([])
  const recommendationType = ref('hot')
  const needPreference = ref(false)
  const categories = ref([])
  const selectedCategories = ref([])
  const preferenceVisible = ref(false)

  const recommendType = computed(() => RECOMMENDATION_TYPE_MAP[recommendationType.value] || '个性推荐')

  const applyRecommendationResponse = (data) => {
    recommendations.value = data?.list || []
    recommendationType.value = data?.type || 'hot'
    needPreference.value = data?.needPreference || false
  }

  const resetRecommendationState = () => {
    recommendations.value = []
    recommendationType.value = 'hot'
    needPreference.value = false
  }

  const fetchCategories = async () => {
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
    if (!userStore.token) {
      resetRecommendationState()
      return null
    }

    const res = await getRecommendations(limit)
    applyRecommendationResponse(res.data)
    return res.data
  }

  const refreshRecommendationList = async () => {
    const res = await refreshRecommendations(limit)
    applyRecommendationResponse(res.data)
    return res.data
  }

  const openPreferenceDialog = async () => {
    await ensureCategoriesLoaded()
    selectedCategories.value = [...(userStore.userInfo?.preferenceCategoryIds || [])]
    preferenceVisible.value = true
  }

  const savePreferences = async () => {
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
    refreshRecommendationList,
    openPreferenceDialog,
    savePreferences,
    resetRecommendationState
  }
}
