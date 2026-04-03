<!-- 景点详情侧边信息区 -->
<template>
  <div class="detail-sidebar">
    <div class="sidebar-card premium-card">
      <p class="sidebar-kicker">Spot Overview</p>
      <h1 class="spot-name">{{ spot.name }}</h1>
      <div class="spot-meta">
        <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
        <span class="meta-count">({{ spot.ratingCount || 0 }}条评价)</span>
        <span>{{ spot.regionName }} / {{ spot.categoryName }}</span>
      </div>
      <div class="spot-price-row">
        <span class="big-price">¥{{ spot.price }}</span>
        <span class="price-label">/人</span>
      </div>
      <el-button type="primary" size="large" class="buy-btn" @click="$emit('buy')">立即购票</el-button>
      <el-button :type="spot.isFavorite ? 'warning' : 'default'" size="large" class="fav-btn" @click="$emit('toggle-favorite')">
        {{ spot.isFavorite ? '已收藏' : '收藏' }}
      </el-button>
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
      <h3 class="sidebar-title">写评价</h3>
      <div class="rating-input">
        <span class="rating-label">评分：</span>
        <el-rate :model-value="ratingForm.score" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" @update:model-value="$emit('update:score', $event)" />
      </div>
      <el-input
        :model-value="ratingForm.comment"
        type="textarea"
        :rows="4"
        placeholder="分享你的旅行体验..."
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

.buy-btn {
  margin-bottom: 10px;
}

.sidebar-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 14px;
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

@media (max-width: 992px) {
  .detail-sidebar {
    width: 100%;
  }
}
</style>
