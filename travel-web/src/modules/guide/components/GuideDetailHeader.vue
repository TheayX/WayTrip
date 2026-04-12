<!-- 攻略详情头部 -->
<template>
  <div class="guide-header premium-card">
    <p class="guide-kicker">旅行攻略</p>
    <h1 class="guide-title">{{ resolveGuideText(guide.title) }}</h1>
    <div class="guide-meta">
      <span class="tag">{{ resolveGuideCategory(guide.category) }}</span>
      <span class="meta-item">浏览 {{ guide.viewCount || 0 }}</span>
      <span class="meta-item">{{ guide.createdAt || '--' }}</span>
    </div>
  </div>
</template>

<script setup>
const resolveGuideText = (value) => value || '--'
const UNKNOWN_GUIDE_DISPLAY = '未知攻略'
const resolveGuideCategory = (value) => value || UNKNOWN_GUIDE_DISPLAY

// 详情头部只展示标题和元信息，避免把正文相关逻辑耦合进来。
defineProps({
  guide: {
    type: Object,
    required: true
  }
})

// 纯展示头部不派发事件，详情页的收藏、跳转等操作都留给外层处理。
</script>

<style lang="scss" scoped>
.guide-header {
  padding: 24px;
  margin-top: -48px;
  position: relative;
  z-index: 1;
  margin-left: 24px;
  margin-right: 24px;
}

.guide-kicker {
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.guide-title {
  font-size: 34px;
  line-height: 1.15;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.04em;
  text-wrap: balance;
}

.guide-meta {
  margin-top: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #64748b;
}

@media (max-width: 768px) {
  .guide-header {
    margin-left: 16px;
    margin-right: 16px;
  }

  .guide-title {
    font-size: 28px;
  }
}
</style>
