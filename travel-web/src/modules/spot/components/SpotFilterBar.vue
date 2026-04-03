<!-- 景点列表筛选栏 -->
<template>
  <section class="filter-bar card">
    <div class="filter-row">
      <div class="filter-group">
        <span class="filter-label">地区</span>
        <el-select
          :model-value="regionId"
          clearable
          placeholder="全部地区"
          @update:model-value="$emit('update:regionId', $event)"
          @change="$emit('change')"
        >
          <el-option label="全部地区" value="" />
          <el-option v-for="item in regions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </div>
      <div class="filter-group">
        <span class="filter-label">分类</span>
        <el-select
          :model-value="categoryId"
          clearable
          placeholder="全部分类"
          @update:model-value="$emit('update:categoryId', $event)"
          @change="$emit('change')"
        >
          <el-option label="全部分类" value="" />
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  regionId: {
    type: [String, Number],
    default: ''
  },
  categoryId: {
    type: [String, Number],
    default: ''
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

defineEmits(['update:regionId', 'update:categoryId', 'change'])
</script>

<style lang="scss" scoped>
.filter-bar {
  padding: 18px;
}

.filter-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-group {
  flex: 1 1 260px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  white-space: nowrap;
  color: #606266;
}
</style>
