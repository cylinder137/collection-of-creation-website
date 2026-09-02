import { createRouter, createWebHashHistory } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const router = createRouter({
  // 使用 hash 模式：静态服务器/内网穿透场景无需额外配置 history fallback
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      component: DefaultLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'products',
          name: 'products',
          component: () => import('@/views/ProductsView.vue'),
          meta: { title: '产品中心' },
        },
        {
          path: 'purchase/:productId?',
          name: 'purchase',
          component: () => import('@/views/PurchaseView.vue'),
          meta: { title: '购买' },
        },
        {
          path: 'activation',
          name: 'activation',
          component: () => import('@/views/ActivationView.vue'),
          meta: { title: '激活码' },
        },
        {
          path: 'admin',
          name: 'admin',
          component: () => import('@/views/AdminView.vue'),
          meta: { title: '管理后台', requiresAuth: true },
        },
      ],
    },
    {
      // 登录页：独立全屏页面，不套 DefaultLayout
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', public: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

// 导航守卫：requiresAuth 页面需登录，未登录跳 /login 并带 redirect；已登录访问登录页回首页
router.beforeEach((to) => {
  const token = localStorage.getItem('coc_token')
  if (to.meta.requiresAuth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && token) {
    return { path: '/' }
  }
  return true
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · 造物集` : '造物集 · 创造者的集合'
})

export default router
