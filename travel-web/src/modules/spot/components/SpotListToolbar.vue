<!-- 景点列表工具栏 -->
<template>
  <section class="state-card premium-card">
    <div class="state-main">
      <p class="state-kicker">景点探索</p>
      <h2 class="state-title">景点列表</h2>
      <p class="state-desc">{{ description }}</p>
    </div>
    <div class="state-actions">
      <button type="button" class="sort-chip" :class="{ active: sortBy === 'heat' }" @click="$emit('sort-change', 'heat')">综合热度</button>
      <button type="button" class="sort-chip" :class="{ active: sortBy === 'rating' }" @click="$emit('sort-change', 'rating')">评分最高</button>
      <button type="button" class="sort-chip" :class="{ active: sortBy === 'price_asc' }" @click="$emit('sort-change', 'price_asc')">价格最低</button>
      <button type="button" class="sort-chip" :class="{ active: sortBy === 'price_desc' }" @click="$emit('sort-change', 'price_desc')">价格最高</button>
      <button type="button" class="sort-chip subtle" @click="$emit('reset')">重置</button>
    </div>
  </section>
</template>

<script setup>
defineProps({
  description: {
    type: String,
    required: true
  },
  sortBy: {
    type: String,
    required: true
  }
})

defineEmits(['sort-change', 'reset'])
</script>

<style lang="scss" scoped>
.state-card {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
}

.state-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.state-title {
  font-size: 32px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.state-desc {
  margin-top: 10px;
  color: #64748b;
  line-height: 1.8;
}

.state-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.sort-chip {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease;
}

.sort-chip.active {
  border-color: rgba(200, 169, 91, 0.45);
  background: #fffdf7;
  color: #8a6a2f;
}

.sort-chip.subtle {
  color: #64748b;
}

@media (max-width: 992px) {
  .state-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .state-actions {
    justify-content: flex-start;
  }
}
</style>
