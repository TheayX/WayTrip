// 推荐算法相关接口
import request from '@/shared/api/request.js'

/**
 * 获取推荐算法配置
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getRecommendationConfig() {
  return request.get('/recommendation/config')
}

/**
 * 更新推荐算法配置
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updateRecommendationConfig(data) {
  return request.put('/recommendation/config', data)
}

/**
 * 获取推荐算法状态
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getRecommendationStatus() {
  return request.get('/recommendation/status')
}

/**
 * 触发更新推荐矩阵
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updateRecommendationMatrix() {
  return request.post('/recommendation/update-matrix')
}

/**
 * 预览推荐结果
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function previewRecommendations(params) {
  return request.get('/recommendation/preview', {
    params: {
      ...params,
      _ts: Date.now()
    }
  })
}

/**
 * 预览相似度邻居
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function previewSimilarityNeighbors(params) {
  return request.get('/recommendation/similarity-preview', {
    params: {
      ...params,
      _ts: Date.now()
    }
  })
}
