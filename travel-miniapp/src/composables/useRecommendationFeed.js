/**
 * 推荐流管理 Composable
 * 
 * 职责：
 * 1. 获取和管理推荐景点列表（个性化/偏好/热门三种类型）
 * 2. 处理用户偏好设置（选择感兴趣的分类）
 * 3. 管理冷启动引导流程
 * 
 * 使用场景：
 * - 首页推荐卡片展示
 * - 用户偏好设置弹窗
 * - 推荐结果轮播刷新
 * 
 * @param {number} limit - 每次获取的推荐数量
 * @returns {object} 推荐流状态和方法
 */
import { computed, ref } from 'vue'
import { getFilters } from '@/api/spot'
import { getRecommendations, rotateRecommendations } from '@/api/home'
import { setPreferences } from '@/api/user'
import { markColdStartGuideCompleted } from '@/utils/cold-start-guide'
import { useUserStore } from '@/stores/user'

// 推荐类型映射表：后端返回的英文类型 → 前端展示的中文文案
const RECOMMENDATION_TYPE_MAP = {
  personalized: '个性推荐',  // 基于协同过滤算法的个性化推荐
  preference: '偏好推荐',    // 基于用户选中分类的推荐
  hot: '热门推荐'            // 全局热门景点
}

/**
 * 推荐流核心逻辑
 * @param {number} limit - 推荐列表数量限制
 */
