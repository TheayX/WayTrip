<!-- 运营概览页面 -->
<template>
  <div class="dashboard premium-dashboard admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">运营总览</p>
        <h1 class="page-title">运营概览</h1>
        <p class="page-subtitle">集中查看核心指标、趋势变化与关键入口。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" :loading="loading" @click="fetchData">
          <el-icon class="mr-1"><RefreshRight /></el-icon> 刷新数据
        </el-button>
      </div>
    </section>

    <div v-if="errorMessage" class="dashboard-error page-error-state">
      <el-result icon="error" title="仪表盘数据加载失败" :sub-title="errorMessage">
        <template #extra>
          <el-button type="primary" :loading="loading" @click="fetchData">重新加载</el-button>
        </template>
      </el-result>
    </div>

    <template v-else>
      <el-alert
        v-if="partialWarning"
        class="dashboard-alert page-status-alert mb-6"
        type="warning"
        show-icon
        :closable="false"
        :title="partialWarning"
      />

      <!-- 顶部：四大趋势指标卡 -->
      <el-row v-loading="loading" element-loading-text="正在加载概览数据..." :gutter="20" class="trend-cards mb-6">
      <el-col :span="6">
        <el-card shadow="hover" class="trend-card tone-blue">
          <div class="flex justify-between items-start mb-2">
            <div>
              <p class="metric-label mb-1">今日收入</p>
              <h3 class="text-2xl font-bold text-gray-800">¥{{ formatCurrency(overview.todayRevenue) }}</h3>
            </div>
            <div class="metric-icon tone-blue">
              <el-icon><Money /></el-icon>
            </div>
          </div>
          <p class="text-xs font-medium mb-3" :class="getDeltaClass(overview.todayRevenue, overview.yesterdayRevenue)">
            {{ formatDelta(overview.todayRevenue, overview.yesterdayRevenue) }} 较昨日
          </p>
          <div ref="sparklineRevenue" class="sparkline h-12 w-full"></div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="trend-card tone-violet">
          <div class="flex justify-between items-start mb-2">
            <div>
              <p class="metric-label mb-1">今日新增用户</p>
              <h3 class="text-2xl font-bold text-gray-800">{{ formatInteger(overview.todayNewUsers) }}</h3>
            </div>
            <div class="metric-icon tone-violet">
              <el-icon><User /></el-icon>
            </div>
          </div>
          <p class="text-xs font-medium mb-3" :class="getDeltaClass(overview.todayNewUsers, overview.yesterdayNewUsers)">
            {{ formatDelta(overview.todayNewUsers, overview.yesterdayNewUsers) }} 较昨日
          </p>
          <div ref="sparklineUsers" class="sparkline h-12 w-full"></div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="trend-card tone-emerald">
          <div class="flex justify-between items-start mb-2">
            <div>
              <p class="metric-label mb-1">今日新增景点</p>
              <h3 class="text-2xl font-bold text-gray-800">{{ formatInteger(overview.todayNewSpots) }}</h3>
            </div>
            <div class="metric-icon tone-emerald">
              <el-icon><Location /></el-icon>
            </div>
          </div>
          <p class="text-xs font-medium mb-3" :class="getDeltaClass(overview.todayNewSpots, overview.yesterdayNewSpots)">
            {{ formatDelta(overview.todayNewSpots, overview.yesterdayNewSpots) }} 较昨日
          </p>
          <div ref="sparklineSpots" class="sparkline h-12 w-full"></div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="trend-card tone-amber">
          <div class="flex justify-between items-start mb-2">
            <div>
              <p class="metric-label mb-1">今日订单</p>
              <h3 class="text-2xl font-bold text-gray-800">{{ formatInteger(overview.todayOrders) }}</h3>
            </div>
            <div class="metric-icon tone-amber">
              <el-icon><ShoppingCart /></el-icon>
            </div>
          </div>
          <p class="text-xs font-medium mb-3" :class="getDeltaClass(overview.todayOrders, overview.yesterdayOrders)">
            {{ formatDelta(overview.todayOrders, overview.yesterdayOrders) }} 较昨日
          </p>
          <div ref="sparklineOrders" class="sparkline h-12 w-full"></div>
        </el-card>
      </el-col>
    </el-row>

      <!-- 中部：趋势 + 今日概览 -->
      <el-row v-loading="loading" element-loading-text="正在加载趋势与榜单..." :gutter="20">
      <el-col :span="16">
        <el-card shadow="hover" class="overview-panel border-0 mb-6">
           <template #header>
            <div class="flex justify-between items-center trend-toolbar">
              <span class="font-bold text-gray-800">订单与收入趋势</span>
              <div class="trend-controls">
                <div class="control-group">
                  <span class="control-label">统计方式</span>
                  <el-radio-group v-model="trendMode" size="small" @change="handleTrendModeChange">
                    <el-radio-button label="weekday">按星期分布</el-radio-button>
                    <el-radio-button label="range">按日期趋势</el-radio-button>
                  </el-radio-group>
                </div>
                <div class="control-divider"></div>
                <div class="control-group">
                  <span class="control-label">时间范围</span>
                  <el-radio-group v-model="selectedRange" size="small" @change="fetchData">
                    <el-radio-button :label="7">7天</el-radio-button>
                    <el-radio-button :label="30">30天</el-radio-button>
                    <el-radio-button :label="180">半年</el-radio-button>
                    <el-radio-button :label="365">一年</el-radio-button>
                    <el-radio-button :label="0">上线至今</el-radio-button>
                  </el-radio-group>
                </div>
              </div>
            </div>
          </template>
          <div
            ref="trendChartShellRef"
            class="trend-chart-shell"
            :class="{ 'is-revealing': trendRevealActive }"
          >
            <div ref="mainLineChartRef" class="w-full" style="height: 300px;"></div>
          </div>
        </el-card>

        <el-card shadow="hover" class="overview-panel border-0">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-bold text-gray-800">近一年订单密集度 (Contribution)</span>
            </div>
          </template>
          <div ref="heatmapRef" class="w-full" style="height: 220px;"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="overview-panel border-0 mb-6">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-bold text-gray-800">今日概览</span>
              <el-tag size="small" type="primary" effect="light" round>实时汇总</el-tag>
            </div>
          </template>
          <div class="today-overview">
            <div class="today-overview-item">
              <span class="overview-label">累计营收</span>
              <span class="overview-value">¥{{ formatCurrency(overview.totalRevenue) }}</span>
            </div>
            <div class="today-overview-item">
              <span class="overview-label">累计订单</span>
              <span class="overview-value">{{ formatInteger(overview.totalOrders) }}</span>
            </div>
            <div class="today-overview-item">
              <span class="overview-label">累计用户</span>
              <span class="overview-value">{{ formatInteger(overview.totalUsers) }}</span>
            </div>
            <div class="today-overview-item">
              <span class="overview-label">已发布景点</span>
              <span class="overview-value">{{ formatInteger(overview.totalSpots) }}</span>
            </div>
            <div class="today-overview-tip">
              昨日收入 ¥{{ formatCurrency(overview.yesterdayRevenue) }}，昨日订单 {{ formatInteger(overview.yesterdayOrders) }}
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="overview-panel border-0">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-bold text-gray-800">热门景点</span>
              <el-tag size="small" type="warning" effect="light" round>重点关注</el-tag>
            </div>
          </template>
          <div class="hot-spots-list">
            <button
              v-for="(spot, index) in hotSpots"
              :key="spot.id"
              type="button"
              class="hot-spot-item"
              @click="goToSpot(spot)"
            >
              <div class="hot-spot-rank" :class="`rank-${Math.min(index + 1, 4)}`">{{ index + 1 }}</div>
              <div class="hot-spot-body">
                <div class="hot-spot-header">
                  <span class="hot-spot-name">{{ spot.name }}</span>
                  <span class="hot-spot-revenue">¥{{ formatCurrency(spot.revenue) }}</span>
                </div>
                <div class="hot-spot-meta">
                  <span>订单 {{ formatInteger(spot.orderCount) }}</span>
                  <span>评分 {{ formatRating(spot.avgRating) }}</span>
                </div>
              </div>
            </button>
            <el-empty v-if="!hotSpots.length" description="暂无热门景点数据" :image-size="72" />
          </div>
        </el-card>
      </el-col>
    </el-row>

      <el-row :gutter="20" class="mt-6">
        <el-col :span="16">
          <el-card shadow="hover" class="border-0 workbench-card">
            <template #header>
              <div class="flex justify-between items-center">
                <span class="font-bold text-gray-800">运营入口</span>
                <el-tag size="small" type="primary" effect="light" round>快捷入口</el-tag>
              </div>
            </template>
            <div class="workbench-grid">
              <button
                v-for="item in workbenchEntries"
                :key="item.title"
                type="button"
                class="workbench-entry"
                @click="goTo(item.path)"
              >
                <div class="workbench-entry-head">
                  <span class="workbench-entry-title">{{ item.title }}</span>
                  <el-tag size="small" effect="plain" :type="item.tagType" round>{{ item.tag }}</el-tag>
                </div>
                <div class="workbench-entry-desc">{{ item.desc }}</div>
                <div class="workbench-entry-action">{{ item.action }}</div>
              </button>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
        <el-card shadow="hover" class="border-0 timeline-card">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-bold text-gray-800">运营提示</span>
              <el-tag size="small" type="success" effect="light" round>建议动作</el-tag>
            </div>
          </template>
          <div class="tips-list">
            <div v-for="item in dashboardTips" :key="item.title" class="tips-item">
              <div class="tips-title">{{ item.title }}</div>
              <div class="tips-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Money, User, Location, ShoppingCart, RefreshRight } from '@element-plus/icons-vue'
