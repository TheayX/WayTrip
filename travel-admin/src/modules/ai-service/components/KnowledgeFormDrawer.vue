<!-- AI 知识表单抽屉 -->
<template>
  <el-drawer
    :model-value="visible"
    :title="mode === 'create' ? '新增知识文档' : '编辑知识文档'"
    size="680px"
    class="knowledge-form-drawer"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-position="top" class="knowledge-form">
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="文档标题" prop="title">
            <el-input v-model="formData.title" maxlength="120" show-word-limit placeholder="请输入文档标题" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :md="12" :sm="24">
          <el-form-item label="知识域" prop="knowledgeDomain">
            <el-select v-model="formData.knowledgeDomain" placeholder="请选择知识域" class="w-full">
              <el-option v-for="item in knowledgeDomainOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :md="12" :sm="24">
          <el-form-item label="来源类型" prop="sourceType">
            <el-select v-model="formData.sourceType" placeholder="请选择来源类型" class="w-full">
              <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :md="12" :sm="24">
          <el-form-item label="来源标识" prop="sourceRef">
            <el-input v-model="formData.sourceRef" maxlength="120" placeholder="例如 policy:refund-rule" />
          </el-form-item>
        </el-col>
        <el-col :md="12" :sm="24">
          <el-form-item label="标签">
            <el-input v-model="formData.tags" maxlength="120" placeholder="多个标签请用英文逗号分隔" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="正文内容" prop="content">
        <el-input
          v-model="formData.content"
          type="textarea"
          :rows="18"
          maxlength="12000"
          show-word-limit
          resize="vertical"
          placeholder="请输入知识正文，支持纯文本或带基础换行的结构化内容"
        />
      </el-form-item>

      <el-alert
        class="form-hint"
        type="info"
        show-icon
        :closable="false"
        title="正文将按文本内容切分为知识分片，请尽量使用清晰段落，避免富文本粘贴带来的格式噪音。"
      />
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="emit('update:visible', false)">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ mode === 'create' ? '创建文档' : '保存修改' }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref } from 'vue'

// 表单实例仅用于触发 Element Plus 校验和重置。
const formRef = ref()

// 表单数据和选项全部由父层提供，保证新增和编辑共用同一份状态源。
defineProps({
  visible: { type: Boolean, required: true },
  mode: { type: String, default: 'create' },
  submitting: { type: Boolean, default: false },
  formData: { type: Object, required: true },
  knowledgeDomainOptions: { type: Array, required: true },
  sourceTypeOptions: { type: Array, required: true }
})

// 提交动作交由父层处理接口调用，抽屉内部只负责校验。
const emit = defineEmits(['update:visible', 'submit'])

// 表单规则集中定义在组件内部，避免父层页面混入过多字段级校验细节。
const rules = {
  title: [
    { required: true, message: '请输入文档标题', trigger: 'blur' },
    { min: 2, max: 120, message: '标题长度需在 2 到 120 个字符之间', trigger: 'blur' }
  ],
  knowledgeDomain: [
    { required: true, message: '请选择知识域', trigger: 'change' }
  ],
  sourceType: [
    { required: true, message: '请选择来源类型', trigger: 'change' }
  ],
  sourceRef: [
    { required: true, message: '请输入来源标识', trigger: 'blur' },
    { min: 2, max: 120, message: '来源标识长度需在 2 到 120 个字符之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入正文内容', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !value.trim()) {
          callback(new Error('正文内容不能为空'))
          return
        }
        if (value.trim().length < 10) {
          callback(new Error('正文内容至少需要 10 个字符'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

// 只有校验通过才允许提交，失败时直接留在当前抽屉内修正。
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    emit('submit')
  } catch (_error) {
    // 交由表单项展示校验信息，避免重复弹窗打断录入。
  }
}

// 暴露给父层的能力仅保留校验和重置，避免直接操作内部实现细节。
defineExpose({
  validate: () => formRef.value?.validate(),
  resetFields: () => formRef.value?.resetFields()
})
</script>

<style lang="scss" scoped>
.knowledge-form {
  padding: 4px 4px 0;
}

.w-full {
  width: 100%;
}

.form-hint {
  margin-top: 8px;
}

.drawer-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-textarea__inner) {
  line-height: 1.75;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--wt-text-regular);
}
</style>
