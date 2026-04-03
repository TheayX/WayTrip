<!-- 攻略列表工具栏 -->
<template>
  <section class="state-card card">
    <div class="state-main">
      <h2 class="state-title">攻略列表</h2>
      <p class="state-desc">{{ description }}</p>
    </div>
    <div class="state-actions">
      <el-button text :type="sortBy === 'time' ? 'primary' : 'default'" @click="$emit('sort-change', 'time')">最新优先</el-button>
      <el-button text :type="sortBy === 'category' ? 'primary' : 'default'" @click="$emit('sort-change', 'category')">分类排序</el-button>
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
  gap: 8px;
}
</style>
