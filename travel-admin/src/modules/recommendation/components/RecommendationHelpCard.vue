<template>
  <!-- 使用说明 -->
  <el-card shadow="hover" class="help-card">
    <template #header>
      <div class="card-header">
        <span class="title">说明与调参指南</span>
      </div>
    </template>

    <div class="help-intro">
      这部分保留给排查和调参时查阅。日常使用可以先看上面的配置区、执行区和预览区；只有在需要确认公式、字段来源、矩阵机制或默认值时，再展开下面的内容。
    </div>

    <el-collapse :model-value="activeCollapse" @update:model-value="emit('update:active-collapse', $event)">
      <el-collapse-item title="先看什么" name="quick-start">
        <div class="help-content">
          <div class="strategy-grid">
            <div class="strategy-item">
              <div class="strategy-title">只想改推荐倾向</div>
              <div class="strategy-desc">先看“离线矩阵构建”，这里决定用户历史行为如何进入相似度计算。</div>
            </div>
            <div class="strategy-item">
              <div class="strategy-title">只想改候选范围</div>
              <div class="strategy-desc">看“在线推荐与候选控制”，这些参数保存后会直接影响新请求。</div>
            </div>
            <div class="strategy-item">
              <div class="strategy-title">只想改热门排序</div>
              <div class="strategy-desc">看“热度与排序”，这部分不影响离线相似度矩阵。</div>
            </div>
            <div class="strategy-item">
              <div class="strategy-title">只想改缓存表现</div>
              <div class="strategy-desc">看“用户推荐缓存”和右侧执行区，不需要改算法参数。</div>
            </div>
          </div>
          <el-alert type="info" :closable="false" style="margin-top: 16px">
            <template #title>
              一个简单判断：改完后如果你希望“景点之间的相似关系”变了，就需要重建相似度矩阵；如果只是影响在线排序、候选规模或缓存，一般保存后就会生效。
            </template>
          </el-alert>
        </div>
      </el-collapse-item>

      <el-collapse-item title="行为权重说明" name="weights">
        <div class="help-content">
          <el-table :data="weightExplanations" stripe style="width: 100%">
            <el-table-column prop="behavior" label="行为类型" width="120" />
            <el-table-column prop="param" label="参数名" width="200" />
            <el-table-column prop="default" label="默认值" width="100" />
            <el-table-column prop="description" label="说明" />
          </el-table>
          <el-alert type="info" :closable="false" style="margin-top: 16px">
            <template #title>
              同一景点上多种行为按加权求和聚合。例如用户先浏览(0.5)再收藏(1.0)，最终权重为 1.5。
            </template>
          </el-alert>
        </div>
      </el-collapse-item>

      <el-collapse-item title="数据库字段说明" name="data-fields">
        <div class="help-content">
          <div class="data-field-group">
            <div class="data-field-title">离线矩阵主体字段</div>
            <el-table :data="cfDataFieldReferences" stripe style="width: 100%">
              <el-table-column prop="table" label="表" width="180" />
              <el-table-column label="涉及字段" min-width="260">
                <template #default="{ row }">
                  <div class="field-pill-list">
                    <span v-for="field in row.fields" :key="field" class="field-pill">{{ field }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="usage" label="用途" min-width="220" />
              <el-table-column prop="phase" label="算法阶段" width="180" />
            </el-table>
          </div>

          <div class="data-field-group">
            <div class="data-field-title">冷启动与热门排序字段</div>
            <el-table :data="coldStartDataFieldReferences" stripe style="width: 100%">
              <el-table-column prop="table" label="表" width="180" />
              <el-table-column label="涉及字段" min-width="260">
                <template #default="{ row }">
                  <div class="field-pill-list">
                    <span v-for="field in row.fields" :key="field" class="field-pill">{{ field }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="usage" label="用途" min-width="220" />
              <el-table-column prop="phase" label="算法阶段" width="180" />
            </el-table>
          </div>
        </div>
      </el-collapse-item>

      <el-collapse-item title="矩阵更新机制" name="matrix">
        <div class="help-content">
          <ul>
            <li><strong>自动更新：</strong>每天凌晨 3:00 定时任务自动重新计算相似度矩阵。</li>
            <li><strong>手动更新：</strong>点击上方「重建相似度矩阵」按钮可立即触发重算。</li>
            <li><strong>什么时候要重建：</strong>交互权重、浏览行为修正、近邻数量 K、相似度矩阵 TTL 这类离线参数改完后。</li>
            <li><strong>什么时候不用重建：</strong>热度、在线候选控制、用户推荐缓存这类参数保存后会直接影响新的推荐请求。</li>
          </ul>
        </div>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<script setup>
defineProps({
  activeCollapse: {
    type: Array,
    required: true
  },
  weightExplanations: {
    type: Array,
    required: true
  },
  cfDataFieldReferences: {
    type: Array,
    required: true
  },
  coldStartDataFieldReferences: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['update:active-collapse'])
</script>

<style lang="scss" scoped>
.help-card {
  border-radius: 22px;
  border: none;
}

.card-header {
  .title {
    font-size: 16px;
    font-weight: 600;
    color: #1f2f3d;
    position: relative;
    padding-left: 12px;

      &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 16px;
        background: #2563eb;
        border-radius: 2px;
      }
  }
}

.help-intro {
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(246, 251, 255, 0.96) 0%, rgba(238, 247, 255, 0.92) 100%);
  border: 1px solid rgba(216, 234, 255, 0.96);
  color: #44607a;
  line-height: 1.7;
  font-size: 13px;
}

.help-content {
  padding: 8px 0;
  line-height: 1.8;
  color: #606266;

  ul {
    padding-left: 20px;

    li {
      margin: 8px 0;
    }
  }

  code {
    padding: 2px 6px;
    background: #f5f5f5;
    border-radius: 4px;
    font-size: 13px;
    color: #722ed1;
  }
}

.strategy-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.strategy-item {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(248, 250, 252, 0.9) 100%);
  border-radius: 18px;
  padding: 18px;
  border: 1px solid rgba(226, 232, 240, 0.96);

  .strategy-title {
    font-weight: 600;
    color: #303133;
    margin-bottom: 8px;
  }

  .strategy-desc {
    font-size: 13px;
    color: #606266;
  }
}

.field-pill-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.data-field-group + .data-field-group {
  margin-top: 16px;
}

.data-field-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 700;
  color: #1f2f3d;
}

.field-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #eef6ff 0%, #f6faff 100%);
  border: 1px solid #cfe3ff;
  color: #245bdb;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .strategy-grid {
    grid-template-columns: 1fr;
  }
}
</style>
