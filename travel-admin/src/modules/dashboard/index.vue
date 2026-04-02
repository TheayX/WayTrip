<template>
  <div class="dashboard premium-dashboard">
    <div class="dashboard-header flex-between mb-6">
      <div class="welcome-text">
        <h2 class="title text-2xl font-bold m-0 text-gray-800">业务数据中心</h2>
        <p class="subtitle text-sm text-gray-400 mt-2">全面监控业务流转与运营健康度</p>
      </div>
      <div class="actions">
        <el-button type="primary" class="modern-btn" @click="fetchData">
          <el-icon class="mr-1"><RefreshRight /></el-icon> 刷新数据
        </el-button>
      </div>
    </div>

    <!-- 顶部：四大趋势指标卡 -->
    <el-row :gutter="20" class="trend-cards mb-6">
      <!-- 收入 -->
      <el-col :span="6">
        <el-card shadow="hover" class="trend-card bg-gradient-to-br from-blue-50 to-white border-blue-100">
          <div class="flex justify-between items-start mb-2">
            <div>
              <p class="text-sm text-gray-500 mb-1">今日收入</p>
              <h3 class="text-2xl font-bold text-gray-800">¥8,459.00</h3>
            </div>
            <div class="p-2 bg-blue-100 text-blue-600 rounded-lg">
              <el-icon><Money /></el-icon>
            </div>
          </div>
          <p class="text-xs text-green-500 font-medium mb-3">+12.5% 较昨日</p>
          <div ref="sparklineRevenue" class="sparkline h-12 w-full"></div>
        </el-card>
      </el-col>

      <!-- 活跃用户 -->
      <el-col :span="6">
        <el-card shadow="hover" class="trend-card bg-gradient-to-br from-purple-50 to-white border-purple-100">
          <div class="flex justify-between items-start mb-2">
            <div>
              <p class="text-sm text-gray-500 mb-1">活跃用户</p>
              <h3 class="text-2xl font-bold text-gray-800">1,245</h3>
            </div>
            <div class="p-2 bg-purple-100 text-purple-600 rounded-lg">
              <el-icon><User /></el-icon>
            </div>
          </div>
          <p class="text-xs text-green-500 font-medium mb-3">+5.2% 较昨日</p>
          <div ref="sparklineUsers" class="sparkline h-12 w-full"></div>
        </el-card>
      </el-col>

      <!-- 新增景点 -->
      <el-col :span="6">
        <el-card shadow="hover" class="trend-card bg-gradient-to-br from-emerald-50 to-white border-emerald-100">
          <div class="flex justify-between items-start mb-2">
            <div>
              <p class="text-sm text-gray-500 mb-1">新增景点</p>
              <h3 class="text-2xl font-bold text-gray-800">32</h3>
            </div>
            <div class="p-2 bg-emerald-100 text-emerald-600 rounded-lg">
              <el-icon><Location /></el-icon>
            </div>
          </div>
          <p class="text-xs text-gray-400 font-medium mb-3">当月累计新增</p>
          <div ref="sparklineSpots" class="sparkline h-12 w-full"></div>
        </el-card>
      </el-col>

      <!-- 订单数 -->
      <el-col :span="6">
        <el-card shadow="hover" class="trend-card bg-gradient-to-br from-orange-50 to-white border-orange-100">
          <div class="flex justify-between items-start mb-2">
            <div>
              <p class="text-sm text-gray-500 mb-1">今日订单</p>
              <h3 class="text-2xl font-bold text-gray-800">156</h3>
            </div>
            <div class="p-2 bg-orange-100 text-orange-600 rounded-lg">
              <el-icon><ShoppingCart /></el-icon>
            </div>
          </div>
          <p class="text-xs text-red-500 font-medium mb-3">-2.1% 较昨日</p>
          <div ref="sparklineOrders" class="sparkline h-12 w-full"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 中部：业务流转动态墙 + 行为热力图 -->
    <el-row :gutter="20">
      <!-- 左侧：行为热力图 -->
      <el-col :span="16">
        <el-card shadow="hover" class="border-0 mb-6">
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
          <div ref="mainLineChartRef" class="w-full" style="height: 300px;"></div>
        </el-card>

        <el-card shadow="hover" class="border-0">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-bold text-gray-800">近一年订单密集度 (Contribution)</span>
            </div>
          </template>
          <div ref="heatmapRef" class="w-full" style="height: 220px;"></div>
        </el-card>
      </el-col>

      <!-- 右侧：实时日志轴 -->
      <el-col :span="8">
        <el-card shadow="hover" class="border-0 timeline-card h-full">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-bold text-gray-800">业务流转动态墙</span>
              <el-tag size="small" type="success" effect="light" round>实时</el-tag>
            </div>
          </template>
          <div class="timeline-container px-2 py-4 h-[550px] overflow-y-auto">
            <el-timeline>
              <el-timeline-item v-for="(activity, index) in timelineActivities" :key="index" :type="activity.type" :color="activity.color" :size="activity.size" :timestamp="activity.timestamp" placement="top">
                <el-card shadow="never" class="timeline-item-card border-none bg-gray-50 rounded-xl">
                  <h4 class="text-sm font-bold text-gray-800 m-0 mb-1">{{ activity.title }}</h4>
                  <p class="text-xs text-gray-500 m-0">{{ activity.desc }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Money, User, Location, ShoppingCart, RefreshRight } from '@element-plus/icons-vue'
