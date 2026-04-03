<!-- 景点列表工具栏 -->
<template>
  <section class="state-card premium-card">
    <div class="state-main">
      <p class="state-kicker">Spot Directory</p>
      <h2 class="state-title">景点列表</h2>
      <p class="state-desc">{{ description }}</p>
    </div>
    <div class="state-actions">
      <el-button text :type="sortBy === 'heat' ? 'primary' : 'default'" @click="$emit('sort-change', 'heat')">综合热度</el-button>
      <el-button text :type="sortBy === 'rating' ? 'primary' : 'default'" @click="$emit('sort-change', 'rating')">评分最高</el-button>
      <el-button text :type="sortBy === 'price_asc' ? 'primary' : 'default'" @click="$emit('sort-change', 'price_asc')">价格最低</el-button>
      <el-button text :type="sortBy === 'price_desc' ? 'primary' : 'default'" @click="$emit('sort-change', 'price_desc')">价格最高</el-button>
      <el-button text @click="$emit('reset')">重置</el-button>
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
