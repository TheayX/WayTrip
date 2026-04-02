<!-- 推荐系统总览页 -->
<template>
  <div class="recommendation-overview-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">Recommendation Overview</p>
        <h1 class="page-title">推荐总览</h1>
        <p class="page-subtitle">查看推荐链路状态、最近行为摘要和相关工作台入口，不在总览页直接修改参数。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" :loading="loading" @click="loadPageData">刷新数据</el-button>
      </div>
    </section>

    <div v-if="errorMessage" class="error-state page-error-state">
      <el-result icon="error" title="推荐总览加载失败" :sub-title="errorMessage">
        <template #extra>
          <el-button type="primary" :loading="loading" @click="loadPageData">重新加载</el-button>
        </template>
      </el-result>
    </div>

    <template v-else>
      <el-alert
        v-if="partialWarning"
        class="page-alert page-status-alert"
        type="warning"
        show-icon
        :closable="false"
        :title="partialWarning"
      />

    <!-- 状态卡片 -->
    <div v-loading="loading" class="hero-grid">
      <el-card shadow="hover" class="hero-card hero-card-engine">
        <div class="hero-label">引擎状态</div>
        <div class="hero-value">{{ status.computing ? '计算中' : '就绪' }}</div>
        <div class="hero-desc">当前推荐引擎的计算状态与可用性</div>
      </el-card>
      <el-card shadow="hover" class="hero-card hero-card-time">
        <div class="hero-label">上次更新</div>
        <div class="hero-value hero-value-sm">{{ status.lastUpdateTime || '暂无记录' }}</div>
        <div class="hero-desc">最近一次矩阵更新或配置生效时间</div>
      </el-card>
      <el-card shadow="hover" class="hero-card hero-card-users">
        <div class="hero-label">覆盖用户</div>
        <div class="hero-value">{{ status.totalUsers ?? '-' }}</div>
        <div class="hero-desc">进入推荐链路的用户规模</div>
      </el-card>
      <el-card shadow="hover" class="hero-card hero-card-spots">
        <div class="hero-label">覆盖景点</div>
        <div class="hero-value">{{ status.totalSpots ?? '-' }}</div>
        <div class="hero-desc">当前参与推荐计算的景点数量</div>
      </el-card>
    </div>

    <el-row v-loading="loading" :gutter="24" class="content-row">
      <el-col :xl="16" :lg="15" :md="24">
        <el-card shadow="hover">
          <!-- 工作台入口 -->
          <template #header>
            <div class="card-header">
              <span>推荐工作台</span>
            </div>
          </template>

          <div class="entry-grid">
            <div
              v-for="item in entryCards"
              :key="item.title"
              class="entry-card"
              :class="item.tone"
              @click="goTo(item.path)"
            >
              <div class="entry-head">
                <div class="entry-title">{{ item.title }}</div>
                <el-tag size="small" effect="plain" :type="item.tagType" round>{{ item.tag }}</el-tag>
              </div>
              <div class="entry-desc">{{ item.desc }}</div>
              <div class="entry-action">进入 {{ item.action }}</div>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="summary-card">
          <!-- 最近行为摘要 -->
          <template #header>
            <div class="card-header">
              <span>最近行为摘要</span>
            </div>
          </template>

          <div class="summary-grid">
            <div class="summary-panel">
              <div class="summary-title">最新收藏</div>
              <div class="summary-main">{{ latestFavorite.spotName || '暂无数据' }}</div>
              <div class="summary-sub">
                {{ latestFavorite.nickname ? `${latestFavorite.nickname} 收藏于 ${latestFavorite.createdAt}` : '可从这里快速回看显式偏好行为' }}
              </div>
            </div>
            <div class="summary-panel">
              <div class="summary-title">最新浏览</div>
              <div class="summary-main">{{ latestView.spotName || '暂无数据' }}</div>
              <div class="summary-sub">
                {{ latestView.nickname ? `${latestView.nickname} 来自 ${latestView.sourceLabel}，浏览于 ${latestView.createdAt}` : '可从这里快速观察最新流量入口' }}
              </div>
            </div>
            <div class="summary-panel">
              <div class="summary-title">高频偏好</div>
              <div class="summary-main">{{ latestPreference.tag || '暂无数据' }}</div>
              <div class="summary-sub">
                {{ latestPreference.nickname ? `${latestPreference.nickname} 的画像最近一次更新时间为 ${latestPreference.updatedAt || '暂无'}` : '可从这里快速观察当前画像标签' }}
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="journey-card">
          <!-- 推荐链路 -->
          <template #header>
            <div class="card-header">
              <span>推荐链路</span>
            </div>
          </template>

          <div class="journey-grid">
            <div v-for="item in journeySteps" :key="item.title" class="journey-step">
              <div class="journey-index">{{ item.index }}</div>
              <div class="journey-title">{{ item.title }}</div>
              <div class="journey-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xl="8" :lg="9" :md="24">
        <el-card shadow="hover" class="panel-card">
          <!-- 管理建议 -->
          <template #header>
            <div class="card-header">
              <span>管理建议</span>
            </div>
          </template>

          <div class="tips-list">
            <div v-for="item in tips" :key="item.title" class="tips-item">
              <div class="tips-title">{{ item.title }}</div>
              <div class="tips-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="panel-card">
          <!-- 快捷操作 -->
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>

          <div class="quick-actions">
            <el-button type="primary" @click="goTo('/recommendation/config')">进入推荐配置</el-button>
            <el-button @click="goTo('/recommendation/config?focus=execution')">进入执行区</el-button>
            <el-button @click="goTo('/recommendation/config?focus=debug')">进入调试预览</el-button>
            <el-button @click="goTo('/view-log')">查看浏览行为</el-button>
            <el-button @click="goTo('/favorite')">查看用户收藏</el-button>
            <el-button @click="goTo('/preference')">查看用户偏好</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getSourceLabel } from '@/shared/constants/view-source.js'
