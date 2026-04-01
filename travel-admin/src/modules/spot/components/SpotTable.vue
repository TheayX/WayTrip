<template>
  <!-- 景点数据表格 -->
  <el-table :data="tableData" v-loading="loading" stripe :row-class-name="getRowClassName">
    <el-table-column prop="id" label="ID" width="80" />
    <el-table-column label="封面" width="100">
      <template #default="{ row }">
        <el-image :src="getImageUrl(row.coverImage)" style="width: 60px; height: 60px" fit="cover" />
      </template>
    </el-table-column>
    <el-table-column prop="name" label="名称" min-width="150" />
    <el-table-column prop="regionName" label="地区" width="100" />
    <el-table-column prop="categoryName" label="分类" width="100" />
    <el-table-column prop="price" label="价格" width="100">
      <template #default="{ row }">¥{{ row.price }}</template>
    </el-table-column>
    <el-table-column prop="avgRating" label="评分" width="80" />
    <el-table-column prop="ratingCount" label="评价数" width="90" />
    <el-table-column label="热度档位" width="110">
      <template #default="{ row }">
        <el-tag :type="getHeatLevelTagType(row.heatLevel)">
          {{ getHeatLevelLabel(row.heatLevel) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="heatScore" label="热度" width="90" />
    <el-table-column label="状态" width="100">
      <template #default="{ row }">
        <el-tag :type="row.published ? 'success' : 'info'">
          {{ row.published ? '已发布' : '未发布' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="updatedAt" label="修改时间" width="180">
      <template #default="{ row }">
        {{ formatDate(row.updatedAt) }}
      </template>
    </el-table-column>
    <el-table-column label="操作" width="320" fixed="right">
      <template #default="{ row }">
        <div class="table-actions">
          <el-button link type="primary" @click="emit('edit', row)">编辑</el-button>
          <el-button link type="primary" @click="emit('heat-edit', row)">热度</el-button>
          <el-button link type="primary" @click="emit('refresh-heat', row)">同步热度</el-button>
          <el-button link type="primary" @click="emit('refresh-rating', row)">同步评分</el-button>
          <el-button link :type="row.published ? 'warning' : 'success'" @click="emit('toggle-publish', row)">
            {{ row.published ? '下架' : '发布' }}
          </el-button>
          <el-button link type="danger" @click="emit('delete', row)">删除</el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
defineProps({
  tableData: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    required: true
  },
  getRowClassName: {
    type: Function,
    required: true
  },
  getImageUrl: {
    type: Function,
    required: true
  },
  formatDate: {
    type: Function,
    required: true
  },
  getHeatLevelLabel: {
    type: Function,
    required: true
  },
  getHeatLevelTagType: {
    type: Function,
    required: true
  }
})

const emit = defineEmits([
  'edit',
  'heat-edit',
  'refresh-heat',
  'refresh-rating',
  'toggle-publish',
  'delete'
])
</script>

<style lang="scss" scoped>
.table-actions {
  white-space: nowrap;
}
</style>
