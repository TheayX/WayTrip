<!-- 攻略卡片 -->
<template>
  <article class="guide-card premium-card" @click="$emit('select', guide)">
    <img :src="getImageUrl(guide.coverImage)" class="guide-image" alt="" />
    <div class="guide-content">
      <span class="guide-category">{{ resolveGuideCategory(guide.category) }}</span>
      <h3 class="guide-title">{{ resolveGuideText(guide.title) }}</h3>
      <p class="guide-summary">{{ resolveGuideSummary(guide.summary) }}</p>
      <div class="guide-meta">
        <span class="guide-views">浏览 {{ guide.viewCount || 0 }}</span>
        <span class="guide-link">继续阅读</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { getImageUrl } from '@/shared/api/client.js'

const UNKNOWN_GUIDE_DISPLAY = '未知攻略'
const resolveGuideText = (value) => value || UNKNOWN_GUIDE_DISPLAY
const resolveGuideCategory = (value) => value || UNKNOWN_GUIDE_DISPLAY
const resolveGuideSummary = (value) => value || '整理路线、玩法与出行经验，帮助你更快形成这次旅程的安排。'

// 卡片只承接展示和选中事件，详情跳转策略由外层列表页面决定。
defineProps({
  guide: {
    type: Object,
    required: true
  }
})

// 选中事件统一上抛，便于列表页复用在网格、轮播等不同容器中。
defineEmits(['select'])
</script>

<style lang="scss" scoped>
.guide-card {
  overflow: hidden;
  cursor: pointer;
}

.guide-card:hover {
  transform: translateY(-2px);
}

.guide-image {
  width: 100%;
  height: 236px;
  object-fit: cover;
}

.guide-content {
  padding: 18px;
}

.guide-category {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #fffdf7;
  color: #8a6a2f;
  font-size: 12px;
  font-weight: 700;
}

.guide-title {
  margin-top: 14px;
  font-size: 20px;
  line-height: 1.35;
  font-weight: 700;
  color: #0f172a;
}

.guide-summary {
  margin-top: 10px;
  color: #64748b;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.guide-meta {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.guide-views {
  color: #64748b;
  font-size: 13px;
}

.guide-link {
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}
</style>
