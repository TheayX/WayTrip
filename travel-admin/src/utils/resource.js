const FALLBACK_API_ORIGIN = 'http://localhost:8080'

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

export const getAdminUploadUrl = (type = 'image') => `${API_ORIGIN}/api/admin/v1/upload/${type}`

export const getResourceUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return `${API_ORIGIN}${url.startsWith('/') ? url : `/${url}`}`
}
