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
        <GuideDetailHeader :guide="guide" />

        <!-- 攻略内容 -->
        <div class="guide-content card" v-html="guide.content"></div>
      </div>

      <!-- 侧边栏 - 关联景点 -->
      <GuideRelatedSpotSidebar
        :spots="guide.relatedSpots || []"
        @select="$router.push(`/spots/${$event.id}?source=guide`)"
      />
    </div>
  </div>
  <div v-else class="page-container">
    <el-skeleton :rows="15" animated />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import GuideDetailHeader from '@/modules/guide/components/GuideDetailHeader.vue'
import GuideRelatedSpotSidebar from '@/modules/guide/components/GuideRelatedSpotSidebar.vue'
import { getGuideDetail } from '@/modules/guide/api.js'
import { getImageUrl } from '@/shared/api/client.js'
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

.guide-cover {
  width: 100%;
  height: 400px;
  object-fit: cover;
  border-radius: 12px;
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

@media (max-width: 992px) {
  .guide-detail-layout {
    flex-direction: column;
  }
}
</style>

