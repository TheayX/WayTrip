<template>
  <div class="ai-chat-widget">
    <button v-if="!isOpen" class="chat-launcher" @click="openChat">
      AI客服
    </button>

    <div v-else class="chat-panel">
      <div class="chat-header">
        <div class="chat-title">WayTrip AI客服</div>
        <div class="chat-actions">
          <button class="text-btn" @click="clearMessages">清空</button>
          <button class="text-btn" @click="closeChat">关闭</button>
        </div>
      </div>

      <div ref="messageListRef" class="message-list">
        <div
          v-for="(item, index) in messages"
          :key="`${item.role}-${index}`"
          class="message-item"
          :class="item.role"
        >
          <div class="message-bubble">{{ item.content }}</div>
        </div>

        <div v-if="loading" class="message-item assistant">
          <div class="message-bubble">正在输入...</div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputText"
          placeholder="输入问题，按 Enter 发送"
          :disabled="loading"
          @keyup.enter="sendMessage"
        />
        <el-button type="primary" :loading="loading" @click="sendMessage">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatWithAi } from '@/shared/api/ai.js'
import {
  AI_CHAT_SESSION_STORAGE_KEY,
  buildWelcomeMessage,
  createSessionId,
  getReplyContent,
  isValidSessionId,
  resolveAiErrorMessage
} from '@/shared/lib/ai-chat.js'

// 组件状态
const isOpen = ref(false)
const loading = ref(false)
const inputText = ref('')
const messageListRef = ref(null)
// 会话 ID 在组件初始化时就准备好，避免首次发送时再生成导致状态分叉。
const sessionId = ref(getOrCreateSessionId())
const messages = ref([buildWelcomeMessage()])

function getOrCreateSessionId() {
  const cached = localStorage.getItem(AI_CHAT_SESSION_STORAGE_KEY)
  // 优先复用当前浏览器内已有会话，保证刷新页面后还能延续短期上下文。
  if (cached) return cached
  const created = createSessionId()
  localStorage.setItem(AI_CHAT_SESSION_STORAGE_KEY, created)
  return created
}

function resetSessionId() {
  // 清空会话时同时重建 sessionId，避免旧上下文继续影响后续问答。
  const created = createSessionId()
  localStorage.setItem(AI_CHAT_SESSION_STORAGE_KEY, created)
  sessionId.value = created
}

function ensureSessionId() {
  if (isValidSessionId(sessionId.value)) return
  resetSessionId()
}

// 视图交互方法
const scrollToBottom = () => {
  nextTick(() => {
    if (!messageListRef.value) return
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  })
}

const openChat = () => {
  isOpen.value = true
  scrollToBottom()
}

const closeChat = () => {
  isOpen.value = false
}

const clearMessages = () => {
  resetSessionId()
  messages.value = [buildWelcomeMessage()]
  scrollToBottom()
}

// 消息发送主链路
const sendMessage = async () => {
  const content = inputText.value.trim()
  if (!content || loading.value) return

  ensureSessionId()

  messages.value.push({ role: 'user', content })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const firstResponse = await chatWithAi(sessionId.value, content)
    let reply = getReplyContent(firstResponse)

    // 空回复时做一次轻量重试，减少模型偶发空内容时的直接失败感。
    if (!reply) {
      const retryResponse = await chatWithAi(sessionId.value, content)
      reply = getReplyContent(retryResponse)
    }

    messages.value.push({
      role: 'assistant',
      content: reply || '抱歉，本次回复为空，请换个问法或稍后再试。'
    })
  } catch (error) {
    const message = resolveAiErrorMessage(error)
    messages.value.push({ role: 'assistant', content: message })
    ElMessage.warning(message)
  } finally {
    loading.value = false
    scrollToBottom()
  }
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
  padding: 12px 18px;
  background: #409eff;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.35);
}

.chat-panel {
  width: 360px;
  height: 520px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.16);
  overflow: hidden;
}

.chat-header {
  height: 54px;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
}

.chat-title {
  font-weight: 600;
  font-size: 14px;
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

.message-list {
  flex: 1;
  padding: 12px;
  background: #f7f9fc;
  overflow-y: auto;
}

.message-item {
  display: flex;
  margin-bottom: 10px;
}

.message-item.user {
  justify-content: flex-end;
}

.message-item.assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.45;
}

.message-item.user .message-bubble {
  background: #409eff;
  color: #fff;
}

.message-item.assistant .message-bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #e4e7ed;
}

.chat-input {
  padding: 10px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 8px;
  align-items: center;
  background: #fff;
}

.chat-input :deep(.el-input) {
  flex: 1;
}

@media (max-width: 768px) {
  .ai-chat-widget {
    right: 12px;
    bottom: 12px;
  }

  .chat-panel {
    width: calc(100vw - 24px);
    height: 66vh;
  }
}
</style>

