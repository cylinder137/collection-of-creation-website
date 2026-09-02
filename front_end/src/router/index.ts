import { createRouter, createWebHistory } from 'vue-router'
import { getAdminToken } from '@/api/http'

const router = createRouter({
  /**
   * history 模式：官网与管理后台共用同一站点
   * - /                 官网主页（产品展示 + 安装包下载）
   * - /admin            管理员登录（官网任何位置均无入口，只能靠管理员手输 URL 直达）
   * - /admin/dashboard  管理后台（需登录）
   */
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/OfficialLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { title: '造物集 · 创造者的集合' },
        },
      ],
    },
    {
      path: '/admin',
      name: 'admin-login',
      component: () => import('@/views/AdminLoginView.vue'),
      meta: { title: '管理员登录 · 造物集' },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/AdminDashboardView.vue'),
          meta: { title: '管理后台 · 造物集' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

// 路由守卫：管理后台需持令牌；已登录时访问登录页直接进后台
router.beforeEach((to) => {
  const token = getAdminToken()

  if (to.matched.some((r) => r.meta.requiresAuth)) {
    if (!token) {
      return { path: '/admin' }
    }
    return true
  }

  if (to.name === 'admin-login' && token) {
    return { path: '/admin/dashboard' }
  }
  return true
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ?? '造物集 · 创造者的集合'
})

export default router
