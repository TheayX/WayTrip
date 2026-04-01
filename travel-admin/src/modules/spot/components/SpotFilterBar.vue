<template>
  <!-- 搜索筛选表单 -->
  <el-form :inline="true" :model="queryParams" class="search-form premium-filter-bar" @submit.prevent>
    <div class="filter-group">
      <el-form-item label="关键词" class="filter-item">
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索景点名称"
          clearable
          class="custom-input"
          :prefix-icon="Search"
          @keyup.enter="emitSearch"
          @clear="emitSearch"
        />
      </el-form-item>
      
      <el-form-item label="地区" class="filter-item">
        <el-cascader
          v-model="uiFilters.regionPath"
          :options="regionCascaderOptions"
          :props="regionCascaderProps"
          clearable
          placeholder="全部分类"
          class="custom-input"
          @change="emitFilterChange"
        />
      </el-form-item>
      
      <el-form-item label="类别" class="filter-item">
        <el-cascader
          v-model="uiFilters.categoryPath"
          :options="categoryCascaderOptions"
          :props="categoryCascaderProps"
          clearable
          placeholder="全部类别"
          class="custom-input"
          @change="emitFilterChange"
          @clear="emitFilterChange"
        />
      </el-form-item>
      
      <el-form-item label="发布状态" class="filter-item">
        <el-select
          v-model="uiFilters.published"
          placeholder="全部状态"
          clearable
          class="custom-input status-select"
          @change="emitFilterChange"
          @clear="emitFilterChange"
        >
          <el-option label="已发布" value="1" />
          <el-option label="未发布" value="0" />
        </el-select>
      </el-form-item>
    </div>

    <div class="action-group">
      <el-button type="primary" :icon="Search" @click="emitSearch" class="search-btn">搜索</el-button>
      <el-button :icon="Refresh" @click="emitReset" class="reset-btn">重置</el-button>
    </div>
  </el-form>
</template>

<script setup>
import { Search, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  queryParams: { type: Object, required: true },
  uiFilters: { type: Object, required: true },
  regionCascaderOptions: { type: Array, required: true },
  categoryCascaderOptions: { type: Array, required: true },
  regionCascaderProps: { type: Object, required: true },
  categoryCascaderProps: { type: Object, required: true }
})

const emit = defineEmits(['search', 'reset', 'filter-change'])

const emitSearch = () => emit('search')
const emitReset = () => emit('reset')
const emitFilterChange = () => emit('filter-change')
</script>

<style lang="scss" scoped>
.premium-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 24px 2px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 24px;
  border: 1px solid #f1f5f9;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0,0,0,0.02);
    border-color: #e2e8f0;
  }
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  flex: 1;
}

.filter-item {
  margin-right: 0 !important;
  margin-bottom: 16px !important;
  
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: #475569;
  }
}

.custom-input {
  width: 200px;
}

.status-select {
  width: 140px;
}

.action-group {
  display: flex;
  gap: 12px;
  margin-left: 24px;
  margin-bottom: 16px;
}

.search-btn {
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);
}

.reset-btn {
  border-color: #cbd5e1;
  color: #475569;
  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
    background-color: var(--el-color-primary-light-9);
  }
}
</style>
