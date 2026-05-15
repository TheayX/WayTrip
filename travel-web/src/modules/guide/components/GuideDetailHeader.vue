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
import { resolveWebGuideCategory, resolveWebGuideDisplayText } from '@/shared/constants/resource-display.js'

const resolveGuideText = (value) => resolveWebGuideDisplayText(value)
const resolveGuideCategory = (value) => resolveWebGuideCategory(value)

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
  padding: 20px;
  margin-top: -36px;
  position: relative;
  z-index: 1;
  margin-left: 20px;
  margin-right: 20px;
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
  font-size: 28px;
  line-height: 1.18;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0;
  text-wrap: balance;
}

.guide-meta {
  margin-top: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #64748b;
}

@media (max-width: 768px) {
  .guide-header {
    margin-top: -24px;
    margin-left: 14px;
    margin-right: 14px;
    padding: 18px;
  }

  .guide-title {
    font-size: 24px;
  }
}
</style>
