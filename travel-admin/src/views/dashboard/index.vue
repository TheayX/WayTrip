<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon users">👥</div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.totalUsers || 0 }}</div>
            <div class="stat-label">用户总数</div>
            <div class="stat-extra">今日新增 +{{ overview.todayNewUsers || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon spots">🏞️</div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.totalSpots || 0 }}</div>
            <div class="stat-label">景点总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon orders">📋</div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.totalOrders || 0 }}</div>
            <div class="stat-label">订单总数</div>
            <div class="stat-extra">今日 +{{ overview.todayOrders || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon revenue">💰</div>
          <div class="stat-info">
            <div class="stat-value">¥{{ formatMoney(overview.totalRevenue) }}</div>
            <div class="stat-label">总收入</div>
            <div class="stat-extra">今日 +¥{{ formatMoney(overview.todayRevenue) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>订单趋势（近7天）</span>
            </div>
          </template>
          <div class="chart-container">
            <div class="simple-chart">
              <div 
                v-for="(item, index) in orderTrend" 
                :key="index"
                class="chart-bar"
                :style="{ height: getBarHeight(item.orderCount) + '%' }"
              >
                <div class="bar-value">{{ item.orderCount }}</div>
                <div class="bar-label">{{ formatDate(item.date) }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>热门景点 TOP5</span>
          </template>
          <div class="hot-spots">
            <div 
              v-for="(spot, index) in hotSpots" 
              :key="spot.id"
              class="hot-spot-item"
            >
              <span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              <span class="name">{{ spot.name }}</span>
              <span class="count">{{ spot.orderCount }}单</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getOverview, getOrderTrend, getHotSpots } from '@/api/dashboard'

const overview = ref({})
const orderTrend = ref([])
const hotSpots = ref([])

// 格式化金额
const formatMoney = (value) => {
  if (!value) return '0.00'
  return Number(value).toFixed(2)
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(5) // 只显示 MM-DD
}

// 计算柱状图高度
const getBarHeight = (count) => {
  const max = Math.max(...orderTrend.value.map(i => i.orderCount), 1)
  return (count / max) * 80
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
  } catch (e) {
    console.error('获取仪表板数据失败', e)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-card {
    :deep(.el-card__body) {
      display: flex;
      align-items: center;
      padding: 20px;
    }
  }

  .stat-icon {
    font-size: 48px;
    margin-right: 20px;
  }

  .stat-info {
    flex: 1;
  }

  .stat-value {
    font-size: 28px;
    font-weight: bold;
    color: #333;
  }

  .stat-label {
    font-size: 14px;
    color: #999;
    margin-top: 4px;
  }

  .stat-extra {
    font-size: 12px;
    color: #67c23a;
    margin-top: 8px;
  }

  .chart-row {
    margin-top: 20px;
  }

  .chart-container {
    height: 300px;
    padding: 20px;
  }

  .simple-chart {
    display: flex;
    align-items: flex-end;
    justify-content: space-around;
    height: 100%;
  }

  .chart-bar {
    width: 60px;
    background: linear-gradient(180deg, #409EFF 0%, #67C23A 100%);
    border-radius: 4px 4px 0 0;
    position: relative;
    min-height: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    transition: height 0.3s;
  }

  .bar-value {
    position: absolute;
    top: -25px;
    font-size: 14px;
    font-weight: bold;
    color: #409EFF;
  }

  .bar-label {
    position: absolute;
    bottom: -25px;
    font-size: 12px;
    color: #999;
  }

  .hot-spots {
    .hot-spot-item {
      display: flex;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }
    }

    .rank {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: bold;
      margin-right: 12px;
      background: #f5f5f5;
      color: #999;

      &.rank-1 {
        background: #ff6b6b;
        color: #fff;
      }

      &.rank-2 {
        background: #ffa502;
        color: #fff;
      }

      &.rank-3 {
        background: #ffd93d;
        color: #fff;
      }
    }

    .name {
      flex: 1;
      font-size: 14px;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .count {
      font-size: 14px;
      color: #409EFF;
      font-weight: bold;
    }
  }
}
</style>
