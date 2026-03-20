import request from '@/utils/request'

export const chatWithAi = (message) => request.post('/ai/chat', { message })

