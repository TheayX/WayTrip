<!-- 攻略富文本编辑器 -->
<template>
  <div class="guide-rich-editor">
    <div ref="toolbarRef" class="editor-toolbar"></div>
    <div ref="editorRef" class="editor-content"></div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import E from 'wangeditor'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入攻略内容' }
})

const emit = defineEmits(['update:modelValue'])

const toolbarRef = ref(null)
const editorRef = ref(null)
let editor = null

const syncEditorHtml = (html) => {
  // 避免父子双向同步时重复写回，减少编辑器光标抖动。
  if (!editor) {
    return
  }
  if (editor.txt.html() !== (html || '')) {
    editor.txt.html(html || '')
  }
}

const createEditor = async () => {
  await nextTick()
  if (!toolbarRef.value || !editorRef.value) {
    return
  }

  // 编辑器实例只在组件挂载后创建，确保工具栏和内容容器都已就绪。
  editor = new E(toolbarRef.value, editorRef.value)
  editor.config.placeholder = props.placeholder
  editor.config.zIndex = 10
  editor.config.menus = [
    'head',
    'bold',
    'fontSize',
    'fontName',
    'italic',
    'underline',
    'strikeThrough',
    'foreColor',
    'backColor',
    'link',
    'list',
    'justify',
    'quote',
    'image',
    'table',
    'code',
    'undo',
    'redo'
  ]
  // 攻略内容以 HTML 存储，便于后续在详情页直接富文本展示。
  editor.config.onchange = (html) => {
    emit('update:modelValue', html)
  }
  editor.create()
  syncEditorHtml(props.modelValue)
}

watch(
  () => props.modelValue,
  (value) => {
    syncEditorHtml(value)
  }
)

onMounted(() => {
  createEditor()
})

onBeforeUnmount(() => {
  if (editor) {
    editor.destroy()
    editor = null
  }
})
</script>

<style lang="scss" scoped>
.guide-rich-editor {
  border: 1px solid var(--wt-border-default);
  border-radius: 14px;
  overflow: hidden;
  background: var(--wt-surface-elevated);
}

.editor-toolbar {
  border-bottom: 1px solid var(--wt-border-default);
  background: var(--wt-surface-muted);
}

.editor-content {
  min-height: 360px;
  padding: 0;
}

:deep(.w-e-text-container) {
  min-height: 360px !important;
  background: var(--wt-surface-elevated);
}

:deep(.w-e-text) {
  padding: 12px 14px !important;
  line-height: 1.8;
  color: var(--wt-text-primary);
}

:deep(.w-e-menu) {
  z-index: 11 !important;
  color: var(--wt-text-regular);
}

:deep(.w-e-menu:hover) {
  background: var(--wt-fill-hover);
  color: var(--el-color-primary);
}

:deep(.w-e-toolbar) {
  background: var(--wt-surface-muted) !important;
  border-color: var(--wt-border-default) !important;
}

:deep(.w-e-text-container),
:deep(.w-e-panel-container) {
  border-color: var(--wt-border-default) !important;
}

:deep(.w-e-panel-container) {
  background: var(--wt-surface-elevated) !important;
  color: var(--wt-text-regular);
  box-shadow: var(--wt-shadow-float);
}
</style>
