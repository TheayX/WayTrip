<template>
  <div class="filter-panel">
    <el-form :inline="true" :model="queryParams" @submit.prevent>
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
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
  padding: 16px 18px;
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
  background: #ffffff;
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
