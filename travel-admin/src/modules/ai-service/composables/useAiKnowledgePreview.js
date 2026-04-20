import { computed, reactive, ref } from 'vue'
import { previewAiKnowledge } from '@/modules/ai-service/api.js'
import { buildEmptyAiPreviewResult, extractAiErrorMessage, formatAiPreviewDomains } from '@/modules/ai-service/utils.js'

/**
 * AI 知识预览组合式函数。
 * <p>
 * 查询测试页和工作台都需要维护同一套“场景、问题、命中结果”状态，这里统一收口，
 * 避免两个页面继续各自维护重复的 loading、空结果和知识域展示逻辑。
 *
 * @param {object} options 组合式参数
 * @param {string} options.defaultScenario 默认场景
 * @param {(scenario: string) => { value: string, domain: string } | null} options.resolveScenarioOption 场景选项解析函数
 * @param {Record<string, string>} options.domainLabels 知识域展示映射
 * @returns {object} 预览状态与操作
 */
export function useAiKnowledgePreview({ defaultScenario, resolveScenarioOption, domainLabels }) {
  const previewing = ref(false)
  const errorMessage = ref('')
  const result = ref(null)
  const hasSubmitted = ref(false)
  const form = reactive({
    scenario: defaultScenario,
    query: ''
  })

  const selectedScenarioOption = computed(() => resolveScenarioOption(form.scenario))
  const selectedScenarioDomainLabel = computed(() => resolveAiDomainLabel(selectedScenarioOption.value?.domain, domainLabels))
  const activeDomainLabel = computed(() => formatAiPreviewDomains(result.value?.domains, result.value?.domain, domainLabels))
  const resultHits = computed(() => Array.isArray(result.value?.hits) ? result.value.hits : [])

  const handlePreview = async ({ emptyQueryMessage, fallbackErrorMessage }) => {
    const query = form.query.trim()

    hasSubmitted.value = true
    errorMessage.value = ''
    result.value = null

    if (!query) {
      errorMessage.value = emptyQueryMessage
      return
    }

    previewing.value = true
    try {
      const res = await previewAiKnowledge({
        scenario: form.scenario,
        query
      })
      result.value = res?.data || buildEmptyAiPreviewResult({
        query,
        scenario: form.scenario,
        domain: selectedScenarioOption.value?.domain || ''
      })
    } catch (error) {
      errorMessage.value = extractAiErrorMessage(error, fallbackErrorMessage)
    } finally {
      previewing.value = false
    }
  }

  return {
    previewing,
    errorMessage,
    result,
    hasSubmitted,
    form,
    selectedScenarioOption,
    selectedScenarioDomainLabel,
    activeDomainLabel,
    resultHits,
    handlePreview
  }
}

/**
 * 将单个知识域转换成页面展示文案。
 *
 * @param {string|null|undefined} value 原始知识域
 * @param {Record<string, string>} domainLabels 知识域文案映射
 * @returns {string} 展示文案
 */
function resolveAiDomainLabel(value, domainLabels) {
  return domainLabels?.[value] || value || '未分类'
}
