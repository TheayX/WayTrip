<!-- 附近景点页 -->
<template>
  <div class="page-container nearby-page">
    <section class="hero premium-card">
      <div>
        <p class="hero-eyebrow">Nearby Detail</p>
        <h2 class="page-title">附近探索</h2>
        <p class="page-subtitle">{{ subtitle }}</p>
      </div>
      <div class="hero-actions">
        <el-button text @click="router.push({ path: APP_ROUTE_PATHS.discover, query: { tab: 'spot', scene: 'nearby' } })">返回发现页</el-button>
        <el-button :loading="loading" type="primary" @click="handleLocate">{{ loading ? '定位中' : '重新定位' }}</el-button>
      </div>
    </section>

    <section v-if="spots.length" class="spot-grid">
      <SpotCard
        v-for="spot in spots"
        :key="spot.id"
        :spot="spot"
        @select="$router.push(`/spots/${spot.id}?source=nearby`)"
      />
    </section>

    <el-empty v-else :description="emptyText">
      <el-button type="primary" @click="handleLocate">重新定位</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/modules/account/store/user.js'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import { getNearbySpots } from '@/modules/home/api.js'
import { APP_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import { getLocationSnapshot, getCurrentLocation } from '@/shared/lib/location.js'

// 基础依赖与用户状态
const router = useRouter()
const userStore = useUserStore()

// 页面数据状态
const loading = ref(false)
const spots = ref([])
const locationStatus = ref('idle')

// 计算属性
const subtitle = computed(() => {
  if (loading.value) return '正在根据当前位置获取周边景点。'
  if (locationStatus.value === 'ready' && spots.value.length) {
    return `共找到 ${spots.value.length} 个景点，最近约 ${formatDistance(spots.value[0].distanceKm)}。`
  }
  if (locationStatus.value === 'empty') return '当前位置附近暂时没有可展示的景点。'
  return '这个页面保留为直达入口，更完整的探索流已经合并到发现页。'
})

const emptyText = computed(() => {
  if (!userStore.isLoggedIn) return '登录后查看附近景点'
  if (locationStatus.value === 'empty') return '附近暂时没有景点'
  return '还没有加载附近景点'
})

// 工具方法
const formatDistance = (value) => {
  const distance = Number(value)
  if (!Number.isFinite(distance)) return '-- km'
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))} m` : `${distance.toFixed(1)} km`
}

// 数据加载方法
const fetchNearby = async (latitude, longitude) => {
  const res = await getNearbySpots(latitude, longitude, 9)
  spots.value = res.data?.list || []
  locationStatus.value = spots.value.length ? 'ready' : 'empty'
}

// 交互处理方法
const handleLocate = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后可查看附近景点')
    return
  }

  loading.value = true
  try {
    const location = await getCurrentLocation()
    await fetchNearby(location.latitude, location.longitude)
  } catch (error) {
    locationStatus.value = 'empty'
    ElMessage.error(error?.message === 'BROWSER_GEOLOCATION_UNSUPPORTED' ? '当前浏览器不支持定位' : '定位失败，请检查浏览器权限')
  } finally {
    loading.value = false
  }
}

// 生命周期
onMounted(async () => {
  // 保留独立页直达体验，但不再让它承担 Web 端主探索入口。
  if (!userStore.isLoggedIn) return

  const snapshot = await getLocationSnapshot()
  if (snapshot.current) {
    await fetchNearby(snapshot.current.latitude, snapshot.current.longitude)
  }
})
</script>

<style lang="scss" scoped>
.nearby-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 4px;
}

.hero {
  padding: 26px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  background:
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.12), transparent 28%),
    linear-gradient(135deg, #f7fdfa 0%, #ffffff 60%, #eefbf6 100%);
}

.hero-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 700;
}

.page-title {
  font-size: 34px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.page-subtitle {
  margin-top: 12px;
  color: #64748b;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.spot-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 992px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .spot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
