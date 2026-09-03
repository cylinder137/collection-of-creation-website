<script setup lang="ts">
import type { Product } from '@/types'

defineProps<{ product: Product }>()
defineEmits<{ buy: [] }>()
</script>

<template>
  <el-card shadow="hover" class="product-card">
    <div class="cover" :style="{ background: `linear-gradient(135deg, #4f6ef7, #7c5cf7)` }">
      <el-icon :size="40" color="#fff"><Box /></el-icon>
    </div>
    <div class="body">
      <h3 class="name">{{ product.name }}</h3>
      <p class="slogan">{{ product.slogan }}</p>
      <p class="desc">{{ product.description }}</p>
      <div class="tags">
        <el-tag v-for="t in product.tags ?? []" :key="t" size="small" type="primary" effect="plain">
          {{ t }}
        </el-tag>
      </div>
      <div class="footer">
        <span class="price">
          <span class="symbol">¥</span>{{ product.price }}
        </span>
        <el-button type="primary" round @click="$emit('buy')">立即购买</el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.product-card {
  margin-bottom: 16px;
  border-radius: 12px;
  overflow: hidden;
}

.cover {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.body {
  padding: 16px 4px 8px;
}

.name {
  font-size: 18px;
  font-weight: 600;
}

.slogan {
  color: var(--brand-color);
  font-size: 13px;
  margin: 4px 0 8px;
}

.desc {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  min-height: 44px;
}

.tags {
  margin: 10px 0 14px;
  display: flex;
  gap: 6px;
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  color: #f56c6c;
  font-size: 22px;
  font-weight: 700;
}

.symbol {
  font-size: 14px;
}
</style>
