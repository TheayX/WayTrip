import { ADMIN_DISPLAY_TEXT, COMMON_DISPLAY_TEXT, SPOT_DISPLAY_TEXT, USER_DISPLAY_TEXT } from '@/shared/constants/resource-display.js'

// 用户主体失效时统一降级到规范文案，避免页面层重复写同样的兜底判断。
export const resolveUserDisplayName = (nickname) => {
  return nickname || USER_DISPLAY_TEXT.DEACTIVATED
}

// 当前约定下，只要展示昵称落到“已注销用户”，页面就不再提供跳转能力。
export const isDeactivatedUserDisplay = (nickname) => {
  return resolveUserDisplayName(nickname) === USER_DISPLAY_TEXT.DEACTIVATED
}

// 景点名称展示统一走后端返回语义，前端只负责空值兜底。
export const resolveSpotDisplayName = (spotName) => {
  return spotName || COMMON_DISPLAY_TEXT.EMPTY
}

// 已失效景点统一禁用跳转，避免列表继续把管理员带到无效详情。
export const isInvalidSpotDisplay = (spotName) => {
  const displayName = resolveSpotDisplayName(spotName)
  return [
    SPOT_DISPLAY_TEXT.OFFLINE,
    SPOT_DISPLAY_TEXT.DELETED,
    SPOT_DISPLAY_TEXT.PURGED,
    SPOT_DISPLAY_TEXT.UNKNOWN
  ].includes(displayName)
}

// 需要根据完整景点对象推导展示语义的场景，统一在这里收口，避免组件重复拼接括号文案。
export const resolveSpotRecordDisplayName = (spot) => {
  if (!spot) {
    return SPOT_DISPLAY_TEXT.PURGED
  }
  if (Number(spot.isDeleted) === 1) {
    return SPOT_DISPLAY_TEXT.DELETED
  }
  if (Number(spot.published) !== 1) {
    return SPOT_DISPLAY_TEXT.OFFLINE
  }
  return resolveSpotDisplayName(spot.name)
}

// 内容资源当前先统一空值兜底，后续接入文章/攻略失效态时继续复用。
export const resolveContentDisplayText = (content) => {
  return content || COMMON_DISPLAY_TEXT.EMPTY
}

// 后台内容创建者属于运营主体，缺失时不再裸露技术占位值。
export const resolveAdminDisplayName = (adminName) => {
  return adminName || ADMIN_DISPLAY_TEXT.UNKNOWN
}
