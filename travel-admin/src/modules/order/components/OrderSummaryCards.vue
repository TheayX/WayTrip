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
  border: 1px solid rgba(219, 228, 240, 0.9);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(248, 250, 252, 0.88) 100%);
  padding: 18px 20px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #cbd5e1;
    transform: translateY(-1px);
  }

  &.active {
    border-color: #2563eb;
    box-shadow: 0 14px 32px rgba(37, 99, 235, 0.12);
  }
}

.summary-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.summary-value {
  margin-top: 12px;
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
}

.summary-hint {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}
</style>
