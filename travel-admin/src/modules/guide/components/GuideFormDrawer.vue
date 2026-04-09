<!-- 攻略新增与编辑抽屉 -->
<template>
  <el-drawer
    :model-value="visible"
    :title="editId ? '编辑攻略' : '新增攻略'"
    size="960px"
    class="guide-form-drawer"
    destroy-on-close
    @update:model-value="emitVisible"
  >
    <div class="drawer-content-wrapper flex flex-col h-full">
      <div class="drawer-body flex flex-1 min-h-0">
        <aside class="section-nav">
          <div class="section-nav-title">录入导航</div>
          <button
            v-for="section in sections"
            :key="section.key"
            type="button"
            class="section-nav-item"
            :class="{ active: activeSection === section.key }"
            @click="scrollToSection(section.key)"
          >
            <span class="section-nav-name">{{ section.label }}</span>
            <span class="section-nav-hint">{{ section.hint }}</span>
          </button>
        </aside>

        <div ref="formContainerRef" class="form-container flex-1 overflow-y-auto p-6">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="custom-form">
            <div :ref="setSectionRef('basic')" class="section-card section-anchor">
              <div class="section-head">
                <div>
                  <h3 class="section-title">基础信息</h3>
                  <p class="section-desc">先确定标题、分类、封面与发布状态。</p>
                </div>
                <el-tag effect="light" type="info">必填优先</el-tag>
              </div>

              <el-row :gutter="24">
                <el-col :span="16">
                  <el-form-item label="攻略标题" prop="title">
                    <el-input v-model="form.title" placeholder="请输入攻略标题" size="large" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="攻略分类" prop="category">
                    <el-select v-model="form.category" placeholder="请选择分类" allow-create filterable class="w-full" size="large">
                      <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="24">
                <el-col :span="14">
                  <el-form-item label="封面图" prop="coverImage">
                    <div class="upload-container">
                      <el-upload
                        class="image-uploader"
                        :action="uploadUrl"
                        :headers="uploadHeaders"
                        :data="uploadData"
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
                <el-col :span="10">
                  <div class="publish-panel">
                    <h4 class="publish-title">发布设置</h4>
                    <p class="publish-desc">先决定当前内容是保存为草稿，还是直接进入已发布状态。</p>
                    <el-radio-group v-model="form.published" class="publish-group">
                      <el-radio :value="false">草稿</el-radio>
                      <el-radio :value="true">已发布</el-radio>
                    </el-radio-group>
                  </div>
                </el-col>
              </el-row>
            </div>

            <div :ref="setSectionRef('spots')" class="section-card section-anchor">
              <div class="section-head">
                <div>
                  <h3 class="section-title">关联景点</h3>
                  <p class="section-desc">把攻略和景点建立关联，方便内容推荐与详情跳转。</p>
                </div>
                <el-tag effect="light" type="success">可选增强</el-tag>
              </div>
              <el-form-item label="关联景点">
                <el-select v-model="form.spotIds" multiple filterable collapse-tags collapse-tags-tooltip placeholder="请选择关联景点" class="w-full" size="large">
                  <el-option
                    v-for="spot in mergedSpotOptions"
                    :key="spot.id"
                    :label="spot.isDeleted === 1 ? `${spot.name}（已删除）` : (spot.published ? spot.name : `${spot.name}（已下架）`)"
                    :value="spot.id"
                    :disabled="spot.isDeleted === 1"
                  />
                </el-select>
              </el-form-item>
            </div>

            <div :ref="setSectionRef('content')" class="section-card section-anchor">
              <div class="section-head">
                <div>
                  <h3 class="section-title">正文内容</h3>
                  <p class="section-desc">正文支持富文本排版，适合直接维护标题、段落、引用和图片说明。</p>
                </div>
                <el-tag effect="light" type="warning">核心内容</el-tag>
              </div>
              <el-form-item label="攻略正文" prop="content">
                <GuideRichEditor
                  v-model="form.content"
                  placeholder="请输入攻略内容，支持直接粘贴富文本或从纯文本开始编辑"
                />
              </el-form-item>
            </div>
          </el-form>
        </div>
      </div>

      <div class="drawer-footer px-6 py-4 bg-white border-t border-gray-100 flex justify-end items-center gap-3">
        <el-button @click="emitVisible(false)">取消</el-button>
        <el-button @click="submit(false)" :loading="submitting">保存草稿</el-button>
        <el-button type="primary" @click="submit(true)" :loading="submitting">保存并发布</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { Edit, Plus } from '@element-plus/icons-vue'
import { nextTick, ref } from 'vue'
import GuideRichEditor from '@/modules/guide/components/GuideRichEditor.vue'

defineProps({
  visible: { type: Boolean, required: true },
  editId: { type: [Number, String, null], default: null },
  form: { type: Object, required: true },
  rules: { type: Object, required: true },
  categories: { type: Array, required: true },
  mergedSpotOptions: { type: Array, required: true },
  uploadUrl: { type: String, required: true },
  uploadHeaders: { type: Object, required: true },
  uploadData: { type: Object, required: true },
  beforeUpload: { type: Function, required: true },
  handleUploadSuccess: { type: Function, required: true },
  handleUploadError: { type: Function, required: true },
  getImageUrl: { type: Function, required: true },
  submitting: { type: Boolean, required: true }
})

