import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { clearAiSession, createAiSession, streamAiChat, submitAiFeedback } from '@/shared/api/ai.js'
import {
  appendAssistantDelta,
  applyAssistantStartEvent,
  buildAssistantMessage,
  buildAssistantStreamingMessage,
  buildUserMessage,
  buildWelcomeMessage,
  cacheAiSessionId,
  clearCachedAiSessionId,
  finalizeAssistantMessage,
  readCachedAiSessionId
} from '@/shared/lib/ai-chat.js'

/**
 * AI 对话状态管理。
 */
export const useAiChatStore = defineStore('ai-chat', () => {
  const isOpen = ref(false)
  const loading = ref(false)
  const sessionId = ref('')
  const messages = ref([buildWelcomeMessage()])

  // 欢迎语之外出现过真实对话时，视为已有活跃消息。
  const hasActiveMessages = computed(() => messages.value.length > 1)

  // 仅返回最近一条已完成的助手消息 ID，供反馈按钮和引用定位使用。
  const latestAssistantMessageId = computed(() => {
    for (let index = messages.value.length - 1; index >= 0; index -= 1) {
      const item = messages.value[index]
      if (item?.role === 'assistant' && !item.pending && item.id) {
        return item.id
      }
    }
    return ''
  })

  /**
   * 确保当前存在可用会话。
   * <p>
   * 优先复用内存态和本地缓存中的会话 ID，只有都不存在时才向后端新建。
   *
   * @returns {Promise<string>}
   */
  async function ensureSession() {
    if (sessionId.value) {
      return sessionId.value
    }

    const cachedSessionId = readCachedAiSessionId()
    if (cachedSessionId) {
      sessionId.value = cachedSessionId
      return cachedSessionId
    }

    const response = await createAiSession()
    const createdSessionId = response?.data?.sessionId || ''
    if (!createdSessionId) {
      throw new Error('AI 会话创建失败，请稍后重试。')
    }

    sessionId.value = createdSessionId
    cacheAiSessionId(createdSessionId)
    return createdSessionId
  }

  /**
   * 打开聊天面板，并确保底层会话已准备好。
   *
   * @returns {Promise<void>}
   */
  async function openChat() {
    isOpen.value = true
    await ensureSession()
  }

  /**
   * 关闭聊天面板，不主动销毁会话。
   */
  function closeChat() {
    isOpen.value = false
  }

  /**
   * 清空当前会话，并立即准备一个新的空白会话。
   *
   * @returns {Promise<void>}
   */
  async function clearConversation() {
    const currentSessionId = sessionId.value || readCachedAiSessionId()
    if (currentSessionId) {
      await clearAiSession(currentSessionId)
    }

    sessionId.value = ''
    clearCachedAiSessionId()
    messages.value = [buildWelcomeMessage()]
    await ensureSession()
  }

  /**
   * 发送一条用户消息，并消费 AI 流式返回结果。
   *
   * @param {{ content: string, scenarioHint?: string, sourcePage?: string }} options 发送参数
   * @returns {Promise<object|null>}
   */
  async function sendMessage({ content, scenarioHint = '', sourcePage = '' }) {
    const trimmedContent = typeof content === 'string' ? content.trim() : ''
    if (!trimmedContent || loading.value) return null

    const currentSessionId = await ensureSession()
    const userMessage = buildUserMessage(trimmedContent)
    const streamingMessage = buildAssistantStreamingMessage()
    messages.value.push(userMessage, streamingMessage)
    loading.value = true

    try {
      await streamAiChat({
        sessionId: currentSessionId,
        message: trimmedContent,
        scenarioHint,
        sourcePage,
        clientTime: Date.now()
      }, {
        onStart: (payload) => {
          applyAssistantStartEvent(streamingMessage, payload)
          if (payload?.sessionId) {
            sessionId.value = payload.sessionId
            cacheAiSessionId(payload.sessionId)
          }
        },
        onDelta: (payload) => {
          appendAssistantDelta(streamingMessage, payload)
        },
        onDone: (payload) => {
          finalizeAssistantMessage(streamingMessage, payload)
        },
        onError: (payload) => {
          throw new Error(payload?.message || 'AI 服务暂时不可用，请稍后重试。')
        }
      })
      return streamingMessage
    } catch (error) {
      removeMessageById(streamingMessage.id)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 提交某条助手消息的反馈。
   *
   * @param {{ messageId: string, feedbackType: string, comment?: string }} options 反馈参数
   * @returns {Promise<void>}
   */
  async function markFeedback({ messageId, feedbackType, comment = '' }) {
    const targetMessage = messages.value.find((item) => item.id === messageId && item.role === 'assistant')
    if (!targetMessage || !targetMessage.feedbackEnabled || !sessionId.value) {
      return
    }

    await submitAiFeedback({
      messageId,
      sessionId: sessionId.value,
      feedbackType,
      comment
    })
    targetMessage.feedbackStatus = feedbackType
  }

  /**
   * 在本地追加一条非服务端返回的助手消息。
   *
   * @param {string} content 文本内容
   */
  function appendLocalAssistantMessage(content) {
    messages.value.push({
      ...buildAssistantMessage({ reply: content }),
      feedbackEnabled: false
    })
  }

  /**
   * 直接把推荐追问转成一次普通发送。
   *
   * @param {string} content 建议问题文本
   * @returns {Promise<object|null>}
   */
  function useSuggestion(content) {
    return sendMessage({ content })
  }

  /**
   * 从消息列表中移除指定消息。
   *
   * @param {string} messageId 消息 ID
   */
  function removeMessageById(messageId) {
    const index = messages.value.findIndex((item) => item.id === messageId)
    if (index !== -1) {
      messages.value.splice(index, 1)
    }
  }

  return {
    isOpen,
    loading,
    sessionId,
    messages,
    hasActiveMessages,
    latestAssistantMessageId,
    openChat,
    closeChat,
    clearConversation,
    sendMessage,
    markFeedback,
    appendLocalAssistantMessage,
    useSuggestion
  }
})
