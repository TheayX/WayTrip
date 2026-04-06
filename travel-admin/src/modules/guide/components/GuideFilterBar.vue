<template>
  <div class="search-form admin-filter-bar">
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
.filter-input {
  width: 240px;
}

.filter-select {
  width: 200px;
}

.status-select { width: 140px; }

@media (max-width: 960px) {

  .filter-input,
  .filter-select,
  .status-select {
    width: 100%;
  }
}
</style>
