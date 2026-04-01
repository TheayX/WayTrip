<template>
  <!-- 状态卡片 -->
  <el-row :gutter="24" class="status-row">
    <el-col :span="6">
      <el-card shadow="hover" class="status-card" :body-style="{ padding: '0px' }">
        <div class="status-card-content engine-bg">
          <div class="status-info">
            <div class="status-label">引擎状态</div>
            <div class="status-value">
              <el-tag :type="status.computing ? 'warning' : 'success'" effect="dark" round>
                {{ status.computing ? '计算中...' : '就绪' }}
              </el-tag>
            </div>
          </div>
          <div class="status-icon">
            <el-icon><Cpu /></el-icon>
          </div>
        </div>
      </el-card>
    </el-col>

    <el-col :span="6">
      <el-card shadow="hover" class="status-card" :body-style="{ padding: '0px' }">
        <div class="status-card-content time-bg">
          <div class="status-info">
            <div class="status-label">上次更新</div>
            <div class="status-value small">{{ status.lastUpdateTime || '暂无记录' }}</div>
          </div>
          <div class="status-icon">
            <el-icon><Timer /></el-icon>
          </div>
        </div>
      </el-card>
    </el-col>

    <el-col :span="6">
      <el-card shadow="hover" class="status-card" :body-style="{ padding: '0px' }">
        <div class="status-card-content users-bg">
          <div class="status-info">
            <div class="status-label">覆盖用户</div>
            <div class="status-value">{{ status.totalUsers ?? '-' }}</div>
          </div>
          <div class="status-icon">
            <el-icon><User /></el-icon>
          </div>
        </div>
      </el-card>
    </el-col>

    <el-col :span="6">
      <el-card shadow="hover" class="status-card" :body-style="{ padding: '0px' }">
        <div class="status-card-content spots-bg">
          <div class="status-info">
            <div class="status-label">覆盖景点</div>
            <div class="status-value">{{ status.totalSpots ?? '-' }}</div>
          </div>
          <div class="status-icon">
            <el-icon><Location /></el-icon>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { Cpu, Timer, User, Location } from '@element-plus/icons-vue'

defineProps({
  status: {
    type: Object,
    required: true
  }
})
</script>

<style lang="scss" scoped>
.status-row {
  margin-bottom: 24px;
}

.status-card {
  border-radius: 12px;
  border: none;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.15) !important;
  }
}

.status-card-content {
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

  &.engine-bg { background: linear-gradient(135deg, #52c41a 0%, #36cfc9 100%); }
  &.time-bg { background: linear-gradient(135deg, #1890ff 0%, #69b1ff 100%); }
  &.users-bg { background: linear-gradient(135deg, #722ed1 0%, #b37feb 100%); }
  &.spots-bg { background: linear-gradient(135deg, #ff4d4f 0%, #ff7a45 100%); }
}

.status-info {
  z-index: 1;

  .status-label {
    font-size: 14px;
    opacity: 0.9;
    margin-bottom: 8px;
  }

  .status-value {
    font-size: 28px;
    font-weight: bold;
    line-height: 1.3;

    &.small {
      font-size: 15px;
      font-weight: normal;
    }
  }
}

.status-icon {
  font-size: 56px;
  opacity: 0.8;
  z-index: 1;
  transition: transform 0.3s;
}

.status-card:hover .status-icon {
  transform: scale(1.1) rotate(5deg);
}
</style>