const emit = defineEmits(['update:visible', 'submit'])

const formRef = ref()
const formContainerRef = ref()
const activeSection = ref('basic')
const sectionRefs = new Map()
// 录入导航与正文区共享同一套 section key，保证滚动定位和高亮状态一致。
const sections = [
  { key: 'basic', label: '基础信息', hint: '标题、分类、封面、发布状态' },
  { key: 'spots', label: '关联景点', hint: '补齐内容与景点关联' },
  { key: 'content', label: '正文内容', hint: '录入主体内容' }
]

const emitVisible = (value) => {
  emit('update:visible', value)
}

const setSectionRef = (key) => (el) => {
  if (el) {
    sectionRefs.set(key, el)
  } else {
    sectionRefs.delete(key)
  }
}

const scrollToSection = async (key) => {
  activeSection.value = key
  await nextTick()
  const target = sectionRefs.get(key)
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const submit = (published) => {
  // 发布态由按钮语义决定，避免在表单内部再额外维护一套提交分支。
  emit('submit', { published })
}

defineExpose({
  validate: () => formRef.value?.validate()
})
</script>

<style lang="scss" scoped>
.guide-form-drawer {
  :deep(.el-drawer__body) {
    padding: 0;
    overflow: hidden;
  }
}

.flex { display: flex; }
.flex-col { flex-direction: column; }
.flex-1 { flex: 1; min-height: 0; }
.h-full { height: 100%; }
.min-h-0 { min-height: 0; }
.overflow-y-auto { overflow-y: auto; }
.p-6 { padding: 24px; }
.px-6 { padding-left: 24px; padding-right: 24px; }
.py-4 { padding-top: 16px; padding-bottom: 16px; }
.mt-2 { margin-top: 8px; }
.w-full { width: 100%; }
.gap-3 { gap: 12px; }

.justify-end { justify-content: flex-end; }
.items-center { align-items: center; }

.bg-white { background-color: var(--wt-surface-elevated); }
.border-t { border-top: 1px solid var(--wt-divider-soft); }
.border-gray-100 { border-color: var(--wt-divider-soft); }

.drawer-body {
  min-height: 0;
}

.section-nav {
  width: 220px;
  padding: 24px 16px 24px 24px;
  border-right: 1px solid var(--wt-divider-soft);
  background: linear-gradient(180deg, var(--wt-surface-muted) 0%, var(--wt-surface-elevated) 100%);
}

.section-nav-title {
  margin-bottom: 14px;
  color: var(--wt-text-regular);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.section-nav-item {
  width: 100%;
  border: 1px solid var(--wt-border-default);
  border-radius: 14px;
  background: var(--wt-surface-elevated);
  padding: 12px 14px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;

  & + & {
    margin-top: 10px;
  }

  &:hover {
    border-color: var(--el-color-primary-light-5);
  }

  &.active {
    border-color: var(--el-color-primary);
    box-shadow: 0 8px 20px rgba(59, 130, 246, 0.08);
  }
}

.section-nav-name {
  display: block;
  color: var(--wt-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.section-nav-hint {
  display: block;
  margin-top: 6px;
  color: var(--wt-text-regular);
  font-size: 12px;
  line-height: 1.5;
}

.section-card {
  margin-bottom: 24px;
  padding: 24px;
  border: 1px solid var(--wt-divider-soft);
  border-radius: 16px;
  background: linear-gradient(180deg, var(--wt-surface-elevated) 0%, var(--wt-surface-muted) 100%);
}

.section-anchor {
  scroll-margin-top: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.section-desc {
  margin: 8px 0 0;
  color: var(--wt-text-regular);
  font-size: 13px;
  line-height: 1.6;
}

.custom-form {
  :deep(.el-form-item__label) {
    font-weight: 600;
    color: var(--wt-text-regular);
    padding-bottom: 4px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  }
}

.publish-panel {
  height: 100%;
  border: 1px solid var(--wt-border-default);
  border-radius: 14px;
  background: var(--wt-surface-elevated);
  padding: 16px;
}

.publish-title {
  margin: 0;
  color: var(--wt-text-primary);
  font-size: 15px;
  font-weight: 700;
}

.publish-desc {
  margin: 8px 0 16px;
  color: var(--wt-text-regular);
  font-size: 13px;
  line-height: 1.6;
}

.publish-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;

  :deep(.el-radio) {
    margin-right: 0;
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
      background: rgba(0, 0, 0, 0.5);
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

@media (max-width: 960px) {
  .drawer-body {
    flex-direction: column;
  }

  .section-nav {
    width: 100%;
    padding: 16px 24px 0;
    border-right: none;
    border-bottom: 1px solid var(--wt-divider-soft);
  }
}
</style>
