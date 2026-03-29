// 首页相关接口
import request from '@/utils/request'

/**
 * 获取首页轮播图
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getBanners = () => request.get('/home/banners')

/**
 * 获取热门景点
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getHotSpots = (limit = 10) => request.get('/home/hot', { params: { limit } })

/**
 * 获取推荐景点
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getRecommendations = (limit = 10) => request.get('/recommendations', { params: { limit } })

/**
 * 刷新推荐景点
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const refreshRecommendations = (limit = 10) => request.post('/recommendations/refresh', null, { params: { limit } })

