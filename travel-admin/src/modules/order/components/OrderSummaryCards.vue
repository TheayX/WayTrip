<!-- 订单状态概览卡片组 -->
<template>
  <section class="summary-grid" v-loading="loading">
    <button
      v-for="card in cards"
      :key="card.key"
      type="button"
      class="summary-card"
      :class="{ active: currentTab === card.key }"
      @click="emit('change-tab', card.key)"
    >
      <div class="summary-label">{{ card.label }}</div>
      <div class="summary-value">{{ card.value }}</div>
      <div class="summary-hint">{{ card.hint }}</div>
    </button>
  </section>
</template>

<script setup>
// 卡片点击只负责切换页签，具体统计口径和数据计算由父页面统一维护。
defineProps({
  loading: { type: Boolean, default: false },
  currentTab: { type: String, required: true },
  cards: { type: Array, required: true }
})

// 子组件只抛出页签切换意图，避免把订单状态枚举硬编码到多个地方。
const emit = defineEmits(['change-tab'])
</script>

<style lang="scss" scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.summary-card {
  border: 1px solid var(--wt-border-default);
  border-radius: 20px;
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  padding: 14px 16px;
  min-height: 112px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    transform: translateY(-1px);
  }

  &.active {
    border-color: var(--el-color-primary);
    box-shadow: 0 14px 32px color-mix(in srgb, var(--el-color-primary) 18%, transparent);
  }
}

.summary-label {
  color: var(--wt-text-regular);
  font-size: 12px;
  font-weight: 600;
}

.summary-value {
  margin-top: 8px;
  color: var(--wt-text-primary);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.15;
}

.summary-hint {
  margin-top: 6px;
  color: var(--wt-text-secondary);
  font-size: 12px;
  line-height: 1.45;
}
</style>
