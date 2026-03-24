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

    <!-- 配置表单 -->
    <el-card shadow="hover" class="config-card">
      <template #header>
        <div class="card-header">
          <div class="title-section">
            <span class="title">算法参数配置</span>
            <el-tag effect="plain" type="info" round>ItemCF — 基于物品的协同过滤</el-tag>
          </div>
          <div class="action-section">
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
              更新矩阵
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="config" label-width="180px" class="config-form">
        <!-- 交互权重 -->
        <div class="form-section">
          <div class="section-title">
            <el-icon><DataLine /></el-icon>
            <span>行为交互权重（公式 r<sub>ui</sub>）</span>
          </div>
          <div class="section-desc">定义不同用户行为对推荐结果的影响力。数值越大，该行为在相似度计算中的贡献越高。</div>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="浏览行为权重">
                <el-input-number v-model="config.weightView" :min="0" :max="10" :step="0.1" :precision="1" />
                <span class="form-tip">浏览基础权重，实际会再乘以来源因子和停留时长因子</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="收藏行为权重">
                <el-input-number v-model="config.weightFavorite" :min="0" :max="10" :step="0.1" :precision="1" />
                <span class="form-tip">用户收藏景点的权重</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="评分因子">
                <el-input-number v-model="config.weightReviewFactor" :min="0" :max="5" :step="0.1" :precision="2" />
                <span class="form-tip">实际权重 = 用户评分 × 该因子</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="已付款订单权重">
                <el-input-number v-model="config.weightOrderPaid" :min="0" :max="10" :step="0.5" :precision="1" />
                <span class="form-tip">用户已付款但未完成的订单</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="已完成订单权重">
                <el-input-number v-model="config.weightOrderCompleted" :min="0" :max="10" :step="0.5" :precision="1" />
                <span class="form-tip">用户已完成的订单，贡献最高</span>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-title">
            <el-icon><DataLine /></el-icon>
            <span>景点热度分数</span>
          </div>
          <div class="section-desc">控制每种行为对 <code>spot.heat_score</code> 的贡献。热度用于热门排序和冷启动，不直接参与协同过滤公式。</div>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="浏览详情加分">
                <el-input-number v-model="config.heatViewIncrement" :min="1" :max="20" :step="1" />
                <span class="form-tip">用户打开景点详情页时增加的热度分数</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="浏览去重窗口">
                <el-input-number v-model="config.heatViewDedupeWindowMinutes" :min="1" :max="1440" :step="1" />
                <span class="form-tip">同一用户同一景点在该时间窗口内重复浏览不重复加热，单位：分钟</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="收藏加分">
                <el-input-number v-model="config.heatFavoriteIncrement" :min="1" :max="20" :step="1" />
                <span class="form-tip">新增收藏或恢复收藏时增加的热度分数</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="评价加分">
                <el-input-number v-model="config.heatReviewIncrement" :min="1" :max="20" :step="1" />
                <span class="form-tip">首次评价或恢复评价时增加的热度分数</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="支付订单加分">
                <el-input-number v-model="config.heatOrderPaidIncrement" :min="1" :max="30" :step="1" />
                <span class="form-tip">订单从待支付变为已支付时增加的热度分数</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="热度重排系数">
                <el-input-number v-model="config.heatRerankFactor" :min="0" :max="1" :step="0.01" :precision="2" />
                <span class="form-tip">最终排序按 CF 分数 + 系数 × 归一化热度 轻量重排，建议保持较小值</span>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 算法参数 -->
        <div class="form-section">
          <div class="section-title">
            <el-icon><Setting /></el-icon>
            <span>算法核心参数</span>
          </div>
          <div class="section-desc">控制协同过滤算法的触发条件和计算精度。</div>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="最少交互数">
                <el-input-number v-model="config.minInteractionsForCF" :min="1" :max="20" :step="1" />
                <span class="form-tip">用户交互少于此数时走冷启动策略</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="近邻数量 K">
                <el-input-number v-model="config.topKNeighbors" :min="5" :max="100" :step="5" />
                <span class="form-tip">相似度矩阵每个景点保留的最近邻数</span>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 缓存参数 -->
        <div class="form-section">
          <div class="section-title">
            <el-icon><Clock /></el-icon>
            <span>缓存策略</span>
          </div>
          <div class="section-desc">控制推荐结果在 Redis 中的缓存时长。</div>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="相似度矩阵 TTL">
                <el-input-number v-model="config.similarityTTLHours" :min="1" :max="168" :step="1" />
                <span class="form-tip">单位：小时，建议 24h</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="用户推荐缓存 TTL">
                <el-input-number v-model="config.userRecTTLMinutes" :min="5" :max="1440" :step="5" />
                <span class="form-tip">单位：分钟，建议 60min</span>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="debug-card">
      <template #header>
        <div class="card-header">
          <div class="title-section">
            <span class="title">推荐调试预览</span>
            <el-tag effect="plain" type="warning" round>仅管理端可见</el-tag>
          </div>
        </div>
      </template>

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
        <el-button type="primary" :loading="previewing" @click="handlePreviewRecommendations">
          调试预览
        </el-button>
      </div>

      <div class="debug-meta" v-if="debugResult">
        <el-tag size="small" type="info">type: {{ debugResult.type }}</el-tag>
        <el-tag size="small" :type="debugResult.needPreference ? 'warning' : 'success'">
          needPreference: {{ debugResult.needPreference ? 'true' : 'false' }}
        </el-tag>
        <el-tag size="small" type="primary">count: {{ debugResult.list?.length || 0 }}</el-tag>
      </div>

      <div v-if="debugResult" class="debug-output">
        <div class="debug-output-title">调试输出</div>
        <pre>{{ debugOutput }}</pre>
      </div>

      <el-table v-if="debugResult?.list?.length" :data="debugResult.list" stripe class="debug-table">
        <el-table-column prop="id" label="景点ID" width="100" />
        <el-table-column prop="name" label="景点名称" min-width="180" />
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column prop="regionName" label="地区" width="140" />
        <el-table-column label="推荐分数" width="160">
          <template #default="{ row }">
            <span v-if="debugForm.debug && row.score != null" class="score-text">{{ Number(row.score).toFixed(4) }}</span>
            <span v-else-if="row.score != null">{{ Number(row.score).toFixed(4) }}</span>
            <span v-else class="score-empty">-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="debugResult ? '本次请求已返回空列表，请结合上方调试输出查看原因' : '暂无调试结果'" />
    </el-card>

    <!-- 使用说明 -->
    <el-card shadow="hover" class="help-card">
      <template #header>
        <div class="card-header">
          <span class="title">使用说明与调参指南</span>
        </div>
      </template>

      <el-collapse v-model="activeCollapse">
        <el-collapse-item title="📐 核心算法公式" name="formula">
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

        <el-collapse-item title="⚖️ 行为权重说明" name="weights">
          <div class="help-content">
            <el-table :data="weightExplanations" stripe style="width: 100%">
              <el-table-column prop="behavior" label="行为类型" width="120" />
              <el-table-column prop="param" label="参数名" width="200" />
              <el-table-column prop="default" label="默认值" width="100" />
              <el-table-column prop="description" label="说明" />
            </el-table>
            <el-alert type="info" :closable="false" style="margin-top: 16px">
              <template #title>
                同一景点上多种行为取最大权重。例如用户先浏览(0.5)再收藏(1.0)，最终权重为 1.0。
              </template>
            </el-alert>
          </div>
        </el-collapse-item>

        <el-collapse-item title="🎯 调参策略建议" name="strategy">
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

        <el-collapse-item title="🔄 矩阵更新机制" name="matrix">
          <div class="help-content">
            <ul>
              <li><strong>自动更新：</strong>每天凌晨 3:00 定时任务自动重新计算相似度矩阵。</li>
              <li><strong>手动更新：</strong>点击上方「更新矩阵」按钮可立即触发重算（请求会阻塞到计算完成）。</li>
              <li><strong>修改参数后：</strong>保存的参数配置会立即对新的推荐请求生效。但相似度矩阵需要手动触发或等待下次定时任务才会用新参数重算。</li>
              <li><strong>并发保护：</strong>同一时间只允许一次矩阵计算，重复触发会被跳过。</li>
            </ul>
          </div>
        </el-collapse-item>

        <el-collapse-item title="📊 默认配置参考" name="defaults">
          <div class="help-content">
            <el-descriptions border :column="2">
              <el-descriptions-item label="浏览权重">0.5</el-descriptions-item>
              <el-descriptions-item label="收藏权重">1.0</el-descriptions-item>
              <el-descriptions-item label="评分因子">0.4</el-descriptions-item>
              <el-descriptions-item label="已付款权重">3.0</el-descriptions-item>
              <el-descriptions-item label="已完成权重">4.0</el-descriptions-item>
              <el-descriptions-item label="浏览热度加分">1</el-descriptions-item>
              <el-descriptions-item label="收藏热度加分">3</el-descriptions-item>
              <el-descriptions-item label="评价热度加分">2</el-descriptions-item>
              <el-descriptions-item label="支付热度加分">5</el-descriptions-item>
              <el-descriptions-item label="浏览去重窗口">30 分钟</el-descriptions-item>
              <el-descriptions-item label="热度重排系数">0.05</el-descriptions-item>
              <el-descriptions-item label="最少交互数">3</el-descriptions-item>
              <el-descriptions-item label="近邻数量 K">20</el-descriptions-item>
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
  previewRecommendations
} from '@/api/recommendation'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Cpu, Timer, User, Location, RefreshLeft, Check, Refresh,
  DataLine, Setting, Clock
} from '@element-plus/icons-vue'

