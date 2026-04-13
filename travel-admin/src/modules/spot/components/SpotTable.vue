<!-- 景点管理数据表格 -->
<template>
  <el-table
    :data="tableData"
    v-loading="loading"
    class="premium-table borderless-table"
    :row-class-name="getRowClassName"
    @selection-change="handleSelectionChange"
  >
    <el-table-column type="selection" width="50" align="center" />
    <el-table-column prop="id" label="ID" width="70" align="center" />
    <el-table-column label="封面" width="90" align="center">
      <template #default="{ row }">
        <el-image :src="getImageUrl(row.coverImage)" class="cover-img" fit="cover" />
      </template>
    </el-table-column>
    <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip align="left">
      <template #default="{ row }">
        <el-button link type="primary" class="spot-name-link" @click="emit('view', row)">{{ row.name }}</el-button>
      </template>
    </el-table-column>
    <el-table-column prop="regionName" label="地区" width="110" align="center" />
    <el-table-column prop="categoryName" label="分类" width="110" align="center">
      <template #default="{ row }">
        <el-tag effect="plain" type="info" size="small">{{ resolveCategoryDisplayName(row.categoryName) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="price" label="价格" width="100" align="center">
      <template #default="{ row }">
        <span class="price-text">¥{{ row.price }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="avgRating" label="评分" width="90" align="center">
      <template #default="{ row }">
        <span class="rating-text"><el-icon color="#f59e0b"><StarFilled /></el-icon> {{ row.avgRating || '暂无' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="热度档位" width="100" align="center">
      <template #default="{ row }">
        <el-tag :type="getHeatLevelTagType(row.heatLevel)" effect="light" class="round-tag capsule-badge">
          {{ getHeatLevelLabel(row.heatLevel) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="热度分数" width="100" align="center">
      <template #default="{ row }">
        <span class="heat-score-text">{{ row.heatScore ?? 0 }}</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="100" align="center">
      <template #default="{ row }">
        <div class="capsule-badge status-capsule" :class="row.published ? 'status-success' : 'status-neutral'">
          <span class="dot"></span>
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
                <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                <el-dropdown-item command="heat">设置热度档位</el-dropdown-item>
                <el-dropdown-item command="sync-heat">同步热度分数</el-dropdown-item>
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
import { resolveCategoryDisplayName } from '@/shared/lib/resource-display.js'

// 表格组件只负责展示和透传行级操作，业务处理全部回到页面层统一执行。
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
  'selection-change', 'edit', 'view', 'heat-edit', 'refresh-heat', 'refresh-rating', 'toggle-publish', 'delete'
])

const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

const handleCommand = (command, row) => {
  // 下拉命令统一在这里转成显式事件，避免模板里出现过长的内联判断。
  switch (command) {
    case 'detail': emit('view', row); break;
    case 'heat': emit('heat-edit', row); break;
    case 'sync-heat': emit('refresh-heat', row); break;
    case 'sync-rating': emit('refresh-rating', row); break;
    case 'toggle-publish': emit('toggle-publish', row); break;
    case 'delete': emit('delete', row); break;
  }
}
</script>

<style lang="scss" scoped>
.cover-img {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.spot-name-link {
  padding: 0;
  min-width: 0;
  height: auto;
  font-weight: 600;
  color: var(--wt-text-primary);
  &:hover {
    color: var(--el-color-primary);
  }
}

:deep(.spot-name-link.is-link) {
  padding-left: 0;
  padding-right: 0;
}

:deep(.spot-name-link .el-button__text) {
  display: inline-block;
  text-align: left;
}

.price-text {
  display: inline-block;
  min-width: 56px;
  font-weight: 600;
  color: #9f1239;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.rating-text {
  font-weight: 600;
  color: var(--wt-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-width: 56px;
  font-variant-numeric: tabular-nums;
}

</style>
