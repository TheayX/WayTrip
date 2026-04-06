<template>
  <!-- 设置热度档位对话框 -->
  <el-dialog :model-value="visible" title="设置热度档位" width="420px" @update:model-value="emitVisible">
    <el-form ref="heatFormRef" :model="heatForm" :rules="heatRules" label-width="110px">
      <el-form-item label="当前评分">
        <span>{{ heatForm.avgRating ?? 0 }}</span>
      </el-form-item>
      <el-form-item label="评价数">
        <span>{{ heatForm.ratingCount ?? 0 }}</span>
      </el-form-item>
      <el-form-item label="当前热度分数">
        <span>{{ heatForm.heatScore ?? 0 }}</span>
      </el-form-item>
      <el-form-item label="热度档位" prop="heatLevel">
        <el-select v-model="heatForm.heatLevel" class="form-w-full" placeholder="请选择热度档位">
          <el-option
            v-for="item in heatLevelOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emitVisible(false)">取消</el-button>
      <el-button type="primary" @click="emit('submit')" :loading="heatSubmitting">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

const heatFormRef = ref()

defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  heatForm: {
    type: Object,
    required: true
  },
  heatRules: {
    type: Object,
    required: true
  },
  heatLevelOptions: {
    type: Array,
    required: true
  },
  heatSubmitting: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['update:visible', 'submit'])

const emitVisible = (value) => {
  emit('update:visible', value)
}

defineExpose({
  validate: () => heatFormRef.value?.validate()
})
</script>
