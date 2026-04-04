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

      <el-collapse-item title="核心算法公式" name="formula">
        <div class="help-content">
          <div class="formula-intro">
            本系统基于 <strong>ItemCF（基于物品的协同过滤）</strong> 计算景点相似度，再结合用户历史交互权重生成推荐分数。
          </div>

          <div class="formula-grid">
            <section class="formula-card">
              <div class="formula-card-head">
                <div class="formula-kicker">公式 1</div>
                <div class="formula-title">IUF 加权余弦相似度</div>
              </div>
              <div class="formula-surface">
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
            </section>

            <section class="formula-card">
              <div class="formula-card-head">
                <div class="formula-kicker">公式 2</div>
                <div class="formula-title">预测评分</div>
              </div>
              <div class="formula-surface">
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
            </section>
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

.help-intro {
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border: 1px solid var(--wt-border-default);
  color: var(--wt-text-regular);
  line-height: 1.7;
  font-size: 13px;
}

.help-content {
  padding: 8px 0;
  line-height: 1.8;
  color: var(--wt-text-regular);

  ul {
    padding-left: 20px;

    li {
      margin: 8px 0;
    }
  }

  code {
    padding: 2px 6px;
    background: var(--wt-surface-muted);
    border-radius: 4px;
    font-size: 13px;
    color: var(--el-color-primary);
  }
}

.formula-intro {
  color: var(--wt-text-regular);
  line-height: 1.8;
}

.formula-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 16px;
}

.formula-card {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border: 1px solid var(--wt-border-default);
  box-shadow: inset 0 1px 0 var(--wt-overlay-bg);
}

.formula-card-head {
  display: grid;
  gap: 6px;
}

.formula-kicker {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--wt-text-secondary);
}

.formula-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.formula-surface {
  overflow-x: auto;
  margin-top: 14px;
  padding: 16px 18px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--wt-surface-muted) 0%, var(--wt-surface-elevated) 100%);
  border: 1px solid var(--wt-border-default);

  math {
    min-width: 520px;
    color: var(--wt-text-primary);
    font-size: 1.04rem;
  }
}

.formula-symbols {
  display: grid;
  gap: 10px;
  margin-top: 14px;

  p {
    margin: 0;
    padding: 12px 14px;
    border-radius: 14px;
    background: var(--wt-overlay-bg);
    border: 1px solid var(--wt-border-soft);
    line-height: 1.75;
  }
}

.strategy-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.strategy-item {
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
  border-radius: 18px;
  padding: 18px;
  border: 1px solid var(--wt-border-default);

  .strategy-title {
    font-weight: 600;
    color: var(--wt-text-primary);
    margin-bottom: 8px;
  }

  .strategy-desc {
    font-size: 13px;
    color: var(--wt-text-regular);
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
  color: var(--wt-text-primary);
}

.field-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--wt-tag-primary-bg);
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 20%, var(--wt-border-default));
  color: var(--wt-tag-primary-text);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .formula-grid,
  .strategy-grid {
    grid-template-columns: 1fr;
  }

  .formula-surface {
    padding: 14px 16px;

    math {
      min-width: 480px;
    }
  }
}
</style>
