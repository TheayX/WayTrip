import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { clearAiSession, createAiSession, chatWithAi, submitAiFeedback } from '@/shared/api/ai.js'
import {
  buildAssistantMessage,
  buildAssistantPendingMessage,
  buildUserMessage,
  buildWelcomeMessage,
  cacheAiSessionId,
  clearCachedAiSessionId,
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

  const hasActiveMessages = computed(() => messages.value.length > 1)

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

  async function openChat() {
    isOpen.value = true
    await ensureSession()
  }

  function closeChat() {
    isOpen.value = false
  }

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

  async function sendMessage({ content, scenarioHint = '', sourcePage = '' }) {
    const trimmedContent = typeof content === 'string' ? content.trim() : ''
    if (!trimmedContent || loading.value) return null

    const currentSessionId = await ensureSession()
    const userMessage = buildUserMessage(trimmedContent)
    const pendingMessage = buildAssistantPendingMessage()
    messages.value.push(userMessage, pendingMessage)
    loading.value = true

    try {
      const response = await chatWithAi({
        sessionId: currentSessionId,
        message: trimmedContent,
        scenarioHint,
        sourcePage,
        clientTime: Date.now()
      })
      const assistantMessage = buildAssistantMessage(response?.data)
      replacePendingMessage(pendingMessage.id, assistantMessage)
      return assistantMessage
    } catch (error) {
      removeMessageById(pendingMessage.id)
      throw error
    } finally {
      loading.value = false
    }
  }

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

  function appendLocalAssistantMessage(content) {
    messages.value.push({
      ...buildAssistantMessage({ reply: content }),
      feedbackEnabled: false
    })
  }

  function useSuggestion(content) {
    return sendMessage({ content })
  }

  function replacePendingMessage(messageId, nextMessage) {
    const index = messages.value.findIndex((item) => item.id === messageId)
    if (index === -1) {
      messages.value.push(nextMessage)
      return
    }
    messages.value.splice(index, 1, nextMessage)
  }

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
    openChat,
    closeChat,
    clearConversation,
    sendMessage,
    markFeedback,
    appendLocalAssistantMessage,
    useSuggestion
  }
})
