<template>
  <div class="recommendation-page">
    <!-- 状态卡片 -->
    <el-row :gutter="24" class="status-row">
      <el-col :span="6">
        <el-card shadow="hover" class="status-card" :body-style="{ padding: '0px' }">
          <div class="status-card-content engine-bg">
            <div class="status-info">
              <div class="status-label">引擎状态</div>
              <div class="status-value">
                <el-tag :type="status.computing ? 'warning' : 'success'" effect="dark" round>
                  {{ status.computing ? '计算中...' : '就绪' }}
                </el-tag>
              </div>
            </div>
            <div class="status-icon">
              <el-icon><Cpu /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card" :body-style="{ padding: '0px' }">
          <div class="status-card-content time-bg">
            <div class="status-info">
              <div class="status-label">上次更新</div>
              <div class="status-value small">{{ status.lastUpdateTime || '暂无记录' }}</div>
            </div>
            <div class="status-icon">
              <el-icon><Timer /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card" :body-style="{ padding: '0px' }">
          <div class="status-card-content users-bg">
            <div class="status-info">
              <div class="status-label">覆盖用户</div>
              <div class="status-value">{{ status.totalUsers ?? '-' }}</div>
            </div>
            <div class="status-icon">
              <el-icon><User /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card" :body-style="{ padding: '0px' }">
          <div class="status-card-content spots-bg">
            <div class="status-info">
              <div class="status-label">覆盖景点</div>
              <div class="status-value">{{ status.totalSpots ?? '-' }}</div>
            </div>
            <div class="status-icon">
              <el-icon><Location /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="workspace-row">
      <el-col :xl="16" :lg="15" :md="24">
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

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="首页来源因子">
                <el-input-number v-model="config.algorithm.viewSourceFactorHome" :min="0" :max="3" :step="0.1" :precision="2" />
                <span class="form-tip">从首页进入详情页的浏览因子</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="搜索来源因子">
                <el-input-number v-model="config.algorithm.viewSourceFactorSearch" :min="0" :max="3" :step="0.1" :precision="2" />
                <span class="form-tip">从搜索结果进入详情页的浏览因子</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="推荐来源因子">
                <el-input-number v-model="config.algorithm.viewSourceFactorRecommend" :min="0" :max="3" :step="0.1" :precision="2" />
                <span class="form-tip">从推荐位进入详情页的浏览因子</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="攻略来源因子">
                <el-input-number v-model="config.algorithm.viewSourceFactorGuide" :min="0" :max="3" :step="0.1" :precision="2" />
                <span class="form-tip">从攻略页进入详情页的浏览因子</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="默认来源因子">
                <el-input-number v-model="config.algorithm.viewSourceFactorDetail" :min="0" :max="3" :step="0.1" :precision="2" />
                <span class="form-tip">默认或二跳进入详情页的浏览因子</span>
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
            <span>热度与排序</span>
            <el-tag size="small" effect="plain" type="success" round>保存后立即生效</el-tag>
          </div>
          <div class="section-title">
            <el-icon><DataLine /></el-icon>
            <span>热度累计与最终重排</span>
          </div>
          <div class="section-desc">控制每种行为如何累加到 <code>spot.heat_score</code>，以及热度如何参与最终排序。不会影响离线相似度矩阵。</div>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="浏览详情加分">
                <el-input-number v-model="config.heat.heatViewIncrement" :min="1" :max="20" :step="1" />
                <span class="form-tip">用户打开景点详情页时增加的热度分数</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="浏览去重窗口">
                <el-input-number v-model="config.heat.heatViewDedupeWindowMinutes" :min="1" :max="1440" :step="1" />
                <span class="form-tip">同一用户同一景点在该时间窗口内重复浏览不重复加热，单位：分钟</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="收藏加分">
                <el-input-number v-model="config.heat.heatFavoriteIncrement" :min="1" :max="20" :step="1" />
                <span class="form-tip">新增收藏或恢复收藏时增加的热度分数</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="评价加分">
                <el-input-number v-model="config.heat.heatReviewIncrement" :min="1" :max="20" :step="1" />
                <span class="form-tip">首次评价或恢复评价时增加的热度分数</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="支付订单加分">
                <el-input-number v-model="config.heat.heatOrderPaidIncrement" :min="1" :max="30" :step="1" />
                <span class="form-tip">订单从待支付变为已支付时增加的热度分数</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="完成订单加分">
                <el-input-number v-model="config.heat.heatOrderCompletedIncrement" :min="1" :max="30" :step="1" />
                <span class="form-tip">订单从已支付变为已完成时增加的热度分数</span>
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

        <!-- 算法参数 -->
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
          <div class="section-inline-note">
            这些参数只影响在线推荐分支切换和候选规模，不需要重建相似度矩阵。
          </div>

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

        <!-- 缓存参数 -->
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
      </el-col>

      <el-col :xl="8" :lg="9" :md="24">
        <el-card shadow="hover" class="execution-card">
          <template #header>
            <div class="card-header">
              <div class="title-section">
                <span class="title">执行区</span>
                <el-tag effect="plain" type="warning" round>先保存，再重建矩阵</el-tag>
              </div>
            </div>
          </template>

          <div class="execution-stack">
            <div class="execution-intro">
              配置修改会立即影响新的推荐请求；相似度矩阵只有在手动更新或定时任务执行后，才会按新参数重算。
            </div>

            <div class="execution-brief">
              <div class="execution-brief-title">执行建议</div>
              <div class="execution-brief-text">
                修改交互权重、浏览行为修正、近邻数量 K、相似度矩阵 TTL 后，保存配置还不够，还需要手动重建相似度矩阵；热度、在线候选和用户缓存参数保存后会直接影响新请求。
              </div>
            </div>

            <div class="matrix-action-callout" :class="{ pending: matrixChangeSummary.needsRebuild }">
              <div class="matrix-action-title">
                <span>重建相似度矩阵</span>
                <el-tag
                  size="small"
                  effect="plain"
                  :type="matrixChangeSummary.needsRebuild ? 'warning' : 'success'"
                  round
                >
                  {{ matrixChangeSummary.needsRebuild ? '存在待重建改动' : '当前无需重建' }}
                </el-tag>
              </div>
              <div class="matrix-action-text">
                只有交互权重、浏览细化因子、TopK 和矩阵缓存相关参数会影响离线相似度矩阵。热度、缓存时长和冷启动参数不需要执行这个操作。
              </div>
            </div>

            <div class="execution-actions">
              <el-button @click="handleResetConfig" round>
                <el-icon><RefreshLeft /></el-icon>
                恢复默认
              </el-button>
              <el-button type="primary" @click="handleSaveConfig" :loading="saving" round>
                <el-icon><Check /></el-icon>
                保存配置
              </el-button>
              <el-button color="#722ed1" @click="handleUpdateMatrix" :loading="updatingMatrix" round>
                <el-icon><Refresh /></el-icon>
                重建相似度矩阵
              </el-button>
            </div>

            <div class="execution-grid">
              <div class="execution-metric">
                <div class="execution-metric-label">浏览去重窗口</div>
                <div class="execution-metric-value">{{ config.heat.heatViewDedupeWindowMinutes }} 分钟</div>
                <div class="execution-metric-desc">同一用户重复浏览同一景点，在该窗口内不重复加热</div>
              </div>
              <div class="execution-metric">
                <div class="execution-metric-label">推荐缓存 TTL</div>
                <div class="execution-metric-value">{{ config.cache.userRecTTLMinutes }} 分钟</div>
                <div class="execution-metric-desc">用户推荐结果缓存时长</div>
              </div>
              <div class="execution-metric">
                <div class="execution-metric-label">矩阵缓存 TTL</div>
                <div class="execution-metric-value">{{ config.cache.similarityTTLHours }} 小时</div>
                <div class="execution-metric-desc">景点相似度矩阵缓存时长</div>
              </div>
              <div class="execution-metric">
                <div class="execution-metric-label">当前矩阵覆盖</div>
                <div class="execution-metric-value">{{ status.totalSpots ?? '-' }} 个景点</div>
                <div class="execution-metric-desc">最近一次离线矩阵计算涉及的景点数量</div>
              </div>
            </div>

            <div class="execution-notes">
              <div class="execution-note">
                <div class="execution-note-title">推荐链路</div>
                <div class="execution-note-text">行为权重决定候选分数，热度参数影响热门排序和最终轻量重排。</div>
              </div>
              <div class="execution-note">
                <div class="execution-note-title">缓存链路</div>
                <div class="execution-note-text">推荐结果和相似度矩阵都在 Redis 中缓存，但更新节奏不同。</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="debug-card preview-card">
      <template #header>
        <div class="card-header">
          <div class="title-section">
            <span class="title">预览与调试</span>
            <el-tag effect="plain" type="warning" round>预览区</el-tag>
          </div>
        </div>
      </template>

      <el-tabs v-model="activePreviewTab" class="preview-tabs">
        <el-tab-pane label="推荐调试预览" name="recommendation">
      <div class="debug-toolbar">
        <div class="debug-field">
          <span class="debug-label">用户 ID</span>
          <el-input-number v-model="debugForm.userId" :min="1" :step="1" controls-position="right" />
        </div>
        <div class="debug-field">
          <span class="debug-label">返回数量</span>
          <el-input-number v-model="debugForm.limit" :min="1" :max="20" :step="1" controls-position="right" />
        </div>
        <div class="debug-field">
          <span class="debug-label">结果来源</span>
          <el-switch v-model="debugForm.refresh" inline-prompt active-text="刷新" inactive-text="缓存" />
        </div>
        <div class="debug-field">
          <span class="debug-label">后端日志</span>
          <el-switch v-model="debugForm.debug" inline-prompt active-text="控制台日志" inactive-text="静默" />
        </div>
        <div class="debug-field">
          <span class="debug-label">稳定模式</span>
          <el-switch v-model="debugForm.stable" inline-prompt active-text="稳定" inactive-text="轮换" />
        </div>
        <el-button type="primary" :loading="previewing" @click="handlePreviewRecommendations">
          调试预览
        </el-button>
      </div>

      <div class="debug-meta" v-if="debugResult">
        <el-tag size="small" :type="recommendationTypeMeta.tagType">{{ recommendationTypeMeta.label }}</el-tag>
        <el-tag size="small" :type="debugResult.needPreference ? 'warning' : 'success'">
          {{ debugResult.needPreference ? '需要补充偏好设置' : '无需偏好引导' }}
        </el-tag>
        <el-tag size="small" type="primary">返回 {{ debugItems.length }} 条</el-tag>
        <el-tag size="small" type="info">{{ debugForm.refresh ? '本次强制刷新' : '本次优先读取缓存' }}</el-tag>
      </div>

      <div v-if="debugResult" class="debug-summary">
        <div class="debug-summary-grid">
          <div v-for="card in debugSummaryCards" :key="card.label" class="summary-card">
            <div class="summary-label">{{ card.label }}</div>
            <div class="summary-value">{{ card.value }}</div>
            <div class="summary-desc">{{ card.desc }}</div>
          </div>
        </div>
      </div>

      <el-alert
        v-if="debugResult"
        class="debug-conclusion"
        :type="recommendationTypeMeta.alertType"
        :closable="false"
        show-icon
      >
        <template #title>{{ recommendationTypeMeta.title }}</template>
        {{ recommendationTypeMeta.description }}
      </el-alert>

      <div v-if="debugInfo" class="debug-pipeline">
        <div class="debug-block-title">后端调试链路</div>
        <div class="pipeline-grid">
          <div class="pipeline-card">
            <div class="pipeline-label">触发原因</div>
            <div class="pipeline-value">{{ debugInfo.triggerReason || '未返回' }}</div>
          </div>
          <div class="pipeline-card">
            <div class="pipeline-label">交互景点数</div>
            <div class="pipeline-value">{{ debugInfo.interactionCount ?? 0 }}</div>
          </div>
          <div class="pipeline-card">
            <div class="pipeline-label">原始候选数</div>
            <div class="pipeline-value">{{ debugInfo.candidateCount ?? 0 }}</div>
          </div>
          <div class="pipeline-card">
            <div class="pipeline-label">过滤后候选数</div>
            <div class="pipeline-value">{{ debugInfo.filteredCount ?? 0 }}</div>
          </div>
        </div>
      </div>

      <div v-if="behaviorStats.length" class="debug-sections">
        <div class="debug-block-title">行为来源统计</div>
        <el-alert type="info" :closable="false" class="behavior-alert" show-icon>
          多次浏览、收藏、评分、下单如果集中在同几个景点上，最终“参与推荐的交互景点数”仍然会很小。
        </el-alert>
        <el-table :data="behaviorStats" stripe size="small" class="debug-table">
          <el-table-column prop="behavior" label="行为类型" width="120" />
          <el-table-column label="记录条数" width="120">
            <template #default="{ row }">
              <span>{{ row.recordCount ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="uniqueSpotCount" label="命中景点数" width="140" />
          <el-table-column prop="description" label="说明" min-width="320" />
        </el-table>
      </div>

      <div v-if="behaviorDetails.length" class="debug-sections">
        <div class="debug-block-title">用户交互行为明细</div>
        <el-collapse>
          <el-collapse-item :title="`查看明细（${behaviorDetails.length} 条）`" name="behavior-details">
            <el-table :data="behaviorDetails" stripe size="small" class="debug-table">
              <el-table-column prop="behavior" label="行为类型" width="140" />
              <el-table-column prop="spotId" label="景点ID" width="100" />
              <el-table-column prop="spotName" label="景点名称" min-width="180" />
              <el-table-column label="行为权重" width="140">
                <template #default="{ row }">
                  <span v-if="row.score != null" class="score-text">{{ Number(row.score).toFixed(4) }}</span>
                  <span v-else class="score-empty">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="明细说明" min-width="280" />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div v-if="debugInsights.length" class="debug-insights">
        <div class="debug-block-title">结果解读</div>
        <div class="insight-list">
          <div v-for="(insight, index) in compactDebugInsights" :key="`${index}-${insight}`" class="insight-item">
            {{ insight }}
          </div>
        </div>
      </div>

      <div v-if="debugNotes.length" class="debug-insights">
        <div class="debug-block-title">后端备注</div>
        <div class="insight-list insight-list--plain">
          <div v-for="(note, index) in debugNotes" :key="`${index}-${note}`" class="insight-item insight-item--blue">
            {{ note }}
          </div>
        </div>
      </div>

      <div v-if="debugSections.length" class="debug-sections">
        <div class="debug-block-title">关键中间结果</div>
        <el-collapse>
          <el-collapse-item
            v-for="section in debugSections"
            :key="section.key"
            :name="section.key"
            :title="`${section.title}（${section.items.length}）`"
          >
            <el-table :data="section.items" stripe size="small">
              <el-table-column prop="spotId" label="景点ID" width="100" />
              <el-table-column prop="spotName" label="景点名称" min-width="180" />
              <el-table-column label="分数/权重" width="140">
                <template #default="{ row }">
                  <span v-if="row.score != null" class="score-text">{{ Number(row.score).toFixed(4) }}</span>
                  <span v-else class="score-empty">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="说明" min-width="260" />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div v-if="resultContributions.length" class="debug-sections">
        <div class="debug-block-title">结果贡献来源</div>
        <el-collapse>
          <el-collapse-item
            v-for="item in resultContributions.slice(0, 8)"
            :key="`contrib-${item.targetSpotId}`"
            :title="`${item.targetSpotName}（最终分数 ${item.finalScore == null ? '-' : Number(item.finalScore).toFixed(4)}）`"
            :name="`contrib-${item.targetSpotId}`"
          >
            <el-table :data="item.contributors" stripe size="small">
              <el-table-column prop="spotId" label="历史景点ID" width="120" />
              <el-table-column prop="spotName" label="历史景点名称" min-width="180" />
              <el-table-column label="贡献分值" width="140">
                <template #default="{ row }">
                  <span v-if="row.score != null" class="score-text">{{ Number(row.score).toFixed(4) }}</span>
                  <span v-else class="score-empty">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="贡献说明" min-width="260" />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div v-if="debugItems.length" class="debug-top-results">
        <div class="debug-block-title">Top 结果速览</div>
        <div class="top-result-list">
          <div v-for="item in topDebugItems" :key="item.id" class="top-result-card">
            <div class="top-result-rank">#{{ item.rank }}</div>
            <div class="top-result-main">
              <div class="top-result-name">{{ item.name }}</div>
              <div class="top-result-meta">
                <span>{{ item.categoryName || '未分类' }}</span>
                <span>{{ item.regionName || '未知地区' }}</span>
              </div>
            </div>
            <div class="top-result-score">
              <div class="score-label">{{ item.score == null ? '无分数' : '推荐分' }}</div>
              <div class="score-value" :class="{ 'score-value--empty': item.score == null }">
                {{ item.scoreText }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="debugResult" class="debug-sections">
        <div class="debug-block-title">原始摘要</div>
        <el-collapse>
          <el-collapse-item name="raw-debug" title="查看请求与响应摘要">
            <div class="debug-output debug-output--compact">
              <pre>{{ debugOutput }}</pre>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <el-table v-if="debugItems.length" :data="debugTableRows" stripe class="debug-table">
        <el-table-column prop="rank" label="排名" width="80" />
        <el-table-column prop="id" label="景点ID" width="100" />
        <el-table-column prop="name" label="景点名称" min-width="180" />
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column prop="regionName" label="地区" width="140" />
        <el-table-column label="价格" width="120">
          <template #default="{ row }">
            <span>{{ row.priceText }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分概况" min-width="180">
          <template #default="{ row }">
            <span>{{ row.ratingText }}</span>
          </template>
        </el-table-column>
        <el-table-column label="推荐分数" width="160">
          <template #default="{ row }">
            <span v-if="row.score != null" class="score-text">{{ row.scoreText }}</span>
            <span v-else class="score-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="结果解读" min-width="260">
          <template #default="{ row }">
            <span>{{ row.reason }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="debugResult ? '本次请求已返回空列表，请结合上方调试输出查看原因' : '暂无调试结果'" />
        </el-tab-pane>

        <el-tab-pane label="相似邻居预览" name="similarity">
      <div class="debug-toolbar">
        <div class="debug-field">
          <span class="debug-label">景点 ID</span>
          <el-input-number v-model="similarityForm.spotId" :min="1" :step="1" controls-position="right" />
        </div>
        <div class="debug-field">
          <span class="debug-label">邻居数量</span>
          <el-input-number v-model="similarityForm.limit" :min="1" :max="20" :step="1" controls-position="right" />
        </div>
        <div class="preview-action-group">
          <el-button-group>
            <el-button class="cache-preview-button" :loading="similarityPreviewing" @click="handlePreviewSimilarity">
              缓存预览
            </el-button>
            <el-button class="update-preview-button" :loading="similarityMatrixPreviewing" @click="handlePreviewSimilarityWithMatrixUpdate">
              更新预览
            </el-button>
          </el-button-group>
        </div>
      </div>

      <div v-if="similarityResult" class="debug-summary">
        <div class="debug-summary-grid debug-summary-grid--triple">
          <div class="summary-card">
            <div class="summary-label">目标景点</div>
            <div class="summary-value summary-value--sm">{{ similarityResult.spotName || '-' }}</div>
            <div class="summary-desc">景点 ID：{{ similarityResult.spotId }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">可用邻居数</div>
            <div class="summary-value">{{ similarityResult.totalNeighbors ?? 0 }}</div>
            <div class="summary-desc">当前从 Redis 相似度矩阵读取</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">矩阵更新时间</div>
            <div class="summary-value summary-value--sm">{{ similarityResult.lastUpdateTime || '暂无记录' }}</div>
            <div class="summary-desc">用于确认预览的矩阵版本</div>
          </div>
        </div>
      </div>

      <el-table v-if="similarityResult?.neighbors?.length" :data="similarityResult.neighbors" stripe class="debug-table">
        <el-table-column prop="spotId" label="相似景点ID" width="120" />
        <el-table-column prop="spotName" label="相似景点名称" min-width="180" />
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column prop="regionName" label="地区" width="140" />
        <el-table-column label="相似度" width="160">
          <template #default="{ row }">
            <span class="score-text">{{ Number(row.similarity).toFixed(6) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="similarityResult ? '当前景点暂无可预览的相似邻居' : '请输入景点 ID 查看相似邻居'" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

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

      <el-collapse v-model="activeCollapse">
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

        <el-collapse-item title="核心算法公式" name="formula">
          <div class="help-content">
            <p>本系统基于 <strong>ItemCF（基于物品的协同过滤）</strong> 算法，核心公式如下：</p>
            <div class="formula-block">
              <div class="formula-title">公式 1：IUF 加权余弦相似度</div>
              <div class="formula-surface formula-surface--left">
                <math display="block" xmlns="http://www.w3.org/1998/Math/MathML">
                  <mrow>
                    <msub><mi>w</mi><mrow><mi>i</mi><mi>j</mi></mrow></msub>
                    <mo>=</mo>
                    <mfrac>
                      <mrow>
                        <munderover>
                          <mo>&#x2211;</mo>
                          <mrow>
                            <mi>u</mi>
                            <mo>&#x2208;</mo>
                            <mi>N</mi><mo>(</mo><mi>i</mi><mo>)</mo>
                            <mo>&#x2229;</mo>
                            <mi>N</mi><mo>(</mo><mi>j</mi><mo>)</mo>
                          </mrow>
                          <mrow />
                        </munderover>
                        <mfrac>
                          <mn>1</mn>
                          <mrow>
                            <mi>log</mi>
                            <mo>(</mo>
                            <mn>1</mn>
                            <mo>+</mo>
                            <mo>|</mo><mi>N</mi><mo>(</mo><mi>u</mi><mo>)</mo><mo>|</mo>
                            <mo>)</mo>
                          </mrow>
                        </mfrac>
                      </mrow>
                      <msqrt>
                        <mrow>
                          <mo>|</mo><mi>N</mi><mo>(</mo><mi>i</mi><mo>)</mo><mo>|</mo>
                          <mo>&#x22C5;</mo>
                          <mo>|</mo><mi>N</mi><mo>(</mo><mi>j</mi><mo>)</mo><mo>|</mo>
                        </mrow>
                      </msqrt>
                    </mfrac>
                  </mrow>
                </math>
              </div>
              <div class="formula-symbols">
                <p><code>w<sub>ij</sub></code>：景点 <code>i</code> 与景点 <code>j</code> 的相似度</p>
                <p><code>N(i)</code>：与景点 <code>i</code> 发生过交互的用户集合</p>
                <p><code>N(j)</code>：与景点 <code>j</code> 发生过交互的用户集合</p>
                <p><code>N(u)</code>：用户 <code>u</code> 历史交互过的景点集合，<code>|N(u)|</code> 为交互景点总数</p>
              </div>
            </div>
            <div class="formula-block">
              <div class="formula-title">公式 2：预测评分</div>
              <div class="formula-surface formula-surface--left">
                <math display="block" xmlns="http://www.w3.org/1998/Math/MathML">
                  <mrow>
                    <msub><mi>P</mi><mrow><mi>u</mi><mi>j</mi></mrow></msub>
                    <mo>=</mo>
                    <mrow>
                      <munderover>
                        <mo>&#x2211;</mo>
                        <mrow>
                          <mi>i</mi>
                          <mo>&#x2208;</mo>
                          <mi>N</mi><mo>(</mo><mi>u</mi><mo>)</mo>
                          <mo>&#x2229;</mo>
                          <mi>S</mi><mo>(</mo><mi>j</mi><mo>,</mo><mi>K</mi><mo>)</mo>
                        </mrow>
                        <mrow />
                      </munderover>
                      <msub><mi>w</mi><mrow><mi>j</mi><mi>i</mi></mrow></msub>
                      <mo>&#x22C5;</mo>
                      <msub><mi>r</mi><mrow><mi>u</mi><mi>i</mi></mrow></msub>
                    </mrow>
                  </mrow>
                </math>
              </div>
              <div class="formula-symbols">
                <p><code>P<sub>uj</sub></code>：用户 <code>u</code> 对景点 <code>j</code> 的预测兴趣分数</p>
                <p><code>w<sub>ji</sub></code>：景点 <code>j</code> 与景点 <code>i</code> 的相似度</p>
                <p><code>r<sub>ui</sub></code>：用户 <code>u</code> 对景点 <code>i</code> 的历史交互权重</p>
                <p><code>S(j, K)</code>：与景点 <code>j</code> 最相似的前 <code>K</code> 个景点集合</p>
              </div>
            </div>
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
            <el-alert type="info" :closable="false" style="margin-top: 16px">
              <template #title>
                协同过滤主体只读取这些业务表字段；管理端调参改的是算法参数，不会改动原始业务数据。
              </template>
            </el-alert>
          </div>
        </el-collapse-item>

        <el-collapse-item title="调参策略建议" name="strategy">
          <div class="help-content">
            <div class="strategy-grid">
              <div class="strategy-item">
                <div class="strategy-title">场景：推荐过于保守</div>
                <div class="strategy-desc">降低 <code>minInteractionsForCF</code>（如 3→2），让更多用户进入协同过滤通道。</div>
              </div>
              <div class="strategy-item">
                <div class="strategy-title">场景：推荐不够多样</div>
                <div class="strategy-desc">增大 <code>topKNeighbors</code>（如 20→30），让每个景点关联更多近邻。</div>
              </div>
              <div class="strategy-item">
                <div class="strategy-title">场景：浏览行为干扰大</div>
                <div class="strategy-desc">降低 <code>weightView</code>（如 0.5→0.2），减少浏览行为对推荐的影响。</div>
              </div>
              <div class="strategy-item">
                <div class="strategy-title">场景：偏向成交转化</div>
                <div class="strategy-desc">提高 <code>weightOrderCompleted</code> 和 <code>weightOrderPaid</code>，让购买行为主导推荐。</div>
              </div>
            </div>
          </div>
        </el-collapse-item>

        <el-collapse-item title="矩阵更新机制" name="matrix">
          <div class="help-content">
            <ul>
              <li><strong>自动更新：</strong>每天凌晨 3:00 定时任务自动重新计算相似度矩阵。</li>
              <li><strong>手动更新：</strong>点击上方「重建相似度矩阵」按钮可立即触发重算（请求会阻塞到计算完成）。</li>
              <li><strong>什么时候要重建：</strong>交互权重、浏览行为修正、近邻数量 K、相似度矩阵 TTL 这类离线参数改完后，需要手动触发或等待下次定时任务才会用新参数重算。</li>
              <li><strong>什么时候不用重建：</strong>热度、在线候选控制、用户推荐缓存这类参数保存后会直接影响新的推荐请求。</li>
              <li><strong>并发保护：</strong>同一时间只允许一次矩阵计算，重复触发会被跳过。</li>
            </ul>
          </div>
        </el-collapse-item>

        <el-collapse-item title="调试与预览说明" name="debugging">
          <div class="help-content">
            <ul>
              <li><strong>推荐调试预览 - 刷新：</strong>会跳过当前用户已有的推荐结果缓存，按当前数据重新计算该用户推荐结果，更适合排查“现在为什么推荐这些景点”。</li>
              <li><strong>缓存副作用：</strong>如果对某个用户执行“刷新”，该用户原有的推荐缓存会被删除并立即写入新的结果，相当于提前更新了这个用户当前可命中的推荐缓存。</li>
              <li><strong>推荐调试预览 - 缓存：</strong>优先查看该用户当前命中的缓存结果，更接近用户端此刻可能拿到的返回内容。</li>
              <li><strong>刷新与重建矩阵的区别：</strong>调试预览里的“刷新”只会重算当前用户的推荐结果，不会重算底层相似度矩阵；要更新离线相似关系，仍然要用上方“重建相似度矩阵”。</li>
              <li><strong>相似邻居 - 缓存预览：</strong>直接查看当前矩阵缓存里的邻居结果，适合确认 Redis 中现有的相似关系。</li>
              <li><strong>相似邻居 - 更新预览：</strong>会先执行一次矩阵更新，再展示该景点的最新相似邻居，适合在调完参数后立即验证离线矩阵效果。</li>
              <li><strong>联调建议：</strong>如果刚改了影响相似度计算的参数，建议先“重建相似度矩阵”，再看“更新预览”或“推荐调试预览 - 刷新”，这样看到的结果更一致。</li>
            </ul>
          </div>
        </el-collapse-item>

        <el-collapse-item title="默认配置参考" name="defaults">
          <div class="help-content">
            <el-descriptions border :column="2">
              <el-descriptions-item label="浏览权重">0.5</el-descriptions-item>
              <el-descriptions-item label="收藏权重">1.0</el-descriptions-item>
              <el-descriptions-item label="评分因子">0.4</el-descriptions-item>
              <el-descriptions-item label="已付款权重">3.0</el-descriptions-item>
              <el-descriptions-item label="已完成权重">4.0</el-descriptions-item>
              <el-descriptions-item label="首页来源因子">0.9</el-descriptions-item>
              <el-descriptions-item label="搜索来源因子">1.2</el-descriptions-item>
              <el-descriptions-item label="推荐来源因子">1.1</el-descriptions-item>
              <el-descriptions-item label="攻略来源因子">1.0</el-descriptions-item>
              <el-descriptions-item label="默认来源因子">1.0</el-descriptions-item>
              <el-descriptions-item label="短停留阈值">10 秒</el-descriptions-item>
              <el-descriptions-item label="中停留阈值">60 秒</el-descriptions-item>
              <el-descriptions-item label="长停留阈值">180 秒</el-descriptions-item>
              <el-descriptions-item label="短停留因子">0.6</el-descriptions-item>
              <el-descriptions-item label="普通停留因子">1.0</el-descriptions-item>
              <el-descriptions-item label="较长停留因子">1.2</el-descriptions-item>
              <el-descriptions-item label="超长停留因子">1.35</el-descriptions-item>
              <el-descriptions-item label="浏览热度加分">1</el-descriptions-item>
              <el-descriptions-item label="收藏热度加分">3</el-descriptions-item>
              <el-descriptions-item label="评价热度加分">2</el-descriptions-item>
              <el-descriptions-item label="支付热度加分">5</el-descriptions-item>
              <el-descriptions-item label="完成热度加分">8</el-descriptions-item>
              <el-descriptions-item label="浏览去重窗口">30 分钟</el-descriptions-item>
              <el-descriptions-item label="热度重排系数">0.05</el-descriptions-item>
              <el-descriptions-item label="最少交互数">3</el-descriptions-item>
              <el-descriptions-item label="近邻数量 K">20</el-descriptions-item>
              <el-descriptions-item label="个性化扩容倍数">2</el-descriptions-item>
              <el-descriptions-item label="冷启动扩容倍数">3</el-descriptions-item>
              <el-descriptions-item label="矩阵 TTL">24 小时</el-descriptions-item>
              <el-descriptions-item label="推荐缓存 TTL">60 分钟</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import {
  getRecommendationConfig,
  updateRecommendationConfig,
  getRecommendationStatus,
  updateRecommendationMatrix,
  previewRecommendations,
  previewSimilarityNeighbors
} from '@/api/recommendation'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Cpu, Timer, User, Location, RefreshLeft, Check, Refresh,
  DataLine, Setting, Clock
} from '@element-plus/icons-vue'

const createDefaultConfig = () => ({
  algorithm: {
    weightView: 0.5,
    weightFavorite: 1.0,
    weightReviewFactor: 0.4,
    weightOrderPaid: 3.0,
    weightOrderCompleted: 4.0,
    viewSourceFactorHome: 0.9,
    viewSourceFactorSearch: 1.2,
    viewSourceFactorRecommend: 1.1,
    viewSourceFactorGuide: 1.0,
    viewSourceFactorDetail: 1.0,
    viewDurationShortThresholdSeconds: 10,
    viewDurationMediumThresholdSeconds: 60,
    viewDurationLongThresholdSeconds: 180,
    viewDurationFactorShort: 0.6,
    viewDurationFactorMedium: 1.0,
    viewDurationFactorLong: 1.2,
    viewDurationFactorVeryLong: 1.35,
    minInteractionsForCF: 3,
    topKNeighbors: 20,
    candidateExpandFactor: 2,
    coldStartExpandFactor: 3
  },
  heat: {
    heatViewIncrement: 1,
    heatFavoriteIncrement: 3,
    heatReviewIncrement: 2,
    heatOrderPaidIncrement: 5,
    heatOrderCompletedIncrement: 8,
    heatViewDedupeWindowMinutes: 30,
    heatRerankFactor: 0.05
  },
  cache: {
    similarityTTLHours: 24,
    userRecTTLMinutes: 60
  }
})

const matrixFieldPaths = [
  'algorithm.weightView',
  'algorithm.weightFavorite',
  'algorithm.weightReviewFactor',
  'algorithm.weightOrderPaid',
  'algorithm.weightOrderCompleted',
  'algorithm.viewSourceFactorHome',
  'algorithm.viewSourceFactorSearch',
  'algorithm.viewSourceFactorRecommend',
  'algorithm.viewSourceFactorGuide',
  'algorithm.viewSourceFactorDetail',
  'algorithm.viewDurationShortThresholdSeconds',
  'algorithm.viewDurationMediumThresholdSeconds',
  'algorithm.viewDurationLongThresholdSeconds',
  'algorithm.viewDurationFactorShort',
  'algorithm.viewDurationFactorMedium',
  'algorithm.viewDurationFactorLong',
  'algorithm.viewDurationFactorVeryLong',
  'algorithm.topKNeighbors',
  'cache.similarityTTLHours'
]

const immediateFieldPaths = [
  'algorithm.minInteractionsForCF',
  'algorithm.candidateExpandFactor',
  'algorithm.coldStartExpandFactor',
  'heat.heatViewIncrement',
  'heat.heatFavoriteIncrement',
  'heat.heatReviewIncrement',
  'heat.heatOrderPaidIncrement',
  'heat.heatOrderCompletedIncrement',
  'heat.heatViewDedupeWindowMinutes',
  'heat.heatRerankFactor',
  'cache.userRecTTLMinutes'
]

const cloneConfig = (value) => JSON.parse(JSON.stringify(value))

const config = reactive(createDefaultConfig())
const savedConfig = ref(cloneConfig(createDefaultConfig()))

const applyConfig = (nextConfig = {}) => {
  const defaults = createDefaultConfig()
  Object.assign(config.algorithm, defaults.algorithm, nextConfig.algorithm || {})
  Object.assign(config.heat, defaults.heat, nextConfig.heat || {})
  Object.assign(config.cache, defaults.cache, nextConfig.cache || {})
}

const getByPath = (target, path) => path.split('.').reduce((acc, key) => acc?.[key], target)
const getChangedPaths = (paths) => paths.filter(path => getByPath(config, path) !== getByPath(savedConfig.value, path))

const matrixChangedPaths = computed(() => getChangedPaths(matrixFieldPaths))
const immediateChangedPaths = computed(() => getChangedPaths(immediateFieldPaths))

const status = reactive({
  lastUpdateTime: null,
  totalUsers: null,
  totalSpots: null,
  computing: false
})

const impactOverviewCards = computed(() => [
  {
    title: '在线即时生效',
    desc: '热度累计、冷启动触发、用户缓存和热度重排在保存后会立刻影响新请求。',
    meta: `${immediateChangedPaths.value.length} 项待保存`,
    tag: '保存即生效',
    tagType: 'success',
    tone: 'tone-live'
  },
  {
    title: '离线相似度矩阵',
    desc: '交互权重、浏览因子、TopK 和矩阵 TTL 会影响离线相似邻居或其缓存节奏。',
    meta: `${matrixChangedPaths.value.length} 项待重建矩阵`,
    tag: '需重建矩阵',
    tagType: 'warning',
    tone: 'tone-matrix'
  },
  {
    title: '保存配置',
    desc: '保存只写入参数，不会自动触发相似度矩阵重建。',
    meta: '建议先保存，再按需重建矩阵',
    tag: '不自动重算',
    tagType: 'info',
    tone: 'tone-save'
  },
  {
    title: '当前矩阵版本',
    desc: '离线参数是否真正生效，要看最近一次矩阵重建时间。',
    meta: status.lastUpdateTime || '尚未生成',
    tag: '离线版本',
    tagType: 'primary',
    tone: 'tone-status'
  }
])

const matrixChangeSummary = computed(() => ({
  count: matrixChangedPaths.value.length,
  needsRebuild: matrixChangedPaths.value.length > 0,
  desc: matrixChangedPaths.value.length
    ? `已改动 ${matrixChangedPaths.value.length} 个离线矩阵相关字段`
    : '当前没有待重建的矩阵参数变更'
}))

const immediateChangeSummary = computed(() => ({
  count: immediateChangedPaths.value.length,
  desc: immediateChangedPaths.value.length
    ? `已改动 ${immediateChangedPaths.value.length} 个在线即时生效字段`
    : '当前没有待保存的即时生效字段变更'
}))

const saving = ref(false)
const updatingMatrix = ref(false)
const previewing = ref(false)
const similarityPreviewing = ref(false)
const similarityMatrixPreviewing = ref(false)
const activePreviewTab = ref('recommendation')
const activeCollapse = ref([])
const debugResult = ref(null)
const similarityResult = ref(null)
const debugForm = reactive({
  userId: 1,
  limit: 6,
  refresh: true,
  debug: true,
  stable: true
})
const similarityForm = reactive({
  spotId: 1,
  limit: 8
})
const debugOutput = computed(() => {
  if (!debugResult.value) return ''

  const lines = [
    `request.userId = ${debugForm.userId}`,
    `request.limit = ${debugForm.limit}`,
    `request.refresh = ${debugForm.refresh}`,
    `request.debug = ${debugForm.debug}`,
    `request.stable = ${debugForm.stable}`,
    `response.type = ${debugResult.value.type}`,
    `response.needPreference = ${debugResult.value.needPreference}`,
    `response.count = ${debugResult.value.list?.length || 0}`
  ]

  if (!debugResult.value.list?.length) {
    lines.push('response.items = []')
    lines.push('说明：当前请求已返回空推荐列表。若 type=preference，通常表示用户命中了偏好冷启动，但对应分类下没有可用景点。')
    return lines.join('\n')
  }

  debugResult.value.list.forEach((item, index) => {
    lines.push(
      `item[${index}] = { id: ${item.id}, name: ${item.name}, score: ${
        item.score == null ? 'null' : Number(item.score).toFixed(4)
      }, category: ${item.categoryName || '-'}, region: ${item.regionName || '-'} }`
    )
  })

  return lines.join('\n')
})

const debugItems = computed(() => debugResult.value?.list || [])
const debugInfo = computed(() => debugResult.value?.debugInfo || null)
const debugNotes = computed(() => debugInfo.value?.notes || [])
const behaviorStats = computed(() => debugInfo.value?.behaviorStats || [])
const behaviorDetails = computed(() => debugInfo.value?.behaviorDetails || [])
const resultContributions = computed(() => debugInfo.value?.resultContributions || [])

const debugSections = computed(() => {
  if (!debugInfo.value) return []
  const sections = [
    { key: 'interactions', title: '用户交互权重', items: debugInfo.value.userInteractions || [] },
    { key: 'candidates', title: '原始候选分数', items: debugInfo.value.candidateScores || [] },
    { key: 'filtered', title: '过滤后候选分数', items: debugInfo.value.filteredScores || [] },
    { key: 'reranked', title: '重排后候选分数', items: debugInfo.value.rerankedScores || [] },
    { key: 'removed', title: '被过滤景点', items: debugInfo.value.filteredOutItems || [] }
  ]
  return sections.filter(section => section.items.length)
})

const compactDebugInsights = computed(() => debugInsights.value.slice(0, 3))

const recommendationTypeMeta = computed(() => {
  const type = debugResult.value?.type
  if (type === 'personalized') {
    return {
      label: '个性化推荐',
      tagType: 'success',
      alertType: 'success',
      title: '当前结果来自协同过滤个性化推荐',
      description: '说明该用户已有足够交互行为，系统已基于 ItemCF、用户历史交互权重和相似景点关系完成推荐。'
    }
  }
  if (type === 'preference') {
    return {
      label: '偏好冷启动',
      tagType: 'warning',
      alertType: 'warning',
      title: '当前结果来自偏好冷启动推荐',
      description: '说明用户个性化交互不足，系统改为按用户偏好分类召回景点。'
    }
  }
  if (type === 'hot') {
    return {
      label: '热门兜底',
      tagType: 'info',
      alertType: 'info',
      title: '当前结果来自热门兜底推荐',
      description: '说明用户交互不足且偏好信息有限，系统回退到热门景点推荐。'
    }
  }
  return {
    label: '未知类型',
    tagType: 'info',
    alertType: 'info',
    title: '当前结果类型未识别',
    description: '请结合后端控制台调试日志进一步确认推荐链路。'
  }
})

const scoreStats = computed(() => {
  const scoredItems = debugItems.value.filter(item => item.score != null)
  if (!scoredItems.length) {
    return {
      count: 0,
      average: null,
      max: null,
      min: null
    }
  }
  const scores = scoredItems.map(item => Number(item.score))
  return {
    count: scoredItems.length,
    average: scores.reduce((sum, value) => sum + value, 0) / scores.length,
    max: Math.max(...scores),
    min: Math.min(...scores)
  }
})

const debugSummaryCards = computed(() => {
  const topItem = debugItems.value[0]
  return [
    {
      label: '推荐来源',
      value: recommendationTypeMeta.value.label,
      desc: debugInfo.value?.triggerReason || (debugResult.value?.needPreference ? '当前链路仍建议补充偏好' : '当前链路无需额外偏好引导')
    },
    {
      label: '返回结果数',
      value: String(debugItems.value.length),
      desc: `请求数量 ${debugForm.limit}，实际返回 ${debugItems.value.length}`
    },
    {
      label: '最高推荐分',
      value: topItem?.score != null ? Number(topItem.score).toFixed(4) : '无',
      desc: topItem ? `Top1：${topItem.name}` : '暂无返回结果'
    },
    {
      label: '平均推荐分',
      value: scoreStats.value.average != null ? scoreStats.value.average.toFixed(4) : '无',
      desc: scoreStats.value.count ? `有 ${scoreStats.value.count} 条结果带个性化分数` : '当前结果没有返回个性化分数'
    }
  ]
})

const debugInsights = computed(() => {
  if (!debugResult.value) return []
  const insights = []
  if (debugResult.value.type === 'personalized') {
    insights.push('当前用户已满足协同过滤触发条件，本次结果优先反映历史交互与相似景点关系。')
  }
  if (debugResult.value.type === 'preference') {
    insights.push('当前结果来自偏好冷启动，建议对比用户偏好分类是否和返回景点分类一致。')
  }
  if (debugResult.value.type === 'hot') {
    insights.push('当前结果来自热门兜底，此时推荐分数字段通常为空，重点看热度和上架数据是否合理。')
  }
  if (debugResult.value.needPreference) {
    insights.push('接口提示需要偏好引导，说明用户侧可以进一步补充偏好标签以改善冷启动效果。')
  }
  if (debugInfo.value?.filteredOutItems?.length) {
    insights.push(`后端共过滤掉 ${debugInfo.value.filteredOutItems.length} 个已交互景点，避免把用户已经看过/买过的景点继续推荐回来。`)
  }
  if (!debugItems.value.length) {
    insights.push('本次返回空列表。优先检查用户偏好命中的分类下是否有已上架且未删除的景点。')
  }
  if (scoreStats.value.count > 1 && scoreStats.value.max != null && scoreStats.value.min != null) {
    insights.push(`当前推荐分数区间为 ${scoreStats.value.min.toFixed(4)} ~ ${scoreStats.value.max.toFixed(4)}，可用于判断结果区分度是否足够。`)
  }
  if (debugForm.debug) {
    insights.push('本次已启用后端详细调试日志，可同步结合服务端控制台查看交互权重、候选分数、过滤与重排信息。')
  }
  return insights
})

const topDebugItems = computed(() =>
  debugItems.value.slice(0, 3).map((item, index) => ({
    ...item,
    rank: index + 1,
    scoreText: item.score == null ? '-' : Number(item.score).toFixed(4)
  }))
)

const debugTableRows = computed(() =>
  debugItems.value.map((item, index) => {
    const scoreText = item.score == null ? '-' : Number(item.score).toFixed(4)
    const priceText = item.price == null ? '-' : `¥${item.price}`
    const ratingText = item.avgRating == null
      ? '暂无评分'
      : `${Number(item.avgRating).toFixed(1)} 分 / ${item.ratingCount || 0} 条评价`
    let reason = '请结合后端详细日志查看交互权重、候选分数与过滤过程。'
    if (debugResult.value?.type === 'personalized') {
      reason = item.score == null
        ? '当前结果来自个性化链路，但该项未返回分数。请检查后端打分与响应填充。'
        : '该景点保留了个性化推荐分，适合继续比对候选分数与热度重排结果。'
    } else if (debugResult.value?.type === 'preference') {
      reason = '该景点来自偏好分类召回，重点确认用户偏好与景点分类是否匹配。'
    } else if (debugResult.value?.type === 'hot') {
      reason = '该景点来自热门兜底，重点确认热度、上架状态和冷启动逻辑。'
    }
    return {
      ...item,
      rank: index + 1,
      scoreText,
      priceText,
      ratingText,
      reason
    }
  })
)

const weightExplanations = [
  { behavior: '浏览', param: 'weightView', default: '0.5', description: '浏览基础权重；实际按 来源因子 × 停留时长因子 细化，来源和时长规则均可在管理端配置' },
  { behavior: '收藏', param: 'weightFavorite', default: '1.0', description: '用户主动收藏景点，表示明确兴趣' },
  { behavior: '评分', param: 'weightReviewFactor', default: '0.4', description: '实际权重 = 评分(1~5) × 因子，如5分评价 = 5×0.4 = 2.0' },
  { behavior: '已付款', param: 'weightOrderPaid', default: '3.0', description: '用户已下单付款但未完成行程，表示强烈意向' },
  { behavior: '已完成', param: 'weightOrderCompleted', default: '4.0', description: '用户已完成行程，最强信号' }
]

const cfDataFieldReferences = [
  {
    table: 'user_spot_view',
    fields: ['user_id', 'spot_id', 'view_source', 'view_duration'],
    usage: '浏览行为权重、来源因子、停留时长因子',
    phase: '交互权重构建'
  },
  {
    table: 'user_spot_favorite',
    fields: ['user_id', 'spot_id', 'is_deleted'],
    usage: '收藏行为权重与已交互过滤',
    phase: '交互权重构建'
  },
  {
    table: 'user_spot_review',
    fields: ['user_id', 'spot_id', 'score', 'is_deleted'],
    usage: '评分权重、评论分值信号',
    phase: '交互权重构建'
  },
  {
    table: 'order',
    fields: ['user_id', 'spot_id', 'status', 'is_deleted'],
    usage: '提取已支付/已完成订单行为权重',
    phase: '交互权重构建'
  }
]

const coldStartDataFieldReferences = [
  {
    table: 'user_preference',
    fields: ['user_id', 'tag', 'is_deleted'],
    usage: '冷启动偏好分类召回',
    phase: '冷启动'
  },
  {
    table: 'spot',
    fields: ['id', 'category_id', 'heat_score', 'is_published', 'is_deleted'],
    usage: '上架过滤、冷启动热门排序、热度重排',
    phase: '候选生成与重排'
  }
]

// 获取配置
const fetchConfig = async () => {
  try {
    const res = await getRecommendationConfig()
    if (res.data) {
      applyConfig(res.data)
      savedConfig.value = cloneConfig({
        algorithm: { ...config.algorithm },
        heat: { ...config.heat },
        cache: { ...config.cache }
      })
    }
  } catch (e) {
    console.error('获取配置失败', e)
  }
}

// 获取状态
const fetchStatus = async () => {
  try {
    const res = await getRecommendationStatus()
    if (res.data) {
      Object.assign(status, res.data)
    }
  } catch (e) {
    console.error('获取状态失败', e)
  }
}

// 保存配置
const handleSaveConfig = async () => {
  try {
    saving.value = true
    const payload = {
      algorithm: { ...config.algorithm },
      heat: { ...config.heat },
      cache: { ...config.cache }
    }
    const matrixChangedCount = matrixChangedPaths.value.length
    const immediateChangedCount = immediateChangedPaths.value.length
    await updateRecommendationConfig(payload)
    savedConfig.value = cloneConfig(payload)
    if (matrixChangedCount > 0 && immediateChangedCount > 0) {
      ElMessage.success('配置已保存：在线参数立即生效；离线矩阵参数需重建相似度矩阵后完全生效')
      return
    }
    if (matrixChangedCount > 0) {
      ElMessage.success('配置已保存：当前改动属于离线矩阵参数，请继续执行“重建相似度矩阵”')
      return
    }
    ElMessage.success('配置已保存：当前改动会在新的推荐请求中立即生效')
    return
  } catch (e) {
    ElMessage.error('保存配置失败')
  } finally {
    saving.value = false
  }
}

// 恢复默认
const handleResetConfig = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要将所有参数恢复为默认值吗？这将覆盖当前配置。',
      '恢复默认配置',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const defaults = createDefaultConfig()
    applyConfig(defaults)
    await updateRecommendationConfig(defaults)
    savedConfig.value = cloneConfig(defaults)
    ElMessage.success('已恢复为默认配置')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('恢复默认失败')
    }
  }
}

// 更新矩阵
const handleUpdateMatrix = async () => {
  try {
    await ElMessageBox.confirm(
      '更新矩阵将使用当前保存的参数重新计算所有景点的相似度。此操作可能耗时较长，确定继续？',
      '手动更新矩阵',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    updatingMatrix.value = true
    status.computing = true
    await updateRecommendationMatrix()
    ElMessage.success('相似度矩阵更新完成！')
    await fetchStatus()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('矩阵更新失败')
    }
  } finally {
    updatingMatrix.value = false
    status.computing = false
  }
}

const handlePreviewRecommendations = async () => {
  if (!debugForm.userId) {
    ElMessage.warning('请输入用户 ID')
    return
  }
  try {
    previewing.value = true
    const res = await previewRecommendations({ ...debugForm })
    debugResult.value = res.data || null
    ElMessage.success('调试预览完成')
  } catch (e) {
    ElMessage.error('调试预览失败')
  } finally {
    previewing.value = false
  }
}

const handlePreviewSimilarity = async () => {
  if (!similarityForm.spotId) {
    ElMessage.warning('请输入景点 ID')
    return
  }
  try {
    similarityPreviewing.value = true
    const res = await previewSimilarityNeighbors({ ...similarityForm })
    similarityResult.value = res.data || null
    ElMessage.success('相似邻居预览完成')
  } catch (e) {
    ElMessage.error('相似邻居预览失败')
  } finally {
    similarityPreviewing.value = false
  }
}

const handlePreviewSimilarityWithMatrixUpdate = async () => {
  if (!similarityForm.spotId) {
    ElMessage.warning('请输入景点 ID')
    return
  }
  try {
    similarityMatrixPreviewing.value = true
    status.computing = true
    await updateRecommendationMatrix()
    const res = await previewSimilarityNeighbors({ ...similarityForm })
    similarityResult.value = res.data || null
    ElMessage.success('矩阵更新完成，已加载最新相似邻居')
    await fetchStatus()
  } catch (e) {
    ElMessage.error('更新预览失败')
  } finally {
    similarityMatrixPreviewing.value = false
    status.computing = false
  }
}

onMounted(() => {
  fetchConfig()
  fetchStatus()
})
</script>

<style lang="scss" scoped>
.recommendation-page {
  .status-row {
    margin-bottom: 24px;
  }

  .workspace-row,
  .preview-row {
    margin-bottom: 24px;
  }

  .status-card {
    border-radius: 12px;
    border: none;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.15) !important;
    }
  }

  .status-card-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24px;
    border-radius: 12px;
    color: #fff;
    position: relative;
    overflow: hidden;

    &::after {
      content: '';
      position: absolute;
      top: -20px;
      right: -20px;
      width: 100px;
      height: 100px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 50%;
    }

    &.engine-bg { background: linear-gradient(135deg, #52c41a 0%, #36cfc9 100%); }
    &.time-bg { background: linear-gradient(135deg, #1890ff 0%, #69b1ff 100%); }
    &.users-bg { background: linear-gradient(135deg, #722ed1 0%, #b37feb 100%); }
    &.spots-bg { background: linear-gradient(135deg, #ff4d4f 0%, #ff7a45 100%); }
  }

  .status-info {
    z-index: 1;

    .status-label {
      font-size: 14px;
      opacity: 0.9;
      margin-bottom: 8px;
    }

    .status-value {
      font-size: 28px;
      font-weight: bold;
      line-height: 1.3;

      &.small {
        font-size: 15px;
        font-weight: normal;
      }
    }
  }

  .status-icon {
    font-size: 56px;
    opacity: 0.8;
    z-index: 1;
    transition: transform 0.3s;
  }

  .status-card:hover .status-icon {
    transform: scale(1.1) rotate(5deg);
  }

  // 配置卡片
  .config-card {
    border-radius: 12px;
    border: none;
    margin-bottom: 24px;

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
          background: #722ed1;
          border-radius: 2px;
        }
      }

      .action-section {
        display: flex;
        gap: 8px;
      }
    }
  }

  .debug-card {
    border-radius: 12px;
    border: none;
    margin-bottom: 24px;
  }

  .preview-card {
    margin-bottom: 24px;
  }

  .preview-tabs :deep(.el-tabs__header) {
    margin-bottom: 18px;
  }

  .preview-tabs :deep(.el-tabs__nav-wrap::after) {
    background-color: #e8edf5;
  }

  .preview-tabs :deep(.el-tabs__item) {
    font-weight: 600;
  }

  .execution-card {
    border-radius: 12px;
    border: none;
    margin-bottom: 24px;
  }

  .execution-stack {
    display: grid;
    gap: 16px;
  }

  .execution-intro {
    padding: 14px 16px;
    border-radius: 12px;
    background: linear-gradient(135deg, #fff7e8 0%, #fff1db 100%);
    border: 1px solid #ffd8a8;
    color: #7a4e00;
    line-height: 1.7;
    font-size: 13px;
  }

  .impact-overview-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 16px;
  }

  .impact-overview-card {
    padding: 16px;
    border-radius: 14px;
    border: 1px solid #e5e7eb;
    background: #fff;
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
    color: #1f2937;
  }

  .impact-overview-desc {
    margin-top: 10px;
    font-size: 12px;
    line-height: 1.7;
    color: #5b6475;
  }

  .impact-overview-meta {
    margin-top: 12px;
    font-size: 12px;
    font-weight: 600;
    color: #334155;
  }

  .impact-overview-card.tone-live {
    background: linear-gradient(135deg, #f3fff7 0%, #ebfff1 100%);
    border-color: #b7ebc6;
  }

  .impact-overview-card.tone-matrix {
    background: linear-gradient(135deg, #fff9f0 0%, #fff4e6 100%);
    border-color: #ffd591;
  }

  .impact-overview-card.tone-save {
    background: linear-gradient(135deg, #f8fbff 0%, #f2f7ff 100%);
    border-color: #d6e4ff;
  }

  .impact-overview-card.tone-status {
    background: linear-gradient(135deg, #f7f5ff 0%, #f0ebff 100%);
    border-color: #d3c3ff;
  }

  .change-hint-panel {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 18px;
  }

  .change-hint-card {
    padding: 14px 16px;
    border-radius: 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
  }

  .change-hint-card.matrix {
    background: #fffaf0;
    border-color: #fbd38d;
  }

  .change-hint-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    font-size: 13px;
    font-weight: 700;
    color: #253046;
  }

  .change-hint-desc {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.7;
    color: #5b6475;
  }

  .section-eyebrow {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 10px;
    font-size: 12px;
    font-weight: 700;
    color: #5b6475;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .section-inline-note {
    margin-bottom: 16px;
    padding: 12px 14px;
    border-radius: 10px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    font-size: 12px;
    line-height: 1.7;
    color: #607086;
  }

  .execution-brief {
    padding: 14px 16px;
    border-radius: 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
  }

  .execution-brief-title {
    font-size: 13px;
    font-weight: 700;
    color: #253046;
  }

  .execution-brief-text {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.7;
    color: #607086;
  }

  .matrix-action-callout {
    padding: 14px 16px;
    border-radius: 12px;
    background: linear-gradient(135deg, #f3fff7 0%, #ebfff1 100%);
    border: 1px solid #b7ebc6;
  }

  .matrix-action-callout.pending {
    background: linear-gradient(135deg, #fff9f0 0%, #fff4e6 100%);
    border-color: #ffd591;
  }

  .matrix-action-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    font-size: 13px;
    font-weight: 700;
    color: #253046;
  }

  .matrix-action-text {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.7;
    color: #607086;
  }

  .execution-actions {
    display: grid;
    gap: 10px;
  }

  .execution-actions .el-button {
    justify-content: center;
    margin-left: 0;
  }

  .execution-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .execution-metric {
    padding: 14px 16px;
    border-radius: 12px;
    background: linear-gradient(135deg, #f7faff 0%, #eef5ff 100%);
    border: 1px solid #d8e7ff;
  }

  .execution-metric-label {
    font-size: 12px;
    color: #6b7280;
  }

  .execution-metric-value {
    margin-top: 8px;
    font-size: 20px;
    font-weight: 700;
    color: #1d4ed8;
    line-height: 1.25;
  }

  .execution-metric-desc {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.6;
    color: #5b6475;
  }

  .execution-notes {
    display: grid;
    gap: 12px;
  }

  .execution-note {
    padding: 14px 16px;
    border-radius: 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
  }

  .execution-note-title {
    font-size: 13px;
    font-weight: 700;
    color: #253046;
  }

  .execution-note-text {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.7;
    color: #607086;
  }

  .debug-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }

  .debug-field {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .debug-label {
    font-size: 13px;
    color: #606266;
    white-space: nowrap;
  }

  .preview-action-group {
    display: flex;
    align-items: center;
  }

  .cache-preview-button {
    border-color: #d7dde8;
    background: #f7f8fb;
    color: #516074;
  }

  .cache-preview-button:hover,
  .cache-preview-button:focus {
    border-color: #bcc8da;
    background: #eef2f8;
    color: #344256;
  }

  .update-preview-button {
    border-color: #ff6b18;
    background: linear-gradient(135deg, #ff7a1a 0%, #ff5a14 100%);
    color: #fff;
    font-weight: 600;
    box-shadow: 0 8px 18px rgba(255, 107, 24, 0.2);
  }

  .update-preview-button:hover,
  .update-preview-button:focus {
    border-color: #ff5a14;
    background: linear-gradient(135deg, #ff862e 0%, #ff641f 100%);
    color: #fff;
  }

  .debug-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
  }

  .debug-summary {
    margin-bottom: 16px;
  }

  .debug-summary-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  .debug-summary-grid--triple {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .summary-card {
    padding: 16px;
    border-radius: 12px;
    background: linear-gradient(135deg, #f8fbff 0%, #eef5ff 100%);
    border: 1px solid #d9e7ff;
  }

  .summary-label {
    margin-bottom: 8px;
    font-size: 12px;
    color: #6b7280;
  }

  .summary-value {
    font-size: 24px;
    font-weight: 700;
    color: #1d4ed8;
    line-height: 1.2;
  }

  .summary-value--sm {
    font-size: 18px;
    line-height: 1.4;
  }

  .summary-desc {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.6;
    color: #5b6475;
  }

  .debug-conclusion {
    margin-bottom: 16px;
  }

  .debug-insights,
  .debug-top-results,
  .debug-pipeline,
  .debug-sections {
    margin-bottom: 16px;
  }

  .behavior-alert {
    margin-bottom: 12px;
  }

  .pipeline-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  .pipeline-card {
    padding: 14px 16px;
    border-radius: 12px;
    background: linear-gradient(135deg, #fffdf6 0%, #fff7e8 100%);
    border: 1px solid #ffe2b7;
  }

  .pipeline-label {
    font-size: 12px;
    color: #8a6b30;
  }

  .pipeline-value {
    margin-top: 8px;
    font-size: 18px;
    font-weight: 700;
    line-height: 1.5;
    color: #7a4e00;
  }

  .debug-block-title {
    margin-bottom: 10px;
    font-size: 14px;
    font-weight: 700;
    color: #253046;
  }

  .insight-list {
    display: grid;
    gap: 10px;
  }

  .insight-list--plain {
    gap: 8px;
  }

  .insight-item {
    padding: 12px 14px;
    border-radius: 10px;
    background: #fff9eb;
    border: 1px solid #ffe2a8;
    color: #8a5a00;
    line-height: 1.7;
    font-size: 13px;
  }

  .insight-item--blue {
    background: #eef6ff;
    border-color: #cfe3ff;
    color: #245bdb;
  }

  .top-result-list {
    display: grid;
    gap: 12px;
  }

  .top-result-card {
    display: grid;
    grid-template-columns: 72px minmax(0, 1fr) 150px;
    align-items: center;
    gap: 14px;
    padding: 16px;
    border-radius: 12px;
    background: linear-gradient(135deg, #ffffff 0%, #f7faff 100%);
    border: 1px solid #e3ecff;
  }

  .top-result-rank {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 16px;
    background: linear-gradient(135deg, #1677ff 0%, #69b1ff 100%);
    color: #fff;
    font-size: 18px;
    font-weight: 700;
  }

  .top-result-name {
    font-size: 16px;
    font-weight: 700;
    color: #1f2937;
  }

  .top-result-meta {
    margin-top: 6px;
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    font-size: 12px;
    color: #667085;
  }

  .top-result-score {
    text-align: right;
  }

  .score-label {
    font-size: 12px;
    color: #6b7280;
  }

  .score-value {
    margin-top: 6px;
    font-family: 'Consolas', 'Menlo', monospace;
    font-size: 22px;
    font-weight: 700;
    color: #1677ff;
  }

  .score-value--empty {
    color: #9ca3af;
  }

  .debug-output {
    margin-bottom: 16px;
    padding: 14px 16px;
    background: #fafbfc;
    border: 1px solid #e5eaf3;
    border-radius: 10px;
  }

  .debug-output--compact {
    margin-bottom: 0;
  }

  .debug-output-title {
    margin-bottom: 10px;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .debug-output pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
    font-size: 13px;
    line-height: 1.7;
    color: #4b5563;
    font-family: 'Consolas', 'Menlo', monospace;
  }

  .debug-table {
    margin-top: 8px;
  }

  .score-text {
    font-family: 'Consolas', 'Menlo', monospace;
    color: #1677ff;
    font-weight: 600;
  }

  .score-empty {
    color: #bfbfbf;
  }

  .config-form {
    .form-section {
      margin-bottom: 32px;
      padding-bottom: 24px;
      border-bottom: 1px solid #f0f2f5;

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
      color: #303133;
      margin-bottom: 8px;

      .el-icon {
        color: #722ed1;
        font-size: 18px;
      }
    }

    .section-desc {
      font-size: 13px;
      color: #909399;
      margin-bottom: 20px;
    }

    .section-subtitle {
      margin: 8px 0 8px;
      font-size: 13px;
      font-weight: 700;
      color: #253046;
    }

    .section-subdesc {
      margin-bottom: 16px;
      font-size: 12px;
      line-height: 1.7;
      color: #607086;
    }

    .form-tip {
      display: block;
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }

    :deep(.el-input-number) {
      width: 200px;
    }
  }

  // 帮助卡片
  .help-card {
    border-radius: 12px;
    border: none;

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
          background: #1890ff;
          border-radius: 2px;
        }
      }
    }
  }

  .help-intro {
    margin-bottom: 16px;
    padding: 14px 16px;
    border-radius: 12px;
    background: linear-gradient(135deg, #f6fbff 0%, #eef7ff 100%);
    border: 1px solid #d8eaff;
    color: #44607a;
    line-height: 1.7;
    font-size: 13px;
  }

  .help-content {
    padding: 8px 0;
    line-height: 1.8;
    color: #606266;

    p {
      margin: 8px 0;
    }

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

  .formula-block {
    background: #fafafa;
    border-radius: 8px;
    padding: 16px;
    margin: 12px 0;
    border-left: 4px solid #722ed1;

    .formula-title {
      font-weight: 600;
      color: #303133;
      margin-bottom: 8px;
    }

    .formula {
      font-family: 'Cambria Math', 'Georgia', serif;
      font-size: 16px;
      color: #1f2f3d;
      padding: 8px 0;
      letter-spacing: 0.5px;
    }
  }

  .formula-intro {
    margin-top: 0;
  }

  .formula-surface {
    overflow-x: auto;
    margin: 12px 0;
    padding: 14px 16px;
    background: #fff;
    border: 1px solid #e6eef8;
    border-radius: 10px;

    math {
      min-width: 560px;
      font-size: 1.08rem;
      color: #1f2f3d;
    }
  }

  .formula-surface--left {
    display: flex;
    justify-content: flex-start;
  }

  .formula-surface--left math {
    display: block;
    margin: 0;
    text-align: left;
  }

  .formula-symbols {
    display: grid;
    gap: 8px;
    margin: 12px 0;

    p {
      margin: 0;
      padding: 10px 12px;
      background: rgba(255, 255, 255, 0.8);
      border-radius: 8px;
    }
  }

  .strategy-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .strategy-item {
    background: #fafafa;
    border-radius: 8px;
    padding: 16px;
    transition: all 0.3s;

    &:hover {
      background: #f0f5ff;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    }

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
    margin-top: 20px;
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
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
  }

  @media (max-width: 1200px) {
    .impact-overview-grid,
    .change-hint-panel {
      grid-template-columns: 1fr;
    }

    .debug-summary-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .execution-grid {
      grid-template-columns: 1fr;
    }

    .pipeline-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .debug-summary-grid--triple {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .workspace-row,
    .preview-row {
      margin-bottom: 16px;
    }

    .execution-grid {
      grid-template-columns: 1fr;
    }

    .impact-overview-grid,
    .change-hint-panel {
      grid-template-columns: 1fr;
    }

    .debug-summary-grid {
      grid-template-columns: 1fr;
    }

    .pipeline-grid {
      grid-template-columns: 1fr;
    }

    .debug-summary-grid--triple {
      grid-template-columns: 1fr;
    }

    .top-result-card {
      grid-template-columns: 1fr;
      text-align: left;
    }

    .top-result-score {
      text-align: left;
    }
  }
}
</style>
