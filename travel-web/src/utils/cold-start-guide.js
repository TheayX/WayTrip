// 推荐冷启动引导状态工具

/**
 * 构建当前用户的冷启动状态存储键
 * @param {number|string} userId
 * @returns {string}
 */
const buildGuideKey = (userId) => `cold_start_guide:${userId}`

/**
 * 解析合法的用户 ID
 * @param {number|string} userId
 * @returns {number|null}
 */
const resolveUserId = (userId) => {
  const value = Number(userId)
  return Number.isInteger(value) && value > 0 ? value : null
}

/**
 * 获取冷启动引导状态
 * @param {number|string} userId
 * @returns {{pending:boolean, skipped:boolean, completed:boolean}}
 */
export const getColdStartGuideState = (userId) => {
  const resolvedUserId = resolveUserId(userId)
  if (!resolvedUserId) {
    return { pending: false, skipped: false, completed: false }
  }

  const raw = localStorage.getItem(buildGuideKey(resolvedUserId))
  const state = raw ? JSON.parse(raw) : {}
  return {
    pending: Boolean(state.pending),
    skipped: Boolean(state.skipped),
    completed: Boolean(state.completed)
  }
}

/**
 * 标记当前用户进入待引导状态
 * @param {number|string} userId
 */
export const markColdStartGuidePending = (userId) => {
  const resolvedUserId = resolveUserId(userId)
  if (!resolvedUserId) return

  localStorage.setItem(buildGuideKey(resolvedUserId), JSON.stringify({
    pending: true,
    skipped: false,
    completed: false
  }))
}

/**
 * 标记当前用户跳过引导
 * @param {number|string} userId
 */
export const markColdStartGuideSkipped = (userId) => {
  const resolvedUserId = resolveUserId(userId)
  if (!resolvedUserId) return

  localStorage.setItem(buildGuideKey(resolvedUserId), JSON.stringify({
    pending: false,
    skipped: true,
    completed: false
  }))
}

/**
 * 标记当前用户完成引导
 * @param {number|string} userId
 */
export const markColdStartGuideCompleted = (userId) => {
  const resolvedUserId = resolveUserId(userId)
  if (!resolvedUserId) return

  localStorage.setItem(buildGuideKey(resolvedUserId), JSON.stringify({
    pending: false,
    skipped: false,
    completed: true
  }))
}
