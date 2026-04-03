<!-- 附近景点页 -->
<template>
  <div class="page-container nearby-page">
    <section class="hero card">
      <div>
        <h2 class="page-title">附近探索</h2>
        <p class="page-subtitle">{{ subtitle }}</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" type="primary" @click="handleLocate">{{ loading ? '定位中' : '重新定位' }}</el-button>
        <el-button text @click="router.push({ path: APP_ROUTE_PATHS.discover, query: { tab: 'spot', scene: 'nearby' } })">返回发现页</el-button>
      </div>
    </section>

    <section v-if="spots.length" class="spot-grid">
      <article v-for="spot in spots" :key="spot.id" class="spot-card card" @click="$router.push(`/spots/${spot.id}?source=nearby`)">
        <img :src="getImageUrl(spot.coverImage)" class="spot-image" alt="" />
        <div class="spot-content">
          <div class="spot-top">
            <h3 class="spot-name">{{ spot.name }}</h3>
            <span class="distance">{{ formatDistance(spot.distanceKm) }}</span>
          </div>
          <div class="spot-meta">
            <span class="tag">{{ spot.regionName || '附近区域' }}</span>
            <span class="tag">{{ spot.categoryName || '景点' }}</span>
          </div>
          <div class="spot-bottom">
            <span class="price">¥{{ spot.price }}</span>
            <span>评分 {{ spot.avgRating || '4.5' }}</span>
          </div>
        </div>
      </article>
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
import { getNearbySpots } from '@/modules/home/api.js'
import { APP_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import { getLocationSnapshot, getCurrentLocation } from '@/shared/lib/location.js'
import { getImageUrl } from '@/shared/api/client.js'

// 基础依赖与用户状态
const router = useRouter()
const userStore = useUserStore()

// 页面数据状态
const loading = ref(false)
const spots = ref([])
const locationStatus = ref('idle')

// 计算属性
const subtitle = computed(() => {
  if (loading.value) return '正在根据当前位置获取周边景点'
  if (locationStatus.value === 'ready' && spots.value.length) {
    return `共找到 ${spots.value.length} 个景点，最近约 ${formatDistance(spots.value[0].distanceKm)}`
  }
  if (locationStatus.value === 'empty') return '当前位置附近暂时没有可展示的景点'
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
  const res = await getNearbySpots(latitude, longitude, 10)
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
}

.hero {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #909399;
}

.spot-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.spot-card {
  cursor: pointer;
}

.spot-image {
  width: 100%;
  height: 240px;
  object-fit: cover;
}

.spot-content {
  padding: 16px;
}

.spot-top,
.spot-meta,
.spot-bottom {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.spot-name {
  font-size: 18px;
  font-weight: 600;
}

.distance {
  color: #409eff;
  font-weight: 600;
}

.spot-meta {
  margin: 12px 0;
}

@media (max-width: 900px) {
  .spot-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
