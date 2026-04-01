<template>
  <!-- 搜索筛选表单 -->
  <el-form :inline="true" :model="queryParams" class="search-form" @submit.prevent>
    <el-form-item label="关键词">
      <el-input
        v-model="queryParams.keyword"
        placeholder="景点名称"
        clearable
        @keyup.enter="emitSearch"
        @clear="emitSearch"
      />
    </el-form-item>
    <el-form-item label="地区">
      <el-cascader
        v-model="uiFilters.regionPath"
        :options="regionCascaderOptions"
        :props="regionCascaderProps"
        clearable
        style="width: 220px"
        placeholder="全部"
        @change="emitFilterChange"
      />
    </el-form-item>
    <el-form-item label="分类">
      <el-cascader
        v-model="uiFilters.categoryPath"
        :options="categoryCascaderOptions"
        :props="categoryCascaderProps"
        clearable
        style="width: 220px"
        placeholder="全部"
        @change="emitFilterChange"
        @clear="emitFilterChange"
      />
    </el-form-item>
    <el-form-item label="状态">
      <el-select
        v-model="uiFilters.published"
        placeholder="全部"
        clearable
        style="width: 140px"
        @change="emitFilterChange"
        @clear="emitFilterChange"
      >
        <el-option label="已发布" value="1" />
        <el-option label="未发布" value="0" />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="emitSearch">搜索</el-button>
      <el-button @click="emitReset">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
const props = defineProps({
  queryParams: {
    type: Object,
    required: true
  },
  uiFilters: {
    type: Object,
    required: true
  },
  regionCascaderOptions: {
    type: Array,
    required: true
  },
  categoryCascaderOptions: {
    type: Array,
    required: true
  },
  regionCascaderProps: {
    type: Object,
    required: true
  },
  categoryCascaderProps: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['search', 'reset', 'filter-change'])

const emitSearch = () => {
  emit('search')
}

const emitReset = () => {
  emit('reset')
}

const emitFilterChange = () => {
  emit('filter-change')
}
</script>

<style lang="scss" scoped>
.search-form {
  margin-bottom: 20px;
}
</style>
