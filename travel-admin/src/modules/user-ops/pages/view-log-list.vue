<template>
  <div class="view-log-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">娴忚琛屼负杩愯惀</p>
        <h1 class="page-title">娴忚琛屼负</h1>
        <p class="page-subtitle">鏌ョ湅娴忚鏉ユ簮銆佸仠鐣欐椂闀夸笌鍏ュ彛琛ㄧ幇銆?/p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="fetchViewList">鍒锋柊鏁版嵁</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">绛涢€夌粨鏋?/div>
        <div class="summary-value">{{ pagination.total }}</div>
        <div class="summary-desc">褰撳墠鏉′欢涓嬬殑娴忚璁板綍鎬绘暟</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">褰撳墠椤靛钩鍧囧仠鐣?/div>
        <div class="summary-value">{{ averageDuration }} 绉?/div>
        <div class="summary-desc">鐢ㄤ簬蹇€熷垽鏂綋鍓嶇瓫閫夌粨鏋滅殑娴忚娣卞害</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">褰撳墠椤典富瑕佹潵婧?/div>
        <div class="summary-value">{{ topSourceLabel }}</div>
        <div class="summary-desc">甯姪鍒ゆ柇鎺ㄨ崘銆佹悳绱€佹敾鐣ョ瓑鍏ュ彛鐨勮础鐚?/div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card">


      <el-alert
        class="source-alert"
        type="info"
        :closable="false"
        show-icon
        title="璇存槑锛氳繖閲屽睍绀虹殑鏄暟鎹簱鍘熷鏉ユ簮鍊硷紱鎺ㄨ崘绠楁硶璁＄畻鏃朵細鍐嶅綊骞跺埌棣栭〉銆佹悳绱€佹敾鐣ャ€佹帹鑽愩€侀粯璁よ繖鍑犱釜鏉ユ簮鎸′綅銆?
      />

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
        <el-form-item label="鏉ユ簮">
          <el-select
            v-model="searchForm.source"
            placeholder="鍏ㄩ儴鏉ユ簮"
            clearable
            style="width: 140px"
            @change="handleSearch"
            @clear="handleSearch"
          >
            <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="娴忚鏃堕棿">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="鑷?
            start-placeholder="寮€濮嬫棩鏈?
            end-placeholder="缁撴潫鏃ユ湡"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">鎼滅储</el-button>
          <el-button @click="handleReset">閲嶇疆</el-button>
        </el-form-item>
      </el-form>

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="娴忚琛屼负鍔犺浇澶辫触" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchViewList">閲嶆柊鍔犺浇</el-button>
          </template>
        </el-result>
      </div>

      <el-table v-else :data="tableData" v-loading="loading" class="ops-table borderless-table">
        <el-table-column prop="id" label="璁板綍ID" width="90" />
        <el-table-column label="鐢ㄦ埛鏄电О" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenUser(row)">{{ row.nickname }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="鏅偣" min-width="240">
          <template #default="{ row }">
            <div class="spot-cell">
              <el-image v-if="row.coverImage" :src="getResourceUrl(row.coverImage)" fit="cover" class="spot-cover" />
              <el-button link type="primary" @click="handleOpenSpot(row)">{{ row.spotName }}</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="鏉ユ簮" width="120" align="center">
          <template #default="{ row }">
            <div class="source-cell">
              <el-tag effect="light">{{ getSourceLabel(row.source) }}</el-tag>
              <div class="source-bucket-text">{{ getSourceBucketLabel(row.source) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="鍋滅暀鏃堕暱" width="120" align="center">
          <template #default="{ row }">{{ row.duration || 0 }} 绉?/template>
        </el-table-column>
        <el-table-column prop="createdAt" label="娴忚鏃堕棿" width="170" align="center" />
        <el-table-column label="鎿嶄綔" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">鍒犻櫎</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchViewList"
          @current-change="fetchViewList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteView, getViewList } from '@/modules/user-ops/api/view-log.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'
import { getResourceUrl } from '@/shared/lib/resource.js'
import { getSourceBucketLabel, getSourceLabel, sourceOptions } from '@/shared/constants/view-source.js'

const router = useRouter()
const route = useRoute()
const skipNextRouteLoad = ref(false)

// 鍒楄〃鐘舵€?
const loading = ref(false)
const tableData = ref([])
const dateRange = ref([])
const errorMessage = ref('')

// 鏌ヨ鍙傛暟
const searchForm = reactive({
  nickname: '',
  spotName: '',
  source: ''
})

// 鍒嗛〉鍙傛暟
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 褰撳墠椤靛钩鍧囧仠鐣欐椂闀?
const averageDuration = computed(() => {
  if (!tableData.value.length) return 0
  const totalDuration = tableData.value.reduce((sum, item) => sum + (item.duration || 0), 0)
  return Math.round(totalDuration / tableData.value.length)
})

// 褰撳墠椤典富瑕佹潵婧?
const topSourceLabel = computed(() => {
  if (!tableData.value.length) return '鏆傛棤'
  const sourceCounter = tableData.value.reduce((acc, item) => {
    const key = item.source || 'unknown'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})
  const topSource = Object.entries(sourceCounter).sort((a, b) => b[1] - a[1])[0]?.[0]
  return getSourceLabel(topSource)
})

// 鑾峰彇娴忚鍒楄〃
const fetchViewList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = {
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getViewList(params)
    tableData.value = res.data.list || []
    pagination.total = res.data.total || 0
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    errorMessage.value = error?.response?.data?.message || error?.message || '璇风◢鍚庨噸璇曟垨妫€鏌ユ帴鍙ｈ繑鍥炪€?
  } finally {
    loading.value = false
  }
}

// 鎼滅储鎿嶄綔
const handleSearch = () => {
  pagination.page = 1
  syncRouteQuery()
  fetchViewList()
}

// 閲嶇疆鎼滅储鏉′欢
const handleReset = () => {
  searchForm.nickname = ''
  searchForm.spotName = ''
  searchForm.source = ''
  dateRange.value = []
  handleSearch()
}

// 鍚屾璺敱鍙傛暟
const syncRouteQuery = () => {
  const nextQuery = {}
  if (searchForm.nickname) nextQuery.nickname = searchForm.nickname
  if (searchForm.spotName) nextQuery.spotName = searchForm.spotName
  if (searchForm.source) nextQuery.source = searchForm.source
  if (dateRange.value?.length === 2) {
    nextQuery.startDate = dateRange.value[0]
    nextQuery.endDate = dateRange.value[1]
  }
  const currentQuery = {}
  if (typeof route.query.nickname === 'string' && route.query.nickname) currentQuery.nickname = route.query.nickname
  if (typeof route.query.spotName === 'string' && route.query.spotName) currentQuery.spotName = route.query.spotName
  if (typeof route.query.source === 'string' && route.query.source) currentQuery.source = route.query.source
  if (typeof route.query.startDate === 'string' && route.query.startDate) currentQuery.startDate = route.query.startDate
  if (typeof route.query.endDate === 'string' && route.query.endDate) currentQuery.endDate = route.query.endDate
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
}

// 鍥炲～璺敱鍙傛暟
const applyRouteQuery = () => {
  searchForm.nickname = typeof route.query.nickname === 'string' ? route.query.nickname : ''
  searchForm.spotName = typeof route.query.spotName === 'string' ? route.query.spotName : ''
  searchForm.source = typeof route.query.source === 'string' ? route.query.source : ''
  if (typeof route.query.startDate === 'string' && typeof route.query.endDate === 'string') {
    dateRange.value = [route.query.startDate, route.query.endDate]
  } else {
    dateRange.value = []
  }
}

// 璺宠浆鐢ㄦ埛椤?
const handleOpenUser = (row) => {
  router.push({ path: '/user', query: { nickname: row.nickname || '' } })
}

// 璺宠浆鏅偣椤?
const handleOpenSpot = (row) => {
  router.push({
    path: '/spot',
    query: {
      keyword: row.spotName || '',
      spotId: row.spotId || ''
    }
  })
}

// 鍒犻櫎娴忚璁板綍
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('纭畾瑕佸垹闄よ繖鏉℃祻瑙堣褰曞悧锛?, '鍒犻櫎纭', { type: 'warning' })
    await deleteView(row.id)
    ElMessage.success('鍒犻櫎鎴愬姛')
    fetchViewList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) {
      ElMessage.error('鍒犻櫎澶辫触')
    }
  }
}

// 椤甸潰鍒濆鍖?
onMounted(() => {
  applyRouteQuery()
  fetchViewList()
})

watch(
  () => route.query,
  () => {
    applyRouteQuery()
    if (skipNextRouteLoad.value) {
      skipNextRouteLoad.value = false
      return
    }
    fetchViewList()
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>

.view-log-page {

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

    :deep(.ops-table) {
      margin-top: 0 !important;
    }

    :deep(.search-form + .ops-table) {
      margin-top: 0 !important;
    }
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

.source-alert {
  margin-bottom: 12px;
  border-radius: 14px;
}

.source-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.source-bucket-text {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

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
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.ops-table {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.ops-table .el-button.is-link) {
  padding: 0;
  margin: 0;
  min-width: 0;
  height: auto;
}

:deep(.ops-table th.el-table__cell) {
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

:deep(.ops-table .el-table__row:hover > td.el-table__cell) {
  background: var(--wt-row-gradient-hover) !important;
}

</style>

