<!-- 景点详情侧边信息区 -->
<template>
  <div class="detail-sidebar">
    <div class="sidebar-card card">
      <h1 class="spot-name">{{ spot.name }}</h1>
      <div class="spot-meta">
        <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
        <span class="meta-count">({{ spot.ratingCount || 0 }}条评价)</span>
        <el-divider direction="vertical" />
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

    <div class="sidebar-card card">
      <div class="detail-item">
        <span class="detail-label">开放时间</span>
        <span class="detail-value">{{ spot.openTime || '暂无信息' }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">景点地址</span>
        <span class="detail-value">{{ spot.address || '暂无信息' }}{{ distanceText ? ` · 距你 ${distanceText}` : '' }}</span>
      </div>
    </div>

    <div class="sidebar-card card">
      <h3 class="sidebar-title">写评价</h3>
      <div class="rating-input">
        <span class="rating-label">评分：</span>
        <el-rate :model-value="ratingForm.score" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" @update:model-value="$emit('update:score', $event)" />
      </div>
      <el-input
        :model-value="ratingForm.comment"
        type="textarea"
        :rows="3"
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
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  padding: 20px;
  border-radius: 12px;
}

.spot-name {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.spot-meta {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.meta-count {
  color: #c0c4cc;
}

.spot-price-row {
  margin-bottom: 16px;
}

.big-price {
  font-size: 32px;
  font-weight: 700;
  color: #f56c6c;
}

.price-label {
  font-size: 14px;
  color: #909399;
}

.buy-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.fav-btn {
  width: 100%;
  height: 40px;
  border-radius: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.detail-label {
  font-size: 14px;
  color: #909399;
  flex-shrink: 0;
}

.detail-value {
  font-size: 14px;
  color: #303133;
  text-align: right;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.rating-input {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.rating-label {
  font-size: 14px;
  color: #606266;
}

.submit-rating-btn {
  width: 100%;
  margin-top: 12px;
  border-radius: 8px;
}

@media (max-width: 992px) {
  .detail-sidebar {
    width: 100%;
  }
}
</style>
