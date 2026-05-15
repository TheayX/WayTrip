<!-- 景点列表筛选栏 -->
<template>
  <section class="filter-bar premium-card">
    <div class="filter-head">
      <p class="filter-kicker">缩小范围</p>
      <h3>筛选条件</h3>
    </div>
    <div class="filter-row">
      <div class="filter-group">
        <span class="filter-label">地区</span>
        <el-cascader
          :model-value="regionPath"
          :options="regions"
          :props="cascaderProps"
          clearable
          placeholder="全部地区"
          @update:model-value="$emit('update:regionPath', $event || [])"
          @change="$emit('change')"
        />
      </div>
      <div class="filter-group">
        <span class="filter-label">分类</span>
        <el-cascader
          :model-value="categoryPath"
          :options="categories"
          :props="cascaderProps"
          clearable
          placeholder="全部分类"
          @update:model-value="$emit('update:categoryPath', $event || [])"
          @change="$emit('change')"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
// 筛选值由父层维护，组件只负责透传地区、分类变更和统一查询动作。
defineProps({
  regionPath: {
    type: Array,
    default: () => []
  },
  categoryPath: {
    type: Array,
    default: () => []
  },
  regions: {
    type: Array,
    default: () => []
  },
  categories: {
    type: Array,
    default: () => []
  }
})

// 通过 update 事件保持组件无状态，避免筛选栏和列表页出现双份源数据。
defineEmits(['update:regionPath', 'update:categoryPath', 'change'])

// 筛选栏使用单入口级联选择，交互与管理端高级筛选保持一致。
const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: true
}
</script>

<style lang="scss" scoped>
.filter-bar {
  padding: 22px;
}

.filter-head {
  margin-bottom: 16px;
}

.filter-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.filter-head h3 {
  font-size: 22px;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.filter-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-group {
  flex: 1 1 260px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.filter-label {
  white-space: nowrap;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
}
</style>
