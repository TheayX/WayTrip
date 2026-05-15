<!-- 景点列表工具栏 -->
<template>
  <section class="state-card premium-card">
    <div class="state-main">
      <p class="state-kicker">景点探索</p>
      <h2 class="state-title">景点列表</h2>
      <p class="state-desc">{{ description }}</p>
    </div>
  </section>
</template>

<script setup>
// 工具栏只暴露排序与重置意图，实际查询参数和列表刷新由父页面统一维护。
defineProps({
  description: {
    type: String,
    required: true
  }
})
</script>

<style lang="scss" scoped>
.state-card {
  padding: 24px;
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
  letter-spacing: 0;
  color: #0f172a;
}

.state-desc {
  margin-top: 10px;
  color: #64748b;
  line-height: 1.8;
}

</style>
