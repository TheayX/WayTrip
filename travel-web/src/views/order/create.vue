<template>
  <div class="page-container" v-if="spot">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: `/spots/${spot.id}` }">{{ spot.name }}</el-breadcrumb-item>
      <el-breadcrumb-item>创建订单</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="order-create-layout">
      <div class="order-main">
        <!-- 景点信息 -->
        <div class="spot-card card">
          <img :src="getImageUrl(spot.coverImage)" class="spot-thumb" alt="" />
          <div class="spot-info">
            <h2 class="spot-name">{{ spot.name }}</h2>
            <span class="spot-price">¥{{ spot.price }}/人</span>
          </div>
        </div>

        <!-- 订单表单 -->
        <div class="form-card card">
          <h3 class="card-title">订单信息</h3>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" size="large">
            <el-form-item label="游玩日期" prop="visitDate">
              <el-date-picker
                v-model="form.visitDate"
                type="date"
                placeholder="请选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="disableDate"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="购票数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :max="99" />
            </el-form-item>
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人姓名" maxlength="50" />
            </el-form-item>
            <el-form-item label="手机号" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 右侧价格明细 -->
      <div class="order-sidebar">
        <div class="price-card card">
          <h3 class="card-title">价格明细</h3>
          <div class="price-row">
            <span>门票单价</span>
            <span>¥{{ spot.price }}</span>
          </div>
          <div class="price-row">
            <span>购买数量</span>
            <span>x{{ form.quantity }}</span>
          </div>
          <el-divider />
          <div class="price-row total">
            <span>合计</span>
            <span class="total-price">¥{{ totalPrice }}</span>
          </div>
          <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleSubmit">
            提交订单
          </el-button>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="page-container">
    <el-skeleton :rows="8" animated />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSpotDetail } from '@/api/spot'
import { createOrder } from '@/api/order'
import { getImageUrl } from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const spot = ref(null)
const submitting = ref(false)

const form = reactive({
  visitDate: '',
  quantity: 1,
  contactName: '',
  contactPhone: ''
})

const rules = {
  visitDate: [{ required: true, message: '请选择游玩日期', trigger: 'change' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const totalPrice = computed(() =>
  spot.value ? (spot.value.price * form.quantity).toFixed(2) : '0.00'
)

const disableDate = (date) => {
  return date.getTime() < Date.now() - 86400000
}

const fetchSpot = async () => {
  try {
    const res = await getSpotDetail(route.params.spotId)
    spot.value = res.data
  } catch (e) {
    ElMessage.error('获取景点信息失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await createOrder({
      spotId: spot.value.id,
      visitDate: form.visitDate,
      quantity: form.quantity,
      contactName: form.contactName,
      contactPhone: form.contactPhone
    })
    ElMessage.success('订单创建成功')
    router.push(`/orders/${res.data?.id || res.data}`)
  } catch (e) { /* ignore */ }
  submitting.value = false
}

onMounted(() => {
  fetchSpot()
})
</script>

<style lang="scss" scoped>
.order-create-layout {
  display: flex;
  gap: 24px;
  margin-top: 8px;
}

.order-main {
  flex: 1;
}

.order-sidebar {
  width: 340px;
  flex-shrink: 0;
}

.spot-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.spot-thumb {
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.spot-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.spot-name {
  font-size: 18px;
  font-weight: 600;
}

.spot-price {
  font-size: 16px;
  color: #f56c6c;
  font-weight: 600;
}

.form-card, .price-card {
  padding: 24px;
  border-radius: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 20px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  color: #606266;

  &.total {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}

.total-price {
  font-size: 24px;
  color: #f56c6c;
  font-weight: 700;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
  margin-top: 16px;
}

@media (max-width: 992px) {
  .order-create-layout {
    flex-direction: column;
  }

  .order-sidebar {
    width: 100%;
  }
}
</style>