import { useTheme } from '@/shared/composables/useTheme.js'
import { getChartThemeTokens } from '@/shared/theme/charts/index.js'
import { getHotSpots, getOrderHeatmap, getOrderTrend, getOverview } from './api.js'

// 图表 DOM 引用统一集中管理，便于初始化、主题切换和销毁时批量处理。
const sparklineRevenue = ref(null)
const sparklineUsers = ref(null)
const sparklineSpots = ref(null)
const sparklineOrders = ref(null)
const heatmapRef = ref(null)
const mainLineChartRef = ref(null)
const trendChartShellRef = ref(null)
const router = useRouter()
const { currentTheme } = useTheme()

const charts = []
let mainLineChart = null
let hasTrendAnimated = false
const loading = ref(false)
const errorMessage = ref('')
const partialWarning = ref('')
const trendRevealActive = ref(false)
const trendMode = ref('weekday')
const selectedRange = ref(0)
const overview = ref({
  totalUsers: 0,
  totalSpots: 0,
  totalOrders: 0,
  totalRevenue: 0,
  todayOrders: 0,
  todayRevenue: 0,
  todayNewUsers: 0,
  todayNewSpots: 0,
  yesterdayOrders: 0,
  yesterdayRevenue: 0,
  yesterdayNewUsers: 0,
  yesterdayNewSpots: 0,
  recentRevenueSeries: [],
  recentUserSeries: [],
  recentSpotSeries: [],
  recentOrderSeries: []
})
const hotSpots = ref([])
const heatmapYear = ref(new Date().getFullYear())

