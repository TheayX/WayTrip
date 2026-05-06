<!-- 推荐参数配置卡片 -->
<template>
  <el-card shadow="hover" class="config-card">
    <template #header>
      <div class="card-header">
        <div class="title-section">
          <span class="title">参数配置</span>
          <el-tag effect="plain" type="info" round>ItemCF 协同过滤 + 热度重排</el-tag>
        </div>
      </div>
    </template>

    <el-form :model="config" label-width="180px" class="config-form">
      <div class="impact-overview-grid">
        <div
          v-for="card in impactOverviewCards"
          :key="card.title"
          class="impact-overview-card"
          :class="card.tone"
        >
          <div class="impact-overview-head">
            <div class="impact-overview-title">{{ card.title }}</div>
            <el-tag size="small" effect="plain" :type="card.tagType" round>{{ card.tag }}</el-tag>
          </div>
          <div class="impact-overview-desc">{{ card.desc }}</div>
          <div class="impact-overview-meta">{{ card.meta }}</div>
        </div>
      </div>

      <div class="change-hint-panel">
        <div class="change-hint-card">
          <div class="change-hint-title">
            <span>即时生效项</span>
            <el-tag size="small" effect="plain" type="success" round>保存后生效</el-tag>
          </div>
          <div class="change-hint-desc">{{ immediateChangeSummary.desc }}</div>
        </div>
        <div class="change-hint-card matrix">
          <div class="change-hint-title">
            <span>矩阵相关项</span>
            <el-tag size="small" effect="plain" type="warning" round>需重建矩阵</el-tag>
          </div>
          <div class="change-hint-desc">{{ matrixChangeSummary.desc }}</div>
        </div>
      </div>

      <div class="form-section">
        <div class="section-eyebrow">
          <span>离线矩阵构建</span>
          <el-tag size="small" effect="plain" type="warning" round>保存后需重建矩阵</el-tag>
        </div>
        <div class="section-title">
          <el-icon><DataLine /></el-icon>
          <span>交互基础权重（公式 r<sub>ui</sub>）</span>
        </div>
        <div class="section-desc">先定义用户对景点的基础交互强度，再把这些行为汇总成离线矩阵使用的 <code>r_ui</code> 权重。</div>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="浏览行为权重">
              <el-input-number v-model="config.algorithm.weightView" :min="0" :max="10" :step="0.1" :precision="1" />
              <span class="form-tip">浏览基础权重，实际会再乘以来源因子和停留时长因子</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收藏行为权重">
              <el-input-number v-model="config.algorithm.weightFavorite" :min="0" :max="10" :step="0.1" :precision="1" />
              <span class="form-tip">用户收藏景点的权重</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="评分因子">
              <el-input-number v-model="config.algorithm.weightReviewFactor" :min="0" :max="5" :step="0.1" :precision="2" />
              <span class="form-tip">实际权重 = 用户评分 × 该因子</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已付款订单权重">
              <el-input-number v-model="config.algorithm.weightOrderPaid" :min="0" :max="10" :step="0.5" :precision="1" />
              <span class="form-tip">用户已付款但未完成的订单</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="已完成订单权重">
              <el-input-number v-model="config.algorithm.weightOrderCompleted" :min="0" :max="10" :step="0.5" :precision="1" />
              <span class="form-tip">用户已完成的订单，贡献最高</span>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-eyebrow">
          <span>离线矩阵构建</span>
          <el-tag size="small" effect="plain" type="warning" round>需重建矩阵</el-tag>
        </div>
        <div class="section-title">
          <el-icon><DataLine /></el-icon>
          <span>浏览行为修正</span>
        </div>
        <div class="section-desc">进一步修正浏览行为在 <code>r_ui</code> 中的贡献。实际浏览权重 = 基础浏览权重 × 来源因子 × 停留时长因子。</div>
        <el-collapse class="source-bucket-note">
          <el-collapse-item title="查看来源挡位说明" name="source-bucket">
            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="说明：数据库保留原始来源值；推荐计算时会把个性推荐、发现页、随心一选、穷游玩法、游客口碑、近期热看归到 recommendation 挡位，把列表、附近、订单、收藏、评价等归到 detail 挡位。"
            />
          </el-collapse-item>
        </el-collapse>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="首页挡位因子">
              <el-input-number v-model="config.algorithm.viewSourceFactorHome" :min="0" :max="3" :step="0.1" :precision="2" />
              <span class="form-tip">算法挡位为 home 的浏览因子</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="搜索挡位因子">
              <el-input-number v-model="config.algorithm.viewSourceFactorSearch" :min="0" :max="3" :step="0.1" :precision="2" />
              <span class="form-tip">算法挡位为 search 的浏览因子</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="推荐挡位因子">
              <el-input-number v-model="config.algorithm.viewSourceFactorRecommendation" :min="0" :max="3" :step="0.1" :precision="2" />
              <span class="form-tip">算法挡位为 recommendation 的浏览因子</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="攻略挡位因子">
              <el-input-number v-model="config.algorithm.viewSourceFactorGuide" :min="0" :max="3" :step="0.1" :precision="2" />
              <span class="form-tip">算法挡位为 guide 的浏览因子</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="详情挡位因子">
              <el-input-number v-model="config.algorithm.viewSourceFactorDetail" :min="0" :max="3" :step="0.1" :precision="2" />
              <span class="form-tip">算法挡位为 detail 的浏览因子</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="短停留阈值">
              <el-input-number v-model="config.algorithm.viewDurationShortThresholdSeconds" :min="1" :max="600" :step="1" />
              <span class="form-tip">短停留判断阈值，单位：秒</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="中停留阈值">
              <el-input-number v-model="config.algorithm.viewDurationMediumThresholdSeconds" :min="1" :max="1200" :step="1" />
              <span class="form-tip">普通停留判断阈值，单位：秒</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="长停留阈值">
              <el-input-number v-model="config.algorithm.viewDurationLongThresholdSeconds" :min="1" :max="1800" :step="1" />
              <span class="form-tip">较长停留判断阈值，单位：秒</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="短停留因子">
              <el-input-number v-model="config.algorithm.viewDurationFactorShort" :min="0" :max="3" :step="0.1" :precision="2" />
              <span class="form-tip">停留时长低于短停留阈值时使用</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="普通停留因子">
              <el-input-number v-model="config.algorithm.viewDurationFactorMedium" :min="0" :max="3" :step="0.1" :precision="2" />
              <span class="form-tip">停留时长介于短停留阈值和中停留阈值之间时使用</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="较长停留因子">
              <el-input-number v-model="config.algorithm.viewDurationFactorLong" :min="0" :max="3" :step="0.1" :precision="2" />
              <span class="form-tip">停留时长介于中停留阈值和长停留阈值之间时使用</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超长停留因子">
              <el-input-number v-model="config.algorithm.viewDurationFactorVeryLong" :min="0" :max="3" :step="0.1" :precision="2" />
              <span class="form-tip">停留时长高于长停留阈值时使用</span>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-subtitle">矩阵规模与缓存</div>
        <div class="section-subdesc">这两个参数同样属于离线矩阵链路：一个控制保留多少相似邻居，一个控制相似矩阵缓存多久。</div>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="近邻数量 K">
              <el-input-number v-model="config.algorithm.topKNeighbors" :min="5" :max="100" :step="5" />
              <span class="form-tip">相似度矩阵每个景点保留的最近邻数</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="相似度矩阵 TTL">
              <el-input-number v-model="config.cache.similarityTTLHours" :min="1" :max="168" :step="1" />
              <span class="form-tip">单位：小时，控制离线相似矩阵结果缓存时长</span>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-eyebrow">
          <span>热度同步与排序</span>
          <el-tag size="small" effect="plain" type="success" round>保存后立即生效</el-tag>
        </div>
        <div class="section-title">
          <el-icon><DataLine /></el-icon>
          <span>热度同步权重与最终重排</span>
        </div>
        <div class="section-desc">控制热度同步时各类行为如何折算为 <code>spot.heat_score</code>，以及热度如何参与最终排序。这里配置的是热度同步规则，不是实时写分逻辑，也不会影响离线相似度矩阵。</div>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="浏览行为权重">
              <el-input-number v-model="config.heat.heatViewIncrement" :min="1" :max="20" :step="1" />
              <span class="form-tip">热度同步时，每条浏览记录折算的分值</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="收藏行为权重">
              <el-input-number v-model="config.heat.heatFavoriteIncrement" :min="1" :max="20" :step="1" />
              <span class="form-tip">热度同步时，每条有效收藏记录折算的分值</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评价行为权重">
              <el-input-number v-model="config.heat.heatReviewIncrement" :min="1" :max="20" :step="1" />
              <span class="form-tip">热度同步时，每条有效评价记录折算的分值</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="支付订单权重">
              <el-input-number v-model="config.heat.heatOrderPaidIncrement" :min="1" :max="30" :step="1" />
              <span class="form-tip">热度同步时，每条已支付订单记录折算的分值</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="完成订单权重">
              <el-input-number v-model="config.heat.heatOrderCompletedIncrement" :min="1" :max="30" :step="1" />
              <span class="form-tip">热度同步时，每条已完成订单记录折算的分值</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="热度重排系数">
              <el-input-number v-model="config.heat.heatRerankFactor" :min="0" :max="1" :step="0.01" :precision="2" />
              <span class="form-tip">最终排序按 CF 分数 + 系数 × 归一化热度 轻量重排，建议保持较小值</span>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-eyebrow">
          <span>在线推荐与候选控制</span>
          <el-tag size="small" effect="plain" type="success" round>保存后立即生效</el-tag>
        </div>
        <div class="section-title">
          <el-icon><Setting /></el-icon>
          <span>在线分支切换与候选规模</span>
        </div>
        <div class="section-desc">控制用户什么时候进入协同过滤通道，以及个性化推荐、冷启动推荐各自拉多大的候选集。</div>
        <el-collapse class="source-bucket-note">
          <el-collapse-item title="查看生效范围说明" name="online-branch-scope">
            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="这些参数只影响在线推荐分支切换和候选规模，不需要重建相似度矩阵。"
            />
          </el-collapse-item>
        </el-collapse>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="最少交互数">
              <el-input-number v-model="config.algorithm.minInteractionsForCF" :min="1" :max="20" :step="1" />
              <span class="form-tip">用户交互少于此数时走冷启动策略</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="个性化候选扩容倍数">
              <el-input-number v-model="config.algorithm.candidateExpandFactor" :min="1" :max="10" :step="1" />
              <span class="form-tip">个性化推荐先计算 limit × 倍数 的候选集，再过滤和排序</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="冷启动扩容倍数">
              <el-input-number v-model="config.algorithm.coldStartExpandFactor" :min="1" :max="10" :step="1" />
              <span class="form-tip">刷新冷启动结果时先拉取更大的候选集，再做轮换截断</span>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="form-section">
        <div class="section-eyebrow">
          <span>缓存与调试</span>
          <el-tag size="small" effect="plain" type="primary" round>缓存命中与调试</el-tag>
        </div>
        <div class="section-title">
          <el-icon><Clock /></el-icon>
          <span>用户推荐缓存</span>
        </div>
        <div class="section-desc">控制用户推荐结果的缓存时长，主要影响缓存命中和调试观察节奏，不改变推荐公式。</div>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="用户推荐缓存 TTL">
              <el-input-number v-model="config.cache.userRecTTLMinutes" :min="5" :max="1440" :step="5" />
              <span class="form-tip">单位：分钟，建议 60min</span>
            </el-form-item>
          </el-col>
        </el-row>
      </div>
    </el-form>
  </el-card>
