<!-- 推荐系统状态卡片组 -->
<template>
  <!-- 状态卡片 -->
  <el-row :gutter="24" class="status-row">
    <el-col :span="6">
      <el-card shadow="hover" class="status-card" :body-style="{ padding: '0px' }">
        <div class="status-card-content engine-bg">
          <div class="status-info">
            <div class="status-label">引擎状态</div>
            <div class="status-value">
              <el-tag :type="status.computing ? 'warning' : 'success'" effect="light" round>
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

// 状态卡片只做信息概览，具体详情和异常处理都留在总览页内部。
defineProps({
  status: {
    type: Object,
    required: true
  }
})

// 纯展示组件不派发事件，状态变化全部由父页面拉取后重新渲染。
</script>

<style lang="scss" scoped>
.status-card {
  border-radius: 20px;
  border: none;
  transition: all 0.3s;
  background: transparent;
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 0 !important;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--wt-shadow-soft) !important;
  }
}

.status-card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-radius: 20px;
  color: var(--wt-text-primary);
  position: relative;
  overflow: hidden;
  min-height: 136px;
  box-sizing: border-box;
  border: 1px solid var(--wt-border-default);
  background:
    radial-gradient(circle at top right, var(--wt-overlay-bg) 0%, transparent 44%),
    linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);

  &::after {
    content: '';
    position: absolute;
    top: -20px;
    right: -20px;
    width: 100px;
    height: 100px;
    background: var(--wt-overlay-bg);
    border-radius: 50%;
  }

  &.engine-bg {
    background:
      radial-gradient(circle at top right, rgba(34, 211, 238, 0.16) 0%, transparent 42%),
      linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  &.time-bg {
    background:
      radial-gradient(circle at top right, rgba(96, 165, 250, 0.18) 0%, transparent 42%),
      linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  &.users-bg {
    background:
      radial-gradient(circle at top right, rgba(148, 163, 184, 0.14) 0%, transparent 42%),
      linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  &.spots-bg {
    background:
      radial-gradient(circle at top right, rgba(251, 191, 36, 0.16) 0%, transparent 42%),
      linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }
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
      font-weight: 600;
      line-height: 1.45;
      word-break: break-word;
    }
  }
}

.status-icon {
  font-size: 56px;
  opacity: 0.72;
  z-index: 1;
  transition: transform 0.3s;
  color: var(--wt-text-secondary);
}

.status-card:hover .status-icon {
  transform: scale(1.1) rotate(5deg);
}

@media (max-width: 1200px) {
  .status-card-content {
    min-height: 124px;
  }
}
</style>
