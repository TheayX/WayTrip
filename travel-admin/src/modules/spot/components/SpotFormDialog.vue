<template>
  <!-- 新增/编辑景点对话框 -->
  <el-dialog :model-value="visible" :title="editId ? '编辑景点' : '新增景点'" width="700px" @update:model-value="emitVisible">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入景点名称" />
      </el-form-item>
      <el-form-item label="价格" prop="price">
        <el-input-number v-model="form.price" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="地区" prop="regionPath">
        <el-cascader
          v-model="form.regionPath"
          :options="regionCascaderOptions"
          :props="regionCascaderProps"
          clearable
          placeholder="请选择地区"
        />
      </el-form-item>
      <el-form-item label="父分类" prop="parentCategoryId">
        <el-select v-model="form.parentCategoryId" placeholder="请选择父分类" @change="emit('parent-category-change')">
          <el-option v-for="item in parentCategoryOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="子分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择子分类" :disabled="!form.parentCategoryId">
          <el-option v-for="item in childCategoryOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input v-model="form.address" placeholder="请输入详细地址" />
      </el-form-item>
      <el-form-item label="经纬度">
        <el-input-number v-model="form.latitude" placeholder="纬度" style="width: 150px" />
        <el-input-number v-model="form.longitude" placeholder="经度" style="width: 150px; margin-left: 10px" />
      </el-form-item>
      <el-form-item label="开放时间">
        <el-input v-model="form.openTime" placeholder="如：08:30-17:00" />
      </el-form-item>
      <el-form-item label="热度档位" prop="heatLevel">
        <el-select v-model="form.heatLevel" placeholder="请选择热度档位">
          <el-option
            v-for="item in heatLevelOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入景点简介" />
      </el-form-item>
      <el-form-item label="封面图">
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
            <el-image
              v-if="form.coverImage"
              :src="getImageUrl(form.coverImage)"
              fit="cover"
              class="uploaded-image"
            />
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <span>点击上传</span>
            </div>
          </el-upload>
          <div class="upload-tip">支持 jpg、png 格式，大小不超过 5MB</div>
        </div>
      </el-form-item>
      <el-form-item label="景点图片">
        <div class="gallery-container">
          <el-upload
            class="gallery-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="uploadData"
            :show-file-list="false"
            :on-success="handleGalleryUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            accept="image/*"
          >
            <el-button type="primary" plain>上传图片</el-button>
          </el-upload>
          <div class="gallery-list" v-if="form.images?.length">
            <div class="gallery-item" v-for="(img, index) in form.images" :key="`${img}-${index}`">
              <el-image :src="getImageUrl(img)" fit="cover" class="gallery-image" />
              <el-button link type="danger" @click="emit('remove-gallery-image', index)">删除</el-button>
            </div>
          </div>
          <div class="upload-tip">可上传多张详情图，按当前顺序保存</div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emitVisible(false)">取消</el-button>
      <el-button type="primary" @click="emit('submit')" :loading="submitting">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { ref } from 'vue'

const formRef = ref()

defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  editId: {
    type: [Number, String, null],
    default: null
  },
  form: {
    type: Object,
    required: true
  },
  rules: {
    type: Object,
    required: true
  },
  regionCascaderOptions: {
    type: Array,
    required: true
  },
  regionCascaderProps: {
    type: Object,
    required: true
  },
  parentCategoryOptions: {
    type: Array,
    required: true
  },
  childCategoryOptions: {
    type: Array,
    required: true
  },
  heatLevelOptions: {
    type: Array,
    required: true
  },
  uploadUrl: {
    type: String,
    required: true
  },
  uploadHeaders: {
    type: Object,
    required: true
  },
  uploadData: {
    type: Object,
    required: true
  },
  beforeUpload: {
    type: Function,
    required: true
  },
  handleUploadSuccess: {
    type: Function,
    required: true
  },
  handleGalleryUploadSuccess: {
    type: Function,
    required: true
  },
  handleUploadError: {
    type: Function,
    required: true
  },
  getImageUrl: {
    type: Function,
    required: true
  },
  submitting: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits([
  'update:visible',
  'submit',
  'parent-category-change',
  'remove-gallery-image'
])

const emitVisible = (value) => {
  emit('update:visible', value)
}

defineExpose({
  validate: () => formRef.value?.validate()
})
</script>

<style lang="scss" scoped>
.upload-container {
  .image-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: border-color 0.3s;

      &:hover {
        border-color: #409eff;
      }
    }
  }

  .uploaded-image {
    width: 150px;
    height: 150px;
    display: block;
  }

  .upload-placeholder {
    width: 150px;
    height: 150px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #8c939d;

    .el-icon {
      font-size: 28px;
      margin-bottom: 8px;
    }

    span {
      font-size: 12px;
    }
  }

  .upload-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}

.gallery-container {
  width: 100%;
}

.gallery-list {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.gallery-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.gallery-image {
  width: 100%;
  height: 90px;
}
</style>
