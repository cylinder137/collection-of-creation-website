<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { ChatDotRound, Close, Promotion } from '@element-plus/icons-vue'
import { sendAiChat, type AiChatMsg } from '../api/ai'

/**
 * 官网悬浮 AI 客服（小造）
 *
 * - 挂在 OfficialLayout 中，仅官网访问者可见（管理后台不展示）
 * - 消息走 POST /api/ai/chat，密钥由后端代理持有，前端零密钥
 */

interface Msg {
  role: 'user' | 'assistant'
  content: string
  error?: boolean
}

const GREETING =
  '你好呀，我是造物集 AI 客服「小造」👋\n关于产品下载、安装激活、购买流程等问题都可以问我；需要人工处理的事项我会帮你转人工～'

const quickReplies = ['怎么下载产品？', '怎么购买激活码？', '激活码怎么使用？', '想联系人工客服']

const open = ref(false)
const loading = ref(false)
const input = ref('')
const list = ref<Msg[]>([{ role: 'assistant', content: GREETING }])
const scrollEl = ref<HTMLElement | null>(null)

const showQuick = () => list.value.length === 1 && !loading.value

function scrollToBottom() {
  nextTick(() => {
    const el = scrollEl.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

watch([() => list.value.length, loading], scrollToBottom)

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''
  list.value.push({ role: 'user', content: text })

  loading.value = true
  try {
    // 传历史上下文（不含 system，后端会统一拼接系统提示词）
    const history: AiChatMsg[] = list.value.map((m) => ({ role: m.role, content: m.content }))
    const reply = await sendAiChat(history)
    list.value.push({ role: 'assistant', content: reply })
  } catch {
    list.value.push({
      role: 'assistant',
      content: '抱歉，AI 客服暂时不可用，请稍后重试；紧急问题可联系人工客服。',
      error: true,
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function quick(text: string) {
  input.value = text
  send()
}
</script>

<template>
  <div class="ai-chat">
    <!-- 悬浮入口 -->
    <transition name="pop">
      <button v-if="!open" class="ai-chat__fab" type="button" aria-label="打开 AI 客服" @click="open = true">
        <el-icon :size="22"><ChatDotRound /></el-icon>
        <span class="ai-chat__fab-badge">AI</span>
      </button>
    </transition>

    <!-- 对话面板 -->
    <transition name="slide">
      <div v-if="open" class="ai-chat__panel">
        <header class="ai-chat__header">
          <div class="ai-chat__header-info">
            <div class="ai-chat__avatar">
              <el-icon :size="17"><ChatDotRound /></el-icon>
            </div>
            <div>
              <div class="ai-chat__title">造物集 AI 客服</div>
              <div class="ai-chat__status"><span class="ai-chat__dot" />小造在线 · DeepSeek 驱动</div>
            </div>
          </div>
          <button class="ai-chat__close" type="button" aria-label="关闭客服" @click="open = false">
            <el-icon><Close /></el-icon>
          </button>
        </header>

        <div ref="scrollEl" class="ai-chat__body">
          <div v-for="(m, i) in list" :key="i" class="ai-chat__row" :class="m.role === 'user' ? 'ai-chat__row--user' : 'ai-chat__row--ai'">
            <div class="ai-chat__bubble" :class="{ 'ai-chat__bubble--error': m.error }">{{ m.content }}</div>
          </div>

          <div v-if="loading" class="ai-chat__row ai-chat__row--ai">
            <div class="ai-chat__bubble ai-chat__typing">
              <span class="ai-chat__typing-dot" />
              <span class="ai-chat__typing-dot" />
              <span class="ai-chat__typing-dot" />
            </div>
          </div>

          <div v-if="showQuick" class="ai-chat__quick">
            <button v-for="q in quickReplies" :key="q" type="button" @click="quick(q)">{{ q }}</button>
          </div>
        </div>

        <footer class="ai-chat__footer">
          <el-input
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            :maxlength="500"
            resize="none"
            placeholder="输入你的问题，Enter 发送"
            @keydown.enter.exact.prevent="send"
          />
          <el-button
            class="ai-chat__send"
            type="primary"
            circle
            :disabled="loading || !input.trim()"
            aria-label="发送"
            @click="send"
          >
            <el-icon><Promotion /></el-icon>
          </el-button>
        </footer>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.ai-chat {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 999;
  font-family: inherit;
}

/* ---------- 悬浮按钮 ---------- */
.ai-chat__fab {
  position: relative;
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f6ef7, #7c5cf0);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(79, 110, 247, 0.38);
  transition: transform 0.18s, box-shadow 0.18s;
}

.ai-chat__fab:hover {
  transform: translateY(-2px) scale(1.04);
  box-shadow: 0 10px 28px rgba(79, 110, 247, 0.5);
}

.ai-chat__fab-badge {
  position: absolute;
  top: -2px;
  right: -4px;
  background: #ff5b5b;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  padding: 4px 6px;
  border-radius: 10px;
  letter-spacing: 0.02em;
}

/* ---------- 对话面板 ---------- */
.ai-chat__panel {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 360px;
  max-width: calc(100vw - 24px);
  height: 520px;
  max-height: calc(100vh - 48px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(15, 23, 42, 0.22);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.ai-chat__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: linear-gradient(135deg, #4f6ef7, #7c5cf0);
  color: #fff;
  flex-shrink: 0;
}

.ai-chat__header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-chat__avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-chat__title {
  font-size: 15px;
  font-weight: 700;
}

.ai-chat__status {
  font-size: 11px;
  opacity: 0.86;
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 2px;
}

.ai-chat__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 0 2px rgba(74, 222, 128, 0.3);
}

.ai-chat__close {
  border: none;
  background: transparent;
  color: #fff;
  cursor: pointer;
  opacity: 0.85;
  display: flex;
  padding: 4px;
  border-radius: 8px;
}

.ai-chat__close:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.16);
}

/* ---------- 消息区 ---------- */
.ai-chat__body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px;
  background: #f6f7fb;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-chat__row {
  display: flex;
}

.ai-chat__row--ai {
  justify-content: flex-start;
}

.ai-chat__row--user {
  justify-content: flex-end;
}

.ai-chat__bubble {
  max-width: 82%;
  padding: 9px 13px;
  border-radius: 14px;
  font-size: 13.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-chat__row--ai .ai-chat__bubble {
  background: #fff;
  color: #1f2430;
  border: 1px solid #eceef5;
  border-bottom-left-radius: 4px;
}

.ai-chat__row--user .ai-chat__bubble {
  background: linear-gradient(135deg, #4f6ef7, #6a5cf5);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-chat__bubble--error {
  color: #b45309 !important;
  background: #fffbeb !important;
  border-color: #fde68a !important;
}

/* 输入中动画 */
.ai-chat__typing {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 13px 15px !important;
}

.ai-chat__typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #aab2c8;
  animation: ai-blink 1.2s infinite ease-in-out;
}

.ai-chat__typing-dot:nth-child(2) {
  animation-delay: 0.18s;
}

.ai-chat__typing-dot:nth-child(3) {
  animation-delay: 0.36s;
}

@keyframes ai-blink {
  0%,
  70%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  35% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

/* 快捷提问 */
.ai-chat__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 2px 0 4px;
}

.ai-chat__quick button {
  border: 1px solid #dde1f5;
  background: #fff;
  color: #4f6ef7;
  font-size: 12.5px;
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s;
}

.ai-chat__quick button:hover {
  background: #4f6ef7;
  color: #fff;
  border-color: #4f6ef7;
}

/* ---------- 输入区 ---------- */
.ai-chat__footer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #eceef5;
  background: #fff;
  flex-shrink: 0;
}

.ai-chat__footer :deep(.el-textarea__inner) {
  border-radius: 10px;
  font-size: 13.5px;
  padding: 8px 10px;
  line-height: 1.5;
  box-shadow: none;
}

.ai-chat__send {
  flex-shrink: 0;
  margin-bottom: 1px;
}

/* ---------- 动效 ---------- */
.pop-enter-active,
.pop-leave-active {
  transition: opacity 0.18s, transform 0.18s;
}

.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: scale(0.7);
}

.slide-enter-active,
.slide-leave-active {
  transition: opacity 0.2s, transform 0.24s;
  transform-origin: bottom right;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.94);
}

@media (max-width: 480px) {
  .ai-chat {
    right: 14px;
    bottom: 14px;
  }
}
</style>
