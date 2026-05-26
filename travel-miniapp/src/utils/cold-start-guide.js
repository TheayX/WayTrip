// 冷启动引导状态存储工具，负责按用户维度记录引导是否待展示、已跳过或已完成。
const buildGuideKey = (userId) => `cold_start_guide:${userId}`

const resolveUserId = (userId) => {
  const value = Number(userId)
  return Number.isInteger(value) && value > 0 ? value : null
}

export const getColdStartGuideState = (userId) => {
  const resolvedUserId = resolveUserId(userId)
  if (!resolvedUserId) {
    return {
      pending: false,
      skipped: false,
      completed: false
    }
  }

  const state = uni.getStorageSync(buildGuideKey(resolvedUserId)) || {}
  return {
    pending: Boolean(state.pending),
    skipped: Boolean(state.skipped),
    completed: Boolean(state.completed)
  }
}

export const markColdStartGuidePending = (userId) => {
  const resolvedUserId = resolveUserId(userId)
  if (!resolvedUserId) return

  // pending 表示本次登录后仍需要展示引导，和 skipped / completed 保持互斥。
  uni.setStorageSync(buildGuideKey(resolvedUserId), {
    pending: true,
    skipped: false,
    completed: false
  })
}

export const markColdStartGuideSkipped = (userId) => {
  const resolvedUserId = resolveUserId(userId)
  if (!resolvedUserId) return

  uni.setStorageSync(buildGuideKey(resolvedUserId), {
    pending: false,
    skipped: true,
    completed: false
  })
}

export const markColdStartGuideCompleted = (userId) => {
  const resolvedUserId = resolveUserId(userId)
  if (!resolvedUserId) return

  // completed 一旦写入，后续即使再次进入首页也不再重复打扰用户。
  uni.setStorageSync(buildGuideKey(resolvedUserId), {
    pending: false,
    skipped: false,
    completed: true
  })
}