import { getRecommendationStatus } from '@/modules/recommendation/api/recommendation.js'
import { getFavoriteList } from '@/modules/user-ops/api/favorite.js'
import { getPreferenceList } from '@/modules/user-ops/api/preference.js'
import { getViewList } from '@/modules/user-ops/api/view-log.js'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const partialWarning = ref('')

// 推荐状态
const status = reactive({
  computing: false,
  lastUpdateTime: '',
  totalUsers: null,
  totalSpots: null
})

// 最近行为摘要
const latestFavorite = reactive({
  nickname: '',
  spotName: '',
  createdAt: ''
})

const latestView = reactive({
  nickname: '',
  spotName: '',
  createdAt: '',
  sourceLabel: ''
})

const latestPreference = reactive({
  nickname: '',
  tag: '',
  updatedAt: ''
})

// 工作台入口
const entryCards = [
  {
    title: '推荐配置',
    desc: '集中管理协同过滤、热度重排、缓存与调试参数。',
    action: '参数配置',
    path: '/recommendation/config',
    tone: 'tone-config',
    tag: '配置中心',
    tagType: 'primary'
  },
  {
    title: '用户偏好',
    desc: '查看用户画像标签，判断分类兴趣分布是否足够稳定。',
    action: '用户偏好',
    path: '/preference',
    tone: 'tone-profile',
    tag: '画像入口',
    tagType: 'success'
  },
  {
    title: '用户收藏',
    desc: '观察显式偏好行为，判断推荐结果是否触发真实收藏。',
    action: '用户收藏',
    path: '/favorite',
    tone: 'tone-favorite',
    tag: '行为入口',
    tagType: 'warning'
  },
  {
    title: '浏览行为',
    desc: '查看来源与停留时长，判断推荐流量是否带来有效点击。',
    action: '浏览行为',
    path: '/view-log',
    tone: 'tone-view',
    tag: '行为入口',
    tagType: 'info'
  }
]

// 推荐链路
const journeySteps = [
  {
    index: '01',
    title: '采集行为',
    desc: '从浏览、收藏、评价、订单中累计用户对景点的交互信号。'
  },
  {
    index: '02',
    title: '构建画像',
    desc: '把偏好标签与行为数据沉淀成用户兴趣画像，供冷启动与运营分析使用。'
  },
  {
    index: '03',
    title: '矩阵计算',
    desc: '通过协同过滤与相似矩阵构建候选，再结合热度做轻量重排。'
  },
  {
    index: '04',
    title: '运营回看',
    desc: '回到偏好、收藏、浏览页面验证推荐链路是否产生有效反馈。'
  }
]

// 管理建议
const tips = [
  {
    title: '优先看浏览来源',
    desc: '如果推荐入口点击占比低，优先排查候选规模、热度重排和前端曝光位。'
  },
  {
    title: '偏好要和收藏联动看',
    desc: '画像标签多但收藏转化低，通常说明标签覆盖广但推荐命中不够准。'
  },
  {
    title: '更新矩阵前先保存',
    desc: '涉及离线矩阵参数的调整，先保存配置再重建矩阵，避免预览结果失真。'
  }
]

// 获取推荐状态
const fetchStatus = async () => {
  const res = await getRecommendationStatus()
  Object.assign(status, res.data || {})
}

// 获取最近行为摘要
const fetchBehaviorSummary = async () => {
  const [favoriteRes, viewRes, preferenceRes] = await Promise.all([
    getFavoriteList({ page: 1, pageSize: 1 }),
    getViewList({ page: 1, pageSize: 1 }),
    getPreferenceList({ page: 1, pageSize: 1 })
  ])

  const favorite = favoriteRes.data.list?.[0]
  const view = viewRes.data.list?.[0]
  const preference = preferenceRes.data.list?.[0]

  Object.assign(latestFavorite, {
    nickname: favorite?.nickname || '',
    spotName: favorite?.spotName || '',
    createdAt: favorite?.createdAt || ''
  })

  Object.assign(latestView, {
    nickname: view?.nickname || '',
    spotName: view?.spotName || '',
    createdAt: view?.createdAt || '',
    sourceLabel: getSourceLabel(view?.source)
  })

  Object.assign(latestPreference, {
    nickname: preference?.nickname || '',
    tag: preference?.preferenceTags?.[0] || '',
    updatedAt: preference?.updatedAt || ''
  })
}

