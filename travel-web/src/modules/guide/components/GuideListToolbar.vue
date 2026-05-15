<!-- 攻略列表工具栏 -->
<template>
  <section class="state-card premium-card">
    <div class="state-main">
      <p class="state-kicker">Travel Notes</p>
      <h2 class="state-title">攻略库</h2>
      <p class="state-desc">{{ description }}</p>
    </div>
  </section>
</template>

<script setup>
// 工具栏只描述当前排序方式，不直接持有列表状态或请求逻辑。
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
