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
          meta: { title: '管理后台' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · 造物集` : '造物集 · 创造者的集合'
})

export default router
