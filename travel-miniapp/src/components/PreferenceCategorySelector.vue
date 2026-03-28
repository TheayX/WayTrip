<template>
  <view class="selector-card">
    <text v-if="eyebrow" class="selector-eyebrow">{{ eyebrow }}</text>
    <text class="selector-title">{{ title }}</text>
    <text v-if="subtitle" class="selector-subtitle">{{ subtitle }}</text>

    <view class="selector-tags">
      <view
        v-for="category in categories"
        :key="category.id"
        class="selector-tag"
        :class="{ active: selectedIds.includes(category.id) }"
        @click="toggleCategory(category.id)"
      >
        <text class="selector-tag-text">{{ category.name }}</text>
      </view>
    </view>

    <view v-if="showFooter" class="selector-footer">
      <text class="selector-count">{{ footerText }}</text>
      <view class="selector-actions">
        <button
          v-if="secondaryText"
          class="selector-btn secondary"
          @click="$emit('secondary')"
        >
          {{ secondaryText }}
        </button>
        <button
          class="selector-btn primary"
          :disabled="primaryDisabled"
          @click="$emit('submit')"
        >
          {{ primaryText }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: Array,
    default: () => []
  },
  title: {
    type: String,
    default: '选择你感兴趣的景点分类'
  },
  subtitle: {
    type: String,
    default: ''
  },
  eyebrow: {
    type: String,
    default: ''
  },
  maxSelect: {
    type: Number,
    default: 5
  },
  primaryText: {
    type: String,
    default: '保存设置'
  },
  secondaryText: {
    type: String,
    default: ''
  },
  showFooter: {
    type: Boolean,
    default: true
  },
  allowEmpty: {
    type: Boolean,
    default: false
  },
  saving: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'submit', 'secondary', 'limit-exceed'])

const selectedIds = computed(() => props.modelValue || [])

const footerText = computed(() => {
  if (props.allowEmpty) {
    return `已选择 ${selectedIds.value.length}/${props.maxSelect}，也可以清空保存`
  }
  return `已选择 ${selectedIds.value.length}/${props.maxSelect}`
})

const primaryDisabled = computed(() => {
  if (props.saving) return true
  if (!props.allowEmpty && selectedIds.value.length === 0) return true
  return false
})

const toggleCategory = (id) => {
  const nextIds = [...selectedIds.value]
  const index = nextIds.indexOf(id)
  if (index > -1) {
    nextIds.splice(index, 1)
    emit('update:modelValue', nextIds)
    return
  }

  if (nextIds.length >= props.maxSelect) {
    emit('limit-exceed', props.maxSelect)
    return
  }

  nextIds.push(id)
  emit('update:modelValue', nextIds)
}
</script>

<style scoped>
.selector-card {
  display: flex;
  flex-direction: column;
}

.selector-eyebrow {
  display: inline-flex;
  align-self: center;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: #eff6ff;
  color: #2563eb;
  font-size: 20rpx;
  letter-spacing: 2rpx;
}

.selector-title {
  display: block;
  margin-top: 12rpx;
  font-size: 34rpx;
  font-weight: 700;
  text-align: center;
  color: #111827;
}

.selector-subtitle {
  display: block;
  margin: 16rpx 0 28rpx;
  font-size: 24rpx;
  line-height: 1.6;
  text-align: center;
  color: #6b7280;
}

.selector-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.selector-tag {
  padding: 16rpx 24rpx;
  border-radius: 999rpx;
  background: #eef2f7;
}

.selector-tag.active {
  background: #2563eb;
}

.selector-tag-text {
  font-size: 24rpx;
  color: #4b5563;
}

.selector-tag.active .selector-tag-text {
  color: #ffffff;
}

.selector-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 28rpx;
}

.selector-count {
  flex: 1;
  font-size: 24rpx;
  color: #6b7280;
}

.selector-actions {
  display: flex;
  gap: 20rpx;
}

.selector-btn {
  min-width: 180rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 42rpx;
  font-size: 28rpx;
  border: none;
}

.selector-btn.secondary {
  background: #eef2f7;
  color: #4b5563;
}

.selector-btn.primary {
  background: #2563eb;
  color: #ffffff;
}

.selector-btn[disabled] {
  background: #c7cbd4;
  color: #ffffff;
}
</style>
