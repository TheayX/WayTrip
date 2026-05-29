// 冷启动引导状态存储工具，负责按用户维度记录引导是否待展示、已跳过或已完成。

// 根据用户 ID 生成冷启动引导缓存键。
const buildGuideKey = (userId) => `cold_start_guide:${userId}`

// 过滤无效用户 ID，避免把游客态写入固定缓存键。
const resolveUserId = (userId) => {
  const value = Number(userId)
  return Number.isInteger(value) && value > 0 ? value : null
}

// 读取当前用户冷启动引导状态。
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

// 标记当前用户需要展示冷启动引导。
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

// 标记当前用户已跳过冷启动引导。
export const markColdStartGuideSkipped = (userId) => {
  const resolvedUserId = resolveUserId(userId)
  if (!resolvedUserId) return

  uni.setStorageSync(buildGuideKey(resolvedUserId), {
    pending: false,
    skipped: true,
    completed: false
  })
}

// 标记当前用户已完成冷启动引导。
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
