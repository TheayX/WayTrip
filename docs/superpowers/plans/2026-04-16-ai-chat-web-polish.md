# AI Chat Web Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `travel-web` 的 AI 聊天页面只在最新 AI 回复下展示“继续问”，并在保持现有站内风格的前提下统一聊天区视觉层级与文案语气。

**Architecture:** 保持现有 `AiChatWidget.vue + ai-chat store + constants` 结构，不改后端协议。通过在展示层判断“最后一条 assistant 消息”来收口建议追问展示，同时把页面文案、状态提示和卡片样式集中到现有组件与常量层，避免新增复杂交互或额外页面。

**Tech Stack:** Vue 3、Pinia、Element Plus、Vite、ESLint、SCSS

---

## File Map

- **Modify:** `travel-web/src/shared/ui/AiChatWidget.vue`
  - 聊天页面主 UI。负责消息渲染、建议追问区域、状态文案、输入区与视觉样式。
- **Modify:** `travel-web/src/shared/store/ai-chat.js`
  - AI 消息列表与欢迎消息状态管理。必要时补充“最新 assistant 消息”辅助能力，但不改协议。
- **Modify:** `travel-web/src/shared/constants/ai-chat.js`
  - 统一承接聊天引导、占位语、状态文案与按钮文案。
- **Check only:** `travel-web/src/shared/lib/ai-chat.js`
  - 若消息构造中已有适合复用的字段定义，优先复用；无必要不改。

---

### Task 1: 收口“继续问”只显示在最新 AI 回复下

**Files:**
- Modify: `travel-web/src/shared/ui/AiChatWidget.vue`
- Modify: `travel-web/src/shared/store/ai-chat.js`
- Check: `travel-web/src/shared/lib/ai-chat.js`

- [ ] **Step 1: 先读现有消息结构并确认 assistant 消息可识别字段**

查看：
- `travel-web/src/shared/store/ai-chat.js`
- `travel-web/src/shared/lib/ai-chat.js`

重点确认：
- assistant 消息的 `role` 字段是否稳定为 `'assistant'`
- `pending`、`suggestions` 是否只存在于 AI 回复消息上
- 欢迎消息是否也属于 assistant（如果是，后续要确保欢迎消息不抢“最新回答”的判断）

- [ ] **Step 2: 在 store 中补一个“最后一条有效 assistant 消息”辅助计算（若当前组件内判断过重）**

如果 `AiChatWidget.vue` 里直接写查找逻辑会比较乱，则在 `travel-web/src/shared/store/ai-chat.js` 增加一个计算属性，例如：

```js
const latestAssistantMessageId = computed(() => {
  const list = messages.value
  for (let index = list.length - 1; index >= 0; index -= 1) {
    const item = list[index]
    if (item?.role === 'assistant' && !item.pending && item.id) {
      return item.id
    }
  }
  return ''
})
```

并在 return 中暴露：

```js
latestAssistantMessageId
```

如果你发现组件内 3-5 行就能清楚判断，也可以不改 store，保持 YAGNI。

- [ ] **Step 3: 先写一个最小 UI 判定逻辑，让建议追问只在最新 assistant 消息下渲染**

在 `travel-web/src/shared/ui/AiChatWidget.vue` 中，把原来的：

```vue
<div v-if="item.role === 'assistant' && item.suggestions?.length" class="assistant-meta">
```

改为类似：

```vue
<div
  v-if="isLatestAssistantMessage(item) && item.suggestions?.length"
  class="assistant-meta assistant-followup"
>
```

并在 `<script setup>` 中增加最小判断函数（如果没在 store 加 computed）：

```js
function isLatestAssistantMessage(target) {
  if (!target || target.role !== 'assistant' || target.pending || !target.id) {
    return false
  }

  for (let index = messages.value.length - 1; index >= 0; index -= 1) {
    const item = messages.value[index]
    if (item?.role === 'assistant' && !item.pending && item.id) {
      return item.id === target.id
    }
  }

  return false
}
```

- [ ] **Step 4: 运行手工检查，确认旧回答下的“继续问”不会再显示**

启动项目：

```bash
npm --prefix "E:/Year-4/Grad-Project/WayTrip/travel-web" run dev
```

手工验证：
1. 连续发 2-3 轮消息；
2. 检查只有最新一条 AI 回复下存在“继续问”；
3. 历史 AI 回复不再显示建议追问；
4. loading/pending 消息下不出现“继续问”。

预期：
- 历史回答完全不显示建议区；
- 最新回答下建议区正常可点。

- [ ] **Step 5: 提交这一小步**

```bash
git -C "E:/Year-4/Grad-Project/WayTrip" add "travel-web/src/shared/ui/AiChatWidget.vue" "travel-web/src/shared/store/ai-chat.js"
git -C "E:/Year-4/Grad-Project/WayTrip" commit -m "fix(web-ai): show follow-up prompts only for latest reply"
```