const getChartPalette = () => getChartThemeTokens(currentTheme.value)

const workbenchEntries = [
  {
    title: '订单中心',
    desc: '查看订单状态流转、排查取消单与人工处理入口。',
    action: '进入订单工作台',
    path: '/order',
    tag: '交易管理',
    tagType: 'warning'
  },
  {
    title: '景点管理',
    desc: '回看热门景点的基础信息、上下架状态与内容完整度。',
    action: '进入景点管理',
    path: '/spot',
    tag: '内容管理',
    tagType: 'success'
  },
  {
    title: '攻略管理',
    desc: '查看最近内容产出，快速进入攻略编辑与审核链路。',
    action: '进入攻略管理',
    path: '/guide',
    tag: '内容运营',
    tagType: 'primary'
  },
  {
    title: '推荐配置',
    desc: '检查推荐策略与执行状态，确认首页结果是否需要干预。',
    action: '进入推荐配置',
    path: '/recommendation/config',
    tag: '推荐系统',
    tagType: 'info'
  }
]

const dashboardTips = [
  {
    title: '先看趋势再进详情',
    desc: '当收入和订单趋势背离时，优先进入订单中心排查取消和退款类状态。'
  },
  {
    title: '热门景点异常要回看内容',
    desc: '如果热门景点数据突然波动，先检查景点上架状态和攻略内容是否被修改。'
  },
  {
    title: '推荐调整不要在首页完成',
    desc: '仪表盘只做发现问题和跳转处理，具体参数修改统一进入推荐配置页。'
  }
]

