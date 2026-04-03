<!-- 攻略卡片 -->
<template>
  <article class="guide-card card" @click="$emit('select', guide)">
    <img :src="getImageUrl(guide.coverImage)" class="guide-image" alt="" />
    <div class="guide-content">
      <h3 class="guide-title">{{ guide.title }}</h3>
      <p class="guide-summary">{{ guide.summary || '带上好心情，发现更多旅行灵感。' }}</p>
      <div class="guide-meta">
        <span class="tag">{{ guide.category || '攻略' }}</span>
        <span class="guide-views">👁 {{ guide.viewCount || 0 }}</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { getImageUrl } from '@/shared/api/client.js'

defineProps({
  guide: {
    type: Object,
    required: true
  }
})

defineEmits(['select'])
</script>

<style lang="scss" scoped>
.guide-card {
  cursor: pointer;
}

.guide-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.guide-content {
  padding: 16px;
}

.guide-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 10px;
}

.guide-summary {
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
}

.guide-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.guide-views {
  color: #909399;
  font-size: 13px;
}
</style>
