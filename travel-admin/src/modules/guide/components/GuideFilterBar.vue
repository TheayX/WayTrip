<!-- 攻略管理筛选栏 -->
<template>
  <div class="search-form admin-filter-bar">
    <el-form :inline="true" :model="queryParams" @submit.prevent>

      <div class="filter-row">
        <div class="filter-main">
          <el-form-item label="攻略标题" class="filter-item">
            <el-input
              v-model="queryParams.keyword"
              placeholder="请输入攻略标题"
              clearable
              class="filter-input"
              @keyup.enter="emit('search')"
              @clear="emit('search')"
            />
          </el-form-item>
          <el-form-item label="分类" class="filter-item">
            <el-select
              v-model="queryParams.category"
              placeholder="全部"
              clearable
              class="filter-select"
              @change="emit('search')"
              @clear="emit('search')"
            >
              <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布状态" class="filter-item">
            <el-select
              v-model="uiFilters.published"
              placeholder="全部"
              clearable
              class="status-select"
              @change="emit('filter-change')"
              @clear="emit('filter-change')"
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
          <el-button type="primary" @click="emit('search')">查询</el-button>
          <el-button @click="emit('reset')">重置</el-button>
        </div>
      </div>

      <!-- 时间筛选属于复盘分析条件，折叠后优先保证首行检索效率。 -->
      <el-collapse-transition>
        <div v-show="showAdvanced" class="advanced-panel guide-advanced-panel">
          <el-form-item label="创建时间" class="filter-item advanced-filter-item">
            <el-date-picker
              v-model="uiFilters.createdDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="date-picker"
              @change="emit('search')"
            />
          </el-form-item>
          <el-form-item label="更新时间" class="filter-item advanced-filter-item">
            <el-date-picker
              v-model="uiFilters.updatedDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="date-picker"
              @change="emit('search')"
            />
          </el-form-item>
        </div>
      </el-collapse-transition>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { CaretTop, Filter } from '@element-plus/icons-vue'

// 组件只维护输入控件，筛选字段解释与请求节奏交给父页面统一控制。
defineProps({
  queryParams: { type: Object, required: true },
  uiFilters: { type: Object, required: true },
  categories: { type: Array, required: true }
})

const emit = defineEmits(['search', 'reset', 'filter-change'])
const showAdvanced = ref(false)

// 过滤条件变更和显式搜索分开上抛，便于父层区分即时筛选与手动触发查询。
</script>

<style lang="scss" scoped>
.filter-input {
  width: 200px;
}

.filter-select {
  width: 100px;
}

.status-select {
  width: 100px; }

.date-picker {
  width: 140px;
}

.toggle-btn {
  gap: 4px;
}

.guide-advanced-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.advanced-filter-item {
  margin-bottom: 0;
}

@media (max-width: 960px) {

  .filter-input,
  .filter-select,
  .status-select {
    width: 100%;
  }

  .date-picker {
    width: 140px;
  }
}

:deep(.date-picker.el-date-editor) {
  width: 240px !important;
}
</style>
