<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const navItems = [
  { path: '/', label: '首页' },
  { path: '/products', label: '产品中心' },
  { path: '/activation', label: '激活码' },
  { path: '/admin', label: '管理后台' },
]

async function onUserCommand(command: string) {
  if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    // 当前在需要登录的页面时退出后回首页（守卫也会拦截，这里主动跳转更顺滑）
    if (route.meta.requiresAuth) {
      router.push('/')
    }
  } else if (command === 'admin') {
    router.push('/admin')
  }
}
</script>

<template>
  <el-container class="layout">
    <el-header class="layout-header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <el-icon :size="22"><MagicStick /></el-icon>
          <span class="logo-text">造物集</span>
        </router-link>
        <nav class="nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: route.path === item.path }"
          >
            {{ item.label }}
          </router-link>
        </nav>
        <div class="header-actions">
          <template v-if="auth.isLoggedIn">
            <el-dropdown trigger="click" @command="onUserCommand">
              <span class="user-entry">
                <el-icon><User /></el-icon>
                <span>{{ auth.userInfo?.nickname || auth.userInfo?.username }}</span>
                <el-icon :size="12"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin">管理后台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button round @click="$router.push('/login')">登录</el-button>
          </template>
          <el-button type="primary" round @click="$router.push('/products')">
            立即购买
          </el-button>
        </div>
      </div>
    </el-header>

    <el-main class="layout-main">
      <router-view />
    </el-main>

    <el-footer class="layout-footer">
      <p>© 2026 大连造物集有限公司 · 造物集官网</p>
      <p class="footer-sub">让每个创造都被认真对待</p>
    </el-footer>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.layout-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 40px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--brand-color);
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}

.nav {
  display: flex;
  gap: 28px;
  flex: 1;
}

.nav-item {
  font-size: 15px;
  color: var(--text-secondary);
  padding: 4px 2px;
  transition: color 0.2s;
}

.nav-item:hover,
.nav-item.active {
  color: var(--brand-color);
  font-weight: 600;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-main);
  cursor: pointer;
  padding: 6px 4px;
  outline: none;
}

.user-entry:hover {
  color: var(--brand-color);
}

.layout-main {
  padding: 0;
  background: var(--bg-page);
}

.layout-footer {
  height: auto;
  padding: 32px 20px;
  background: #1f2329;
  color: rgba(255, 255, 255, 0.75);
  text-align: center;
  line-height: 1.8;
}

.footer-sub {
  font-size: 13px;
  opacity: 0.6;
}
</style>
