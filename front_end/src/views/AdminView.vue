<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api'
import type { Order } from '@/types'

const orders = ref<Order[]>([])
const loading = ref(false)
const passing = ref<string | null>(null)

const activeTab = ref<'pending' | 'all'>('pending')

const pendingOrders = computed(() => orders.value.filter((o) => o.status === 0))
const shownOrders = computed(() =>
  activeTab.value === 'pending' ? pendingOrders.value : orders.value,
)

async function loadOrders() {
  loading.value = true
  try {
    orders.value = await orderApi.list()
  } catch (e: any) {
    ElMessage.error(e?.message || '订单列表加载失败')
  } finally {
    loading.value = false
  }
}

/** 一键通过（人工核验收款成功） */
async function pass(order: Order) {
  try {
    await ElMessageBox.confirm(
      `确认已收到订单 ${order.orderNo} 的款项（¥${order.amount}）？通过后用户即可激活。`,
      '人工核验通过',
      { type: 'warning', confirmButtonText: '确认收款，通过', cancelButtonText: '取消' },
    )
  } catch {
    return // 用户取消
  }
  passing.value = order.orderNo
  try {
    await orderApi.reviewPass(order.orderNo)
    ElMessage.success(`订单 ${order.orderNo} 已通过审核`)
    await loadOrders()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    passing.value = null
  }
}

function fmtTime(t?: string | null) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}

/** 订单状态：0待支付(待人工核验) 1已支付 2已取消 3已退款 4已签发 */
function statusOf(status: number) {
  switch (status) {
    case 0:
      return { type: 'warning' as const, text: '待人工核验' }
    case 1:
      return { type: 'success' as const, text: '已支付' }
    case 2:
      return { type: 'info' as const, text: '已取消' }
    case 3:
      return { type: 'danger' as const, text: '已退款' }
    case 4:
      return { type: 'primary' as const, text: '已签发' }
    default:
      return { type: 'info' as const, text: `未知(${status})` }
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-container">
    <h2 class="section-title">管理后台</h2>
    <p class="section-subtitle">
      人工核验支付：核对收款后一键通过，用户即可在激活页签发激活码
    </p>

    <el-tabs v-model="activeTab">
      <el-tab-pane name="pending">
        <template #label>
          待审核
          <el-badge
            v-if="pendingOrders.length"
            :value="pendingOrders.length"
            class="tab-badge"
          />
        </template>
        <el-card shadow="never">
          <el-table v-loading="loading" :data="shownOrders" empty-text="暂无待审核订单">
            <el-table-column prop="orderNo" label="订单号" min-width="190" />
            <el-table-column prop="productName" label="产品" min-width="110" />
            <el-table-column label="金额" width="100">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusOf(row.status).type">{{ statusOf(row.status).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="下单时间" width="170">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 0"
                  type="success"
                  size="small"
                  :loading="passing === row.orderNo"
                  @click="pass(row)"
                >
                  一键通过
                </el-button>
                <span v-else class="done-text">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="全部订单" name="all">
        <el-card shadow="never">
          <el-table v-loading="loading" :data="shownOrders" empty-text="暂无订单">
            <el-table-column prop="orderNo" label="订单号" min-width="190" />
            <el-table-column prop="productName" label="产品" min-width="110" />
            <el-table-column label="金额" width="100">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusOf(row.status).type">{{ statusOf(row.status).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="付款时间" width="170">
              <template #default="{ row }">{{ fmtTime(row.paidAt) }}</template>
            </el-table-column>
            <el-table-column label="下单时间" width="170">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 0"
                  type="success"
                  size="small"
                  :loading="passing === row.orderNo"
                  @click="pass(row)"
                >
                  一键通过
                </el-button>
                <span v-else class="done-text">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-alert
      class="mt"
      title="提示"
      description="当前为人工核验模式（微信支付 API 尚未开通）：用户扫码转账后，管理员在此确认收款并点击「一键通过」，订单即变为已支付，用户可前往激活页签发激活码。"
      type="info"
      show-icon
      :closable="false"
    />
  </div>
</template>

<style scoped>
.section-subtitle {
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.tab-badge {
  margin-left: 6px;
}

.mt {
  margin-top: 16px;
}

.done-text {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
