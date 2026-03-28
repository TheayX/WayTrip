import { post } from '@/utils/request'

// /auth/* 仅用于登录注册和微信绑定流程。

/**
 * 微信登录
 */
export const wxLogin = (code) => {
  return post('/auth/wx-login', { code })
}

/**
 * 小程序端绑定手机号（匹配已有账户则合并openid）
 */
export const wxBindPhone = (data) => {
  return post('/auth/wx-bind-phone', data)
}

/**
 * 小程序端第一步校验手机号密码
 */
export const prepareWxBindPhone = (data) => {
  return post('/auth/wx-prepare-bind-phone', data)
}

