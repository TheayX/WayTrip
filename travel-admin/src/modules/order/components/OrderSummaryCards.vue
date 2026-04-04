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
defineProps({
  loading: { type: Boolean, default: false },
  currentTab: { type: String, required: true },
  cards: { type: Array, required: true }
})

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
  padding: 18px 20px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    transform: translateY(-1px);
  }

  &.active {
    border-color: #2563eb;
    box-shadow: 0 14px 32px rgba(37, 99, 235, 0.12);
  }
}

.summary-label {
  color: var(--wt-text-regular);
  font-size: 13px;
  font-weight: 600;
}

.summary-value {
  margin-top: 12px;
  color: var(--wt-text-primary);
  font-size: 28px;
  font-weight: 700;
}

.summary-hint {
  margin-top: 8px;
  color: var(--wt-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
