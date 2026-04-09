// 后台资源地址辅助方法
const FALLBACK_API_ORIGIN = 'http://localhost:8080'

// 开发环境优先走显式配置，缺失时再退回本地后端默认地址。
const getApiOrigin = () => {
  if (import.meta.env.VITE_API_ORIGIN) {
    return import.meta.env.VITE_API_ORIGIN.replace(/\/$/, '')
  }

  if (import.meta.env.DEV) {
    return FALLBACK_API_ORIGIN
  }

  if (typeof window !== 'undefined') {
    return window.location.origin
  }

  return FALLBACK_API_ORIGIN
}

const API_ORIGIN = getApiOrigin()

// 上传地址按资源类型拼接，便于图片、文件等上传接口复用。
export const getAdminUploadUrl = (type = 'image') => `${API_ORIGIN}/api/admin/v1/upload/${type}`

// 相对资源地址统一拼成完整 URL，避免页面层手写域名和斜杠处理。
export const getResourceUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return `${API_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`
}
