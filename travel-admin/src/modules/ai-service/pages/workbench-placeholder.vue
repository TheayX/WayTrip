<!-- AI 工作台占位页 -->
<template>
  <div class="ai-placeholder-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">AI Workbench</p>
        <h1 class="page-title">AI 工作台</h1>
        <p class="page-subtitle">一期先保留入口，后续承接调试、查询验证与运行观察能力。</p>
      </div>
      <div class="hero-actions">
        <el-tag type="warning" effect="dark" round>即将开放</el-tag>
      </div>
    </section>

    <el-row :gutter="24" class="content-row">
      <el-col :xl="16" :lg="15" :md="24">
        <el-card shadow="hover" class="placeholder-card">
          <template #header>
            <div class="card-header">
              <span>规划能力</span>
            </div>
          </template>

          <div class="capability-list">
            <div v-for="item in plannedCapabilities" :key="item.title" class="capability-item">
              <div class="capability-title">{{ item.title }}</div>
              <div class="capability-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xl="8" :lg="9" :md="24">
        <el-card shadow="hover" class="placeholder-card status-card">
          <template #header>
            <div class="card-header">
              <span>开放说明</span>
            </div>
          </template>

          <div class="status-panel">
            <div class="status-title">即将开放</div>
            <div class="status-desc">当前阶段先完成知识库、场景配置与概览页，工作台将在后续任务中承接可操作能力。</div>
            <el-alert
              type="info"
              show-icon
              :closable="false"
              title="建议先从 AI 服务概览页查看知识状态与规划入口。"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const plannedCapabilities = [
  {
    title: '场景切换与状态查看',
    desc: '面向客服、订单顾问、景点问答等场景统一查看启用状态、知识域绑定与服务摘要。'
  },
  {
    title: 'RAG 查询调试',
    desc: '支持输入测试问题，回看命中文档、分片内容与检索链路，方便定位知识召回不足。'
  },
  {
    title: '提示词与回复观察',
    desc: '预留系统提示词、候选回答与运行日志的查看区，帮助管理员判断输出是否稳定。'
  }
]
</script>

<style lang="scss" scoped>
.ai-placeholder-page {
  .content-row {
    align-items: stretch;
  }

  .placeholder-card {
    height: 100%;
  }

  .capability-list,
  .status-panel {
    display: grid;
    gap: 14px;
  }

  .capability-item {
    padding: 18px;
    border-radius: 14px;
    border: 1px solid var(--wt-border-default);
    background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  }

  .capability-title,
  .status-title {
    font-size: 16px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .capability-desc,
  .status-desc {
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--wt-text-regular);
  }

  .status-card :deep(.el-card__body) {
    height: 100%;
  }
}
</style>
