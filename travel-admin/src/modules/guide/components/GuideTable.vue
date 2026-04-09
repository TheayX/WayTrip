<!-- 攻略管理数据表格 -->
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
    <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip align="left">
      <template #default="{ row }">
        <el-button link type="primary" class="guide-title-link" @click="emit('view', row)">{{ row.title }}</el-button>
      </template>
    </el-table-column>
    <el-table-column prop="category" label="分类" width="120" align="center">
      <template #default="{ row }">
        <el-tag effect="plain" type="info" size="small">{{ row.category || '未分类' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="adminName" label="创建者" width="140" align="center" show-overflow-tooltip>
      <template #default="{ row }">
        <span class="admin-name-text">{{ row.adminName || '未记录' }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="viewCount" label="浏览量" width="100" align="center">
      <template #default="{ row }">
        <span class="count-text">{{ row.viewCount ?? 0 }}</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="100" align="center">
      <template #default="{ row }">
        <div class="capsule-badge status-capsule" :class="row.published ? 'status-success' : 'status-neutral'">
          <span class="dot"></span>
          {{ row.published ? '已发布' : '未发布' }}
        </div>
      </template>
    </el-table-column>
    <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
      <template #default="{ row }">
        {{ formatDate(row.createdAt) }}
      </template>
    </el-table-column>
    <el-table-column prop="updatedAt" label="修改时间" width="180" align="center">
      <template #default="{ row }">
        {{ formatDate(row.updatedAt) }}
      </template>
    </el-table-column>
    <el-table-column label="操作" width="160" fixed="right" align="center">
      <template #default="{ row }">
        <div class="table-actions">
          <el-button link type="primary" @click="emit('edit', row)">编辑</el-button>

          <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
            <el-button link type="primary">
              更多 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                <el-dropdown-item command="edit-view-count">修改浏览量</el-dropdown-item>
                <el-dropdown-item command="toggle-publish" :class="row.published ? 'danger-text' : 'success-text'">
                  {{ row.published ? '下架攻略' : '发布攻略' }}
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided class="danger-text">删除攻略</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue'

// 列表仅负责渲染和事件分发，具体变更动作由页面层统一处理。
defineProps({
  tableData: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  getImageUrl: { type: Function, required: true },
  formatDate: { type: Function, required: true },
  getRowClassName: { type: Function, required: true }
})

const emit = defineEmits(['selection-change', 'view', 'edit', 'edit-view-count', 'toggle-publish', 'delete'])

const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

const handleCommand = (command, row) => {
  // 下拉菜单命令在组件内收口，避免模板里堆叠过多业务分支。
  switch (command) {
    case 'detail':
      emit('view', row)
      break
    case 'edit-view-count':
      emit('edit-view-count', row)
      break
    case 'toggle-publish':
      emit('toggle-publish', row)
      break
    case 'delete':
      emit('delete', row)
      break
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

.guide-title-link {
  padding: 0;
  min-width: 0;
  height: auto;
  font-weight: 600;
  color: var(--wt-text-primary);

  &:hover {
    color: var(--el-color-primary);
  }
}

:deep(.guide-title-link.el-button.is-link) {
  padding-left: 0;
  padding-right: 0;
}

:deep(.guide-title-link .el-button__text) {
  display: inline-block;
  text-align: left;
}

.count-text {
  font-weight: 600;
  color: var(--wt-text-primary);
  font-variant-numeric: tabular-nums;
}

.admin-name-text {
  color: var(--wt-text-regular);
  font-weight: 500;
}

.borderless-table {
  :deep(.el-table__inner-wrapper::before) {
    display: none;
  }

  :deep(td.el-table__cell),
  :deep(th.el-table__cell.is-leaf) {
    border-bottom: 1px solid var(--wt-divider-faint);
  }

  :deep(.el-table__row) {
    transition: all 0.2s ease;

    td {
      padding: 12px 0;
    }
  }

  :deep(.el-table__row:hover > td.el-table__cell) {
    background: var(--wt-row-gradient-hover) !important;
  }
}

.capsule-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;

  &.status-capsule {
    gap: 6px;

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
    }
  }

  &.status-success {
    background-color: var(--wt-tag-success-bg);
    color: var(--wt-tag-success-text);

    .dot {
      background-color: var(--el-color-success);
    }
  }

  &.status-neutral {
    background-color: var(--wt-tag-info-bg);
    color: var(--wt-text-regular);

    .dot {
      background-color: var(--wt-text-secondary);
    }
  }
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.danger-text {
  color: #e11d48 !important;
}

.success-text {
  color: #10b981 !important;
}
</style>