如果 store 未改，就不要把它加入 `git add`。

---

### Task 2: 统一聊天区视觉层级与卡片结构

**Files:**
- Modify: `travel-web/src/shared/ui/AiChatWidget.vue`

- [ ] **Step 1: 先整理模板结构，给不同层级区域加清晰 class 名**

在 `AiChatWidget.vue` 中，把 AI 回复相关区域整理为：
- 主气泡：`message-bubble`
- 次级信息容器：`assistant-meta`
- 建议追问区：`assistant-followup`
- 引用区：保留 `citation-list`
- 反馈区：保留 `feedback-row`

例如建议追问区标题可以改成：

```vue
<div class="meta-title meta-title--followup">
  <el-icon><ChatLineSquare /></el-icon>
  继续问
</div>
```

这样标题更短，语义更像回答延伸操作。

- [ ] **Step 2: 调整消息区样式，让回答卡片更稳、更统一**

在 `<style scoped lang="scss">` 中优化这些块：

```scss
.message-list {
  padding: 20px 18px 24px;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
}

.message-item {
  margin-bottom: 20px;
}

.message-card {
  max-width: 85%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.75;
}

.message-item.assistant .message-bubble {
  background: #ffffff;
  color: #1e293b;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}
```

目标：
- assistant 回复更像稳定的内容卡片；
- user 气泡与 assistant 卡片区分更自然；
- 留白更规整，不要一块松一块紧。

- [ ] **Step 3: 专门优化“继续问”区域样式，让它像回答底部延伸操作区**

新增/调整样式：

```scss
.assistant-followup {
  margin-top: 2px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(186, 230, 253, 0.7);
}

.meta-title--followup {
  color: #0369a1;
  margin-bottom: 10px;
}

.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestion-chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: #eff6ff;
  border: 1px solid rgba(125, 211, 252, 0.7);
  color: #075985;
}
```

原则：
- 它应依附在最新回答下面；
- 权重低于主回答，高于普通辅助说明；
- 不做成夸张大按钮区。

- [ ] **Step 4: 优化输入区与头部细节，使其和消息区风格统一**

调整这些样式块：

```scss
.chat-header {
  min-height: 84px;
  padding: 16px 18px;
}

.chat-title {
  font-size: 16px;
  font-weight: 700;
}

.chat-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);
}

.chat-input {
  padding: 14px 16px 16px;
  background: rgba(255, 255, 255, 0.96);
}

.chat-input :deep(.el-textarea__inner) {
  background: #f8fafc;
  border: 1px solid rgba(226, 232, 240, 0.9);
}
```

目标：
- 头部更克制，不显得太“科技炫技”；
- 输入区更像产品输入区，而不是 demo 组件。

- [ ] **Step 5: 运行前端构建验证视觉改动至少未破坏编译**

运行：

```bash
npm --prefix "E:/Year-4/Grad-Project/WayTrip/travel-web" run build
```

预期：
- Vite build 成功
- 无 Vue 模板编译错误
- 无样式语法错误

- [ ] **Step 6: 提交这一小步**

```bash
git -C "E:/Year-4/Grad-Project/WayTrip" add "travel-web/src/shared/ui/AiChatWidget.vue"
git -C "E:/Year-4/Grad-Project/WayTrip" commit -m "style(web-ai): polish chat layout and message hierarchy"
```

---

### Task 3: 收口文案与状态提示语气

**Files:**
- Modify: `travel-web/src/shared/constants/ai-chat.js`
- Modify: `travel-web/src/shared/ui/AiChatWidget.vue`
- Check: `travel-web/src/shared/lib/ai-chat.js`

- [ ] **Step 1: 先把 AI 页面常用文案集中到 constants 中**

在 `travel-web/src/shared/constants/ai-chat.js` 中新增/整理类似结构：

```js
export const AI_CHAT_COPY = {
  launcher: 'AI 助手',
  title: 'WayTrip AI 助手',
  subtitle: '聊景点、问订单、找灵感',
  inputPlaceholder: '输入你的问题，比如景点推荐、行程规划或订单咨询',
  typing: '正在整理回答，请稍候…',
  followupTitle: '继续问',
  feedbackUpvote: '有帮助',
  feedbackDownvote: '还不够准确',
  clearTitle: '清空对话',
  closeTitle: '关闭聊天'
}
```

如果当前文件里已有适合合并的常量，优先复用，不要平行造新概念。

- [ ] **Step 2: 在组件模板中替换硬编码文案**

把 `AiChatWidget.vue` 里的这类硬编码文案替换为常量引用：

