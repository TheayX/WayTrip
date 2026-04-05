<template>
  <!-- 新增/编辑景点抽屉表单 -->
  <el-drawer
      :model-value="visible"
      :title="editId ? '编辑景点' : '新增景点'"
      size="760px"
      @update:model-value="emitVisible"
      class="spot-form-drawer"
      destroy-on-close
  >
    <div class="drawer-content-wrapper flex flex-col h-full">
      <!-- 顶部步骤条 -->
      <div class="steps-container px-6 py-4 bg-gray-50 border-b border-gray-100">
        <el-steps :active="activeStep" finish-status="success" simple>
          <el-step v-for="(step, index) in stepOptions" :key="step.title" :icon="step.icon">
            <template #title>
              <button type="button" class="step-title-btn" @click="goToStep(index)">
                {{ step.title }}
              </button>
            </template>
          </el-step>
        </el-steps>
      </div>

      <!-- 表单区域 -->
      <div class="form-container flex-1 overflow-y-auto p-6">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top" class="custom-form">
          <!-- Step 0: 基础信息 -->
          <div v-show="activeStep === 0" class="step-panel animate-fade-in">
            <h3 class="section-title">基础录入</h3>

            <el-row :gutter="24">
              <el-col :span="24">
                <el-form-item label="景点名称" prop="name">
                  <el-input v-model="form.name" placeholder="请输入景点名称" size="large" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="归属地区" prop="regionPath">
                  <el-cascader
                      v-model="form.regionPath"
                      :options="regionCascaderOptions"
                      :props="regionCascaderProps"
                      clearable
                      placeholder="请选择地区"
                      class="w-full"
                      size="large"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="价格 (¥)" prop="price">
                  <el-input-number v-model="form.price" :min="0" :precision="2" class="w-full" size="large" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="父分类" prop="parentCategoryId">
                  <el-select v-model="form.parentCategoryId" placeholder="请选择父分类" class="w-full" size="large" @change="emit('parent-category-change')">
                    <el-option v-for="item in parentCategoryOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="子分类" prop="categoryId">
                  <el-select v-model="form.categoryId" placeholder="请选择子分类" :disabled="!form.parentCategoryId" class="w-full" size="large">
                    <el-option v-for="item in childCategoryOptions" :key="item.id" :label="item.name" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="24">
                <el-form-item label="一句话简介">
                  <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入诱人的景点简介..." />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- Step 1: 位置与营业 -->
          <div v-show="activeStep === 1" class="step-panel animate-fade-in">
            <h3 class="section-title">定位与状态</h3>

            <el-row :gutter="24">
              <el-col :span="24">
                <el-form-item label="详细地址" prop="address">
                  <el-input v-model="form.address" placeholder="请输入详细地址，便于导航" size="large">
                    <template #prefix><el-icon><Location /></el-icon></template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="坐标：经度 (Longitude)">
                  <el-input-number v-model="form.longitude" placeholder="获取经度" class="w-full" size="large" :precision="6" :step="0.000001" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="坐标：纬度 (Latitude)">
                  <el-input-number v-model="form.latitude" placeholder="获取纬度" class="w-full" size="large" :precision="6" :step="0.000001" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="开放时间">
                  <el-input v-model="form.openTime" placeholder="例如：全年 08:30-17:00" size="large">
                    <template #prefix><el-icon><Clock /></el-icon></template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="初始热度档位" prop="heatLevel">
                  <el-select v-model="form.heatLevel" placeholder="请选择热度档位" class="w-full" size="large">
                    <el-option v-for="item in heatLevelOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- Step 2: 图文素材 -->
          <div v-show="activeStep === 2" class="step-panel animate-fade-in">
            <h3 class="section-title">画廊与展示图</h3>

            <el-row :gutter="24">
              <el-col :span="24">
                <el-form-item label="首页封面图 (Cover Image)">
                  <div class="upload-container">
                    <el-upload
                        class="image-uploader"
                        :action="uploadUrl"
                        :headers="uploadHeaders"
                        :data="coverUploadData"
                        :show-file-list="false"
                        :on-success="handleUploadSuccess"
                        :on-error="handleUploadError"
                        :before-upload="beforeUpload"
                        accept="image/*"
                    >
                      <template #trigger>
                        <div v-if="form.coverImage" class="uploaded-mask-wrapper">
                          <el-image :src="getImageUrl(form.coverImage)" fit="cover" class="uploaded-image" />
                          <div class="hover-mask"><el-icon><Edit /></el-icon> 更换封面</div>
                        </div>
                        <div v-else class="upload-placeholder">
                          <el-icon><Plus /></el-icon>
                          <span class="mt-2">点击上传封面</span>
                        </div>
                      </template>
                    </el-upload>
                    <div class="upload-tip mt-2">推荐尺寸 800x600，支持 jpg/png 格式，大小不超过 5MB</div>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider />

            <el-row :gutter="24">
              <el-col :span="24">
                <el-form-item label="详情页滚动图集 (Gallery)">
                  <div class="gallery-container bg-gray-50 p-4 rounded-xl border border-gray-100 border-dashed">
                    <div class="flex justify-between items-center mb-4">
                      <span class="text-sm text-gray-500">已上传 {{ form.images?.length || 0 }} 张图</span>
                      <el-upload
                          class="gallery-uploader-inline"
                          :action="uploadUrl"
                          :headers="uploadHeaders"
                          :data="galleryUploadData"
                          :show-file-list="false"
                          :on-success="handleGalleryUploadSuccess"
                          :on-error="handleUploadError"
                          :before-upload="beforeUpload"
                          accept="image/*"
                      >
                        <el-button type="primary" plain class="modern-btn-plain"><el-icon class="mr-1"><Upload /></el-icon>继续添加图片</el-button>
                      </el-upload>
                    </div>

                    <div class="gallery-list" v-if="form.images?.length">
                      <div class="gallery-item relative group" v-for="(img, index) in form.images" :key="`${img}-${index}`">
                        <el-image :src="getImageUrl(img)" fit="cover" class="gallery-image rounded-lg" />
                        <!-- Hover mask with delete button -->
                        <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center rounded-lg">
                          <el-button type="danger" circle size="small" @click.stop="emit('remove-gallery-image', index)">
                            <el-icon><Delete /></el-icon>
                          </el-button>
                        </div>
                      </div>
                    </div>

                    <el-empty v-else description="暂无详情图" :image-size="60" class="py-4" />
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </el-form>
      </div>

      <!-- 底部操作区 -->
      <div class="drawer-footer px-6 py-4 bg-white border-t border-gray-100 flex justify-between items-center shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
        <div>
          <el-button v-if="activeStep > 0" @click="activeStep--"><el-icon class="mr-1"><ArrowLeft /></el-icon> 上一步</el-button>
        </div>
        <div class="flex gap-3">
          <el-button @click="emitVisible(false)">取消</el-button>
          <el-button v-if="activeStep < 2" type="primary" @click="nextStep" class="modern-btn">下一步 <el-icon class="ml-1"><ArrowRight /></el-icon></el-button>
          <el-button v-if="activeStep === 2" type="primary" @click="submit" :loading="submitting" class="modern-btn">
            <el-icon class="mr-1"><Check /></el-icon> 确认保存
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { Plus, Document, Location, Picture, Clock, Edit, Upload, Delete, ArrowLeft, ArrowRight, Check } from '@element-plus/icons-vue'
import { ref, watch } from 'vue'

