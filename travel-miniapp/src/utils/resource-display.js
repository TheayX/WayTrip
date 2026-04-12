// 用户端资源失效和空值展示统一收口，避免小程序页面散落硬编码。
export const MINIAPP_RESOURCE_DISPLAY = {
  user: {
    unknown: '未知用户'
  },
  spot: {
    offline: '已下架景点',
    deleted: '已删除景点',
    purged: '已清除景点',
    unknown: '未知景点'
  },
  guide: {
    unknown: '未知攻略'
  }
}

// 用户端景点失效语义统一弱化成“未知景点”，避免页面暴露后台治理术语。
export const MINIAPP_INVALID_SPOT_NAMES = [
  MINIAPP_RESOURCE_DISPLAY.spot.offline,
  MINIAPP_RESOURCE_DISPLAY.spot.deleted,
  MINIAPP_RESOURCE_DISPLAY.spot.purged,
  MINIAPP_RESOURCE_DISPLAY.spot.unknown
]

export const resolveMiniappGuideDisplayText = (value) => value || MINIAPP_RESOURCE_DISPLAY.guide.unknown

export const resolveMiniappGuideCategory = (value) => value || MINIAPP_RESOURCE_DISPLAY.guide.unknown

export const resolveMiniappUserDisplayName = (value) => value || MINIAPP_RESOURCE_DISPLAY.user.unknown

export const resolveMiniappSpotDisplayName = (spotName, emptyText = '--') => {
  return MINIAPP_INVALID_SPOT_NAMES.includes(spotName) ? MINIAPP_RESOURCE_DISPLAY.spot.unknown : (spotName || emptyText)
}

export const isMiniappInvalidSpotDisplay = (spotName) => {
  return MINIAPP_INVALID_SPOT_NAMES.includes(resolveMiniappSpotDisplayName(spotName))
}
