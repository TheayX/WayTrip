// 用户端资源失效和空值展示统一收口，避免页面散落硬编码。
export const WEB_RESOURCE_DISPLAY = {
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
export const WEB_INVALID_SPOT_NAMES = [
  WEB_RESOURCE_DISPLAY.spot.offline,
  WEB_RESOURCE_DISPLAY.spot.deleted,
  WEB_RESOURCE_DISPLAY.spot.purged,
  WEB_RESOURCE_DISPLAY.spot.unknown
]

export const resolveWebGuideDisplayText = (value) => value || WEB_RESOURCE_DISPLAY.guide.unknown

export const resolveWebGuideCategory = (value) => value || WEB_RESOURCE_DISPLAY.guide.unknown

export const resolveWebUserDisplayName = (value) => value || WEB_RESOURCE_DISPLAY.user.unknown

export const resolveWebSpotDisplayName = (spotName, emptyText = '--') => {
  return WEB_INVALID_SPOT_NAMES.includes(spotName) ? WEB_RESOURCE_DISPLAY.spot.unknown : (spotName || emptyText)
}

export const isWebInvalidSpotDisplay = (spotName) => {
  return WEB_INVALID_SPOT_NAMES.includes(resolveWebSpotDisplayName(spotName))
}
