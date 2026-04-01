<template>
  <!-- 景点数据表格 -->
  <el-table :data="tableData" v-loading="loading" stripe class="premium-table" :row-class-name="getRowClassName">
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column label="封面" width="90">
      <template #default="{ row }">
        <el-image :src="getImageUrl(row.coverImage)" class="cover-img" fit="cover" />
      </template>
    </el-table-column>
    <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip>
      <template #default="{ row }">
        <span class="spot-name">{{ row.name }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="regionName" label="地区" width="110" />
    <el-table-column prop="categoryName" label="分类" width="110">
      <template #default="{ row }">
        <el-tag effect="plain" type="info" size="small">{{ row.categoryName }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="price" label="价格" width="100">
      <template #default="{ row }">
        <span class="price-text">¥{{ row.price }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="avgRating" label="评分" width="80">
      <template #default="{ row }">
        <span class="rating-text"><el-icon color="#f59e0b"><StarFilled /></el-icon> {{ row.avgRating || '暂无' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="热度档位" width="110">
      <template #default="{ row }">
        <el-tag :type="getHeatLevelTagType(row.heatLevel)" effect="light" class="round-tag">
          {{ getHeatLevelLabel(row.heatLevel) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="100" align="center">
      <template #default="{ row }">
        <div class="status-indicator">
          <span class="status-dot" :class="row.published ? 'is-success' : 'is-info'"></span>
          {{ row.published ? '已上架' : '未上架' }}
        </div>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="160" fixed="right" align="center">
      <template #default="{ row }">
        <div class="table-actions">
          <el-button link type="primary" @click="emit('edit', row)">编辑</el-button>
          
          <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)" class="more-actions">
            <el-button link type="primary" class="more-btn">
              更多 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="heat">热度设置</el-dropdown-item>
                <el-dropdown-item command="sync-heat">同步热度</el-dropdown-item>
                <el-dropdown-item command="sync-rating">同步评分</el-dropdown-item>
                <el-dropdown-item command="toggle-publish" :class="row.published ? 'danger-text' : 'success-text'">
                  {{ row.published ? '下架景点' : '发布景点' }}
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided class="danger-text">删除景点</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { ArrowDown, StarFilled } from '@element-plus/icons-vue'

defineProps({
  tableData: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  getRowClassName: { type: Function, required: true },
  getImageUrl: { type: Function, required: true },
  formatDate: { type: Function, required: true },
  getHeatLevelLabel: { type: Function, required: true },
  getHeatLevelTagType: { type: Function, required: true }
})

const emit = defineEmits([
  'edit', 'heat-edit', 'refresh-heat', 'refresh-rating', 'toggle-publish', 'delete'
])

const handleCommand = (command, row) => {
  switch (command) {
    case 'heat': emit('heat-edit', row); break;
    case 'sync-heat': emit('refresh-heat', row); break;
    case 'sync-rating': emit('refresh-rating', row); break;
    case 'toggle-publish': emit('toggle-publish', row); break;
    case 'delete': emit('delete', row); break;
  }
}
</script>

<style lang="scss" scoped>
.premium-table {
  border-radius: 8px;
  overflow: hidden;
  --el-table-border-color: #f1f5f9;
}

.cover-img {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
}

.spot-name {
  font-weight: 600;
  color: #1f2937;
}

.price-text {
  font-weight: 600;
  color: #ef4444;
}

.rating-text {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.round-tag {
  border-radius: 12px;
  padding: 0 10px;
}

.status-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: #4b5563;
  
  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    
    &.is-success { background-color: #10b981; box-shadow: 0 0 0 2px #d1fae5; }
    &.is-info { background-color: #9ca3af; box-shadow: 0 0 0 2px #f3f4f6; }
  }
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.more-actions {
  margin-left: 8px;
  .more-btn {
    font-weight: 500;
  }
}

.danger-text {
  color: #ef4444 !important;
}
.success-text {
  color: #10b981 !important;
}
</style>