const config = reactive({
  weightView: 0.5,
  weightFavorite: 1.0,
  weightReviewFactor: 0.4,
  weightOrderPaid: 3.0,
  weightOrderCompleted: 4.0,
  heatViewIncrement: 1,
  heatFavoriteIncrement: 3,
  heatReviewIncrement: 2,
  heatOrderPaidIncrement: 5,
  heatViewDedupeWindowMinutes: 30,
  heatRerankFactor: 0.05,
  minInteractionsForCF: 3,
  topKNeighbors: 20,
  similarityTTLHours: 24,
  userRecTTLMinutes: 60
})

const status = reactive({
  lastUpdateTime: null,
  totalUsers: null,
  totalSpots: null,
  computing: false
})

const saving = ref(false)
const updatingMatrix = ref(false)
const previewing = ref(false)
const activeCollapse = ref([])
const debugResult = ref(null)
const debugForm = reactive({
  userId: 1,
  limit: 6,
  refresh: false,
  debug: true
})
const debugOutput = computed(() => {
  if (!debugResult.value) return ''

  const lines = [
    `request.userId = ${debugForm.userId}`,
    `request.limit = ${debugForm.limit}`,
    `request.refresh = ${debugForm.refresh}`,
    `request.debug = ${debugForm.debug}`,
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

const defaultConfig = {
  weightView: 0.5,
  weightFavorite: 1.0,
  weightReviewFactor: 0.4,
  weightOrderPaid: 3.0,
  weightOrderCompleted: 4.0,
  heatViewIncrement: 1,
  heatFavoriteIncrement: 3,
  heatReviewIncrement: 2,
  heatOrderPaidIncrement: 5,
  heatViewDedupeWindowMinutes: 30,
  heatRerankFactor: 0.05,
  minInteractionsForCF: 3,
  topKNeighbors: 20,
  similarityTTLHours: 24,
  userRecTTLMinutes: 60
}

const weightExplanations = [
  { behavior: '浏览', param: 'weightView', default: '0.5', description: '浏览基础权重；实际按 来源因子 × 停留时长因子 细化，例如搜索进入、停留更久会更高' },
  { behavior: '收藏', param: 'weightFavorite', default: '1.0', description: '用户主动收藏景点，表示明确兴趣' },
  { behavior: '评分', param: 'weightReviewFactor', default: '0.4', description: '实际权重 = 评分(1~5) × 因子，如5分评价 = 5×0.4 = 2.0' },
  { behavior: '已付款', param: 'weightOrderPaid', default: '3.0', description: '用户已下单付款但未完成行程，表示强烈意向' },
  { behavior: '已完成', param: 'weightOrderCompleted', default: '4.0', description: '用户已完成行程，最强信号' }
]

// 获取配置
const fetchConfig = async () => {
  try {
    const res = await getRecommendationConfig()
    if (res.data) {
      Object.assign(config, res.data)
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
    await updateRecommendationConfig({ ...config })
    ElMessage.success('算法配置已保存！新的推荐请求将使用更新后的参数。')
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
    Object.assign(config, { ...defaultConfig })
    await updateRecommendationConfig({ ...defaultConfig })
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

  .debug-meta {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
  }

  .debug-output {
    margin-bottom: 16px;
    padding: 14px 16px;
    background: #fafbfc;
    border: 1px solid #e5eaf3;
    border-radius: 10px;
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
}
</style>