import { getOrderTrend } from './api.js'

// Refs for charts
const sparklineRevenue = ref(null)
const sparklineUsers = ref(null)
const sparklineSpots = ref(null)
const sparklineOrders = ref(null)
const heatmapRef = ref(null)
const mainLineChartRef = ref(null)

const charts = []
const trendMode = ref('weekday')
const selectedRange = ref(0)

// Mock Data
const generateSparklineData = () => Array.from({length: 10}, () => Math.floor(Math.random() * 100) + 20)

const timelineActivities = ref([
  { title: '新订单产生', desc: '用户 "李小明" 预订了 "九寨沟风景区" 门票 (¥240.00)', timestamp: '刚刚', type: 'primary', color: '#3b82f6', size: 'large' },
  { title: '用户发表攻略', desc: '用户 "背包客" 发布了长篇攻略《三亚湾五日游防坑指南》', timestamp: '5分钟前', color: '#10b981' },
  { title: '景点上新', desc: '管理员 "System" 上架了新景点 "环球影城全天票"', timestamp: '半小时前', color: '#8b5cf6' },
  { title: '大额订单付款', desc: '用户 "张**" 支付了订单 #2024040188219 (¥3,200.00)', timestamp: '1小时前', type: 'warning', color: '#f59e0b' },
  { title: '系统通告', desc: '推荐引擎矩阵已根据最新数据完成自动重排计算', timestamp: '2小时前', color: '#64748b' },
  { title: '新订单产生', desc: '用户 "王五" 预订了 "故宫博物院" 门票 (¥60.00)', timestamp: '3小时前', color: '#3b82f6' },
  { title: '异常拦截', desc: '系统成功拦截一次异常的退款请求', timestamp: '4小时前', color: '#ef4444' }
])

const initSparklines = () => {
  const configs = [
    { ref: sparklineRevenue, color: '#3b82f6', areaColor: 'rgba(59, 130, 246, 0.1)' },
    { ref: sparklineUsers, color: '#8b5cf6', areaColor: 'rgba(139, 92, 246, 0.1)' },
    { ref: sparklineSpots, color: '#10b981', areaColor: 'rgba(16, 185, 129, 0.1)' },
    { ref: sparklineOrders, color: '#f97316', areaColor: 'rgba(249, 115, 22, 0.1)' }
  ]

  configs.forEach(cfg => {
    if (!cfg.ref.value) return
    const chart = echarts.init(cfg.ref.value)
    charts.push(chart)
    chart.setOption({
      grid: { left: 0, right: 0, top: 0, bottom: 0 },
      xAxis: { type: 'category', show: false },
      yAxis: { type: 'value', show: false },
      series: [{
        data: generateSparklineData(),
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { color: cfg.color, width: 2 },
        areaStyle: { color: cfg.areaColor }
      }]
    })
  })
}

const getVirtualData = (year) => {
  const date = +echarts.time.parse(year + '-01-01');
  const end = +echarts.time.parse(+year + 1 + '-01-01');
  const dayTime = 3600 * 24 * 1000;
  const data = [];
  for (let time = date; time < end; time += dayTime) {
    data.push([
      echarts.time.format(time, '{yyyy}-{MM}-{dd}', false),
      Math.floor(Math.random() * 100) // Dummy order count
    ]);
  }
  return data;
}

