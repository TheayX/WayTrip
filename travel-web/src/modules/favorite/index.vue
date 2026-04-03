<!-- 我的收藏页 -->
<template>
  <div class="page-container">
    <AccountPageHeader title="我的收藏" subtitle="保留想去的景点，方便后续统一比较和下单。" />

    <div v-loading="loading" class="favorite-grid">
      <div
        v-for="spot in favoriteList"
        :key="spot.id"
        class="fav-card card"
        @click="$router.push(`/spots/${spot.id}?source=favorite`)"
      >
        <div class="fav-img-wrapper">
          <img :src="getImageUrl(spot.coverImage)" class="fav-img" alt="" />
        </div>
        <div class="fav-info">
          <h3 class="fav-name">{{ spot.name }}</h3>
          <p class="fav-region">{{ spot.regionName }} · {{ spot.categoryName }}</p>
          <div class="fav-bottom">
            <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
            <span class="price">¥{{ spot.price }}</span>
          </div>
        </div>
        <el-button
          class="remove-btn"
          type="danger"
          text
          size="small"
          @click.stop="handleRemove(spot.id)"
        >取消收藏</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && favoriteList.length === 0" description="暂无收藏，去发现喜欢的景点吧~">
      <el-button type="primary" @click="$router.push('/spots')">去逛逛</el-button>
    </el-empty>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AccountPageHeader from '@/modules/account/components/AccountPageHeader.vue'
import { getFavoriteList, removeFavorite } from '@/modules/favorite/api.js'
import { getImageUrl } from '@/shared/api/client.js'
import { ElMessage } from 'element-plus'

// 页面数据状态
const favoriteList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 12
const total = ref(0)

// 数据加载方法
const fetchList = async () => {
  loading.value = true
  try {
    const res = await getFavoriteList(page.value, pageSize)
    favoriteList.value = res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    // 收藏列表请求失败时保持当前界面状态，避免重复打断用户操作。
  }
  loading.value = false
}

// 交互处理方法
const handleRemove = async (spotId) => {
  try {
    await removeFavorite(spotId)
    favoriteList.value = favoriteList.value.filter(item => item.id !== spotId)
    total.value--
    ElMessage.success('已取消收藏')
  } catch (e) {
    // 取消收藏失败时由接口层提示，这里不再重复弹窗。
  }
}

// 生命周期
onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.favorite-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 200px;
}

.fav-card {
  cursor: pointer;
  border-radius: 12px;
  position: relative;
}

.fav-img-wrapper {
  aspect-ratio: 16 / 10;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
}

.fav-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;

  .fav-card:hover & {
    transform: scale(1.05);
  }
}

.fav-info {
  padding: 14px;
}

.fav-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.fav-region {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.fav-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remove-btn {
  position: absolute;
  top: 8px;
  right: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 992px) {
  .favorite-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

