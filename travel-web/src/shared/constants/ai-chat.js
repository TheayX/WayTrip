import { ROUTE_NAMES } from '@/shared/constants/route-names.js'

// 集中维护 AI 客服在 Web 端的可见路由白名单。
export const AI_CHAT_VISIBLE_ROUTE_NAMES = [
  ROUTE_NAMES.home,
  ROUTE_NAMES.discover,
  ROUTE_NAMES.spotList,
  ROUTE_NAMES.spotDetail,
  ROUTE_NAMES.guideList,
  ROUTE_NAMES.guideDetail,
  ROUTE_NAMES.search,
  ROUTE_NAMES.randomPick,
  ROUTE_NAMES.budgetTravel,
  ROUTE_NAMES.travelerReviews,
  ROUTE_NAMES.trendingViews,
  ROUTE_NAMES.more,
  ROUTE_NAMES.recommendations,
  ROUTE_NAMES.nearby,
  ROUTE_NAMES.favorites
]

// AI 场景提示按路由集中维护，避免聊天组件自己保留一整份分发规则。
export const AI_CHAT_SCENARIO_ROUTE_GROUPS = [
  {
    scenarioHint: 'ORDER_ADVISOR',
    routeNames: [ROUTE_NAMES.orderList, ROUTE_NAMES.orderDetail, ROUTE_NAMES.orderCreate]
  },
  {
    scenarioHint: 'SPOT_QA',
    routeNames: [ROUTE_NAMES.spotList, ROUTE_NAMES.spotDetail, ROUTE_NAMES.search]
  },
  {
    scenarioHint: 'GUIDE_QA',
    routeNames: [ROUTE_NAMES.guideList, ROUTE_NAMES.guideDetail]
  },
  {
    scenarioHint: 'RECOMMENDATION_EXPLAINER',
    routeNames: [ROUTE_NAMES.recommendations, ROUTE_NAMES.nearby]
  },
  {
    scenarioHint: 'USER_PROFILE_ANALYZER',
    routeNames: [ROUTE_NAMES.profile, ROUTE_NAMES.favorites]
  },
  {
    scenarioHint: 'TRAVEL_PLANNER',
    routeNames: [
      ROUTE_NAMES.discover,
      ROUTE_NAMES.randomPick,
      ROUTE_NAMES.budgetTravel,
      ROUTE_NAMES.travelerReviews,
      ROUTE_NAMES.trendingViews,
      ROUTE_NAMES.more
    ]
  }
]

// AI 聊天组件的静态文案统一收口在这里，避免散落在多个视图文件中。
export const AI_CHAT_COPY = {
  launcher: 'AI 助手',
  title: 'WayTrip AI 助手',
  subtitle: '聊景点、问订单、找灵感',
  welcome: '你好，我是 WayTrip AI 助手。你可以问我景点推荐、行程规划、订单咨询和平台规则问题。',
  welcomeSuggestions: [
    { text: '帮我规划周末一日游', scenarioHint: 'TRAVEL_PLANNER', sourcePage: '' },
    { text: '看看我适合哪些景点', scenarioHint: 'TRAVEL_PLANNER', sourcePage: '' },
    { text: '查询我的订单问题', scenarioHint: 'ORDER_ADVISOR', sourcePage: '' }
  ],
  inputPlaceholder: '输入你的问题，比如景点推荐、行程规划或订单咨询',
  pendingMessage: '正在整理这条问题的答案…',
  typing: '正在整理回答，请稍候',
  followupTitle: '猜你想问',
  citationsTitle: '参考',
  citationFallbackTitle: '参考资料',
  citationFallbackType: '知识库',
  feedbackUpvote: '有帮助',
  feedbackDownvote: '还不够准确',
  clearTitle: '清空对话',
  closeTitle: '关闭聊天',
  send: '发送',
  feedbackThanks: '感谢你的反馈',
  feedbackRecorded: '已记录这条问题'
}

/**
 * 根据当前路由名称解析 AI 场景提示。
 *
 * @param {string|symbol|null|undefined} routeName 当前路由名
 * @returns {string} 轻量场景提示
 */
export function resolveAiScenarioHintByRouteName(routeName) {
  const matchedGroup = AI_CHAT_SCENARIO_ROUTE_GROUPS.find(group => group.routeNames.includes(routeName))
  return matchedGroup?.scenarioHint || ''
}
