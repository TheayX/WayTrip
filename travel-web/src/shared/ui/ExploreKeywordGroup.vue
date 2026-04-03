<template>
  <div class="keyword-group" :class="{ compact, boxed: !compact }">
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

.keyword-group.boxed {
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 30px -28px rgba(15, 23, 42, 0.3);
}

.keyword-group.compact {
  justify-content: center;
}

.group-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.keyword-chip {
  min-height: 34px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: #f8fafc;
  color: #334155;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.keyword-chip:hover {
  transform: translateY(-1px);
  background: #f1f5f9;
}

.keyword-chip.ghost {
  background: #f1f5f9;
  color: #475569;
}
</style>
