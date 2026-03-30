<!-- 攻略详情页 -->
<template>
  <div class="page-container" v-if="guide">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/guides' }">攻略</el-breadcrumb-item>
      <el-breadcrumb-item>{{ guide.title }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="guide-detail-layout">
      <div class="guide-main">
        <!-- 封面图 -->
        <img :src="getImageUrl(guide.coverImage)" class="guide-cover" alt="" />

        <!-- 标题信息 -->
        <div class="guide-header card">
          <h1 class="guide-title">{{ guide.title }}</h1>
          <div class="guide-meta">
            <span class="tag">{{ guide.category }}</span>
            <span class="meta-item">👁 {{ guide.viewCount }}</span>
            <span class="meta-item">{{ guide.createdAt }}</span>
          </div>
        </div>

        <!-- 攻略内容 -->
        <div class="guide-content card" v-html="guide.content"></div>
      </div>

      <!-- 侧边栏 - 关联景点 -->
      <div class="guide-sidebar" v-if="guide.relatedSpots?.length">
        <div class="sidebar-card card">
          <h3 class="sidebar-title">相关景点</h3>
          <div
            v-for="spot in guide.relatedSpots"
            :key="spot.id"
            class="related-spot"
            @click="$router.push(`/spots/${spot.id}?source=guide`)"
          >
            <img :src="getImageUrl(spot.coverImage)" class="related-img" alt="" />
            <div class="related-info">
              <span class="related-name">{{ spot.name }}</span>
              <span class="price">¥{{ spot.price }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="page-container">
    <el-skeleton :rows="15" animated />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getGuideDetail } from '@/api/guide'
import { getImageUrl } from '@/utils/request'
import { ElMessage } from 'element-plus'

const GUIDE_DETAIL_UPDATED_KEY = 'guide_detail_updated'

// 基础依赖与路由状态
const route = useRoute()

// 页面数据状态
const guide = ref(null)

// 数据加载方法
const fetchDetail = async () => {
  try {
    const res = await getGuideDetail(route.params.id)
    guide.value = res.data
    if (guide.value?.id) {
      localStorage.setItem(GUIDE_DETAIL_UPDATED_KEY, JSON.stringify({
        id: guide.value.id,
        title: guide.value.title,
        coverImage: guide.value.coverImage,
        summary: guide.value.summary || '',
        category: guide.value.category,
        viewCount: guide.value.viewCount,
        createdAt: guide.value.createdAt
      }))
    }
  } catch (e) {
    ElMessage.error('获取攻略详情失败')
  }
}

// 生命周期
onMounted(() => {
  fetchDetail()
})
</script>

<style lang="scss" scoped>
.guide-detail-layout {
  display: flex;
  gap: 24px;
  margin-top: 8px;
}

.guide-main {
  flex: 1;
  min-width: 0;
}

.guide-sidebar {
  width: 300px;
  flex-shrink: 0;
}

.guide-cover {
  width: 100%;
  height: 400px;
  object-fit: cover;
  border-radius: 12px;
}

.guide-header {
  padding: 24px;
  margin-top: -40px;
  position: relative;
  z-index: 1;
  border-radius: 12px;
  margin-left: 20px;
  margin-right: 20px;
}

.guide-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.guide-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #909399;
}

.guide-content {
  padding: 24px;
  margin-top: 16px;
  border-radius: 12px;
  line-height: 1.8;
  font-size: 15px;
  color: #303133;

  :deep(img) {
    max-width: 100%;
    border-radius: 8px;
    margin: 12px 0;
  }
}

.sidebar-card {
  padding: 20px;
  border-radius: 12px;
  position: sticky;
  top: 80px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.related-spot {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f5f7fa;
    border-radius: 8px;
    padding-left: 8px;
    padding-right: 8px;
  }
}

.related-img {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.related-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.related-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

@media (max-width: 992px) {
  .guide-detail-layout {
    flex-direction: column;
  }

  .guide-sidebar {
    width: 100%;
  }
}
</style>

