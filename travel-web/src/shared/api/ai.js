import request from '@/shared/api/client.js'

/**
 * 调用 AI 客服对话接口。
 *
 * @param {string} sessionId 会话 ID
 * @param {string} message 用户消息
 * @returns {Promise<any>} AI 客服回复结果
 */
export const chatWithAi = (sessionId, message) => request.post(
  '/ai/chat',
  // 会话 ID 与消息体一起上送，服务端依赖它串联历史对话与分钟级限流。
  { sessionId, message },
  { timeout: 60000 }
)

