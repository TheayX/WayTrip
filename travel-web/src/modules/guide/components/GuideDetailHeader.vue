<!-- 攻略详情头部 -->
<template>
  <div class="guide-header card">
    <h1 class="guide-title">{{ guide.title }}</h1>
    <div class="guide-meta">
      <span class="tag">{{ guide.category }}</span>
      <span class="meta-item">👁 {{ guide.viewCount }}</span>
      <span class="meta-item">{{ guide.createdAt }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  guide: {
    type: Object,
    required: true
  }
})
</script>

<style lang="scss" scoped>
.guide-header {
  padding: 24px;
  margin-top: -40px;
  position: relative;
  z-index: 1;
  border-radius: 12px;
  margin-left: 20px;
  margin-right: 20px;
}

.guide-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.guide-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #909399;
}
</style>
