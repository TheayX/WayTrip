<template>
  <div class="filter-panel">
    <el-form :inline="true" :model="queryParams" @submit.prevent>
      <div class="filter-caption">
        <span class="filter-title">筛选攻略</span>
        <span class="filter-subtitle">按标题关键词、分类和发布状态快速找到需要更新的攻略内容。</span>
      </div>
      <div class="filter-row">
        <div class="filter-main">
          <el-form-item class="filter-item">
            <el-input
              v-model="queryParams.keyword"
              placeholder="搜索攻略标题"
              clearable
              class="filter-input"
              @keyup.enter="emit('search')"
              @clear="emit('search')"
            />
          </el-form-item>
          <el-form-item class="filter-item">
            <el-select
              v-model="queryParams.category"
              placeholder="全部分类"
              clearable
              class="filter-select"
              @change="emit('search')"
              @clear="emit('search')"
            >
              <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item class="filter-item">
            <el-select
              v-model="uiFilters.published"
              placeholder="全部状态"
              clearable
              class="status-select"
              @change="emit('filter-change')"
              @clear="emit('filter-change')"
            >
              <el-option label="已发布" value="1" />
              <el-option label="未发布" value="0" />
            </el-select>
          </el-form-item>
        </div>

        <div class="filter-actions">
          <el-button type="primary" @click="emit('search')">搜索</el-button>
          <el-button @click="emit('reset')">重置</el-button>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup>
defineProps({
  queryParams: { type: Object, required: true },
  uiFilters: { type: Object, required: true },
  categories: { type: Array, required: true }
})

const emit = defineEmits(['search', 'reset', 'filter-change'])
</script>

<style lang="scss" scoped>
.filter-panel {
  margin-top: 16px;
  margin-bottom: 20px;
  border: 1px solid rgba(219, 228, 240, 0.9);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.82) 0%, rgba(255, 255, 255, 0.9) 100%);
  padding: 16px 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.filter-caption {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 14px;
}

.filter-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.filter-subtitle {
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.filter-row,
.filter-main,
.filter-actions {
  display: flex;
  gap: 12px;
}

.filter-row {
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.filter-main {
  flex-wrap: wrap;
  align-items: center;
  flex: 1;
}

.filter-actions {
  align-items: center;
  justify-content: flex-end;
}

.filter-item {
  margin-bottom: 0;
}

.filter-input {
  width: 240px;
}

.filter-select {
  width: 200px;
}

.status-select {
  width: 140px;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

@media (max-width: 960px) {
  .filter-row {
    flex-direction: column;
  }

  .filter-actions {
    width: 100%;
  }

  .filter-actions :deep(.el-button) {
    width: 100%;
  }

  .filter-input,
  .filter-select,
  .status-select {
    width: 100%;
  }
}
</style>
