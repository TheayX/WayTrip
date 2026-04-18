import request from '@/shared/api/client.js'
import { useUserStore } from '@/modules/account/store/user.js'

// AI 会话接口基础路径。
const AI_SESSION_BASE_PATH = '/ai/sessions'

// AI 对话接口路径。
const AI_CHAT_PATH = '/ai/chat'

// AI 反馈接口路径。
const AI_FEEDBACK_PATH = '/ai/feedback'

/**
 * 创建新的 AI 会话。
 *
 * @returns {Promise<any>} 会话创建结果
 */
export const createAiSession = () => request.post(AI_SESSION_BASE_PATH)

/**
 * 清空指定 AI 会话。
 *
 * @param {string} sessionId 会话 ID
 * @returns {Promise<any>} 清空结果
 */
export const clearAiSession = (sessionId) => request.delete(`${AI_SESSION_BASE_PATH}/${sessionId}`)

/**
 * 调用 AI 对话流式接口。
 *
 * @param {Object} payload 对话请求参数
 * @param {Object} handlers 事件回调
 * @returns {Promise<void>}
 */
export const streamAiChat = async (payload, handlers = {}) => {
  const userStore = useUserStore()
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), 120000)

  try {
    const response = await fetch(`/api/v1${AI_CHAT_PATH}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {})
      },
      body: JSON.stringify(payload),
      signal: controller.signal
    })

    if (!response.ok || !response.body) {
      throw await buildStreamRequestError(response)
    }

    await consumeSseStream(response.body, handlers)
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new Error('AI 响应超时，请稍后重试。')
    }
    throw error
  } finally {
    window.clearTimeout(timeoutId)
  }
}

/**
 * 提交 AI 回复反馈。
 *
 * @param {Object} payload 反馈请求参数
 * @returns {Promise<any>} 反馈结果
 */
export const submitAiFeedback = (payload) => request.post(AI_FEEDBACK_PATH, payload)

/**
 * 消费后端返回的 SSE 文本流。
 *
 * @param {ReadableStream<Uint8Array>} stream 文本流
 * @param {Object} handlers 事件回调
 * @returns {Promise<void>}
 */
async function consumeSseStream(stream, handlers) {
  const reader = stream.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      if (buffer.trim()) {
        dispatchSseBlock(buffer, handlers)
      }
      break
    }

    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() || ''
    blocks.forEach((block) => dispatchSseBlock(block, handlers))
  }
}

/**
 * 解析单个 SSE 事件块并分发到对应回调。
 *
 * @param {string} block 事件块
 * @param {Object} handlers 事件回调
 */
function dispatchSseBlock(block, handlers) {
  const trimmedBlock = typeof block === 'string' ? block.trim() : ''
  if (!trimmedBlock) return

  const lines = trimmedBlock.split('\n')
  let eventName = 'message'
  const dataLines = []

  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
      return
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  })

  const rawData = dataLines.join('\n')
  const payload = rawData ? JSON.parse(rawData) : null
  // 这里记录浏览器实际收到 SSE 事件的时间点，便于区分“后端已发出”和“前端何时收到”。
  console.debug('[WayTrip AI SSE] event received', {
    eventName,
    receivedAt: Date.now(),
    payload
  })
  const handlerMap = {
    start: handlers.onStart,
    delta: handlers.onDelta,
    done: handlers.onDone,
    error: handlers.onError
  }
  const handler = handlerMap[eventName]
  if (typeof handler === 'function') {
    handler(payload)
  }
}

/**
 * 将流式接口的非 2xx 响应转换成统一异常。
 *
 * @param {Response} response 原始响应
 * @returns {Promise<Error>}
 */
async function buildStreamRequestError(response) {
  try {
    const result = await response.json()
    return new Error(result?.message || 'AI 服务暂时不可用，请稍后重试。')
  } catch {
    return new Error(response.status === 401 || response.status === 403
      ? '当前登录状态无法使用 AI 客服，请重新登录后再试。'
      : 'AI 服务暂时不可用，请稍后重试。')
  }
}