</template>

<script setup>
import { DataLine, Setting, Clock } from '@element-plus/icons-vue'

// 该卡片只承接配置展示与表单双向绑定，不直接触发保存动作。
defineProps({
  config: { type: Object, required: true },
  impactOverviewCards: { type: Array, required: true },
  immediateChangeSummary: { type: Object, required: true },
  matrixChangeSummary: { type: Object, required: true }
})

// 配置值直接双向写入父层模型，确保预览区和执行区能立即感知最新参数。
</script>

<style lang="scss" scoped>
.config-card {
  border-radius: 22px;
  border: none;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;

    .title-section {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .title {
      font-size: 16px;
      font-weight: 600;
      color: var(--wt-text-primary);
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
        background: var(--el-color-primary);
        border-radius: 2px;
      }
    }
  }
}

.impact-overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.impact-overview-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--wt-border-default);
  box-shadow: inset 0 1px 0 var(--wt-overlay-bg);
}

.impact-overview-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.impact-overview-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.impact-overview-desc {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--wt-text-regular);
}

.impact-overview-meta {
  margin-top: 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--wt-text-primary);
}

.impact-overview-card.tone-live {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--wt-tag-success-bg) 72%, var(--wt-surface-elevated)) 0%,
    color-mix(in srgb, var(--wt-tag-success-bg) 48%, var(--wt-surface-muted)) 100%
  );
  border-color: color-mix(in srgb, var(--wt-tag-success-text) 18%, var(--wt-border-default));
}

