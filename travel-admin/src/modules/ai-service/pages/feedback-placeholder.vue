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
        <el-tag type="warning" effect="light" round>即将开放</el-tag>
      </div>
    </section>

    <section class="summary-grid metric-cards--order">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">反馈入口</div>
        <div class="summary-value">{{ summaryMetrics.entryCount }}</div>
        <div class="summary-desc">规划中的用户反馈、人工兜底与低命中样本入口</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">归因层级</div>
        <div class="summary-value">{{ summaryMetrics.causeCount }}</div>
        <div class="summary-desc">用于区分知识、路由、表达和人工处理问题</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">闭环动作</div>
        <div class="summary-value">{{ summaryMetrics.actionCount }}</div>
        <div class="summary-desc">后续承接补录、重建、标注和运营提醒</div>
      </el-card>
    </section>

    <el-row :gutter="24" class="content-row">
      <el-col :xl="16" :lg="15" :md="24">
        <el-card shadow="hover" class="placeholder-card admin-management-card">
          <template #header>
            <div class="card-header">
              <span>规划能力</span>
            </div>
          </template>

          <div class="capability-list">
            <div v-for="item in plannedCapabilities" :key="item.title" class="capability-item feature-panel feature-panel--soft">
              <div class="capability-head">
                <span class="capability-index">{{ item.index }}</span>
                <div>
                  <div class="capability-title">{{ item.title }}</div>
                  <div class="capability-desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xl="8" :lg="9" :md="24">
        <el-card shadow="hover" class="placeholder-card status-card admin-management-card">
          <template #header>
            <div class="card-header">
              <span>开放说明</span>
            </div>
          </template>

          <div class="status-panel feature-panel feature-panel--warning">
            <div class="status-title">即将开放</div>
            <div class="status-desc">当前阶段先完成知识底座和占位导航，后续会补齐反馈采集、问题归因与知识回写能力。</div>
            <div class="status-checklist">
              <div v-for="item in rolloutChecklist" :key="item" class="status-checklist__item">
                <span class="status-dot"></span>
                <span>{{ item }}</span>
              </div>
            </div>
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
import { computed } from 'vue'

// 反馈闭环页当前仍是规划占位，先用静态卡片明确后续会承接的能力边界。
const plannedCapabilities = [
  {
    index: '01',
    title: '反馈样本沉淀',
    desc: '汇总用户不满意回答、低命中问法与人工兜底会话，形成可回看的问题样本池。'
  },
  {
    index: '02',
    title: '问题归因分层',
    desc: '区分知识缺失、知识过期、场景路由不准和回答表达问题，帮助后续优化有明确方向。'
  },
  {
    index: '03',
    title: '知识优化回写',
    desc: '预留把高频反馈转成知识补录、文档重建和运营提醒的闭环动作入口。'
  }
]

// 占位页也给出可量化结构，避免页面只有说明文字导致信息密度偏低。
const summaryMetrics = computed(() => ({
  entryCount: 3,
  causeCount: 4,
  actionCount: plannedCapabilities.length
}))

const rolloutChecklist = [
  '接入 AI 回复反馈数据',
  '补齐人工兜底记录聚合',
  '建立知识补录与重建流转'
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

  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .capability-list,
  .status-panel {
    display: grid;
    gap: 14px;
  }

  .capability-item {
    padding: 18px;
  }

  .capability-head {
    display: flex;
    align-items: flex-start;
    gap: 14px;
  }

  .capability-index {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    background: var(--wt-surface-elevated);
    color: var(--wt-accent-blue-text);
    font-size: 13px;
    font-weight: 700;
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

  .status-checklist {
    display: grid;
    gap: 10px;
    margin: 4px 0;
  }

  .status-checklist__item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--wt-text-regular);
  }

  .status-dot {
    width: 7px;
    height: 7px;
    flex-shrink: 0;
    border-radius: 50%;
    background: var(--wt-tag-warning-text);
  }

  @media (max-width: 1200px) {
    .summary-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
