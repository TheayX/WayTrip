<template>
  <!-- 搜索筛选表单 -->
  <div class="search-form admin-filter-bar">
    <el-form :inline="true" :model="queryParams" @submit.prevent>

      <div class="filter-row">
        <div class="filter-main">
          <el-form-item class="filter-item">
            <el-input
              v-model="queryParams.keyword"
              placeholder="搜索景点名称"
              clearable
              class="filter-input"
              :prefix-icon="Search"
              @keyup.enter="emitSearch"
              @clear="emitSearch"
            />
          </el-form-item>

          <el-form-item class="filter-item">
            <el-select
              v-model="uiFilters.published"
              placeholder="发布状态"
              clearable
              class="status-select"
              @change="emitFilterChange"
              @clear="emitFilterChange"
            >
              <el-option label="已发布" value="1" />
              <el-option label="未发布" value="0" />
            </el-select>
          </el-form-item>

          <el-button type="primary" link class="toggle-btn" @click="showAdvanced = !showAdvanced">
            <el-icon><Filter v-if="!showAdvanced" /><CaretTop v-else /></el-icon>
            {{ showAdvanced ? '收起条件' : '更多条件' }}
          </el-button>
        </div>

        <div class="filter-actions">
          <el-button type="primary" @click="emitSearch">查询</el-button>
          <el-button @click="emitReset">重置</el-button>
        </div>
      </div>

      <!-- 高级筛选区域 -->
      <el-collapse-transition>
        <div v-show="showAdvanced" class="advanced-panel advanced-filters">
          <el-form-item label="地区" class="filter-item">
            <el-cascader
              v-model="uiFilters.regionPath"
              :options="regionCascaderOptions"
              :props="regionCascaderProps"
              clearable
              placeholder="全国任意地区"
              class="filter-cascader"
              @change="emitFilterChange"
            />
          </el-form-item>

          <el-form-item label="分类" class="filter-item">
            <el-cascader
              v-model="uiFilters.categoryPath"
              :options="categoryCascaderOptions"
              :props="categoryCascaderProps"
              clearable
              placeholder="全部分类"
              class="filter-cascader"
              @change="emitFilterChange"
              @clear="emitFilterChange"
            />
          </el-form-item>

        </div>
      </el-collapse-transition>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search, Filter, CaretTop } from '@element-plus/icons-vue'

defineProps({
  queryParams: { type: Object, required: true },
  uiFilters: { type: Object, required: true },
  regionCascaderOptions: { type: Array, required: true },
  categoryCascaderOptions: { type: Array, required: true },
  regionCascaderProps: { type: Object, required: true },
  categoryCascaderProps: { type: Object, required: true }
})

const emit = defineEmits(['search', 'reset', 'filter-change'])

const showAdvanced = ref(false)

const emitSearch = () => emit('search')
const emitReset = () => emit('reset')
const emitFilterChange = () => emit('filter-change')
</script>

<style lang="scss" scoped>
.advanced-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-input {
  width: 260px;
}

.filter-cascader {
  width: 192px;
}

.status-select {
  width: 140px;
}

.toggle-btn {
  gap: 4px;
}

@media (max-width: 960px) {
  .filter-input,
  .filter-cascader,
  .status-select {
    width: 100%;
  }
}
</style>