// 首屏先初始化图表实例，后续只更新 option，避免反复销毁重建带来闪烁。
const initSparklines = () => {
  const palette = getChartPalette()
  const configs = [
    { ref: sparklineRevenue, color: palette.blue, areaColor: 'rgba(59, 130, 246, 0.16)' },
    { ref: sparklineUsers, color: palette.violet, areaColor: 'rgba(139, 92, 246, 0.16)' },
    { ref: sparklineSpots, color: palette.emerald, areaColor: 'rgba(16, 185, 129, 0.16)' },
    { ref: sparklineOrders, color: palette.amber, areaColor: 'rgba(249, 115, 22, 0.16)' }
  ]

  configs.forEach(cfg => {
    if (!cfg.ref.value) return
    const chart = echarts.init(cfg.ref.value)
    charts.push(chart)
    chart.setOption(buildSparklineOption([], cfg.color, cfg.areaColor))
  })
}

const buildSparklineOption = (data, color, areaColor) => ({
  animationDuration: 600,
  animationEasing: 'linear',
  grid: { left: 0, right: 0, top: 0, bottom: 0 },
  xAxis: { type: 'category', show: false, data: data.map((_, index) => index) },
  yAxis: { type: 'value', show: false },
  series: [{
    data,
    type: 'line',
    smooth: true,
    showSymbol: false,
    lineStyle: { color, width: 2 },
    areaStyle: { color: areaColor }
  }]
})

// 概览指标刷新后只回填 sparkline 数据，不重新创建实例。
const updateSparklines = () => {
  const palette = getChartPalette()
  const targets = [
    {
      ref: sparklineRevenue.value,
      data: (overview.value.recentRevenueSeries || []).map(item => Number(item || 0)),
      color: palette.blue,
      areaColor: 'rgba(59, 130, 246, 0.16)'
    },
    {
      ref: sparklineUsers.value,
      data: (overview.value.recentUserSeries || []).map(item => Number(item || 0)),
      color: palette.violet,
      areaColor: 'rgba(139, 92, 246, 0.16)'
    },
    {
      ref: sparklineSpots.value,
      data: (overview.value.recentSpotSeries || []).map(item => Number(item || 0)),
      color: palette.emerald,
      areaColor: 'rgba(16, 185, 129, 0.16)'
    },
    {
      ref: sparklineOrders.value,
      data: (overview.value.recentOrderSeries || []).map(item => Number(item || 0)),
      color: palette.amber,
      areaColor: 'rgba(249, 115, 22, 0.16)'
    }
  ]

  targets.forEach((item) => {
    const chart = charts.find(target => target.getDom() === item.ref)
    if (!chart) return
    chart.setOption(buildSparklineOption(item.data, item.color, item.areaColor), true)
  })
}

