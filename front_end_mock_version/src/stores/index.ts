import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { Product } from '@/types'

/**
 * 全局状态示例：产品列表缓存 + 当前选中的产品。
 * 后续可扩展：用户信息、购物车、激活记录等。
 */
export const useAppStore = defineStore('app', () => {
  const products = ref<Product[]>([])
  const currentProduct = ref<Product | null>(null)
  const loading = ref(false)

  function setProducts(list: Product[]) {
    products.value = list
  }

  function setCurrentProduct(product: Product | null) {
    currentProduct.value = product
  }

  return {
    products,
    currentProduct,
    loading,
    setProducts,
    setCurrentProduct,
  }
})
