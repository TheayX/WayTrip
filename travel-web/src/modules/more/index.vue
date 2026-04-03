<!-- 更多功能页 -->
<template>
  <div class="page-container more-page">
    <section class="hero-card">
      <h1>更多功能</h1>
      <p>首页保留高频入口，常用能力和特色玩法统一收在这里。</p>
    </section>

    <section v-for="group in entryGroups" :key="group.title" class="group-card card">
      <h2>{{ group.title }}</h2>
      <div class="grid-list">
        <article
          v-for="item in group.items"
          :key="item.id"
          class="grid-item"
          :class="{ disabled: item.available === false }"
          @click="handleEntryClick(item)"
        >
          <div class="grid-icon" :class="item.theme">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
          <span v-if="item.available === false" class="grid-badge">敬请期待</span>
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
  Guide,
  LocationFilled,
  MagicStick,
  MapLocation,
  Opportunity,
  Promotion,
  Stopwatch
} from '@element-plus/icons-vue'
import { APP_ROUTE_PATHS } from '@/shared/constants/route-paths.js'

const router = useRouter()

const entryGroups = [
  {
    title: '常用浏览',
    items: [
      { id: 'spots', title: '景点大全', desc: '按区域和分类浏览', icon: LocationFilled, theme: 'blue', path: `${APP_ROUTE_PATHS.spots}?sortBy=heat` },
      { id: 'guides', title: '出游攻略', desc: '看路线和游玩经验', icon: Guide, theme: 'orange', path: APP_ROUTE_PATHS.guides },
      { id: 'recommend', title: '个性推荐', desc: '查看个性化推荐', icon: Promotion, theme: 'amber', path: APP_ROUTE_PATHS.recommendations },
      { id: 'nearby', title: '附近探索', desc: '定位查看周边景点', icon: MapLocation, theme: 'emerald', path: APP_ROUTE_PATHS.nearby }
    ]
  },
  {
    title: '特色功能',
    items: [
      { id: 'random-pick', title: '随心一选', desc: '随机抽一个目的地', icon: MagicStick, theme: 'purple', path: APP_ROUTE_PATHS.randomPick },
      { id: 'budget', title: '穷游玩法', desc: '低预算景点和攻略', icon: Discount, theme: 'orange', path: APP_ROUTE_PATHS.budgetTravel },
      { id: 'reviews', title: '游客口碑', desc: '看游客真实评价', icon: ChatDotRound, theme: 'blue', path: APP_ROUTE_PATHS.travelerReviews },
      { id: 'trending', title: '近期热看', desc: '看看最近浏览更高的景点', icon: Stopwatch, theme: 'amber', path: APP_ROUTE_PATHS.trendingViews }
    ]
  },
  {
    title: '即将上线',
    items: [
      { id: 'city-topic', title: '城市专题', desc: '按城市整理主题玩法', icon: Flag, theme: 'blue', available: false },
      { id: 'holiday-plan', title: '假日玩法', desc: '节日和假期的出游专题', icon: Opportunity, theme: 'orange', available: false },
      { id: 'route-list', title: '路线清单', desc: '半日游和一日游路线组合', icon: Connection, theme: 'amber', available: false },
      { id: 'activity-zone', title: '活动专区', desc: '后续活动和限时专题入口', icon: Grid, theme: 'purple', available: false }
    ]
  }
]

const handleEntryClick = (item) => {
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
  padding: 24px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.hero-card {
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 55%, #f5f3ff 100%);
}

.hero-card h1,
.group-card h2 {
  color: #111827;
}

.hero-card p {
  margin-top: 12px;
  color: #64748b;
  line-height: 1.8;
}

.grid-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 20px;
}

.grid-item {
  position: relative;
  padding: 22px;
  border-radius: 20px;
  background: #f8fafc;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.grid-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.grid-item.disabled {
  opacity: 0.86;
}

.grid-item.disabled:hover {
  transform: none;
}

.grid-icon {
  width: 64px;
  height: 64px;
  border-radius: 20px;
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

.grid-icon.emerald {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  color: #059669;
}

.grid-icon.purple {
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  color: #7c3aed;
}

.grid-item h3 {
  margin-top: 16px;
  margin-bottom: 10px;
  color: #1f2937;
  font-size: 18px;
}

.grid-item p {
  color: #64748b;
  line-height: 1.7;
}

.grid-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 768px) {
  .grid-list {
    grid-template-columns: 1fr;
  }
}
</style>
