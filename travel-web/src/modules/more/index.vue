<!-- 更多功能页 -->
<template>
  <div class="page-container more-page">
    <section class="hero-card premium-card">
      <div>
        <p class="hero-eyebrow">Feature Collection</p>
        <h1>玩法合集</h1>
        <p>这里不再重复承载景点、攻略、推荐和附近等主路径，只保留更适合做专题浏览的特色玩法入口。</p>
      </div>
      <div class="hero-actions">
        <el-button text type="primary" @click="router.push(APP_ROUTE_PATHS.discover)">返回发现页</el-button>
      </div>
    </section>

    <section class="group-card premium-card">
      <div class="section-head">
        <div>
          <p class="hero-eyebrow">Featured Scenes</p>
          <h2>特色功能</h2>
        </div>
      </div>

      <div class="grid-list">
        <article
          v-for="item in featureEntries"
          :key="item.id"
          class="grid-item"
          @click="handleEntryClick(item)"
        >
          <div class="grid-icon" :class="item.theme">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
          <span class="grid-link">进入玩法</span>
        </article>
      </div>
    </section>

    <section class="group-card premium-card">
      <div class="section-head">
        <div>
          <p class="hero-eyebrow">Coming Soon</p>
          <h2>后续扩展</h2>
        </div>
      </div>

      <div class="grid-list">
        <article
          v-for="item in upcomingEntries"
          :key="item.id"
          class="grid-item disabled"
          @click="handleEntryClick(item)"
        >
          <div class="grid-icon" :class="item.theme">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
          <span class="grid-badge">敬请期待</span>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  Connection,
  Discount,
  Flag,
  Grid,
  MagicStick,
  Opportunity,
  Stopwatch
} from '@element-plus/icons-vue'
import { APP_ROUTE_PATHS } from '@/shared/constants/route-paths.js'

// 更多页只承担专题入口聚合，不重复承载首页和发现页已经存在的主路径内容。
const router = useRouter()

const featureEntries = [
  { id: 'random-pick', title: '随心一选', desc: '随机抽一个目的地，适合不想做决定的时候。', icon: MagicStick, theme: 'purple', path: APP_ROUTE_PATHS.randomPick },
  { id: 'budget', title: '穷游玩法', desc: '按更低预算筛景点和攻略，适合轻预算出行。', icon: Discount, theme: 'orange', path: APP_ROUTE_PATHS.budgetTravel },
  { id: 'reviews', title: '游客口碑', desc: '优先看用户评价，再决定要不要深入浏览。', icon: ChatDotRound, theme: 'blue', path: APP_ROUTE_PATHS.travelerReviews },
  { id: 'trending', title: '近期热看', desc: '看看最近浏览更高的景点，快速补热门灵感。', icon: Stopwatch, theme: 'amber', path: APP_ROUTE_PATHS.trendingViews }
]

const upcomingEntries = [
  { id: 'city-topic', title: '城市专题', desc: '按城市整理主题玩法与精选目的地。', icon: Flag, theme: 'blue', available: false },
  { id: 'holiday-plan', title: '假日玩法', desc: '节日和假期的阶段性出游专题入口。', icon: Opportunity, theme: 'orange', available: false },
  { id: 'route-list', title: '路线清单', desc: '半日游和一日游路线组合，适合快速决策。', icon: Connection, theme: 'amber', available: false },
  { id: 'activity-zone', title: '活动专区', desc: '后续活动与限时专题统一在这里聚合。', icon: Grid, theme: 'purple', available: false }
]

const handleEntryClick = (item) => {
  // 未开放入口统一给出轻提示，避免用户进入空白页或占位路由。
  if (item.available === false || !item.path) {
    ElMessage.info('功能开发中')
    return
  }
  router.push(item.path)
}
</script>

<style lang="scss" scoped>
.more-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 8px;
  padding-bottom: 32px;
}

.hero-card,
.group-card {
  padding: 26px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-end;
  background:
    radial-gradient(circle at top right, rgba(124, 58, 237, 0.14), transparent 30%),
    linear-gradient(135deg, #f8fbff 0%, #ffffff 58%, #f5f3ff 100%);
}

.hero-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 700;
}

.hero-card h1,
.group-card h2 {
  color: #0f172a;
  letter-spacing: -0.03em;
}

.hero-card h1 {
  font-size: 36px;
}

.hero-card p {
  margin-top: 12px;
  color: #64748b;
  line-height: 1.85;
  max-width: 760px;
}

.section-head {
  margin-bottom: 18px;
}

.grid-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.grid-item {
  position: relative;
  padding: 22px;
  border-radius: 22px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.grid-item:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
  box-shadow: 0 18px 28px -22px rgba(15, 23, 42, 0.32);
}

.grid-item.disabled {
  opacity: 0.9;
}

.grid-item.disabled:hover {
  transform: none;
  border-color: #e2e8f0;
  box-shadow: none;
}

.grid-icon {
  width: 64px;
  height: 64px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.grid-icon.blue {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #2563eb;
}

.grid-icon.orange {
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
  color: #ea580c;
}

.grid-icon.amber {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  color: #d97706;
}

.grid-icon.purple {
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  color: #7c3aed;
}

.grid-item h3 {
  margin-top: 16px;
  margin-bottom: 10px;
  color: #0f172a;
  font-size: 20px;
}

.grid-item p {
  color: #64748b;
  line-height: 1.8;
  font-size: 14px;
}

.grid-link,
.grid-badge {
  display: inline-flex;
  align-items: center;
  margin-top: 16px;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.grid-link {
  background: #eff6ff;
  color: #1d4ed8;
}

.grid-badge {
  background: rgba(15, 23, 42, 0.06);
  color: #64748b;
}

@media (max-width: 768px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .grid-list {
    grid-template-columns: 1fr;
  }
}
</style>
