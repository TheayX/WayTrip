import request from '@/shared/api/client.js'

export const chatWithAi = (sessionId, message) => request.post('/ai/chat', { sessionId, message })
