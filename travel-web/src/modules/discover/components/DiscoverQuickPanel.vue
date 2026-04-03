<!-- 发现页快捷入口区 -->
<template>
  <section class="quick-panel">
    <div v-for="item in items" :key="item.key" class="quick-card card" @click="item.handler()">
      {{ item.label }}
    </div>
  </section>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  }
})
</script>

<style lang="scss" scoped>
.quick-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.quick-card {
  padding: 18px;
  text-align: center;
  cursor: pointer;
  font-weight: 600;
}

@media (max-width: 900px) {
  .quick-panel {
    grid-template-columns: 1fr;
  }
}
</style>
