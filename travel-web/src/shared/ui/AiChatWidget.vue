<template>
  <div class="ai-chat-widget">
    <button v-if="!isOpen" class="chat-launcher" @click="openChat">
      <el-icon class="launcher-icon"><Service /></el-icon>
      AI 助手
    </button>

    <div v-else class="chat-panel">
      <div class="chat-header">
        <div class="chat-header-left">
          <div class="chat-avatar">
            <el-icon><Monitor /></el-icon>
          </div>
          <div>
            <div class="chat-title">WayTrip AI 助手</div>
            <div class="chat-subtitle">推荐、规划、订单与规则问答</div>
          </div>
        </div>
        <div class="chat-actions">
          <button class="icon-btn" :disabled="loading" @click="clearMessages" title="清空对话">
            <el-icon><Delete /></el-icon>
          </button>
          <button class="icon-btn" @click="closeChat" title="关闭">
            <el-icon><Close /></el-icon>
          </button>
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

            <div v-if="item.role === 'assistant' && item.citations?.length" class="assistant-meta">
              <div class="meta-title">
                <el-icon><Document /></el-icon>
                知识参考
              </div>
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
              <div class="meta-title">
                <el-icon><ChatLineSquare /></el-icon>
                你也可以继续问
              </div>
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

        <div v-if="loading" class="typing-hint">
          <div class="typing-dots">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
          正在生成回复，请稍候...
        </div>
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
        <el-button class="send-btn" type="primary" :loading="loading" @click="sendMessage">
          <template #icon v-if="!loading">
            <el-icon><Position /></el-icon>
          </template>
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Bottom, Top, Service, Monitor, 
  Delete, Close, Position, Document,
  ChatLineSquare
} from '@element-plus/icons-vue'
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
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  border-radius: 999px;
  padding: 14px 24px;
  background: linear-gradient(135deg, #0f766e, #0ea5e9);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.3);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
}

.chat-launcher:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 32px rgba(14, 165, 233, 0.4);
}

.launcher-icon {
  font-size: 20px;
}

.chat-panel {
  width: 480px;
  height: 720px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid rgba(203, 213, 225, 0.6);
  border-radius: 24px;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.16), 0 12px 24px rgba(15, 23, 42, 0.08);
  overflow: hidden;
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: bottom right;
}

@keyframes slideUp {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.chat-header {
  min-height: 80px;
  background: linear-gradient(135deg, #0f766e, #0369a1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.chat-title {
  font-weight: 600;
  font-size: 16px;
  letter-spacing: 0.5px;
}

.chat-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  border: none;
  background: rgba(255, 255, 255, 0.1);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.05);
}

.icon-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  transform: none;
}

.message-list {
  flex: 1;
  padding: 24px 20px;
  background: #f8fafc;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-list::-webkit-scrollbar {
  width: 6px;
}
.message-list::-webkit-scrollbar-thumb {
  background: rgba(203, 213, 225, 0.8);
  border-radius: 3px;
}
.message-list::-webkit-scrollbar-track {
  background: transparent;
}

.message-item {
  display: flex;
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-item.user {
  justify-content: flex-end;
}

.message-item.assistant {
  justify-content: flex-start;
}

.message-card {
  max-width: 85%;
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 18px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-item.assistant .message-bubble {
  background: #fff;
  color: #1e293b;
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-bottom-left-radius: 4px;
}

.assistant-meta {
  margin-top: 12px;
  padding-left: 4px;
}

.meta-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
}

.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestion-chip {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(14, 165, 233, 0.3);
  background: rgba(240, 249, 255, 0.8);
  color: #0369a1;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.suggestion-chip:hover {
  background: rgba(14, 165, 233, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.1);
}

.suggestion-chip:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
  box-shadow: none;
}

.citation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.citation-card {
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  transition: box-shadow 0.2s;
}

.citation-card:hover {
  box-shadow: 0 4px 12px rgba(148, 163, 184, 0.1);
}

.citation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.citation-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.citation-type {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #e0f2fe;
  color: #0284c7;
  font-weight: 500;
}

.citation-snippet {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feedback-row {
  margin-top: 12px;
  padding-left: 4px;
  display: flex;
  gap: 10px;
}

.feedback-btn {
  height: 32px;
  padding: 0 14px;
  border: 1px solid rgba(203, 213, 225, 0.8);
  border-radius: 999px;
  background: #fff;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.feedback-btn:hover:not(:disabled) {
  background: #f8fafc;
  color: #475569;
}

.feedback-btn.active {
  border-color: rgba(14, 165, 233, 0.5);
  background: #e0f2fe;
  color: #0284c7;
}

.feedback-btn:disabled {
  cursor: not-allowed;
}

.typing-hint {
  padding: 8px 12px;
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 10px;
}

.typing-dots {
  display: inline-flex;
  gap: 4px;
}

.dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #94a3b8;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.chat-input {
  padding: 16px 20px;
  border-top: 1px solid rgba(226, 232, 240, 0.8);
  display: flex;
  gap: 12px;
  align-items: flex-end;
  background: #fff;
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}

.chat-input :deep(.el-input),
.chat-input :deep(.el-textarea) {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  min-height: 48px !important;
  border-radius: 20px;
  padding: 12px 16px;
  font-size: 14px;
  background: #f1f5f9;
  border-color: transparent;
  box-shadow: none;
  transition: all 0.2s ease;
  line-height: 1.5;
}

.chat-input :deep(.el-textarea__inner):focus {
  background: #fff;
  box-shadow: 0 0 0 1px #0ea5e9 inset;
}

.send-btn {
  height: 48px;
  border-radius: 24px;
  padding: 0 24px;
  font-weight: 600;
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  border: none;
  transition: opacity 0.2s, transform 0.2s;
  flex-shrink: 0;
}

.send-btn:hover {
  opacity: 0.95;
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .ai-chat-widget {
    right: 12px;
    bottom: 12px;
  }

  .chat-panel {
    width: calc(100vw - 24px);
    height: 75vh;
    border-radius: 20px;
  }

  .chat-header {
    padding: 14px 16px;
  }

  .message-card {
    max-width: 90%;
  }

  .chat-input {
    padding: 12px 16px;
  }
}
</style>