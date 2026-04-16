<!-- AI 场景配置卡片 -->
<template>
  <el-card shadow="hover" class="scenario-card">
    <div class="scenario-card__header">
      <div>
        <div class="scenario-card__title-row">
          <h3 class="scenario-card__title">{{ scenario.title }}</h3>
          <el-tag size="small" effect="plain" round>
            {{ scenario.scenario }}
          </el-tag>
        </div>
        <div class="scenario-card__meta">
          <el-tag size="small" effect="light" :type="scenario.enabled ? 'success' : 'info'" round>
            {{ scenario.enabled ? '已启用' : '已停用' }}
          </el-tag>
          <span>{{ domainLabel }}</span>
        </div>
      </div>

      <el-button type="primary" plain @click="emit('edit', scenario)">编辑本地配置</el-button>
    </div>

    <div class="scenario-card__body">
      <div class="scenario-section">
        <div class="scenario-section__label">场景说明</div>
        <div class="scenario-section__value">{{ scenario.description || '暂无说明' }}</div>
      </div>

      <div class="scenario-section">
        <div class="scenario-section__label">示例问题</div>
        <div class="scenario-section__value scenario-section__value--quote">
          {{ scenario.exampleQuestion || '暂无示例问题' }}
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  scenario: { type: Object, required: true },
  domainLabel: { type: String, default: '未分类' }
})

const emit = defineEmits(['edit'])
</script>

<style lang="scss" scoped>
.scenario-card {
  border-radius: 18px;
  border: 1px solid var(--wt-border-default);

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 18px;
    padding: 20px;
  }
}

.scenario-card__header,
.scenario-card__title-row,
.scenario-card__meta {
  display: flex;
  gap: 10px;
}

.scenario-card__header,
.scenario-card__title-row {
  align-items: flex-start;
  justify-content: space-between;
}

.scenario-card__header {
  gap: 16px;
}

.scenario-card__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.scenario-card__meta {
  margin-top: 12px;
  flex-wrap: wrap;
  align-items: center;
  font-size: 12px;
  color: var(--wt-text-secondary);
}

.scenario-card__body {
  display: grid;
  gap: 14px;
}

.scenario-section {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--wt-border-default);
  background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
}

.scenario-section__label {
  font-size: 12px;
  color: var(--wt-text-secondary);
}

.scenario-section__value {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--wt-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

.scenario-section__value--quote {
  color: var(--wt-text-primary);
}

@media (max-width: 768px) {
  .scenario-card__header,
  .scenario-card__title-row {
    flex-direction: column;
  }
}
</style>
