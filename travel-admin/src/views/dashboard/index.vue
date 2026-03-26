<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="24" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '0px' }">
          <div class="stat-card-content users-bg">
            <div class="stat-info">
              <div class="stat-label">用户总数</div>
              <div class="stat-value">{{ overview.totalUsers || 0 }}</div>
              <div class="stat-extra">
                <span class="trend up"><el-icon><Top /></el-icon>今日新增 {{ overview.todayNewUsers || 0 }}</span>
              </div>
            </div>
            <div class="stat-icon">
              <el-icon><UserFilled /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '0px' }">
          <div class="stat-card-content spots-bg">
            <div class="stat-info">
              <div class="stat-label">景点总数</div>
              <div class="stat-value">{{ overview.totalSpots || 0 }}</div>
              <div class="stat-extra">
                <span class="text">风景无限好</span>
              </div>
            </div>
            <div class="stat-icon">
              <el-icon><LocationInformation /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '0px' }">
          <div class="stat-card-content orders-bg">
            <div class="stat-info">
              <div class="stat-label">订单总数</div>
              <div class="stat-value">{{ overview.totalOrders || 0 }}</div>
              <div class="stat-extra">
                <span class="trend up"><el-icon><Top /></el-icon>今日 {{ overview.todayOrders || 0 }}单</span>
              </div>
            </div>
            <div class="stat-icon">
              <el-icon><Tickets /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '0px' }">
          <div class="stat-card-content revenue-bg">
            <div class="stat-info">
              <div class="stat-label">总收入</div>
              <div class="stat-value">¥{{ formatMoney(overview.totalRevenue) }}</div>
              <div class="stat-extra">
                <span class="trend up"><el-icon><Top /></el-icon>今日 ¥{{ formatMoney(overview.todayRevenue) }}</span>
              </div>
            </div>
            <div class="stat-icon">
              <el-icon><Money /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="24" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover" class="box-card">
          <template #header>
            <div class="card-header">
              <span class="title">订单趋势图</span>
              <el-button color="#1890ff" @click="handleUpdateMatrix" :loading="updatingMatrix" round>
                <el-icon><RefreshRight /></el-icon>
                更新推荐引擎矩阵
              </el-button>
            </div>
          </template>
          <div class="chart-container" ref="chartRef"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="box-card ranking-card">
          <template #header>
            <div class="card-header">
              <span class="title">热门景点 TOP5</span>
              <el-tag effect="light" type="warning" round>本月榜单</el-tag>
            </div>
          </template>
          <div class="hot-spots">
            <div 
              v-for="(spot, index) in hotSpots" 
              :key="spot.id"
              class="hot-spot-item"
            >
              <div class="rank-badge" :class="'rank-' + (index + 1)">
                {{ index + 1 }}
              </div>
              <div class="spot-info">
                <div class="name">{{ spot.name }}</div>
                <el-progress 
                  :percentage="(spot.orderCount / (hotSpots[0]?.orderCount || 1)) * 100" 
                  :show-text="false"
                  :color="getProgressColor(index)"
                  :stroke-width="6"
                />
              </div>
              <div class="count">{{ spot.orderCount }} 单</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, shallowRef, nextTick, onUnmounted } from 'vue'
import { getOverview, getOrderTrend, getHotSpots } from '@/api/dashboard'
import { updateRecommendationMatrix } from '@/api/recommendation'
import { ElMessage } from 'element-plus'
import { RefreshRight, UserFilled, LocationInformation, Tickets, Money, Top } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const overview = ref({})
const orderTrend = ref([])
const hotSpots = ref([])
const updatingMatrix = ref(false)

const chartRef = ref(null)
const chartInstance = shallowRef(null)

// 触发更新推荐矩阵
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

// 格式化金额
const formatMoney = (value) => {
  if (!value) return '0.00'
  return Number(value).toFixed(2)
}

// 获取进度条颜色
const getProgressColor = (index) => {
  const colors = ['#f56c6c', '#e6a23c', '#5cb87a', '#1890ff', '#8c939d']
  return colors[index] || colors[4]
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return
  chartInstance.value = echarts.init(chartRef.value)
  
  const dates = orderTrend.value.map(i => i.date.substring(5))
  const data = orderTrend.value.map(i => i.orderCount)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      top: '12%',
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        data: dates,
        axisTick: { alignWithLabel: true },
        axisLine: { lineStyle: { color: '#e0e6ed' } },
        axisLabel: { color: '#666' }
      }
    ],
    yAxis: [
      {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { type: 'dashed', color: '#ebeef5' } },
        axisLabel: { color: '#666' }
      }
    ],
    series: [
      {
        name: '订单量',
        type: 'bar',
        barWidth: '40%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2b9dfc' },
              { offset: 0.7, color: '#116df0' },
              { offset: 1, color: '#116df0' }
            ])
          }
        },
        data: data
      }
    ]
  }

  chartInstance.value.setOption(option)
}

