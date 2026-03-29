import { computed, ref } from 'vue'
import { getFilters } from '@/api/spot'
import { getRecommendations, refreshRecommendations } from '@/api/home'
import { setPreferences } from '@/api/user'
import { markColdStartGuideCompleted } from '@/utils/cold-start-guide'
import { useUserStore } from '@/stores/user'

const RECOMMENDATION_TYPE_MAP = {
  personalized: '为你推荐',
  preference: '猜你喜欢',
  hot: '热门推荐'
}

export const useRecommendationFeed = (limit) => {
  const userStore = useUserStore()
  const recommendations = ref([])
  const recommendationType = ref('hot')
  const needPreference = ref(false)
  const categories = ref([])
  const selectedCategories = ref([])
  const preferenceVisible = ref(false)

  const recommendType = computed(() => {
    return RECOMMENDATION_TYPE_MAP[recommendationType.value] || '为你推荐'
  })

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

    try {
      const res = await getFilters()
      categories.value = res.data?.categories || []
    } catch (error) {
      console.error('获取分类失败', error)
    }
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

    try {
      const res = await getRecommendations(limit)
      applyRecommendationResponse(res.data)
      return res.data
    } catch (error) {
      console.error('获取推荐失败', error)
      return null
    }
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
      .map(id => categories.value.find(cat => cat.id === id)?.name)
      .filter(Boolean)

    await setPreferences({ categoryIds })
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
