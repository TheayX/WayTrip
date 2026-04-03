<template>
  <div class="suggestion-grid">
    <article
      v-for="item in items"
      :key="item.id"
      class="suggestion-card card"
      @click="$emit('select', item)"
    >
      <img :src="getImageUrl(item.image)" class="suggestion-image" alt="" />
      <div class="suggestion-content">
        <h4>{{ item.title }}</h4>
        <p>{{ item.subtitle }}</p>
      </div>
    </article>
  </div>
</template>

<script setup>
import { getImageUrl } from '@/shared/api/client.js'

defineProps({
  items: {
    type: Array,
    default: () => []
  }
})

defineEmits(['select'])
</script>

<style lang="scss" scoped>
.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.suggestion-card {
  cursor: pointer;
  overflow: hidden;
}

.suggestion-image {
  width: 100%;
  height: 160px;
  object-fit: cover;
}

.suggestion-content {
  padding: 14px 16px;
}

.suggestion-content h4 {
  margin-bottom: 6px;
  color: #0f172a;
}

.suggestion-content p {
  color: #64748b;
}

@media (max-width: 992px) {
  .suggestion-grid {
    grid-template-columns: 1fr;
  }
}
</style>