const formRef = ref()
const activeStep = ref(0)

const props = defineProps({
  visible: { type: Boolean, required: true },
  editId: { type: [Number, String, null], default: null },
  form: { type: Object, required: true },
  rules: { type: Object, required: true },
  regionCascaderOptions: { type: Array, required: true },
  regionCascaderProps: { type: Object, required: true },
  parentCategoryOptions: { type: Array, required: true },
  childCategoryOptions: { type: Array, required: true },
  heatLevelOptions: { type: Array, required: true },
  uploadUrl: { type: String, required: true },
  uploadHeaders: { type: Object, required: true },
  coverUploadData: { type: Object, required: true },
  galleryUploadData: { type: Object, required: true },
  beforeUpload: { type: Function, required: true },
  handleUploadSuccess: { type: Function, required: true },
  handleGalleryUploadSuccess: { type: Function, required: true },
  handleUploadError: { type: Function, required: true },
  getImageUrl: { type: Function, required: true },
  submitting: { type: Boolean, required: true }
})

const emit = defineEmits([
  'update:visible', 'submit', 'parent-category-change', 'remove-gallery-image'
])

// 重置步骤状态
watch(() => props.visible, (newVal) => {
  if (newVal) {
    activeStep.value = 0
  }
})

const emitVisible = (value) => {
  emit('update:visible', value)
}

const stepOptions = [
  { title: '基础信息', icon: Document },
  { title: '位置与营业', icon: Location },
  { title: '图文素材', icon: Picture }
]

const goToStep = (stepIndex) => {
  activeStep.value = stepIndex
}

