import request from '@/utils/request'

export const chatWithAi = (sessionId, message) => request.post('/ai/chat', { sessionId, message })
