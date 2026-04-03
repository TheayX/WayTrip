<!-- 首页快捷入口区 -->
<template>
  <section class="quick-grid">
    <div v-for="item in items" :key="item.id" class="quick-card premium-card" @click="item.handler()">
      <div class="quick-icon" :class="item.theme">
        <el-icon><component :is="item.icon" /></el-icon>
      </div>
      <div class="quick-copy">
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
      </div>
      <span class="quick-arrow">进入</span>
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.quick-card {
  min-height: 158px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  cursor: pointer;
}

.quick-card:hover {
  transform: translateY(-2px);
}

.quick-copy {
  flex: 1;
}

.quick-copy h3 {
  margin-bottom: 10px;
  font-size: 20px;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.quick-copy p {
  color: #64748b;
  line-height: 1.75;
  font-size: 14px;
}

.quick-icon {
  width: 56px;
  height: 56px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.78);
}

.quick-icon.blue {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #2563eb;
}

.quick-icon.orange {
  background: linear-gradient(135deg, #fff7ed 0%, #fed7aa 100%);
  color: #ea580c;
}

.quick-icon.amber {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  color: #b45309;
}

.quick-icon.emerald {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  color: #059669;
}

.quick-arrow {
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 1024px) {
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
