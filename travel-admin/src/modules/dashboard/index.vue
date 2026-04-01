<template>
  <div class="dashboard premium-dashboard">
    <!-- 顶部欢迎语与操作区 -->
    <div class="dashboard-header">
      <div class="welcome-text">
        <h2 class="title" style="margin: 0; font-size: 24px;">首页概览</h2>
        <p class="subtitle" style="color: #64748b; margin-top: 8px;">在这里监控您的业务数据和运营健康度</p>
      </div>
      <div class="actions">
        <el-button type="primary" class="modern-btn" @click="handleUpdateMatrix" :loading="updatingMatrix">
          <el-icon class="el-icon--left"><MagicStick /></el-icon>
          驱动推荐矩阵
        </el-button>
      </div>
    </div>

    <!-- 顶层核心指标卡片群 -->
    <el-row :gutter="24" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-body">
            <div class="metric-content">
              <span class="metric-title">总用户数 (Users)</span>
              <div class="metric-value">{{ overview.totalUsers || 0 }}</div>
              <div class="metric-trend success">
                <el-icon><TrendCharts /></el-icon>
                <span>今日新增 {{ overview.todayNewUsers || 0 }}</span>
              </div>
            </div>
            <div class="metric-icon-wrap" style="background-color: #eff6ff; color: #3b82f6;">
              <el-icon><User /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-body">
            <div class="metric-content">
              <span class="metric-title">景点库存 (Spots)</span>
              <div class="metric-value">{{ overview.totalSpots || 0 }}</div>
              <div class="metric-trend neutral">
                <el-icon><MapLocation /></el-icon>
                <span>风景数据尽在掌握</span>
              </div>
            </div>
            <div class="metric-icon-wrap" style="background-color: #f5f3ff; color: #8b5cf6;">
              <el-icon><LocationInformation /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-body">
            <div class="metric-content">
              <span class="metric-title">总订单数 (Orders)</span>
              <div class="metric-value">{{ overview.totalOrders || 0 }}</div>
              <div class="metric-trend warning">
                <el-icon><DataLine /></el-icon>
                <span>今日 {{ overview.todayOrders || 0 }} 单</span>
              </div>
            </div>
            <div class="metric-icon-wrap" style="background-color: #fff7ed; color: #f97316;">
              <el-icon><ShoppingCart /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-body">
            <div class="metric-content">
              <span class="metric-title">累计收入 (Revenue)</span>
              <div class="metric-value">¥{{ formatMoney(overview.totalRevenue) }}</div>
              <div class="metric-trend success">
                <el-icon><Money /></el-icon>
                <span>今日 ¥{{ formatMoney(overview.todayRevenue) }}</span>
              </div>
            </div>
            <div class="metric-icon-wrap" style="background-color: #ecfdf5; color: #10b981;">
              <el-icon><Wallet /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 下半部分图表与排行榜 -->
    <el-row :gutter="24" class="chart-row" style="margin-top: 24px;">
      <!-- 左侧：现代化趋势折线图 -->
      <el-col :span="16">
        <el-card shadow="hover" class="panel-card chart-panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">近7日订单成交趋势</span>
            </div>
          </template>
          <div class="chart-container" ref="chartRef" style="height: 380px; width: 100%;"></div>
        </el-card>
      </el-col>

      <!-- 右侧：榜单排行 -->
      <el-col :span="8">
        <el-card shadow="hover" class="panel-card rank-panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">热门景点 TOP 5</span>
              <el-tag effect="light" size="small" type="primary" class="round-tag">当月活跃</el-tag>
            </div>
          </template>
          <div class="rank-list">
            <div class="rank-item" v-for="(spot, index) in hotSpots" :key="spot.id">
              <div class="rank-medal" :class="'medal-' + (index + 1)">{{ index + 1 }}</div>
              <div class="rank-content">
                <div class="rank-name">{{ spot.name }}</div>
                <div class="rank-bar-wrap">
                  <div class="rank-bar" :style="{ width: ((spot.orderCount / (hotSpots[0]?.orderCount || 1)) * 100) + '%' }"></div>
                </div>
              </div>
              <div class="rank-score">{{ spot.orderCount }} <span style="font-size: 12px; color: #94a3b8;">单</span></div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, shallowRef, nextTick, onUnmounted } from 'vue'
import { getOverview, getOrderTrend, getHotSpots } from '@/modules/dashboard/api.js'
import { updateRecommendationMatrix } from '@/modules/recommendation/api/recommendation.js'
import { ElMessage } from 'element-plus'
import { MagicStick, TrendCharts, User, MapLocation, LocationInformation, DataLine, ShoppingCart, Money, Wallet } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const overview = ref({})
const orderTrend = ref([])
const hotSpots = ref([])
const updatingMatrix = ref(false)

