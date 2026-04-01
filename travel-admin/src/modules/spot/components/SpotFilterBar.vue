<template>
  <!-- 搜索筛选表单 -->
  <div class="search-form premium-filter-bar mb-6">
    <el-form :inline="true" :model="queryParams" @submit.prevent>
      <div class="flex-between">
        <div class="filter-group flex gap-4">
          <el-form-item class="filter-item mb-0">
            <el-input
              v-model="queryParams.keyword"
              placeholder="搜索景点名称..."
              clearable
              class="custom-input search-input"
              :prefix-icon="Search"
              @keyup.enter="emitSearch"
              @clear="emitSearch"
              style="width: 260px;"
            />
          </el-form-item>
          
          <el-form-item class="filter-item mb-0" v-if="!showAdvanced">
            <el-select
              v-model="uiFilters.published"
              placeholder="发布状态"
              clearable
              class="custom-input status-select"
              style="width: 140px;"
              @change="emitFilterChange"
              @clear="emitFilterChange"
            >
              <el-option label="已发布" value="1" />
              <el-option label="未发布" value="0" />
            </el-select>
          </el-form-item>

          <el-button type="primary" link @click="showAdvanced = !showAdvanced" class="ml-2 font-medium">
            <el-icon class="mr-1"><Filter v-if="!showAdvanced" /><CaretTop v-else /></el-icon>
            {{ showAdvanced ? '收起筛选' : '更多筛选' }}
          </el-button>
        </div>

        <div class="action-group flex gap-2">
          <el-button type="primary" @click="emitSearch" class="modern-btn px-6">查询</el-button>
          <el-button @click="emitReset" class="modern-btn-plain px-6">重置</el-button>
        </div>
      </div>

      <!-- 高级筛选区域 -->
      <el-collapse-transition>
        <div v-show="showAdvanced" class="advanced-filters mt-4 pt-4 border-t border-gray-100 flex flex-wrap gap-4">
          <el-form-item label="地区" class="filter-item mb-2">
            <el-cascader
              v-model="uiFilters.regionPath"
              :options="regionCascaderOptions"
              :props="regionCascaderProps"
              clearable
              placeholder="全国任意地区"
              class="custom-input w-48"
              @change="emitFilterChange"
            />
          </el-form-item>
          
          <el-form-item label="分类" class="filter-item mb-2">
            <el-cascader
              v-model="uiFilters.categoryPath"
              :options="categoryCascaderOptions"
              :props="categoryCascaderProps"
              clearable
              placeholder="全部分类"
              class="custom-input w-48"
              @change="emitFilterChange"
              @clear="emitFilterChange"
            />
          </el-form-item>

          <el-form-item label="发布状态" class="filter-item mb-2">
            <el-select
              v-model="uiFilters.published"
              placeholder="全部状态"
              clearable
              class="custom-input status-select w-32"
              @change="emitFilterChange"
              @clear="emitFilterChange"
            >
              <el-option label="已发布" value="1" />
              <el-option label="未发布" value="0" />
            </el-select>
          </el-form-item>
        </div>
      </el-collapse-transition>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search, Refresh, Filter, CaretTop } from '@element-plus/icons-vue'

const props = defineProps({
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
.premium-filter-bar {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px 24px;
  border: 1px solid #f1f5f9;
  transition: all 0.3s ease;

  &:hover {
    border-color: #e2e8f0;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  }
}

.flex { display: flex; }
.flex-wrap { flex-wrap: wrap; }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
.gap-2 { gap: 8px; }
.gap-4 { gap: 16px; }
.mb-0 { margin-bottom: 0 !important; }
.mb-2 { margin-bottom: 8px !important; }
.mb-6 { margin-bottom: 24px; }
.mt-4 { margin-top: 16px; }
.pt-4 { padding-top: 16px; }
.ml-2 { margin-left: 8px; }
.mr-1 { margin-right: 4px; }
.px-6 { padding-left: 24px; padding-right: 24px; }
.border-t { border-top: 1px solid #f1f5f9; }
.w-48 { width: 192px; }
.w-32 { width: 128px; }
.font-medium { font-weight: 500; }

:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  background-color: white;
  
  &:hover {
    box-shadow: 0 0 0 1px var(--el-color-primary) inset, 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  }
}
</style>
