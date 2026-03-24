import request from '@/utils/request'

// 获取推荐算法配置
export function getRecommendationConfig() {
  return request.get('/recommendation/config')
}

// 更新推荐算法配置
export function updateRecommendationConfig(data) {
  return request.put('/recommendation/config', data)
}

// 获取推荐引擎运行状态
export function getRecommendationStatus() {
  return request.get('/recommendation/status')
}

// 手动更新相似度矩阵
export function updateRecommendationMatrix() {
  return request.post('/recommendation/update-matrix')
}