```vue
{{ AI_CHAT_COPY.title }}
{{ AI_CHAT_COPY.subtitle }}
{{ AI_CHAT_COPY.followupTitle }}
{{ AI_CHAT_COPY.typing }}
:placeholder="AI_CHAT_COPY.inputPlaceholder"
:title="AI_CHAT_COPY.clearTitle"
:title="AI_CHAT_COPY.closeTitle"
```

并在 `<script setup>` 中引入：

```js
import { AI_CHAT_COPY } from '@/shared/constants/ai-chat.js'
```

- [ ] **Step 3: 微调状态提示文案，统一为产品语气**

在 `AiChatWidget.vue` 中，重点替换这些文本：

```vue
<template v-if="item.pending">正在整理这条问题的答案…</template>
```

```vue
{{ AI_CHAT_COPY.typing }}
```

反馈按钮改为：

```vue
<span>{{ AI_CHAT_COPY.feedbackUpvote }}</span>
<span>{{ AI_CHAT_COPY.feedbackDownvote }}</span>
```

如果错误提示通过 `resolveAiErrorMessage` 返回，则不在这里重复做协议层改造，只保证页面静态文案先统一。

- [ ] **Step 4: 运行 lint 和 build，确认文案收口没有引入引用错误**

先运行：

```bash
npm --prefix "E:/Year-4/Grad-Project/WayTrip/travel-web" run lint
```

再运行：

```bash
npm --prefix "E:/Year-4/Grad-Project/WayTrip/travel-web" run build
```

预期：
- lint 通过
- build 通过

- [ ] **Step 5: 提交这一小步**

```bash
git -C "E:/Year-4/Grad-Project/WayTrip" add "travel-web/src/shared/constants/ai-chat.js" "travel-web/src/shared/ui/AiChatWidget.vue"
git -C "E:/Year-4/Grad-Project/WayTrip" commit -m "refactor(web-ai): unify chat copy and status text"
```

---

### Task 4: 端到端回归与人工验收

**Files:**
- Modify if needed: `travel-web/src/shared/ui/AiChatWidget.vue`
- Modify if needed: `travel-web/src/shared/store/ai-chat.js`
- Modify if needed: `travel-web/src/shared/constants/ai-chat.js`

- [ ] **Step 1: 启动本地前端并人工走完整聊天流程**

运行：

```bash
npm --prefix "E:/Year-4/Grad-Project/WayTrip/travel-web" run dev
```

手动检查：
1. 打开 AI 聊天组件；
2. 发送第一轮消息，确认回复正常；
3. 点击“继续问”建议，发起第二轮；
4. 再发送第三轮自由输入；
5. 滚动查看历史消息，确认旧回复不再显示建议追问；
6. 清空对话后重新开始，确认欢迎态与输入区文案自然。

- [ ] **Step 2: 对照 spec 做逐项验收记录**

手工核对以下清单：

```text
[ ] 只有最新 AI 回复下展示“继续问”
[ ] 历史 AI 回复不显示建议区
[ ] 顶部引导语更克制自然
[ ] 输入框占位语更像产品语言
[ ] 正在生成/加载提示语统一
[ ] 回答卡片留白与层级更统一
[ ] 页面整体风格未偏离现有站内风格
```

如果有任一项不满足，回到对应文件微调后重新 build。

- [ ] **Step 3: 跑最终新鲜构建验证**

运行：

```bash
npm --prefix "E:/Year-4/Grad-Project/WayTrip/travel-web" run build
```

预期：
- 最终构建成功，作为本次完成依据

- [ ] **Step 4: 提交最终收口改动**

```bash
git -C "E:/Year-4/Grad-Project/WayTrip" add "travel-web/src/shared/ui/AiChatWidget.vue" "travel-web/src/shared/store/ai-chat.js" "travel-web/src/shared/constants/ai-chat.js"
git -C "E:/Year-4/Grad-Project/WayTrip" commit -m "feat(web-ai): refine chat follow-up prompts and polish UI"
```

---

## Spec Coverage Check

对应 spec 要求的实现映射：
- “继续问只属于最新 AI 回答” → Task 1
- “统一聊天区视觉层级与留白” → Task 2
- “优化标题、说明、占位文本、状态提示与建议追问文案” → Task 3
- “仅限 travel-web，不改协议、不扩展到其他端” → 整个计划的文件边界已限制在 `travel-web`
- “手动验证与编译通过” → Task 4

## Self-Review Notes

- 无 `TODO/TBD` 占位项。
- 所有任务都包含明确文件路径、操作步骤、命令与预期结果。
- 没有把后端改造或跨端改造混入计划。
- 代码步骤均围绕现有 `AiChatWidget.vue / ai-chat.js / ai-chat constants`，未引入不必要新架构。
