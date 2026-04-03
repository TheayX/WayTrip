<!-- 首页快捷入口区 -->
<template>
  <section class="quick-grid">
    <div v-for="item in items" :key="item.id" class="quick-card card" @click="item.handler()">
      <div class="quick-icon" :class="item.theme">
        <el-icon><component :is="item.icon" /></el-icon>
      </div>
      <div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
      </div>
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
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.quick-card {
  padding: 20px;
  display: flex;
  gap: 16px;
  cursor: pointer;
  align-items: center;
}

.quick-card h3 {
  margin-bottom: 8px;
  font-size: 18px;
}

.quick-card p {
  color: #909399;
  line-height: 1.6;
}

.quick-icon {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.quick-icon.blue {
  background: #dbeafe;
  color: #2563eb;
}

.quick-icon.orange {
  background: #ffedd5;
  color: #ea580c;
}

.quick-icon.amber {
  background: #fef3c7;
  color: #d97706;
}

.quick-icon.emerald {
  background: #d1fae5;
  color: #059669;
}

@media (max-width: 1024px) {
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
