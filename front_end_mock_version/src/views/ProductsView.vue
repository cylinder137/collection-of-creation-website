<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores'
import { productApi } from '@/api'
import ProductCard from '@/components/ProductCard.vue'

const router = useRouter()
const store = useAppStore()

onMounted(async () => {
  try {
    store.setProducts(await productApi.list())
  } catch {
    // 后端未就绪：页面直接展示内置演示数据
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

function goBuy(productId: number) {
  router.push(`/purchase/${productId}`)
}
</script>

<template>
  <div class="page-container">
    <h2 class="section-title">产品中心</h2>
    <p class="section-subtitle">选择适合你的创造工具</p>
    <el-row :gutter="24">
      <el-col v-for="p in store.products" :key="p.id" :xs="24" :sm="12" :md="8">
        <ProductCard :product="p" @buy="goBuy(p.id)" />
      </el-col>
    </el-row>
  </div>
</template>
