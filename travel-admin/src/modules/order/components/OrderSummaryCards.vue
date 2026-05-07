<!-- 订单状态概览卡片组 -->
<template>
  <section class="summary-grid" v-loading="loading">
    <button
      v-for="card in cards"
      :key="card.key"
      type="button"
      class="summary-card summary-card--interactive"
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
</style>
