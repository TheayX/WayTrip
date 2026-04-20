// AI 知识域展示文案映射。
export const AI_KNOWLEDGE_DOMAIN_LABELS = {
  PLATFORM_POLICY: '平台规则',
  SPOT_KNOWLEDGE: '景点知识',
  GUIDE_KNOWLEDGE: '攻略知识',
  ACCOUNT_HELP: '账号帮助'
}

// 管理端固定展示的 AI 场景配置。
export const AI_SCENARIO_CONFIGS = [
  {
    key: 'customer-service',
    title: '通用客服',
    scenario: 'CUSTOMER_SERVICE',
    knowledgeDomain: 'PLATFORM_POLICY',
    enabled: true,
    description: '兜底处理平台规则、常见咨询与无法明确归类的问题。',
    exampleQuestion: '平台的退款规则和人工客服处理时效是怎样的？'
  },
  {
    key: 'order-advisor',
    title: '订单顾问',
    scenario: 'ORDER_ADVISOR',
    knowledgeDomain: 'PLATFORM_POLICY',
    enabled: true,
    description: '面向订单状态、退款流程、售后规则等平台订单问题。',
    exampleQuestion: '订单取消后多久会原路退款，在哪里查看进度？'
  },
  {
    key: 'spot-qa',
    title: '景点问答',
    scenario: 'SPOT_QA',
    knowledgeDomain: 'SPOT_KNOWLEDGE',
    enabled: true,
    description: '回答景点门票、开放时间、位置与游玩亮点等问题。',
    exampleQuestion: '黄鹤楼景区几点开门，建议预留多长时间游玩？'
  },
  {
    key: 'guide-qa',
    title: '攻略问答',
    scenario: 'GUIDE_QA',
    knowledgeDomain: 'GUIDE_KNOWLEDGE',
    enabled: true,
    description: '回答玩法建议、避坑提醒与攻略内容相关问题。',
    exampleQuestion: '第一次去武汉两天一晚，住宿和路线怎么安排更方便？'
  }
]

// 表单下拉框使用的轻量场景选项，避免页面层重复做映射转换。
export const AI_SCENARIO_OPTIONS = AI_SCENARIO_CONFIGS.map(({ title, scenario, knowledgeDomain }) => ({
  label: title,
  value: scenario,
  domain: knowledgeDomain
}))

// 管理端做订单边界与售后规则联调时，默认落到订单顾问更符合真实链路。
export const AI_PREVIEW_DEFAULT_SCENARIO = 'ORDER_ADVISOR'
