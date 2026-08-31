<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { orderApi } from '@/api'
import type { Order } from '@/types'

const orders = ref<Order[]>([])
const loading = ref(false)

const tabs = [
  { name: 'orders', label: '订单管理' },
  { name: 'codes', label: '激活码管理' },
]
const activeTab = ref('orders')

async function loadOrders() {
  loading.value = true
  try {
    // TODO: 后端提供订单列表接口后替换
    orders.value = [
      {
        id: 1,
        orderNo: 'D202608270001',
        productId: 1,
        productName: 'coBrain',
        amount: 99,
        status: 'PAID',
        createdAt: '2026-08-27 14:00:00',
      },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)

const statusMap: Record<Order['status'], { text: string; type: 'warning' | 'success' | 'info' }> = {
  PENDING: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'success' },
  CANCELLED: { text: '已取消', type: 'info' },
}
</script>

<template>
  <div class="page-container">
    <h2 class="section-title">管理后台</h2>
    <p class="section-subtitle">订单与激活记录管理（需后端鉴权后开放）</p>

    <el-tabs v-model="activeTab">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name">
        <el-card shadow="never" v-if="t.name === 'orders'">
          <el-table v-loading="loading" :data="orders" empty-text="暂无订单">
            <el-table-column prop="orderNo" label="订单号" min-width="160" />
            <el-table-column prop="productName" label="产品" min-width="120" />
            <el-table-column prop="amount" label="金额" width="100">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusMap[row.status].type">{{ statusMap[row.status].text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="下单时间" width="170" />
          </el-table>
        </el-card>

        <el-card shadow="never" v-else>
          <el-alert
            title="激活码管理（占位）"
            description="后端接口就绪后，在此展示全部激活码与绑定机器码，支持禁用/重新签发操作。"
            type="info"
            show-icon
            :closable="false"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
