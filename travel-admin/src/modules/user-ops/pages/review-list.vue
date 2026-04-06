<!-- 璇勪环绠＄悊椤甸潰 -->
<template>
  <div class="review-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">鐢ㄦ埛璇勪环杩愯惀</p>
        <h1 class="page-title">璇勪环绠＄悊</h1>
        <p class="page-subtitle">鏌ョ湅鐢ㄦ埛璇勪环骞跺鐞嗗紓甯稿唴瀹广€?/p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="fetchReviewList">鍒锋柊鏁版嵁</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">褰撳墠缁撴灉</div>
        <div class="summary-value">{{ pagination.total }}</div>
        <div class="summary-desc">绗﹀悎褰撳墠绛涢€夋潯浠剁殑璇勪环鏁伴噺</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">鏈〉骞冲潎璇勫垎</div>
        <div class="summary-value">{{ currentPageAverageScore }}</div>
        <div class="summary-desc">鐢ㄤ簬蹇€熷垽鏂綋鍓嶉〉璇勪环鏁翠綋璐ㄩ噺</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">浣庡垎璇勪环</div>
        <div class="summary-value">{{ lowScoreCount }}</div>
        <div class="summary-desc">鏈〉璇勫垎灏忎簬绛変簬 2 鍒嗙殑璇勪环鏁伴噺</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card">


      <!-- 鎼滅储琛ㄥ崟 -->
      <el-form :model="searchForm" inline class="search-form" @submit.prevent>

        <el-form-item label="鐢ㄦ埛鏄电О">
          <el-input
            v-model="searchForm.nickname"
            placeholder="璇疯緭鍏ョ敤鎴锋樀绉?
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="鏅偣鍚嶇О">
          <el-input
            v-model="searchForm.spotName"
            placeholder="璇疯緭鍏ユ櫙鐐瑰悕绉?
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">鎼滅储</el-button>
          <el-button @click="handleReset">閲嶇疆</el-button>
        </el-form-item>
      </el-form>

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="璇勪环绠＄悊鍔犺浇澶辫触" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchReviewList">閲嶆柊鍔犺浇</el-button>
          </template>
        </el-result>
      </div>

      <!-- 璇勪环鍒楄〃 -->
      <el-table v-else :data="reviewList" v-loading="loading" class="review-table borderless-table">
        <el-table-column label="鐢ㄦ埛" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="row.avatar" :size="36">{{ row.nickname?.[0] }}</el-avatar>
              <el-button link type="primary" class="nickname-link" @click="handleOpenUser(row)">
                {{ row.nickname }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="鏅偣" min-width="220">
          <template #default="{ row }">
            <div class="spot-cell">
              <el-image
                v-if="row.coverImageUrl"
                :src="row.coverImageUrl"
                fit="cover"
                class="spot-cover"
                preview-disabled
              />
              <el-button link type="primary" @click="handleOpenSpot(row)">
                {{ row.spotName || `鏅偣 #${row.spotId}` }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="璇勫垎" width="100">
          <template #default="{ row }">
            <span class="score-text">鈽?{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="璇勪环鍐呭" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="鍒涘缓鏃堕棿" width="170" align="center" />
        <el-table-column prop="updatedAt" label="鏇存柊鏃堕棿" width="170" align="center" />
        <el-table-column label="鎿嶄綔" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">杩濊鍒犻櫎</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 鍒嗛〉鍣?-->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchReviewList"
          @current-change="fetchReviewList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteReview, getReviewList } from '@/modules/user-ops/api/review.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'

const router = useRouter()

// 鍒楄〃鐘舵€?
const loading = ref(false)
const reviewList = ref([])
const errorMessage = ref('')

// 鏌ヨ鍙傛暟
const searchForm = reactive({
  nickname: '',
  spotName: ''
})

// 鍒嗛〉鍙傛暟
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})
const currentPageAverageScore = computed(() => {
  if (!reviewList.value.length) return '0.0'
  const totalScore = reviewList.value.reduce((sum, item) => sum + Number(item.score || 0), 0)
  return (totalScore / reviewList.value.length).toFixed(1)
})
const lowScoreCount = computed(() => reviewList.value.filter((item) => Number(item.score || 0) <= 2).length)

// 鑾峰彇璇勪环鍒楄〃
const fetchReviewList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getReviewList({
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    reviewList.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (error) {
    reviewList.value = []
    pagination.total = 0
    errorMessage.value = error?.response?.data?.message || error?.message || '璇风◢鍚庨噸璇曟垨妫€鏌ユ帴鍙ｈ繑鍥炪€?
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchReviewList()
}

const handleReset = () => {
  searchForm.nickname = ''
  searchForm.spotName = ''
  handleSearch()
}

const handleOpenUser = (row) => {
  router.push({ path: '/user', query: { nickname: row.nickname || '' } })
}

// 璺宠浆鏅偣椤碉紝骞跺鐢ㄦ櫙鐐圭鐞嗛〉鐨勮嚜鍔ㄥ畾浣嶄笌璇︽儏鎵撳紑鑳藉姏銆?
const handleOpenSpot = (row) => {
  router.push({
    path: '/spot',
    query: {
      keyword: row.spotName || '',
      spotId: row.spotId || ''
    }
  })
}

// 鍒犻櫎璇勪环
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('纭畾瑕佸垹闄よ繖鏉¤瘎浠峰悧锛熷垹闄ゅ悗浼氬悓姝ユ洿鏂版櫙鐐硅瘎鍒嗐€?, '鍒犻櫎纭', {
      type: 'warning'
    })
    await deleteReview(row.id)
    ElMessage.success('鍒犻櫎鎴愬姛')
    fetchReviewList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) {
      ElMessage.error('鍒犻櫎澶辫触')
    }
  }
}

// 椤甸潰鍒濆鍖?
onMounted(() => {
  fetchReviewList()
})
</script>

<style lang="scss" scoped>

.review-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .management-card {
    border-radius: 22px;

    :deep(.el-card__body) {
      padding-top: 4px !important;
    }

    :deep(.search-form) {
      margin-top: 4px !important;
      margin-bottom: 4px !important;
      padding-top: 10px !important;
      padding-bottom: 10px !important;
    }

    :deep(.search-form .el-form-item) {
      margin-bottom: 0 !important;
    }

    :deep(.review-table) {
      margin-top: 0 !important;
    }

    :deep(.search-form + .review-table) {
      margin-top: 0 !important;
    }
  }

  .filter-caption {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 14px;
  }

  .filter-title {
    font-size: 13px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .filter-subtitle {
    font-size: 12px;
    line-height: 1.6;
    color: var(--wt-text-regular);
  }
}

.user-cell,
.spot-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.spot-cover {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
}

.nickname-link {
  font-weight: 600;
}

.score-text {
  color: #f59e0b;
  font-weight: 700;
  font-size: 14px;
}

.review-table {
  border-radius: 18px;
  overflow: hidden;
}

:deep(.review-table .el-button.is-link) {
  padding: 0;
  margin: 0;
  min-width: 0;
  height: auto;
}

:deep(.review-table th.el-table__cell) {
  background: var(--wt-fill-hover);
  color: var(--wt-text-secondary);
  font-weight: 600;
}

:deep(.borderless-table .el-table__inner-wrapper::before) {
  display: none;
}

:deep(.borderless-table td.el-table__cell),
:deep(.borderless-table th.el-table__cell.is-leaf) {
  border-bottom: 1px solid var(--wt-divider-faint);
}

:deep(.review-table .el-table__row:hover > td.el-table__cell) {
  background: var(--wt-row-gradient-hover) !important;
}

</style>

