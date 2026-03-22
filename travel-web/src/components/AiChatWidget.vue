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
import { chatWithAi } from '@/api/ai'

const SESSION_STORAGE_KEY = 'waytrip_ai_session_id'

const isOpen = ref(false)
const loading = ref(false)
const inputText = ref('')
const messageListRef = ref(null)
const sessionId = ref(getOrCreateSessionId())
const messages = ref([buildWelcomeMessage()])

function buildWelcomeMessage() {
  return { role: 'assistant', content: '你好，我是 WayTrip AI 客服。你可以问我景点、攻略、订单相关问题。' }
}

function getOrCreateSessionId() {
  const cached = localStorage.getItem(SESSION_STORAGE_KEY)
  if (cached) return cached
  const created = createSessionId()
  localStorage.setItem(SESSION_STORAGE_KEY, created)
  return created
}

function createSessionId() {
  return `web_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

function resetSessionId() {
  const created = createSessionId()
  localStorage.setItem(SESSION_STORAGE_KEY, created)
  sessionId.value = created
}

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

const sendMessage = async () => {
  const content = inputText.value.trim()
  if (!content || loading.value) return

  messages.value.push({ role: 'user', content })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await chatWithAi(sessionId.value, content)
    const reply = res?.data?.reply || '抱歉，暂时没有可用回复。'
    messages.value.push({ role: 'assistant', content: reply })
  } catch {
    messages.value.push({ role: 'assistant', content: '抱歉，AI 服务暂不可用，请稍后再试。' })
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

