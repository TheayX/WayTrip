<!-- 景点详情侧边信息区 -->
<template>
  <div class="detail-sidebar">
    <div class="sidebar-card premium-card">
      <p class="sidebar-kicker">目的地概览</p>
      <h1 class="spot-name">{{ spot.name }}</h1>
      <div class="spot-meta">
        <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
        <span class="meta-count">({{ spot.ratingCount || 0 }}条评价)</span>
        <span>{{ spot.regionName }} / {{ spot.categoryName }}</span>
      </div>
      <div class="spot-stats">
        <span>{{ spot.favoriteCount ?? 0 }}收藏</span>
        <span>{{ spot.viewCount ?? 0 }}浏览</span>
      </div>
      <div class="spot-price-row">
        <span class="big-price">¥{{ spot.price }}</span>
        <span class="price-label">/人</span>
      </div>
      <div class="action-group">
        <el-button type="primary" size="large" class="buy-btn" @click="$emit('buy')">立即购票</el-button>
        <el-button size="large" class="fav-btn" :class="{ active: spot.isFavorite }" @click="$emit('toggle-favorite')">
          {{ spot.isFavorite ? '已收藏' : '收藏景点' }}
        </el-button>
      </div>
      <div class="time-info">
        <div class="time-item">
          <span class="time-label">创建时间</span>
          <span class="time-value">{{ formatDateTime(spot.createdAt) }}</span>
        </div>
        <div class="time-item">
          <span class="time-label">更新时间</span>
          <span class="time-value">{{ formatDateTime(spot.updatedAt) }}</span>
        </div>
      </div>
    </div>

    <div class="sidebar-card premium-card">
      <h3 class="sidebar-title">出行信息</h3>
      <div class="detail-item">
        <span class="detail-label">开放时间</span>
        <span class="detail-value">{{ spot.openTime || '暂无信息' }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">景点地址</span>
        <span class="detail-value">{{ spot.address || '暂无信息' }}{{ distanceText ? ` · 距你 ${distanceText}` : '' }}</span>
      </div>
    </div>

    <div class="sidebar-card premium-card">
      <h3 class="sidebar-title">分享体验</h3>
      <div class="rating-input">
        <span class="rating-label">评分：</span>
        <el-rate :model-value="ratingForm.score" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" @update:model-value="$emit('update:score', $event)" />
      </div>
      <el-input
        :model-value="ratingForm.comment"
        type="textarea"
        :rows="4"
        placeholder="写下这次游玩的真实感受..."
        maxlength="500"
        show-word-limit
        @update:model-value="$emit('update:comment', $event)"
      />
      <el-button type="primary" class="submit-rating-btn" :loading="submittingRating" @click="$emit('submit-rating')">
        提交评价
      </el-button>
    </div>
  </div>
</template>

<script setup>
// 时间格式化在侧栏内本地完成，避免为了展示型逻辑扩散全局工具函数。
const formatDateTime = (value) => {
  if (!value) return '暂无信息'
  const raw = typeof value === 'string' ? value.replace(' ', 'T') : value
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return String(value).replace('T', ' ').slice(0, 19)
  }
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

// 侧栏集中承接购票、收藏和评价入口，详情页主体只保留内容展示。
defineProps({
  spot: {
    type: Object,
    required: true
  },
  distanceText: {
    type: String,
    default: ''
  },
  ratingForm: {
    type: Object,
    required: true
  },
  submittingRating: {
    type: Boolean,
    default: false
  }
})

// 评价表单的输入和值变更都交回父层，便于和登录态、提交状态统一联动。
defineEmits(['buy', 'toggle-favorite', 'submit-rating', 'update:score', 'update:comment'])
</script>

<style lang="scss" scoped>
.detail-sidebar {
  width: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  padding: 22px;
}

.sidebar-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.spot-name {
  font-size: 30px;
  line-height: 1.12;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.04em;
  margin-bottom: 10px;
}

.spot-meta {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-count {
  color: #94a3b8;
}

.spot-price-row {
  margin-bottom: 18px;
}

.spot-stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-bottom: 18px;
  font-size: 13px;
  color: #64748b;
}

.big-price {
  font-size: 36px;
  font-weight: 700;
  color: #ef4444;
}

.price-label {
  font-size: 14px;
  color: #64748b;
}

.buy-btn,
.fav-btn,
.submit-rating-btn {
  width: 100%;
}

.action-group {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 10px;
}

.buy-btn,
.fav-btn {
  min-height: 46px;
}

.fav-btn {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #334155;
  box-shadow: none;
}

.fav-btn:hover,
.fav-btn:focus-visible {
  border-color: rgba(200, 169, 91, 0.45);
  color: #8a6a2f;
  background: #fffdf7;
}

.fav-btn.active {
  border-color: rgba(200, 169, 91, 0.45);
  color: #8a6a2f;
  background: #fffdf7;
}

.sidebar-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 14px;
  letter-spacing: -0.02em;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f7;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 14px;
  color: #64748b;
  flex-shrink: 0;
}

.detail-value {
  font-size: 14px;
  color: #0f172a;
  text-align: right;
  line-height: 1.7;
}

.rating-input {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.rating-label {
  font-size: 14px;
  color: #475569;
}

.submit-rating-btn {
  margin-top: 14px;
}

.time-info {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eef2f7;
  display: grid;
  gap: 10px;
}

.time-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.time-label {
  color: #64748b;
  flex-shrink: 0;
}

.time-value {
  color: #0f172a;
  text-align: right;
}

@media (max-width: 992px) {
  .detail-sidebar {
    width: 100%;
  }


  .action-group {
    grid-template-columns: 1fr;
  }
}
</style>