const initHeatmap = () => {
  if (!heatmapRef.value) return
  const chart = echarts.init(heatmapRef.value)
  charts.push(chart)
  
  const year = new Date().getFullYear().toString()
  
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
        color: ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'] // GitHub Colors
      }
    },
    calendar: {
      top: 30,
      left: 30,
      right: 30,
      cellSize: ['auto', 16],
      range: year,
      itemStyle: {
        borderWidth: 2,
        borderColor: '#fff',
        borderRadius: 4
      },
      splitLine: { show: false },
      yearLabel: { show: false },
      dayLabel: { color: '#94a3b8', fontSize: 10 },
      monthLabel: { color: '#94a3b8', fontSize: 10 }
    },
    series: {
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: getVirtualData(year)
    }
  })
}

const initMainLineChart = () => {
  if (!mainLineChartRef.value) return
  const chart = echarts.init(mainLineChartRef.value)
  charts.push(chart)
}

const formatMoney = (value) => Number(value || 0)

const formatAxisLabel = (label) => {
  if (trendMode.value === 'weekday') return label
  const [, month, day] = `${label}`.split('-')
  return month && day ? `${month}-${day}` : label
}

const updateMainLineChart = (list) => {
  const chart = charts.find(item => item.getDom() === mainLineChartRef.value)
  if (!chart) return

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' },
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
    legend: { data: ['订单量', '收入'], bottom: 0 },
    grid: { left: '2%', right: '2%', top: '5%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: list.map(item => formatAxisLabel(item.date)),
      axisLine: { lineStyle: { color: '#e2e8f0' } }
    },
    yAxis: [
      {
        type: 'value',
        name: '订单量',
        splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } }
      },
      {
        type: 'value',
        name: '收入',
        axisLabel: {
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
        itemStyle: { color: '#3b82f6' },
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
        itemStyle: { color: '#10b981' }
      }
    ]
  })
}

const fetchData = async () => {
  const response = await getOrderTrend(selectedRange.value, trendMode.value)
  updateMainLineChart(response.data?.list || [])
  setTimeout(() => {
    window.dispatchEvent(new Event('resize'))
  }, 100)
}

const handleTrendModeChange = () => {
  // 两种统计方式切换时回到各自最常用的默认时间范围，避免口径混淆。
  selectedRange.value = trendMode.value === 'weekday' ? 0 : 7
  fetchData()
}

const handleResize = () => {
  charts.forEach(c => c.resize())
}

onMounted(() => {
  nextTick(() => {
    initSparklines()
    initHeatmap()
    initMainLineChart()
    window.addEventListener('resize', handleResize)
    fetchData()
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
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
.text-gray-800 { color: #1f2937; }
.text-gray-500 { color: #6b7280; }
.text-gray-400 { color: #9ca3af; }
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

.trend-card {
  border-radius: 16px;
  overflow: hidden;
  border-width: 1px;
  border-style: solid;
}
.border-blue-100 { border-color: #dbeafe; }
.border-purple-100 { border-color: #ede9fe; }
.border-emerald-100 { border-color: #d1fae5; }
.border-orange-100 { border-color: #ffedd5; }

/* Subtle gradients */
.bg-gradient-to-br {
  background-image: linear-gradient(to bottom right, var(--tw-gradient-stops));
}
.from-blue-50 { --tw-gradient-from: #eff6ff; --tw-gradient-stops: var(--tw-gradient-from), #fff; }
.from-purple-50 { --tw-gradient-from: #faf5ff; --tw-gradient-stops: var(--tw-gradient-from), #fff; }
.from-emerald-50 { --tw-gradient-from: #ecfdf5; --tw-gradient-stops: var(--tw-gradient-from), #fff; }
.from-orange-50 { --tw-gradient-from: #fff7ed; --tw-gradient-stops: var(--tw-gradient-from), #fff; }

.trend-toolbar {
  gap: 16px;
  flex-wrap: wrap;
}

.trend-controls {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 12px 16px;
  border: 1px solid #d9e3ef;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.control-group {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
}

.control-label {
  font-size: 12px;
  color: #475569;
  font-weight: 700;
  white-space: nowrap;
}

.control-divider {
  width: 1px;
  height: 40px;
  background: #dbe3ee;
}

.timeline-container {
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;
  }
}

.timeline-item-card {
  transition: all 0.3s ease;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px -2px rgba(0, 0, 0, 0.05);
  }
}

:deep(.el-timeline-item__node--large) {
  width: 16px;
  height: 16px;
  left: -2px;
}
</style>