const nextStep = async () => {
  // 可以在这里做部分校验，例如基础信息填完才能下一步
  // let fieldsToValidate = []
  // if (activeStep.value === 0) fieldsToValidate = ['name', 'regionPath', 'parentCategoryId', 'categoryId', 'price']
  // if (activeStep.value === 1) fieldsToValidate = ['address', 'heatLevel']

  try {
    // 简略实现：暂时允许直接翻页，最后统一校验
    goToStep(activeStep.value + 1)
  } catch (e) {
    // 校验失败拦截
  }
}

const submit = () => {
  emit('submit')
}

defineExpose({
  validate: () => formRef.value?.validate()
})
</script>

<style lang="scss" scoped>
.spot-form-drawer {
  :deep(.el-drawer__body) {
    padding: 0;
    overflow: hidden;
  }
}

.flex { display: flex; }
.flex-col { flex-direction: column; }
.flex-1 { flex: 1; min-height: 0; }
.h-full { height: 100%; }
.overflow-y-auto { overflow-y: auto; }
.p-6 { padding: 24px; }
.px-6 { padding-left: 24px; padding-right: 24px; }
.py-4 { padding-top: 16px; padding-bottom: 16px; }
.mt-2 { margin-top: 8px; }
.mb-4 { margin-bottom: 16px; }
.mr-1 { margin-right: 4px; }
.ml-1 { margin-left: 4px; }
.w-full { width: 100%; }

.border-b { border-bottom: 1px solid var(--wt-divider-soft); }
.border-t { border-top: 1px solid var(--wt-divider-soft); }
.bg-gray-50 { background-color: var(--wt-surface-muted); }
.bg-white { background-color: var(--wt-surface-elevated); }

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--wt-text-primary);
  margin-top: 0;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--wt-divider-soft);
}

/* Custom Input overwrites for form */
.custom-form {
  :deep(.el-form-item__label) {
    font-weight: 600;
    color: var(--wt-text-regular);
    padding-bottom: 4px;
  }

  :deep(.el-input__wrapper), :deep(.el-textarea__inner) {
    box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  }
}

.upload-container {
  .image-uploader {
    :deep(.el-upload) {
      border: 2px dashed var(--wt-border-default);
      border-radius: 12px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: all 0.3s ease;
      background: var(--wt-surface-muted);
      width: 240px;
      height: 160px;

      &:hover {
        border-color: var(--el-color-primary);
        background: var(--el-color-primary-light-9);
      }
    }
  }

  .uploaded-mask-wrapper {
    position: relative;
    width: 240px;
    height: 160px;

    .hover-mask {
      position: absolute;
      inset: 0;
      background: rgba(0,0,0,0.5);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.3s;
      font-weight: 600;
      gap: 4px;
    }

    &:hover .hover-mask {
      opacity: 1;
    }
  }

  .uploaded-image {
    width: 240px;
    height: 160px;
    display: block;
  }

  .upload-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--wt-text-secondary);

    .el-icon { font-size: 32px; }
    span { font-size: 13px; font-weight: 500; }
  }

  .upload-tip {
    font-size: 12px;
    color: var(--wt-text-secondary);
  }
}

.gallery-container {
  width: 100%;
}

.gallery-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.gallery-item {
  border: 1px solid var(--wt-border-default);
  padding: 4px;
  background: var(--wt-surface-elevated);
}

.gallery-image {
  width: 100%;
  height: 100px;
  border-radius: 6px;
}

/* 基础原子类补充 */
.justify-between { justify-content: space-between; }
.relative { position: relative; }
.absolute { position: absolute; }
.inset-0 { top: 0; right: 0; bottom: 0; left: 0; }
.opacity-0 { opacity: 0; }
.opacity-100 { opacity: 1; }
.group:hover .group-hover\:opacity-100 { opacity: 1; }
.transition-opacity { transition-property: opacity; transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1); transition-duration: 300ms; }
.bg-black\/50 { background-color: rgba(0, 0, 0, 0.5); }
.rounded-lg { border-radius: 8px; }
.rounded-xl { border-radius: 12px; }
.shadow-\[0_-4px_6px_-1px_rgba\(0\,0\,0\,0\.05\)\] { box-shadow: 0 -4px 6px -1px rgba(0, 0, 0, 0.05); }

/* Fade in animation */
.animate-fade-in {
  animation: fadeIn 0.4s ease-out forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.step-title-btn {
  border: 0;
  background: transparent;
  padding: 0;
  color: inherit;
  font: inherit;
  cursor: pointer;
}

:deep(.el-step.is-simple .el-step__title) {
  font-size: 14px;
  font-weight: 600;
}
</style>
