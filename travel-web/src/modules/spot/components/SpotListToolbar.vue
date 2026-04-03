<!-- 景点列表工具栏 -->
<template>
  <section class="state-card card">
    <div class="state-main">
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
  padding: 22px;
}

.state-main {
  margin-bottom: 16px;
}

.state-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
}

.state-desc {
  color: #909399;
  line-height: 1.7;
}

.state-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
