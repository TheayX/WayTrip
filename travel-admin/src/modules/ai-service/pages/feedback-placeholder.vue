<!-- AI 反馈闭环占位页 -->
<template>
  <div class="ai-placeholder-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">AI Feedback Loop</p>
        <h1 class="page-title">反馈闭环</h1>
        <p class="page-subtitle">一期先展示后续规划，用于承接用户反馈、人工兜底与知识优化动作。</p>
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
            <div class="status-desc">当前阶段先完成知识底座和占位导航，后续会补齐反馈采集、问题归因与知识回写能力。</div>
            <el-alert
              type="info"
              show-icon
              :closable="false"
              title="建议先通过 AI 服务概览页观察知识域覆盖，再进入此页查看后续规划。"
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
    title: '反馈样本沉淀',
    desc: '汇总用户不满意回答、低命中问法与人工兜底会话，形成可回看的问题样本池。'
  },
  {
    title: '问题归因分层',
    desc: '区分知识缺失、知识过期、场景路由不准和回答表达问题，帮助后续优化有明确方向。'
  },
  {
    title: '知识优化回写',
    desc: '预留把高频反馈转成知识补录、文档重建和运营提醒的闭环动作入口。'
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