export const useRecommendationFeed = (limit) => {
  // ========== 依赖注入 ==========
  const userStore = useUserStore()  // 用户状态管理（token、用户信息、偏好等）

  // ========== 响应式状态 ==========
  /** @type {ref<Array>} 推荐景点列表 */
  const recommendations = ref([])
  
  /** @type {ref<string>} 当前推荐类型：'personalized' | 'preference' | 'hot' */
  const recommendationType = ref('hot')
  
  /** @type {ref<boolean>} 是否需要引导用户设置偏好（冷启动场景） */
  const needPreference = ref(false)
  
  /** @type {ref<Array>} 所有可选的分类列表（从后端获取） */
  const categories = ref([])
  
  /** @type {ref<Array>} 用户当前选中的分类 ID 列表 */
  const selectedCategories = ref([])
  
  /** @type {ref<boolean>} 偏好设置弹窗是否显示 */
  const preferenceVisible = ref(false)

  // ========== 计算属性 ==========
  /**
   * 将英文推荐类型转换为中文展示文案
   * 用于页面上显示「个性推荐」「偏好推荐」「热门推荐」标签
   */
  const recommendType = computed(() => {
    return RECOMMENDATION_TYPE_MAP[recommendationType.value] || '个性推荐'
  })

  // ========== 内部工具方法 ==========
  
  /**
   * 应用推荐接口响应数据到本地状态
   * @param {object} data - 后端返回的推荐数据
   * @param {Array} data.list - 推荐景点列表
   * @param {string} data.type - 推荐类型
   * @param {boolean} data.needPreference - 是否需要引导设置偏好
   */
  const applyRecommendationResponse = (data) => {
    recommendations.value = data?.list || []
    recommendationType.value = data?.type || 'hot'
    needPreference.value = data?.needPreference || false
  }

  /**
   * 重置推荐状态为初始值（未登录或出错时调用）
   */
  const resetRecommendationState = () => {
    recommendations.value = []
    recommendationType.value = 'hot'
    needPreference.value = false
  }

  /**
   * 从后端获取所有景点分类列表
   * 用于偏好设置弹窗中的分类选项
   * 注意：未登录用户不加载分类（返回空数组）
   */
  const fetchCategories = async () => {
    if (!userStore.token) {
      categories.value = []
      return
    }

    try {
      const res = await getFilters()  // GET /api/spots/filters
      categories.value = res.data?.categories || []
    } catch (error) {
      console.error('获取分类失败', error)
    }
  }

  /**
   * 确保分类列表已加载（懒加载模式）
   * 如果 categories 为空，则先调用 fetchCategories
   * 避免重复请求
   */
  const ensureCategoriesLoaded = async () => {
    if (!categories.value.length) {
      await fetchCategories()
    }
  }

  // ========== 对外暴露的核心方法 ==========
  
  /**
   * 获取推荐列表（首次加载或下拉刷新时调用）
   * 
   * 流程：
   * 1. 检查登录状态，未登录则重置状态
   * 2. 调用后端推荐接口 GET /api/home/recommendations?limit=xxx
   * 3. 根据返回数据更新推荐列表和类型
   * 
   * @returns {Promise<object|null>} 推荐数据或 null（失败时）
   */
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

  /**
   * 轮播/换一批推荐（点击「换一批」按钮时调用）
   * 
   * 与 fetchRecommendationList 的区别：
   * - 不检查登录状态（假设已登录）
   * - 调用的是专门的轮播接口，后端会返回不同的推荐结果
   * 
   * @returns {Promise<object>} 新的推荐数据
   */
  const rotateRecommendationList = async () => {
    const res = await rotateRecommendations(limit)  // POST /api/home/recommendations/rotate
    applyRecommendationResponse(res.data)
    return res.data
  }

  /**
   * 打开偏好设置弹窗
   * 
   * 流程：
   * 1. 确保分类列表已加载（懒加载）
   * 2. 从用户 store 中读取已选中的分类 ID
   * 3. 显示偏好设置弹窗
   */
  const openPreferenceDialog = async () => {
    await ensureCategoriesLoaded()
    // 回显用户已选的偏好分类
    selectedCategories.value = [...(userStore.userInfo?.preferenceCategoryIds || [])]
    preferenceVisible.value = true
  }

  /**
   * 保存用户偏好设置
   * 
   * 流程：
   * 1. 获取选中的分类 ID 列表
   * 2. 根据 ID 查找对应的分类名称（用于展示）
   * 3. 调用后端接口保存偏好 PUT /api/user/preferences
   * 4. 更新本地用户 store 中的偏好信息
   * 5. 标记冷启动引导已完成
   * 6. 关闭弹窗
   * 
   * @returns {Promise<Array<string>>} 选中的分类名称列表
   */
  const savePreferences = async () => {
    const categoryIds = [...selectedCategories.value]
    // 根据 ID 查找分类名称（用于前端展示和用户 store）
    const categoryNames = categoryIds
      .map(id => categories.value.find(cat => cat.id === id)?.name)
      .filter(Boolean)  // 过滤掉找不到的分类

    // 保存到后端
    await setPreferences({ categoryIds })  // PUT /api/user/preferences
    
    // 同步更新本地用户状态（避免重新请求）
    userStore.updatePreferences({
      preferences: categoryNames,
      preferenceCategoryIds: categoryIds,
      preferenceCategoryNames: categoryNames
    })
    
    // 标记冷启动引导已完成（后续不再弹出）
    markColdStartGuideCompleted(userStore.userInfo?.id)
    
    // 关闭弹窗
    preferenceVisible.value = false
    
    return categoryNames
  }

  // ========== 返回值（供组件使用）==========
  return {
    // 状态
    recommendations,        // 推荐景点列表
    recommendationType,     // 当前推荐类型（英文）
    needPreference,         // 是否需要引导设置偏好
    categories,             // 所有可选分类
    selectedCategories,     // 用户选中的分类
    preferenceVisible,      // 偏好弹窗显示状态
    recommendType,          // 推荐类型中文名（计算属性）
    
    // 方法
    fetchCategories,           // 获取分类列表
    ensureCategoriesLoaded,    // 确保分类已加载
    fetchRecommendationList,   // 获取推荐列表
    rotateRecommendationList,  // 轮播推荐
    openPreferenceDialog,      // 打开偏好弹窗
    savePreferences,           // 保存偏好
    resetRecommendationState   // 重置状态
  }
}
