<template>
  <div class="ai-chat-widget">
    <button v-if="!isOpen" class="chat-launcher" @click="openChat">
      AI 助手
    </button>

    <div v-else class="chat-panel">
      <div class="chat-header">
        <div>
          <div class="chat-title">WayTrip AI 助手</div>
          <div class="chat-subtitle">推荐、规划、订单与规则问答</div>
        </div>
        <div class="chat-actions">
          <button class="text-btn" :disabled="loading" @click="clearMessages">清空</button>
          <button class="text-btn" @click="closeChat">关闭</button>
        </div>
      </div>

      <div ref="messageListRef" class="message-list">
        <div
          v-for="(item, index) in messages"
          :key="item.id || `${item.role}-${index}`"
          class="message-item"
          :class="item.role"
        >
          <div class="message-card">
            <div class="message-bubble">
              <template v-if="item.pending">正在分析并查询数据...</template>
              <template v-else>{{ item.content }}</template>
            </div>

            <div v-if="item.role === 'assistant' && item.toolCalls?.length" class="assistant-meta">
              <div class="meta-title">本次调用的能力</div>
              <div class="meta-list">
                <div
                  v-for="tool in item.toolCalls"
                  :key="`${item.id}-${tool.toolName}-${tool.summary}`"
                  class="meta-chip"
                  :class="{ danger: tool.success === false }"
                >
                  <el-icon><Operation /></el-icon>
                  <span>{{ tool.summary || tool.toolName }}</span>
                </div>
              </div>
            </div>

            <div v-if="item.role === 'assistant' && item.citations?.length" class="assistant-meta">
              <div class="meta-title">知识参考</div>
              <div class="citation-list">
                <div
                  v-for="citation in item.citations"
                  :key="`${item.id}-${citation.title}-${citation.sourceRef}`"
                  class="citation-card"
                >
                  <div class="citation-head">
                    <span class="citation-title">{{ citation.title || '参考资料' }}</span>
                    <span class="citation-type">{{ citation.sourceType || '知识库' }}</span>
                  </div>
                  <div v-if="citation.snippet" class="citation-snippet">{{ citation.snippet }}</div>
                </div>
              </div>
            </div>

            <div v-if="item.role === 'assistant' && item.suggestions?.length" class="assistant-meta">
              <div class="meta-title">你也可以继续问</div>
              <div class="suggestion-list">
                <button
                  v-for="suggestion in item.suggestions"
                  :key="`${item.id}-${suggestion}`"
                  type="button"
                  class="suggestion-chip"
                  :disabled="loading"
                  @click="sendSuggestion(suggestion)"
                >
                  {{ suggestion }}
                </button>
              </div>
            </div>

            <div v-if="item.role === 'assistant' && item.feedbackEnabled && !item.pending" class="feedback-row">
              <button
                type="button"
                class="feedback-btn"
                :class="{ active: item.feedbackStatus === 'UPVOTE' }"
                :disabled="item.feedbackStatus === 'UPVOTE'"
                @click="submitFeedback(item.id, 'UPVOTE')"
              >
                <el-icon><Top /></el-icon>
                <span>有帮助</span>
              </button>
              <button
                type="button"
                class="feedback-btn"
                :class="{ active: item.feedbackStatus === 'DOWNVOTE' }"
                :disabled="item.feedbackStatus === 'DOWNVOTE'"
                @click="submitFeedback(item.id, 'DOWNVOTE')"
              >
                <el-icon><Bottom /></el-icon>
                <span>不准确</span>
              </button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="typing-hint">正在生成回复，请稍候...</div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputText"
          :autosize="{ minRows: 1, maxRows: 4 }"
          type="textarea"
          resize="none"
          placeholder="输入问题，支持景点推荐、行程规划、订单咨询"
          :disabled="loading"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <el-button type="primary" :loading="loading" @click="sendMessage">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bottom, Operation, Top } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { resolveAiErrorMessage } from '@/shared/lib/ai-chat.js'
import { useAiChatStore } from '@/shared/store/ai-chat.js'

const route = useRoute()
const aiChatStore = useAiChatStore()
const { isOpen, loading, messages } = storeToRefs(aiChatStore)

const inputText = ref('')
const messageListRef = ref(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (!messageListRef.value) return
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  })
}

const openChat = async () => {
  try {
    await aiChatStore.openChat()
    scrollToBottom()
  } catch (error) {
    ElMessage.warning(resolveAiErrorMessage(error))
  }
}

const closeChat = () => {
  aiChatStore.closeChat()
}

const clearMessages = async () => {
  try {
    await aiChatStore.clearConversation()
    scrollToBottom()
  } catch (error) {
    const message = resolveAiErrorMessage(error)
    ElMessage.warning(message)
  }
}

