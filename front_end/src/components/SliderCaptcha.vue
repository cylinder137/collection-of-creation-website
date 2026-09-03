<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RefreshLeft } from '@element-plus/icons-vue'

/**
 * 拼图滑块人机验证（拖动图片填空）
 *
 * 纯前端自绘实现，无第三方依赖、无外部图片请求：
 * - 背景为程序化绘制的「图片」（品牌渐变 + 光斑 + 水印）
 * - 在随机位置挖一个缺口（半透明遮罩），拖动画块使其与缺口重合即通过
 * - 用于官网点击下载链接前的轻量人机验证
 */

const emit = defineEmits<{ success: [] }>()

const W = 320 // 画布逻辑宽度
const H = 180 // 画布逻辑高度
const PW = 44 // 拼块尺寸
const PH = 44

const canvasRef = ref<HTMLCanvasElement | null>(null)
const trackRef = ref<HTMLDivElement | null>(null)
const knobRef = ref<HTMLDivElement | null>(null)

const dragging = ref(false)
const success = ref(false)
const failCount = ref(0)
const value = ref(0) // 滑块 CSS 像素位置

let bgCanvas: HTMLCanvasElement | null = null
let targetX = 0 // 缺口目标位置（画布逻辑像素）
let targetY = 0
let startPointerX = 0
let startValue = 0

/* ---------- 程序化背景图片 ---------- */
function drawBackground(): HTMLCanvasElement {
  const c = document.createElement('canvas')
  c.width = W
  c.height = H
  const ctx = c.getContext('2d')!
  // 品牌渐变底
  const grad = ctx.createLinearGradient(0, 0, W, H)
  grad.addColorStop(0, '#3b5bfd')
  grad.addColorStop(0.5, '#6d5df6')
  grad.addColorStop(1, '#9a5cf0')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, W, H)
  // 光斑
  const spots: Array<[number, number, number, string]> = [
    [W * 0.22, H * 0.28, 90, 'rgba(255,255,255,0.18)'],
    [W * 0.78, H * 0.62, 120, 'rgba(255,255,255,0.12)'],
    [W * 0.6, H * 0.18, 60, 'rgba(255,214,102,0.25)'],
    [W * 0.12, H * 0.85, 70, 'rgba(255,214,102,0.15)'],
  ]
  for (const [x, y, r, color] of spots) {
    const g = ctx.createRadialGradient(x, y, 0, x, y, r)
    g.addColorStop(0, color)
    g.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fill()
  }
  // 装饰圆环
  ctx.strokeStyle = 'rgba(255,255,255,0.22)'
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.arc(W * 0.85, H * 0.22, 26, 0, Math.PI * 2)
  ctx.stroke()
  ctx.beginPath()
  ctx.arc(W * 0.12, H * 0.45, 16, 0, Math.PI * 2)
  ctx.stroke()
  // 水印文字
  ctx.fillStyle = 'rgba(255,255,255,0.5)'
  ctx.font = 'bold 26px system-ui, sans-serif'
  ctx.textAlign = 'right'
  ctx.textBaseline = 'bottom'
  ctx.fillText('ZAOWUJI', W - 14, H - 10)
  return c
}

/* ---------- 绘制一帧 ---------- */
function render() {
  const canvas = canvasRef.value
  if (!canvas || !bgCanvas) return
  const ctx = canvas.getContext('2d')!
  ctx.clearRect(0, 0, W, H)
  ctx.drawImage(bgCanvas, 0, 0, W, H)

  // 缺口：抠出区域做暗色遮罩 + 描边
  ctx.save()
  ctx.beginPath()
  roundedRect(ctx, targetX, targetY, PW, PH, 6)
  ctx.clip()
  ctx.fillStyle = 'rgba(10, 16, 40, 0.5)'
  ctx.fillRect(targetX, targetY, PW, PH)
  ctx.restore()
  ctx.save()
  ctx.strokeStyle = 'rgba(255,255,255,0.85)'
  ctx.lineWidth = 1.5
  ctx.beginPath()
  roundedRect(ctx, targetX, targetY, PW, PH, 6)
  ctx.stroke()
  ctx.restore()

  // 拼块：随滑块移动（内部坐标 = CSS 值 × 缩放比）
  const rect = canvas.getBoundingClientRect()
  const scale = W / (rect.width || W)
  const px = value.value * scale
  ctx.save()
  ctx.shadowColor = 'rgba(0,0,0,0.35)'
  ctx.shadowBlur = 8
  ctx.drawImage(bgCanvas, targetX, targetY, PW, PH, px, targetY, PW, PH)
  ctx.restore()
  ctx.save()
  ctx.strokeStyle = 'rgba(255,255,255,0.9)'
  ctx.lineWidth = 2
  ctx.beginPath()
  roundedRect(ctx, px, targetY, PW, PH, 6)
  ctx.stroke()
  ctx.restore()

  // 成功标记
  if (success.value) {
    ctx.fillStyle = 'rgba(34, 197, 94, 0.92)'
    ctx.font = 'bold 22px system-ui, sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('✓', targetX + PW / 2, targetY + PH / 2 + 2)
  }
}

function roundedRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  if (typeof ctx.roundRect === 'function') {
    ctx.roundRect(x, y, w, h, r)
  } else {
    ctx.rect(x, y, w, h)
  }
}

/* ---------- 重置 / 初始化 ---------- */
function reset() {
  success.value = false
  value.value = 0
  failCount.value = 0
  const margin = 10
  targetX = margin + Math.random() * (W - PW - margin * 2)
  targetY = 22 + Math.random() * (H - PH - 44)
  bgCanvas = drawBackground()
  render()
}

/* ---------- 拖拽 ---------- */
function onKnobDown(e: PointerEvent) {
  if (success.value || dragging.value) return
  dragging.value = true
  startPointerX = e.clientX
  startValue = value.value
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
}

function onKnobMove(e: PointerEvent) {
  if (!dragging.value || success.value) return
  const track = trackRef.value
  const knob = knobRef.value
  if (!track || !knob) return
  const max = track.clientWidth - knob.clientWidth
  const dx = e.clientX - startPointerX
  value.value = Math.min(max, Math.max(0, startValue + dx))
  render()
}

function onKnobUp() {
  if (!dragging.value) return
  dragging.value = false
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const scale = W / (rect.width || W)
  const px = value.value * scale
  if (Math.abs(px - targetX) <= 5) {
    success.value = true
    render()
    setTimeout(() => emit('success'), 350)
  } else {
    failCount.value += 1
    value.value = 0
    render()
  }
}

onMounted(() => {
  reset()
})
</script>

<template>
  <div class="slider-captcha" :class="{ 'is-shake': failCount > 0 && !success }" @animationend="failCount = 0">
    <div class="slider-captcha__img">
      <canvas ref="canvasRef" :width="W" :height="H" />
      <button
        class="slider-captcha__refresh"
        type="button"
        title="换一张"
        :disabled="success"
        @click="reset"
      >
        <el-icon><RefreshLeft /></el-icon>
      </button>
      <div v-if="success" class="slider-captcha__tip slider-captcha__tip--ok">验证通过 ✓</div>
    </div>

    <p class="slider-captcha__hint">按住下方滑块，向右拖动到图片缺口位置</p>

    <div ref="trackRef" class="slider-captcha__track">
      <div class="slider-captcha__groove">
        <span class="slider-captcha__groove-text">{{ success ? '验证通过' : '拖动滑块完成拼图' }}</span>
        <div
          ref="knobRef"
          class="slider-captcha__knob"
          :class="{ 'is-success': success }"
          :style="{ transform: `translateX(${value}px)` }"
          @pointerdown="onKnobDown"
          @pointermove="onKnobMove"
          @pointerup="onKnobUp"
          @pointercancel="onKnobUp"
        >
          <span v-if="!success" class="slider-captcha__arrows">»</span>
          <span v-else class="slider-captcha__check">✓</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.slider-captcha {
  user-select: none;
  -webkit-user-select: none;
}

.slider-captcha__img {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
}

.slider-captcha__img canvas {
  display: block;
  width: 100%;
  height: auto;
}

.slider-captcha__refresh {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.88);
  color: #4f6ef7;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.15);
}

.slider-captcha__refresh:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.slider-captcha__tip {
  position: absolute;
  left: 12px;
  top: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(34, 197, 94, 0.92);
  color: #fff;
}

.slider-captcha__hint {
  margin: 10px 2px 8px;
  font-size: 12.5px;
  color: #8a93a6;
}

.slider-captcha__track {
  padding: 2px 0;
}

.slider-captcha__groove {
  position: relative;
  height: 42px;
  border-radius: 999px;
  background: #eef0f6;
  border: 1px solid #e2e5ef;
  overflow: hidden;
}

.slider-captcha__groove-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #a0a8ba;
  pointer-events: none;
}

.slider-captcha__knob {
  position: absolute;
  left: 0;
  top: 0;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f6ef7, #7c5cf0);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  box-shadow: 0 3px 10px rgba(79, 110, 247, 0.4);
  touch-action: none;
}

.slider-captcha__knob:active {
  cursor: grabbing;
}

.slider-captcha__knob.is-success {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}

.slider-captcha__arrows {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -2px;
}

.slider-captcha__check {
  font-size: 17px;
  font-weight: 700;
}

/* 验证失败抖动提醒 */
.is-shake .slider-captcha__img {
  animation: slider-shake 0.3s;
}

@keyframes slider-shake {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-6px);
  }
  75% {
    transform: translateX(6px);
  }
}
</style>
