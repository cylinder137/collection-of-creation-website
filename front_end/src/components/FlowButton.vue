<script setup lang="ts">
/**
 * FlowButton — 流光按钮（Vue 移植版）
 *
 * 原型来自 React/Tailwind 的 flow-button.tsx，此处按本项目技术栈
 * （Vue 3 + scoped CSS，无 Tailwind）等价移植：
 * - 左箭头滑入 / 右箭头滑出（800ms 回弹缓动）
 * - 中央圆形扩散填充（悬停时 220px 圆形展开覆盖按钮）
 * - 圆角从 100px 收拢到 12px，文字随箭头方向平移
 * - 按下时轻微缩小反馈
 */
withDefaults(
  defineProps<{
    text?: string
    disabled?: boolean
  }>(),
  {
    text: 'Modern Button',
    disabled: false,
  },
)

defineEmits<{ click: [event: MouseEvent] }>()
</script>

<template>
  <button
    type="button"
    class="flow-btn"
    :class="{ 'is-disabled': disabled }"
    :disabled="disabled"
    @click="$emit('click', $event)"
  >
    <!-- 左箭头：悬停时从左侧滑入 -->
    <svg
      class="flow-arrow flow-arrow--left"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path d="M5 12h14" />
      <path d="m12 5 7 7-7 7" />
    </svg>

    <!-- 文案：悬停时向右平移 -->
    <span class="flow-text">{{ text }}</span>

    <!-- 中央扩散圆 -->
    <span class="flow-circle" aria-hidden="true" />

    <!-- 右箭头：悬停时向右侧滑出 -->
    <svg
      class="flow-arrow flow-arrow--right"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path d="M5 12h14" />
      <path d="m12 5 7 7-7 7" />
    </svg>
  </button>
</template>

<style scoped>
.flow-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 12px 32px;
  border: 1.5px solid rgba(51, 51, 51, 0.4);
  border-radius: 100px;
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  color: #111111;
  cursor: pointer;
  transition:
    border-color 600ms cubic-bezier(0.23, 1, 0.32, 1),
    color 600ms cubic-bezier(0.23, 1, 0.32, 1),
    border-radius 600ms cubic-bezier(0.23, 1, 0.32, 1),
    transform 200ms ease;
}

.flow-btn:hover:not(.is-disabled) {
  border-color: transparent;
  color: #ffffff;
  border-radius: 12px;
}

.flow-btn:active:not(.is-disabled) {
  transform: scale(0.95);
}

.flow-btn.is-disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.flow-text {
  position: relative;
  z-index: 1;
  transform: translateX(-12px);
  transition: transform 800ms ease-out;
}

.flow-btn:hover:not(.is-disabled) .flow-text {
  transform: translateX(12px);
}

.flow-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--brand-color, #4f6ef7);
  opacity: 0;
  transform: translate(-50%, -50%);
  transition:
    width 800ms cubic-bezier(0.19, 1, 0.22, 1),
    height 800ms cubic-bezier(0.19, 1, 0.22, 1),
    opacity 800ms cubic-bezier(0.19, 1, 0.22, 1);
  pointer-events: none;
}

.flow-btn:hover:not(.is-disabled) .flow-circle {
  width: 220px;
  height: 220px;
  opacity: 1;
}

.flow-arrow {
  position: absolute;
  width: 16px;
  height: 16px;
  z-index: 9;
  color: var(--brand-color, #4f6ef7);
  transition:
    left 800ms cubic-bezier(0.34, 1.56, 0.64, 1),
    right 800ms cubic-bezier(0.34, 1.56, 0.64, 1),
    color 600ms cubic-bezier(0.23, 1, 0.32, 1);
  pointer-events: none;
}

.flow-arrow--left {
  left: -25%;
}

.flow-btn:hover:not(.is-disabled) .flow-arrow--left {
  left: 16px;
  color: #ffffff;
}

.flow-arrow--right {
  right: 16px;
}

.flow-btn:hover:not(.is-disabled) .flow-arrow--right {
  right: -25%;
  color: #ffffff;
}

/* 深色背景下可换用浅色描边/文字（预留修饰类） */
.flow-btn--inverted {
  border-color: rgba(255, 255, 255, 0.4);
  color: #ffffff;
}
</style>
