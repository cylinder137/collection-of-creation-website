<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import AiChatWidget from '../components/AiChatWidget.vue'

/**
 * 官网布局：顶栏 + 内容 + 页脚
 *
 * ⚠️ 安全约定：本布局（以及全站任何页面）不得出现指向 /admin 的链接、按钮或入口。
 * 管理后台仅靠管理员手动输入 http://<域名>/admin 进入。
 */

const navItems = [
  { target: '#products', label: '产品下载' },
  { target: '#guide', label: '安装与激活' },
  { target: '#faq', label: '常见问题' },
  { target: '#about', label: '关于我们' },
]

const activeAnchor = ref('#products')
const scrolled = ref(false)

function scrollTo(target: string) {
  const el = document.querySelector(target)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

let observer: IntersectionObserver | null = null

onMounted(() => {
  const sections = navItems
    .map((item) => document.querySelector(item.target))
    .filter((el): el is Element => el !== null)

  observer = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((e) => e.isIntersecting)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0]
      if (visible?.target.id) {
        activeAnchor.value = `#${visible.target.id}`
      }
    },
    { rootMargin: '-72px 0px -60% 0px', threshold: [0.1, 0.4, 0.75] },
  )
  sections.forEach((el) => observer?.observe(el))

  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => {
  observer?.disconnect()
  window.removeEventListener('scroll', onScroll)
})

function onScroll() {
  scrolled.value = window.scrollY > 8
}
</script>

<template>
  <div class="official">
    <header class="header" :class="{ 'header--scrolled': scrolled }">
      <div class="container header-inner">
        <a class="logo" href="/" @click.prevent="scrollTo('#top')">
          <span class="logo-mark">造</span>
          <span class="logo-text">造物集</span>
        </a>

        <nav class="nav">
          <a
            v-for="item in navItems"
            :key="item.target"
            class="nav-item"
            :class="{ 'nav-item--active': activeAnchor === item.target }"
            :href="item.target"
            @click.prevent="scrollTo(item.target)"
          >
            {{ item.label }}
          </a>
        </nav>

        <div class="header-actions">
          <el-button type="primary" round @click="scrollTo('#products')">
            <el-icon class="mr-1"><Download /></el-icon>
            下载客户端
          </el-button>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">
      <div class="container footer-inner">
        <div class="footer-brand">
          <div class="footer-logo">
            <span class="logo-mark">造</span>
            <span class="logo-text">造物集</span>
          </div>
          <p class="footer-slogan">造物集 · 创造者的集合</p>
          <p class="footer-sub">让每一次创造，都从趁手的工具开始。</p>
        </div>

        <div class="footer-links">
          <div class="footer-col">
            <h4>产品</h4>
            <a href="#products" @click.prevent="scrollTo('#products')">产品下载</a>
            <a href="#guide" @click.prevent="scrollTo('#guide')">安装与激活</a>
          </div>
          <div class="footer-col">
            <h4>帮助</h4>
            <a href="#faq" @click.prevent="scrollTo('#faq')">常见问题</a>
            <a href="#about" @click.prevent="scrollTo('#about')">关于我们</a>
          </div>
        </div>
      </div>

      <div class="container footer-bottom">
        <span>© 2026 大连造物集有限公司</span>
        <span class="footer-icp">辽ICP备 0000000 号</span>
      </div>
    </footer>

    <!-- 官网悬浮 AI 客服（小造 · DeepSeek） -->
    <AiChatWidget />
  </div>
</template>

<style scoped>
.official {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ---------- 顶栏 ---------- */

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: var(--header-height);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid transparent;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.header--scrolled {
  border-bottom-color: var(--border-color);
  box-shadow: var(--shadow-sm);
}

.header-inner {
  height: var(--header-height);
  display: flex;
  align-items: center;
  gap: 40px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(79, 110, 247, 0.32);
}

.logo-text {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-main);
}

.nav {
  display: flex;
  gap: 30px;
  flex: 1;
}

.nav-item {
  position: relative;
  font-size: 15px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 6px 0;
  transition: color 0.18s;
}

.nav-item:hover {
  color: var(--text-main);
}

.nav-item--active {
  color: var(--brand-color);
  font-weight: 600;
}

.nav-item--active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -2px;
  transform: translateX(-50%);
  width: 18px;
  height: 2px;
  border-radius: 2px;
  background: var(--brand-color);
}

.header-actions {
  flex-shrink: 0;
}

.mr-1 {
  margin-right: 4px;
}

.main {
  flex: 1;
}

/* ---------- 页脚 ---------- */

.footer {
  background: #14161c;
  color: rgba(255, 255, 255, 0.62);
  padding-top: 56px;
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  gap: 48px;
  flex-wrap: wrap;
  padding-bottom: 40px;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.footer-logo .logo-text {
  color: #fff;
}

.footer-slogan {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 500;
}

.footer-sub {
  font-size: 13px;
  margin-top: 4px;
}

.footer-links {
  display: flex;
  gap: 72px;
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-col h4 {
  font-size: 14px;
  color: #fff;
  font-weight: 600;
  margin-bottom: 4px;
}

.footer-col a {
  font-size: 14px;
  cursor: pointer;
  transition: color 0.18s;
}

.footer-col a:hover {
  color: #fff;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 13px;
}

@media (max-width: 860px) {
  .nav {
    display: none;
  }

  .header-inner {
    justify-content: space-between;
  }

  .footer-links {
    gap: 48px;
  }
}
</style>
