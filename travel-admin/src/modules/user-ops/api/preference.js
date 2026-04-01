// 用户偏好相关接口
import request from '@/shared/api/request.js'

/**
 * 获取用户偏好列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getPreferenceList(params) {
  return request.get('/user-insights/preferences', { params })
}