// 获取数据
const fetchData = async () => {
  try {
    const [overviewRes, trendRes, hotRes] = await Promise.all([
      getOverview(),
      getOrderTrend(7),
      getHotSpots(5)
    ])
    overview.value = overviewRes.data || {}
    orderTrend.value = trendRes.data?.list || []
    hotSpots.value = hotRes.data?.list || []
    
    // 初始化图表
    nextTick(() => {
      initChart()
    })
  } catch (e) {
    console.error('获取仪表板数据失败', e)
  }
}

// 监听窗口大小变化
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
.dashboard {
  .stat-row {
    margin-bottom: 24px;
  }
  
  .stat-card {
    border-radius: 12px;
    border: none;
    transition: all 0.3s;
    
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.15) !important;
    }
  }

  .stat-card-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24px;
    border-radius: 12px;
    color: #fff;
    position: relative;
    overflow: hidden;

    &::after {
      content: '';
      position: absolute;
      top: -20px;
      right: -20px;
      width: 100px;
      height: 100px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 50%;
    }

    &.users-bg { background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%); }
    &.spots-bg { background: linear-gradient(135deg, #722ed1 0%, #eb2f96 100%); }
    &.orders-bg { background: linear-gradient(135deg, #ff4d4f 0%, #ff7a45 100%); }
    &.revenue-bg { background: linear-gradient(135deg, #52c41a 0%, #fadb14 100%); }
  }

  .stat-info {
    z-index: 1;
    .stat-label {
      font-size: 14px;
      opacity: 0.9;
      margin-bottom: 8px;
    }
    .stat-value {
      font-size: 32px;
      font-weight: bold;
      line-height: 1.2;
      margin-bottom: 12px;
    }
    .stat-extra {
      font-size: 12px;
      display: flex;
      align-items: center;
      gap: 4px;
      
      .trend {
        background: rgba(255, 255, 255, 0.2);
        padding: 2px 8px;
        border-radius: 10px;
        display: inline-flex;
        align-items: center;
        gap: 2px;
      }
      .text {
        opacity: 0.8;
      }
    }
  }

  .stat-icon {
    font-size: 64px;
    opacity: 0.8;
    z-index: 1;
    transition: transform 0.3s;
  }
  
  .stat-card:hover .stat-icon {
    transform: scale(1.1) rotate(5deg);
  }

  .box-card {
    border-radius: 12px;
    border: none;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .title {
        font-size: 16px;
        font-weight: 600;
        color: #1f2f3d;
        position: relative;
        padding-left: 12px;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 50%;
          transform: translateY(-50%);
          width: 4px;
          height: 16px;
          background: #1890ff;
          border-radius: 2px;
        }
      }
    }
  }

  .chart-container {
    height: 380px;
    width: 100%;
  }

  .ranking-card {
    height: 100%;
    
    :deep(.el-card__body) {
      padding: 0 20px 20px 20px;
    }
  }

  .hot-spots {
    .hot-spot-item {
      display: flex;
      align-items: center;
      padding: 18px 0;
      border-bottom: 1px solid #f0f2f5;
      transition: background-color 0.3s;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background-color: #fafbfc;
        border-radius: 6px;
      }
    }

    .rank-badge {
      width: 28px;
      height: 28px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: bold;
      margin-right: 16px;
      background: #f4f4f5;
      color: #909399;
      flex-shrink: 0;

      &.rank-1 { background: #fef0f0; color: #f56c6c; }
      &.rank-2 { background: #fdf6ec; color: #e6a23c; }
      &.rank-3 { background: #f0f9eb; color: #67c23a; }
    }

    .spot-info {
      flex: 1;
      margin-right: 16px;
      min-width: 0;
      
      .name {
        font-size: 15px;
        color: #303133;
        margin-bottom: 8px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-weight: 500;
      }
    }

    .count {
      font-size: 14px;
      color: #606266;
      font-weight: bold;
      min-width: 50px;
      text-align: right;
    }
  }
}
</style>
