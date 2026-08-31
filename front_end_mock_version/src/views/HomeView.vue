<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores'
import { productApi } from '@/api'
import type { Product } from '@/types'
import ProductCard from '@/components/ProductCard.vue'

const router = useRouter()
const store = useAppStore()

const features = [
  { icon: 'Key', title: '激活码秒发', desc: '购买后系统自动签发激活码，绑定机器码，安全可靠' },
  { icon: 'Wallet', title: '微信支付', desc: '官方收款，订单状态实时可查' },
  { icon: 'Monitor', title: '跨平台支持', desc: 'Windows / macOS 全平台适用' },
]

onMounted(async () => {
  try {
    const list = await productApi.list()
    store.setProducts(list)
  } catch {
    // 后端未就绪时使用内置演示数据，保证页面可用
    store.setProducts([
      {
        id: 1,
        name: 'coBrain',
        slogan: '白板笔记编辑器',
        description: '思维可视化与知识整理的一体化白板笔记工具。',
        price: 99,
        platforms: ['Windows', 'macOS'],
        tags: ['效率', '笔记'],
      },
      {
        id: 2,
        name: 'coBrain Pro',
        slogan: '白板笔记 · 专业版',
        description: '在标准版基础上提供云端同步、多人协作与更多模板。',
        price: 199,
        platforms: ['Windows', 'macOS'],
        tags: ['效率', '协作'],
      },
    ])
  }
})

function goBuy(product: Product) {
  store.setCurrentProduct(product)
  router.push(`/purchase/${product.id}`)
}
</script>

<template>
  <div>
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">造物集 · 创造者的集合</h1>
        <p class="hero-desc">大连造物集有限公司，致力于打造好用、可靠的生产力软件。<br />从 coBrain 开始，让每一次创造都被认真对待。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="$router.push('/products')">
            浏览产品
          </el-button>
          <el-button size="large" round @click="$router.push('/activation')">
            激活码入口
          </el-button>
        </div>
      </div>
    </section>

    <!-- 特性 -->
    <section class="page-container">
      <h2 class="section-title">为什么选择造物集</h2>
      <p class="section-subtitle">简单、可靠、值得信赖</p>
      <el-row :gutter="24">
        <el-col v-for="f in features" :key="f.title" :xs="24" :sm="8">
          <el-card shadow="hover" class="feature-card">
            <el-icon :size="32" color="var(--brand-color)"><component :is="f.icon" /></el-icon>
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 产品 -->
    <section class="page-container">
      <h2 class="section-title">热门产品</h2>
      <p class="section-subtitle">为创造者打造的工具集合</p>
      <el-row :gutter="24">
        <el-col v-for="p in store.products" :key="p.id" :xs="24" :sm="12" :md="8">
          <ProductCard :product="p" @buy="goBuy(p)" />
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<style scoped>
.hero {
  background: linear-gradient(135deg, #4f6ef7 0%, #7c5cf7 100%);
  color: #fff;
  padding: 96px 20px;
  text-align: center;
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 16px;
}

.hero-desc {
  font-size: 17px;
  line-height: 1.9;
  opacity: 0.92;
  margin-bottom: 32px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.feature-card {
  text-align: center;
  margin-bottom: 16px;
}

.feature-card h3 {
  margin: 12px 0 8px;
  font-size: 17px;
}

.feature-card p {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}
</style>