const initHeatmap = () => {
  if (!heatmapRef.value) return
  const chart = echarts.init(heatmapRef.value)
  charts.push(chart)
  const palette = getChartPalette()

  chart.setOption({
    tooltip: {
      position: 'top',
      formatter: function (p) {
        return p.data[0] + ' : ' + p.data[1] + ' 单';
      }
    },
    visualMap: {
      min: 0,
      max: 100,
      calculable: false,
      show: false,
      inRange: {
        color: palette.heatmap
      }
    },
    calendar: {
      top: 30,
      left: 30,
      right: 30,
      cellSize: ['auto', 16],
      range: String(heatmapYear.value),
      itemStyle: {
        borderWidth: 2,
        borderColor: palette.panelBackground,
        borderRadius: 4
      },
      splitLine: { show: false },
      yearLabel: { show: false },
      dayLabel: { color: palette.textSecondary, fontSize: 10 },
      monthLabel: { color: palette.textSecondary, fontSize: 10 }
    },
    series: {
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: []
    }
  })
}

const updateHeatmap = (year, list) => {
  const chart = charts.find(target => target.getDom() === heatmapRef.value)
  if (!chart) return
  const maxValue = Math.max(...list.map(item => Number(item.orderCount || 0)), 0)
  const palette = getChartPalette()
  chart.setOption({
    visualMap: {
      min: 0,
      max: Math.max(maxValue, 1),
      calculable: false,
      show: false,
      inRange: {
        color: palette.heatmap
      }
    },
    calendar: {
      range: String(year)
    },
    series: {
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: list.map(item => [item.date, Number(item.orderCount || 0)])
    }
  })
}

const initMainLineChart = () => {
  if (!mainLineChartRef.value) return
  if (mainLineChart) return
  mainLineChart = echarts.init(mainLineChartRef.value)
  charts.push(mainLineChart)
}

const formatMoney = (value) => Number(value || 0)
const formatCurrency = (value) => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const formatInteger = (value) => Number(value || 0).toLocaleString('zh-CN')
const formatRating = (value) => Number(value || 0).toFixed(1)

const formatDelta = (current, previous) => {
  const currentValue = Number(current || 0)
  const previousValue = Number(previous || 0)
  if (previousValue === 0) {
    if (currentValue === 0) return '0.0%'
    return '+100.0%'
  }
  const diff = ((currentValue - previousValue) / previousValue) * 100
  const sign = diff > 0 ? '+' : ''
  return `${sign}${diff.toFixed(1)}%`
}

const getDeltaClass = (current, previous) => {
  const diff = Number(current || 0) - Number(previous || 0)
  if (diff > 0) return 'text-green-500'
  if (diff < 0) return 'text-red-500'
  return 'text-gray-400'
}

const formatAxisLabel = (label) => {
  if (trendMode.value === 'weekday') return label
  const [, month, day] = `${label}`.split('-')
  return month && day ? `${month}-${day}` : label
}

