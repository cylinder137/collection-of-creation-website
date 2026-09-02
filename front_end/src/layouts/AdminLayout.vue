<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAdminStore } from '@/stores'

/**
 * 管理后台布局
 *
 * ⚠️ 该布局与官网完全隔离，官网任何页面都不存在进入此处的链接。
 * 进入方式只有一种：管理员手动访问 http://<域名>/admin。
 */
const adminStore = useAdminStore()
const router = useRouter()
const checking = ref(true)

onMounted(async () => {
  try {
    await adminStore.fetchProfile()
  } catch {
    // 令牌失效：交给 http 拦截器跳登录页
    adminStore.logout()
    router.replace('/admin')
  } finally {
    checking.value = false
  }
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出管理后台？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  adminStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/admin')
}

function backToSite() {
  window.location.href = '/'
}
</script>

<template>
  <div class="admin-layout">
    <header class="admin-header">
      <div class="admin-header-inner">
        <div class="admin-brand">
          <span class="admin-brand-mark">造</span>
          <div>
            <div class="admin-brand-title">造物集管理后台</div>
            <div class="admin-brand-sub">Collection of Creation · Console</div>
          </div>
        </div>

        <div class="admin-header-right">
          <el-tag v-if="adminStore.profile" type="info" effect="plain" round>
            {{ adminStore.isSuperAdmin ? '超级管理员' : '普通管理员' }}
          </el-tag>
          <span v-if="adminStore.profile" class="admin-user">
            {{ adminStore.profile.nickname }}
          </span>
          <el-button link @click="backToSite">
            <el-icon class="mr-1"><HomeFilled /></el-icon>
            返回官网
          </el-button>
          <el-button link type="danger" @click="handleLogout">
            <el-icon class="mr-1"><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </div>
    </header>

    <main class="admin-main">
      <el-skeleton v-if="checking" :rows="6" animated />
      <router-view v-else />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f4f5f9;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: 60px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
}

.admin-header-inner {
  max-width: 1280px;
  height: 60px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.admin-brand-title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
}

.admin-brand-sub {
  font-size: 11.5px;
  color: var(--text-tertiary);
}

.admin-header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.admin-user {
  font-size: 14px;
  font-weight: 600;
}

.admin-main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
}

.mr-1 {
  margin-right: 4px;
}
</style>
