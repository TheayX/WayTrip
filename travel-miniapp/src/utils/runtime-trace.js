const TRACE_STORE_KEY = '__WAYTRIP_RUNTIME_TRACE__'
const TRACE_LIMIT = 200

/**
 * 读取当前页面路由，便于把异常和页面切换、请求来源关联起来。
 */
export const getCurrentPageRoute = () => {
  try {
    const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
    const currentPage = pages[pages.length - 1]
    return currentPage?.route || 'unknown'
  } catch (error) {
    return 'unknown'
  }
}

/**
 * 统一记录运行时关键事件，方便回放 timeout 发生前最后一步。
 */
export const traceRuntime = (category, payload = {}) => {
  const entry = {
    time: new Date().toISOString(),
    category,
    route: payload.route || getCurrentPageRoute(),
    ...payload
  }

  try {
    const traceStore = globalThis[TRACE_STORE_KEY] || []
    traceStore.push(entry)
    if (traceStore.length > TRACE_LIMIT) {
      traceStore.splice(0, traceStore.length - TRACE_LIMIT)
    }
    globalThis[TRACE_STORE_KEY] = traceStore
  } catch (error) {}

  console.log(`[运行时追踪][${category}]`, entry)
  return entry
}

export const getRuntimeTrace = () => {
  return globalThis[TRACE_STORE_KEY] || []
}