.impact-overview-card.tone-matrix {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--wt-tag-warning-bg) 74%, var(--wt-surface-elevated)) 0%,
    color-mix(in srgb, var(--wt-tag-warning-bg) 52%, var(--wt-surface-muted)) 100%
  );
  border-color: color-mix(in srgb, var(--wt-tag-warning-text) 22%, var(--wt-border-default));
}

.impact-overview-card.tone-save {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--el-color-primary-light-9) 58%, var(--wt-surface-elevated)) 0%,
    color-mix(in srgb, var(--el-color-primary-light-9) 34%, var(--wt-surface-muted)) 100%
  );
  border-color: color-mix(in srgb, var(--el-color-primary) 14%, var(--wt-border-default));
}

.impact-overview-card.tone-status {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--wt-accent-cyan-bg) 72%, var(--wt-surface-elevated)) 0%,
    color-mix(in srgb, var(--wt-accent-cyan-bg) 42%, var(--wt-surface-muted)) 100%
  );
  border-color: color-mix(in srgb, var(--wt-accent-cyan-text) 18%, var(--wt-border-default));
}

.change-hint-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.change-hint-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border: 1px solid var(--wt-border-default);
}

.change-hint-card.matrix {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--wt-tag-warning-bg) 72%, var(--wt-surface-elevated)) 0%,
    color-mix(in srgb, var(--wt-tag-warning-bg) 50%, var(--wt-surface-muted)) 100%
  );
  border-color: color-mix(in srgb, var(--wt-tag-warning-text) 22%, var(--wt-border-default));
}

.change-hint-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.change-hint-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--wt-text-regular);
}

.section-eyebrow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--wt-text-regular);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.config-form {
  .form-section {
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid var(--wt-divider-soft);

    &:last-child {
      border-bottom: none;
      margin-bottom: 0;
      padding-bottom: 0;
    }
  }

    .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: var(--wt-text-primary);
    margin-bottom: 8px;

      .el-icon {
        color: var(--el-color-primary);
        font-size: 18px;
      }
    }

  .section-desc {
    font-size: 13px;
    color: var(--wt-text-regular);
    margin-bottom: 20px;
  }

  .source-bucket-note {
    margin-bottom: 20px;
  }

  .section-subtitle {
    margin: 8px 0 8px;
    font-size: 13px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .section-subdesc {
    margin-bottom: 16px;
    font-size: 12px;
    line-height: 1.7;
    color: var(--wt-text-regular);
  }

  .form-tip {
    display: block;
    font-size: 12px;
    color: var(--wt-text-regular);
    margin-top: 4px;
  }

  :deep(.el-input-number) {
    width: 200px;
  }
}

@media (max-width: 1200px) {
  .impact-overview-grid,
  .change-hint-panel {
    grid-template-columns: 1fr;
  }
}
</style>
