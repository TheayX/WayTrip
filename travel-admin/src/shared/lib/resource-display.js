import { USER_DISPLAY_TEXT } from '@/shared/constants/resource-display.js'

// 用户主体失效时统一降级到规范文案，避免页面层重复写同样的兜底判断。
export const resolveUserDisplayName = (nickname) => {
  return nickname || USER_DISPLAY_TEXT.DEACTIVATED
}

// 当前约定下，只要展示昵称落到“已注销用户”，页面就不再提供跳转能力。
export const isDeactivatedUserDisplay = (nickname) => {
  return resolveUserDisplayName(nickname) === USER_DISPLAY_TEXT.DEACTIVATED
}
