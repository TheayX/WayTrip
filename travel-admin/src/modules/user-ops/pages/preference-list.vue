<template>
  <div class="preference-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">鐢ㄦ埛鐢诲儚杩愯惀</p>
        <h1 class="page-title">鐢ㄦ埛鍋忓ソ</h1>
        <p class="page-subtitle">鏌ョ湅鐢ㄦ埛鐢诲儚鏍囩涓庡叴瓒ｅ垎甯冦€?/p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="fetchPreferenceList">鍒锋柊鏁版嵁</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">瑕嗙洊鐢ㄦ埛</div>
        <div class="summary-value">{{ pagination.total }}</div>
        <div class="summary-desc">褰撳墠绛涢€夋潯浠朵笅鐨勭敤鎴风敾鍍忚褰曟暟</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">褰撳墠椤垫爣绛炬暟</div>
        <div class="summary-value">{{ currentPageTagCount }}</div>
        <div class="summary-desc">褰撳墠椤垫墍鏈夌敤鎴峰亸濂芥爣绛剧殑绱鏁伴噺</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">褰撳墠椤甸珮棰戝亸濂?/div>
        <div class="summary-value">{{ topPreferenceTag }}</div>
        <div class="summary-desc">鐢ㄤ簬蹇€熻瀵熷綋鍓嶇瓫閫夌粨鏋滅殑涓诲鍋忓ソ鏂瑰悜</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card">


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
        <el-form-item label="鍋忓ソ鍒嗙被">
          <el-select
            v-model="searchForm.categoryId"
            placeholder="鍏ㄩ儴"
            clearable
            filterable
            style="width: 220px"
            @change="handleSearch"
            @clear="handleSearch"
          >
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">鎼滅储</el-button>
          <el-button @click="handleReset">閲嶇疆</el-button>
        </el-form-item>
      </el-form>

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="鐢ㄦ埛鍋忓ソ鍔犺浇澶辫触" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchPreferenceList">閲嶆柊鍔犺浇</el-button>
          </template>
        </el-result>
      </div>

      <el-table v-else :data="tableData" v-loading="loading" class="ops-table borderless-table">
        <el-table-column prop="userId" label="鐢ㄦ埛ID" width="90" />
        <el-table-column label="鐢ㄦ埛鏄电О" min-width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenUser(row)">{{ row.nickname }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="鎵嬫満鍙? width="150" align="center">
          <template #default="{ row }">{{ formatPhone(row.phone) }}</template>
        </el-table-column>
        <el-table-column label="鍋忓ソ鏍囩" min-width="260">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="tag in row.preferenceTags || []" :key="tag" effect="light" round>{{ tag }}</el-tag>
              <span v-if="!row.preferenceTags?.length" class="empty-text">鏈缃?/span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="鏈€杩戞洿鏂版椂闂? width="170" align="center" />
        <el-table-column prop="createdAt" label="娉ㄥ唽鏃堕棿" width="170" align="center" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchPreferenceList"
          @current-change="fetchPreferenceList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPreferenceList } from '@/modules/user-ops/api/preference.js'
import { getFilters } from '@/modules/spot/api.js'

const router = useRouter()
const route = useRoute()
const skipNextRouteLoad = ref(false)

// 鍒楄〃鐘舵€?
const loading = ref(false)
const tableData = ref([])
const categoryOptions = ref([])
const errorMessage = ref('')

// 鏌ヨ鍙傛暟
const searchForm = reactive({
  nickname: '',
  categoryId: null
})

// 鍒嗛〉鍙傛暟
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 褰撳墠椤电粺璁?
const currentPageTagCount = computed(() => {
  return tableData.value.reduce((sum, item) => sum + (item.preferenceTags?.length || 0), 0)
})

const topPreferenceTag = computed(() => {
  const counter = tableData.value.reduce((acc, item) => {
    ;(item.preferenceTags || []).forEach((tag) => {
      acc[tag] = (acc[tag] || 0) + 1
    })
    return acc
  }, {})
  return Object.entries(counter).sort((a, b) => b[1] - a[1])[0]?.[0] || '鏆傛棤'
})

// 鏍煎紡鍖栨墜鏈哄彿鏄剧ず
const formatPhone = (phone) => {
  if (!phone || !phone.trim()) return '鏈粦瀹?
  const normalized = phone.trim()
  if (/^1\d{10}$/.test(normalized)) {
    return `${normalized.slice(0, 3)}****${normalized.slice(7)}`
  }
  return normalized
}

// 鏋勫缓鍒嗙被閫夐」
const buildCategoryOptions = (nodes = [], level = 0) => {
  return nodes.reduce((acc, node) => {
    acc.push({
      id: node.id,
      label: `${'  '.repeat(level)}${level > 0 ? '鈹?' : ''}${node.name}`
    })
    if (Array.isArray(node.children) && node.children.length) {
      acc.push(...buildCategoryOptions(node.children, level + 1))
    }
    return acc
  }, [])
}

// 鑾峰彇绛涢€夐」
const fetchFilters = async () => {
  const res = await getFilters()
  categoryOptions.value = buildCategoryOptions(res.data.categoryTree || [])
}

// 鑾峰彇鍋忓ソ鍒楄〃
const fetchPreferenceList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getPreferenceList({
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
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
  fetchPreferenceList()
}

// 閲嶇疆鎼滅储鏉′欢
const handleReset = () => {
  searchForm.nickname = ''
  searchForm.categoryId = null
  handleSearch()
}

// 鍚屾璺敱鍙傛暟
const syncRouteQuery = () => {
  const nextQuery = {}
  if (searchForm.nickname) nextQuery.nickname = searchForm.nickname
  if (searchForm.categoryId != null) nextQuery.categoryId = String(searchForm.categoryId)
  const currentQuery = {}
  if (typeof route.query.nickname === 'string' && route.query.nickname) currentQuery.nickname = route.query.nickname
  if (typeof route.query.categoryId === 'string' && route.query.categoryId) currentQuery.categoryId = route.query.categoryId
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
}

// 鍥炲～璺敱鍙傛暟
const applyRouteQuery = () => {
  searchForm.nickname = typeof route.query.nickname === 'string' ? route.query.nickname : ''
  searchForm.categoryId = typeof route.query.categoryId === 'string' ? Number(route.query.categoryId) : null
}

const handleOpenUser = (row) => {
  router.push({ path: '/user', query: { nickname: row.nickname || '' } })
}

// 椤甸潰鍒濆鍖?
onMounted(() => {
  applyRouteQuery()
  fetchFilters()
  fetchPreferenceList()
})

watch(
  () => route.query,
  () => {
    applyRouteQuery()
    if (skipNextRouteLoad.value) {
      skipNextRouteLoad.value = false
      return
    }
    fetchPreferenceList()
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>

.preference-page {

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

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  color: #909399;
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