const loadPageData = async () => {
  loading.value = true
  errorMessage.value = ''
  partialWarning.value = ''

  const results = await Promise.allSettled([
    fetchStatus(),
    fetchBehaviorSummary()
  ])

  const failedSections = []
  if (results[0].status === 'rejected') failedSections.push('推荐状态')
  if (results[1].status === 'rejected') failedSections.push('行为摘要')

  if (failedSections.length === results.length) {
    const firstError = results.find(item => item.status === 'rejected')
    errorMessage.value = firstError?.reason?.response?.data?.message || firstError?.reason?.message || '请稍后重试或检查接口返回。'
  } else if (failedSections.length) {
    partialWarning.value = `部分区块未成功刷新：${failedSections.join('、')}。当前页面已保留可用数据。`
  }

  loading.value = false
}

// 页面跳转
const goTo = (path) => {
  router.push(path)
}

// 页面初始化
onMounted(async () => {
  loadPageData()
})
</script>

<style lang="scss" scoped>
.recommendation-overview-page {
  .page-alert {
    margin-bottom: 4px;
  }

  .hero-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
    margin-bottom: 24px;
  }

  .hero-card {
    border: none;
    color: #fff;
    overflow: hidden;
    position: relative;

    :deep(.el-card__body) {
      padding: 22px !important;
    }
  }

  .hero-card-engine {
    background: linear-gradient(135deg, #0f9d58 0%, #36cfc9 100%);
  }

  .hero-card-time {
    background: linear-gradient(135deg, #1677ff 0%, #69b1ff 100%);
  }

  .hero-card-users {
    background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
  }

  .hero-card-spots {
    background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%);
  }

  .hero-label {
    font-size: 13px;
    opacity: 0.88;
  }

  .hero-value {
    margin-top: 10px;
    font-size: 28px;
    font-weight: 700;
    line-height: 1.2;
  }

  .hero-value-sm {
    font-size: 16px;
    line-height: 1.5;
  }

  .hero-desc {
    margin-top: 10px;
    font-size: 12px;
    line-height: 1.7;
    opacity: 0.92;
  }

  .content-row {
    margin-bottom: 24px;
  }

  .entry-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .entry-card {
    padding: 18px;
    border-radius: 14px;
    border: 1px solid #e7edf7;
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 24px -16px rgba(15, 23, 42, 0.35);
    }
  }

  .entry-card.tone-config {
    background: linear-gradient(135deg, #f5f9ff 0%, #edf4ff 100%);
  }

  .entry-card.tone-profile {
    background: linear-gradient(135deg, #f2fff7 0%, #ebfff1 100%);
  }

  .entry-card.tone-favorite {
    background: linear-gradient(135deg, #fffaf0 0%, #fff4e6 100%);
  }

  .entry-card.tone-view {
    background: linear-gradient(135deg, #f8faff 0%, #f1f5ff 100%);
  }

  .entry-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .entry-title {
    font-size: 16px;
    font-weight: 700;
    color: #1f2937;
  }

  .entry-desc {
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.7;
    color: #5b6475;
  }

  .entry-action {
    margin-top: 14px;
    font-size: 12px;
    font-weight: 700;
    color: #245bdb;
  }

  .journey-card {
    margin-top: 24px;
  }

  .summary-card {
    margin-top: 24px;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }

  .summary-panel {
    padding: 18px;
    border-radius: 14px;
    border: 1px solid #e7edf7;
    background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  }

  .summary-title {
    font-size: 13px;
    color: #6b7280;
  }

  .summary-main {
    margin-top: 10px;
    font-size: 20px;
    font-weight: 700;
    line-height: 1.35;
    color: #1f2937;
  }

  .summary-sub {
    margin-top: 10px;
    font-size: 12px;
    line-height: 1.7;
    color: #607086;
  }

  .journey-grid {
    display: grid;
    gap: 14px;
  }

  .journey-step {
    padding: 16px 18px;
    border-radius: 14px;
    border: 1px solid #e7edf7;
    background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  }

  .journey-index {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    background: linear-gradient(135deg, #1677ff 0%, #69b1ff 100%);
    color: #fff;
    font-size: 14px;
    font-weight: 700;
  }

  .journey-title {
    margin-top: 12px;
    font-size: 15px;
    font-weight: 700;
    color: #1f2937;
  }

  .journey-desc {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.7;
    color: #5b6475;
  }

  .panel-card + .panel-card {
    margin-top: 24px;
  }

  .tips-list {
    display: grid;
    gap: 12px;
  }

  .tips-item {
    padding: 14px 16px;
    border-radius: 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
  }

  .tips-title {
    font-size: 13px;
    font-weight: 700;
    color: #253046;
  }

  .tips-desc {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.7;
    color: #607086;
  }

  .quick-actions {
    display: grid;
    gap: 10px;

    .el-button {
      margin-left: 0;
      justify-content: center;
    }
  }

  @media (max-width: 1200px) {
    .hero-grid,
    .entry-grid,
    .summary-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .hero-grid,
    .entry-grid,
    .summary-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