const sendMessage = async () => {
  const content = inputText.value.trim()
  if (!content || loading.value) return
  inputText.value = ''
  scrollToBottom()

  try {
    await aiChatStore.sendMessage({
      content,
      sourcePage: normalizeSourcePage(),
      scenarioHint: resolveScenarioHint()
    })
  } catch (error) {
    const message = resolveAiErrorMessage(error)
    aiChatStore.appendLocalAssistantMessage(message)
    ElMessage.warning(message)
  } finally {
    scrollToBottom()
  }
}

const sendSuggestion = async (suggestion) => {
  inputText.value = suggestion
  await sendMessage()
}

const submitFeedback = async (messageId, feedbackType) => {
  try {
    await aiChatStore.markFeedback({ messageId, feedbackType })
    ElMessage.success(feedbackType === 'UPVOTE' ? '感谢你的反馈' : '已记录这条问题')
  } catch (error) {
    ElMessage.warning(resolveAiErrorMessage(error))
  }
}

function normalizeSourcePage() {
  return typeof route.name === 'string' ? route.name : route.path
}

function resolveScenarioHint() {
  if (route.path.includes('/orders')) {
    return 'order'
  }
  if (route.path.includes('/spots') || route.path.includes('/guides')) {
    return 'travel'
  }
  return ''
}
</script>

<style lang="scss" scoped>
.ai-chat-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1200;
}

.chat-launcher {
  border: none;
  border-radius: 999px;
  padding: 13px 20px;
  background: linear-gradient(135deg, #0f766e, #0ea5e9);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 16px 34px rgba(14, 165, 233, 0.28);
}

.chat-panel {
  width: 400px;
  height: 600px;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid rgba(203, 213, 225, 0.88);
  border-radius: 24px;
  box-shadow: 0 28px 50px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.chat-header {
  min-height: 72px;
  background: linear-gradient(135deg, #0f766e, #0369a1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
}

.chat-title {
  font-weight: 600;
  font-size: 15px;
}

.chat-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.text-btn {
  border: none;
  background: transparent;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.text-btn:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.message-list {
  flex: 1;
  padding: 16px;
  background:
    radial-gradient(circle at top, rgba(125, 211, 252, 0.16), transparent 34%),
    linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  overflow-y: auto;
}

.message-item {
  display: flex;
  margin-bottom: 14px;
}

.message-item.user {
  justify-content: flex-end;
}

.message-item.assistant {
  justify-content: flex-start;
}

.message-card {
  max-width: 88%;
}

.message-bubble {
  padding: 12px 14px;
  border-radius: 16px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: #fff;
  border-bottom-right-radius: 6px;
}

.message-item.assistant .message-bubble {
  background: #fff;
  color: #1e293b;
  border: 1px solid rgba(226, 232, 240, 0.96);
  border-bottom-left-radius: 6px;
  box-shadow: 0 16px 28px rgba(148, 163, 184, 0.12);
}

.assistant-meta {
  margin-top: 8px;
}

.meta-title {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.meta-list,
.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-chip,
.suggestion-chip {
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(186, 230, 253, 0.96);
  background: rgba(240, 249, 255, 0.96);
  color: #0f172a;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.meta-chip.danger {
  border-color: rgba(254, 205, 211, 0.96);
  background: rgba(255, 241, 242, 0.96);
}

.suggestion-chip {
  cursor: pointer;
}

.suggestion-chip:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.citation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.citation-card {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.96);
}

.citation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.citation-title {
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
}

.citation-type {
  font-size: 11px;
  color: #0369a1;
}

.citation-snippet {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #475569;
}

.feedback-row {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.feedback-btn {
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(203, 213, 225, 0.92);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  color: #475569;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.feedback-btn.active {
  border-color: rgba(14, 165, 233, 0.96);
  background: rgba(224, 242, 254, 0.96);
  color: #0369a1;
}

.feedback-btn:disabled {
  cursor: not-allowed;
}

.typing-hint {
  padding-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.chat-input {
  padding: 12px;
  border-top: 1px solid rgba(226, 232, 240, 0.92);
  display: flex;
  gap: 8px;
  align-items: flex-end;
  background: #fff;
}

.chat-input :deep(.el-input) {
  flex: 1;
}

.chat-input :deep(.el-textarea) {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  min-height: 44px;
  border-radius: 16px;
  padding-top: 11px;
  padding-bottom: 11px;
}

@media (max-width: 768px) {
  .ai-chat-widget {
    right: 12px;
    bottom: 12px;
  }

  .chat-panel {
    width: calc(100vw - 24px);
    height: 72vh;
  }

  .chat-header {
    align-items: flex-start;
  }

  .message-card {
    max-width: 100%;
  }
}
</style>