const updateMainLineChart = (list) => {
  if (!mainLineChartRef.value) return
  initMainLineChart()
  if (!mainLineChart) return

  const palette = getChartPalette()
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: palette.tooltipBackground,
      borderColor: palette.borderColor,
      textStyle: { color: palette.textPrimary },
      formatter(params) {
        const current = list[params[0]?.dataIndex]
        if (!current) return ''
        return [
          current.date,
          `${params[0].marker}订单量：${current.orderCount}`,
          `${params[1].marker}收入：¥${formatMoney(current.revenue).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
        ].join('<br/>')
      }
    },
    legend: {
      data: ['订单量', '收入'],
      bottom: 0,
      textStyle: { color: palette.textSecondary }
    },
    grid: { left: '2%', right: '2%', top: '5%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: list.map(item => formatAxisLabel(item.date)),
      axisLine: { lineStyle: { color: palette.borderColor } },
      axisLabel: { color: palette.textSecondary }
    },
    yAxis: [
      {
        type: 'value',
        name: '订单量',
        nameTextStyle: { color: palette.textSecondary },
        axisLabel: { color: palette.textSecondary },
        splitLine: { lineStyle: { type: 'dashed', color: palette.splitLine } }
      },
      {
        type: 'value',
        name: '收入',
        nameTextStyle: { color: palette.textSecondary },
        axisLabel: {
          color: palette.textSecondary,
          formatter(value) {
            return `¥${Number(value).toLocaleString('zh-CN')}`
          }
        }
      }
    ],
    series: [
      {
        name: '订单量',
        type: 'line',
        smooth: true,
        data: list.map(item => Number(item.orderCount || 0)),
        yAxisIndex: 0,
        showSymbol: false,
        animationDuration: hasTrendAnimated ? 300 : 900,
        animationDelay: (_, index) => hasTrendAnimated ? 0 : index * 80,
        animationEasing: 'linear',
        animationEasingUpdate: 'linear',
        itemStyle: { color: palette.blue },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0)' }
          ])
        }
      },
      {
        name: '收入',
        type: 'line',
        smooth: true,
        data: list.map(item => formatMoney(item.revenue)),
        yAxisIndex: 1,
        showSymbol: false,
        animationDuration: hasTrendAnimated ? 300 : 900,
        animationDelay: (_, index) => hasTrendAnimated ? 0 : index * 80,
        animationEasing: 'linear',
        animationEasingUpdate: 'linear',
        itemStyle: { color: palette.emerald }
      }
    ],
    animationDuration: hasTrendAnimated ? 300 : 900,
    animationDurationUpdate: 300,
    animationEasing: 'linear',
    animationEasingUpdate: 'linear'
  }

  if (!hasTrendAnimated) {
    // 首次渲染先铺零值骨架，再播放一次完整入场动画，避免首屏跳变过硬。
    const labels = list.map(item => formatAxisLabel(item.date))
    const zeroOrderData = labels.map(() => 0)
    const zeroRevenueData = labels.map(() => 0)

    mainLineChart.clear()
    mainLineChart.setOption({
      tooltip: option.tooltip,
      legend: option.legend,
      grid: option.grid,
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: labels,
        axisLine: { lineStyle: { color: palette.borderColor } },
        axisLabel: { color: palette.textSecondary }
      },
      yAxis: option.yAxis,
      series: [
        {
          ...option.series[0],
          data: zeroOrderData
        },
        {
          ...option.series[1],
          data: zeroRevenueData
        }
      ],
      animation: false
    }, true)

    requestAnimationFrame(() => {
      mainLineChart?.setOption(option)
      hasTrendAnimated = true
      playTrendRevealAnimation()
    })
    return
  }

  mainLineChart.setOption(option)
  playTrendRevealAnimation()
}

const playTrendRevealAnimation = () => {
  const shell = trendChartShellRef.value
  if (!shell) return
  trendRevealActive.value = false
  void shell.offsetWidth
  trendRevealActive.value = true
}

const fetchData = async () => {
  loading.value = true
  errorMessage.value = ''
  partialWarning.value = ''

  const results = await Promise.allSettled([
    getOrderTrend(selectedRange.value, trendMode.value),
    getOverview(),
    getHotSpots(6),
    getOrderHeatmap(heatmapYear.value)
  ])

  const [trendResult, overviewResult, hotSpotsResult, heatmapResult] = results
  const failedSections = []

  // 各区块允许局部失败，优先保留还能展示的数据，避免整个概览页直接空白。
  if (trendResult.status === 'fulfilled') {
    updateMainLineChart(trendResult.value.data?.list || [])
    mainLineChart?.resize()
  } else {
    updateMainLineChart([])
    failedSections.push('趋势图')
  }

  if (overviewResult.status === 'fulfilled') {
    overview.value = {
      ...overview.value,
      ...(overviewResult.value.data || {})
    }
    updateSparklines()
  } else {
    failedSections.push('概览指标')
  }

  if (hotSpotsResult.status === 'fulfilled') {
    hotSpots.value = hotSpotsResult.value.data?.list || []
  } else {
    hotSpots.value = []
    failedSections.push('热门景点')
  }

  if (heatmapResult.status === 'fulfilled') {
    updateHeatmap(heatmapResult.value.data?.year || heatmapYear.value, heatmapResult.value.data?.list || [])
  } else {
    updateHeatmap(heatmapYear.value, [])
    failedSections.push('订单热力图')
  }

  if (failedSections.length === results.length) {
    const firstError = results.find(item => item.status === 'rejected')
    errorMessage.value = firstError?.reason?.response?.data?.message || firstError?.reason?.message || '请稍后重试或检查接口返回。'
  } else if (failedSections.length) {
    partialWarning.value = `部分区块未成功刷新：${failedSections.join('、')}。当前页面已保留可用数据。`
  }

  loading.value = false
}

const handleTrendModeChange = () => {
  // 两种统计方式切换时回到各自最常用的默认时间范围，避免口径混淆。
  selectedRange.value = trendMode.value === 'weekday' ? 0 : 7
  fetchData()
}

const handleResize = () => {
  charts.forEach(c => c.resize())
}

const goToSpot = (spot) => {
  if (!spot?.id) return
  router.push({
    path: '/spot',
    query: {
      keyword: spot.name,
      spotId: String(spot.id)
    }
  })
}

const goTo = (path) => {
  router.push(path)
}

onMounted(() => {
  nextTick(() => {
    initSparklines()
    initHeatmap()
    window.addEventListener('resize', handleResize)
    fetchData()
  })
})

watch(currentTheme, async () => {
  // 主题切换时同步刷新图表配色，保证深浅主题下图表对比度稳定。
  await nextTick()
  updateSparklines()
  updateHeatmap(heatmapYear.value, [])
  fetchData()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
  mainLineChart = null
  hasTrendAnimated = false
})
</script>

<style lang="scss" scoped>
/* Ensure Tailwind utility classes exist or map to custom SCSS */
.flex { display: flex; }
.justify-between { justify-content: space-between; }
.items-start { align-items: flex-start; }
.items-center { align-items: center; }
.mb-6 { margin-bottom: 24px; }
.mb-3 { margin-bottom: 12px; }
.mb-2 { margin-bottom: 8px; }
.mb-1 { margin-bottom: 4px; }
.mt-2 { margin-top: 8px; }
.mr-1 { margin-right: 4px; }
.text-2xl { font-size: 24px; line-height: 32px; }
.text-sm { font-size: 14px; line-height: 20px; }
.text-xs { font-size: 12px; line-height: 16px; }
.font-bold { font-weight: 700; }
.font-medium { font-weight: 500; }
.text-gray-800 { color: var(--wt-text-primary); }
.text-gray-500 { color: var(--wt-text-regular); }
.text-gray-400 { color: var(--wt-text-secondary); }
.text-blue-600 { color: #2563eb; }
.text-purple-600 { color: #7c3aed; }
.text-emerald-600 { color: #059669; }
.text-orange-600 { color: #ea580c; }
.text-green-500 { color: #22c55e; }
.text-red-500 { color: #ef4444; }
.bg-blue-100 { background-color: #dbeafe; }
.bg-purple-100 { background-color: #ede9fe; }
.bg-emerald-100 { background-color: #d1fae5; }
.bg-orange-100 { background-color: #ffedd5; }
.bg-gray-50 { background-color: #f9fafb; }
.border-none { border: none; }
.border-0 { border: 0; }
.rounded-lg { border-radius: 8px; }
.rounded-xl { border-radius: 12px; }
.p-2 { padding: 8px; }
.px-2 { padding-left: 8px; padding-right: 8px; }
.py-4 { padding-top: 16px; padding-bottom: 16px; }
.h-12 { height: 48px; }
.w-full { width: 100%; }
.m-0 { margin: 0; }
.overflow-y-auto { overflow-y: auto; }
.mt-6 { margin-top: 24px; }

.dashboard { display: flex; flex-direction: column; gap: 20px; }

.dashboard-error {
  padding: 12px 0 4px;
}

.dashboard-alert {
  margin-bottom: 4px;
}

.trend-card {
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid var(--wt-border-default);
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
}

.trend-card :deep(.el-card__body) {
  padding: 22px 22px 18px !important;
}

.tone-blue {
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.12) 0%, var(--wt-surface-elevated) 100%);
}

.tone-violet {
  background: linear-gradient(180deg, rgba(139, 92, 246, 0.12) 0%, var(--wt-surface-elevated) 100%);
}

.tone-emerald {
  background: linear-gradient(180deg, rgba(16, 185, 129, 0.12) 0%, var(--wt-surface-elevated) 100%);
}

.tone-amber {
  background: linear-gradient(180deg, rgba(249, 115, 22, 0.12) 0%, var(--wt-surface-elevated) 100%);
}

.metric-label {
  font-size: 13px;
  color: var(--wt-text-regular);
  font-weight: 600;
}

.metric-icon {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
}

.metric-icon.tone-blue {
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
}

.metric-icon.tone-violet {
  background: rgba(124, 58, 237, 0.1);
  color: #7c3aed;
}

.metric-icon.tone-emerald {
  background: rgba(5, 150, 105, 0.1);
  color: #059669;
}

.metric-icon.tone-amber {
  background: rgba(217, 119, 6, 0.1);
  color: #d97706;
}

.overview-panel {
  border-radius: 22px;
}

.trend-toolbar {
  gap: 16px;
  flex-wrap: wrap;
}

.trend-controls {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 12px 16px;
  border: 1px solid var(--wt-border-default);
  border-radius: 16px;
  background: linear-gradient(180deg, var(--wt-surface-muted) 0%, var(--wt-surface-elevated) 100%);
  box-shadow: var(--wt-shadow-soft);
}

.control-group {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 12px;
  background: var(--wt-surface-elevated);
}

.control-label {
  font-size: 12px;
  color: var(--wt-text-regular);
  font-weight: 700;
  white-space: nowrap;
}

.control-divider {
  width: 1px;
  height: 40px;
  background: var(--wt-border-default);
}

.trend-chart-shell {
  overflow: hidden;
}

.trend-chart-shell.is-revealing {
  animation: trend-reveal 900ms linear;
}

@keyframes trend-reveal {
  from {
    clip-path: inset(0 100% 0 0);
  }
  to {
    clip-path: inset(0 0 0 0);
  }
}

.today-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.today-overview-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border: 1px solid var(--wt-border-default);
}

.overview-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--wt-text-regular);
}

.overview-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.today-overview-tip {
  grid-column: 1 / -1;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

.hot-spots-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 280px;
}

.hot-spot-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--wt-border-default);
  border-radius: 16px;
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.hot-spot-item:hover {
  border-color: var(--el-color-primary-light-5);
  transform: translateY(-1px);
  box-shadow: var(--wt-shadow-soft);
}

.hot-spot-rank {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--wt-text-regular);
  background: var(--wt-border-default);
}

.rank-1 {
  color: #92400e;
  background: #fef3c7;
}

.rank-2 {
  color: #374151;
  background: #e5e7eb;
}

.rank-3 {
  color: #9a3412;
  background: #fed7aa;
}

.hot-spot-body {
  min-width: 0;
  flex: 1;
}

.hot-spot-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 6px;
}

.hot-spot-name {
  min-width: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--wt-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-spot-revenue {
  flex: none;
  font-size: 13px;
  font-weight: 700;
  color: #059669;
}

.hot-spot-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--wt-text-regular);
}

.workbench-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.workbench-entry {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--wt-border-default);
  background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.workbench-entry:hover {
  transform: translateY(-2px);
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--wt-shadow-soft);
}

.workbench-entry-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.workbench-entry-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.workbench-entry-desc {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--wt-text-regular);
}

.workbench-entry-action {
  margin-top: 14px;
  font-size: 12px;
  font-weight: 700;
  color: #1d4ed8;
}

.tips-list {
  display: grid;
  gap: 12px;
}

.tips-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, var(--wt-surface-muted) 0%, var(--wt-surface-elevated) 100%);
  border: 1px solid var(--wt-border-default);
}

.tips-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.tips-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--wt-text-regular);
}

@media (max-width: 1200px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