const chartRef = ref(null)
const chartInstance = shallowRef(null)

const handleUpdateMatrix = async () => {
  try {
    updatingMatrix.value = true
    await updateRecommendationMatrix()
    ElMessage.success('推荐引擎矩阵更新计算完成！')
  } catch (e) {
    ElMessage.error('触发推荐矩阵更新失败')
  } finally {
    updatingMatrix.value = false
  }
}

const formatMoney = (value) => {
  if (!value) return '0.00'
  return Number(value).toFixed(2)
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance.value = echarts.init(chartRef.value)
  
  const dates = orderTrend.value.map(i => i.date.substring(5))
  const data = orderTrend.value.map(i => i.orderCount)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: 'rgba(0,0,0,0.1)' } },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' },
      extraCssText: 'box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1); border-radius: 8px;'
    },
    grid: {
      top: '10%', left: '2%', right: '3%', bottom: '0%', containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', margin: 16 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#64748b' }
    },
    series: [
      {
        name: '订单量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        showSymbol: false,
        lineStyle: {
          width: 3,
          color: '#3b82f6',
          shadowColor: 'rgba(59, 130, 246, 0.3)',
          shadowBlur: 10,
          shadowOffsetY: 5
        },
        itemStyle: {
          color: '#3b82f6',
          borderColor: '#ffffff',
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.2)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.01)' }
          ])
        },
        data: data
      }
    ]
  }

  chartInstance.value.setOption(option)
}

const fetchData = async () => {
  try {
    const [overviewRes, trendRes, hotRes] = await Promise.all([
      getOverview(), getOrderTrend(7), getHotSpots(5)
    ])
    overview.value = overviewRes.data || {}
    orderTrend.value = trendRes.data?.list || []
    hotSpots.value = hotRes.data?.list || []
    
    nextTick(() => {
      initChart()
    })
  } catch (e) {
    console.error('获取仪表板数据失败', e)
  }
}

const handleResize = () => {
  chartInstance.value?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance.value?.dispose()
})
</script>

<style lang="scss" scoped>
.premium-dashboard {
  .dashboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding-bottom: 16px;

    .title {
      color: #0f172a;
      font-weight: 700;
    }
  }

  .modern-btn {
    border-radius: 8px;
    background: linear-gradient(135deg, #3b82f6, #6366f1);
    border: none;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
    transition: all 0.3s;
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
    }
  }

  .metric-card {
    border-radius: 16px;
    background: #ffffff;
    border: 1px solid #f1f5f9;
    
    .metric-body {
      padding: 18px 20px;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .metric-content {
      .metric-title {
        color: #64748b;
        font-size: 14px;
        font-weight: 500;
        display: block;
        margin-bottom: 8px;
      }
      .metric-value {
        color: #0f172a;
        font-size: 30px;
        font-weight: 700;
        margin-bottom: 12px;
        line-height: 1;
      }
      .metric-trend {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        font-weight: 500;
        
        &.success { color: #10b981; }
        &.warning { color: #f59e0b; }
        &.neutral { color: #8b5cf6; }
      }
    }

    .metric-icon-wrap {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
    }
  }

  .panel-card {
    border-radius: 16px;
    border: 1px solid #f1f5f9;
    height: 100%;

    :deep(.el-card__header) {
      padding: 20px 24px 0;
      border-bottom: none;
    }
    :deep(.el-card__body) {
      padding: 24px;
    }

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .panel-title {
        font-size: 16px;
        font-weight: 600;
        color: #0f172a;
      }
    }
  }

  .round-tag {
    border-radius: 12px;
    padding: 0 10px;
  }

  .rank-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
    margin-top: 8px;

    .rank-item {
      display: flex;
      align-items: center;
      
      .rank-medal {
        width: 28px;
        height: 28px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 13px;
        margin-right: 16px;
        background: #f1f5f9;
        color: #64748b;

        &.medal-1 { background: #fee2e2; color: #ef4444; }
        &.medal-2 { background: #fef3c7; color: #f59e0b; }
        &.medal-3 { background: #dcfce7; color: #10b981; }
      }

      .rank-content {
        flex: 1;
        margin-right: 16px;

        .rank-name {
          color: #334155;
          font-weight: 600;
          font-size: 14px;
          margin-bottom: 6px;
        }

        .rank-bar-wrap {
          height: 6px;
          background: #f1f5f9;
          border-radius: 3px;
          overflow: hidden;

          .rank-bar {
            height: 100%;
            background: linear-gradient(90deg, #60a5fa, #3b82f6);
            border-radius: 3px;
            transition: width 1s ease-in-out;
          }
        }
      }

      .rank-score {
        font-weight: 700;
        color: #0f172a;
        font-size: 16px;
        text-align: right;
        min-width: 40px;
      }
    }
  }
}
</style>
