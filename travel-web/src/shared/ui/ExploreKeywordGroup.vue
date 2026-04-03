<template>
  <div class="keyword-group" :class="{ compact }">
    <span class="group-label">{{ title }}</span>
    <button
      v-for="item in items"
      :key="item"
      type="button"
      class="keyword-chip"
      :class="{ ghost: variant === 'ghost' }"
      @click="$emit('select', item)"
    >
      {{ item }}
    </button>
    <el-button v-if="clearable" text @click="$emit('clear')">清空</el-button>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  items: {
    type: Array,
    default: () => []
  },
  variant: {
    type: String,
    default: 'solid'
  },
  compact: {
    type: Boolean,
    default: false
  },
  clearable: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select', 'clear'])
</script>

<style lang="scss" scoped>
.keyword-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.keyword-group.compact {
  justify-content: center;
}

.group-label {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.keyword-chip {
  padding: 8px 14px;
  border: 0;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  cursor: pointer;
  font-size: 13px;
}

.keyword-chip.ghost {
  background: #f1f5f9;
  color: #475569;
}
</style>
