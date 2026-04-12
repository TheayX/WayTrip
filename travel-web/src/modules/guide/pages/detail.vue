<!-- 攻略详情页 -->
<template>
  <div class="page-container" v-if="guide">
    <div class="guide-detail-layout">
      <div class="guide-main">
        <img :src="getImageUrl(guide.coverImage)" class="guide-cover" alt="" />

        <GuideDetailHeader :guide="guide" />

        <div v-if="hasGuideHtmlContent" class="guide-content premium-card" v-html="guide.content"></div>
        <div v-else class="guide-content guide-content--plain premium-card">{{ resolveGuideText(guide.content) }}</div>
      </div>

      <GuideRelatedSpotSidebar
        :spots="guide.relatedSpots || []"
        @select="$router.push(buildSpotDetailRoute($event.id, SPOT_DETAIL_SOURCE.GUIDE))"
      />
    </div>
  </div>
  <div v-else class="page-container">
    <el-skeleton :rows="15" animated />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import GuideDetailHeader from '@/modules/guide/components/GuideDetailHeader.vue'
import GuideRelatedSpotSidebar from '@/modules/guide/components/GuideRelatedSpotSidebar.vue'
import { getGuideDetail } from '@/modules/guide/api.js'
import { getImageUrl } from '@/shared/api/client.js'
import { ElMessage } from 'element-plus'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'

const GUIDE_DETAIL_UPDATED_KEY = 'guide_detail_updated'
const resolveGuideText = (value) => value || '--'

// 基础依赖与路由状态
const route = useRoute()

// 页面数据状态
const guide = ref(null)
const hasGuideHtmlContent = computed(() => /<[^>]+>/.test(guide.value?.content || ''))

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
  padding-top: 4px;
}

.guide-main {
  flex: 1;
  min-width: 0;
}

.guide-cover {
  width: 100%;
  height: 430px;
  object-fit: cover;
  border-radius: 28px;
}

.guide-content {
  padding: 28px;
  margin-top: 18px;
  line-height: 1.9;
  font-size: 15px;
  color: #334155;

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    color: #0f172a;
    line-height: 1.35;
    margin: 1.4em 0 0.7em;
    letter-spacing: -0.02em;
  }

  :deep(p) {
    margin: 1.05em 0;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 16px;
    margin: 16px 0;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 1.4em;
    margin: 1em 0;
  }

  :deep(blockquote) {
    margin: 1.2em 0;
    padding: 14px 18px;
    border-left: 3px solid rgba(200, 169, 91, 0.5);
    background: #fffdf7;
    color: #475569;
    border-radius: 16px;
  }
}

.guide-content--plain {
  white-space: pre-line;
}

@media (max-width: 992px) {
  .guide-detail-layout {
    flex-direction: column;
  }

  .guide-cover {
    height: 320px;
  }
}
</style>
