<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores'
import { activationApi, orderApi } from '@/api'
import { getMachineCode, getStorageMode, setupSharedFolder } from '@/utils/device'
import type { ActivationCode, Order } from '@/types'

const store = useAppStore()
const records = ref<ActivationCode[]>([])
const activating = ref(false)
const machineCode = ref('')
const storageMode = ref<'shared' | 'local' | 'none'>('none')
const authorizing = ref(false)
const orderInfo = ref<Order | null>(null)
const orderLoading = ref(false)

const form = reactive({
  productId: 1,
  // 机器码由前端自动获取，无需用户填写
  orderNo: sessionStorage.getItem('zwj_last_order_no') ?? '',
})

const formRef = ref()

async function loadOrderInfo() {
  if (!form.orderNo) {
    orderInfo.value = null
    return
  }
  orderLoading.value = true
  try {
    orderInfo.value = await orderApi.detail(form.orderNo)
  } catch {
    orderInfo.value = null
  } finally {
    orderLoading.value = false
  }
}

/** 订单状态：0待人工核验 1已支付 2已取消 3已退款 4已签发 */
function orderStatusOf(status: number) {
  switch (status) {
    case 0:
      return { type: 'warning' as const, text: '待人工核验（管理员确认收款后可激活）' }
    case 1:
      return { type: 'success' as const, text: '已支付，可以激活' }
    case 2:
      return { type: 'info' as const, text: '已取消' }
    case 3:
      return { type: 'danger' as const, text: '已退款' }
    case 4:
      return { type: 'primary' as const, text: '已签发（可重复获取）' }
    default:
      return { type: 'info' as const, text: `未知(${status})` }
  }
}

async function loadRecords() {
  if (!machineCode.value) return
  try {
    records.value = await activationApi.list(machineCode.value)
  } catch {
    records.value = []
  }
}

async function refreshMachineCode() {
  machineCode.value = await getMachineCode()
  await loadRecords()
}

onMounted(async () => {
  storageMode.value = await getStorageMode()
  await refreshMachineCode()
  await loadOrderInfo()
})

async function authorizeFolder() {
  authorizing.value = true
  try {
    const res = await setupSharedFolder()
    if (res.ok) {
      storageMode.value = 'shared'
      ElMessage.success('共享文件夹已生效：所有浏览器将使用同一机器码')
      await refreshMachineCode()
    } else {
      ElMessage.warning(res.message)
    }
  } finally {
    authorizing.value = false
  }
}

async function activate() {
  if (!machineCode.value) return
  // 人工核验模式：关联订单待审核时拦截并提示
  if (orderInfo.value && orderInfo.value.status === 0) {
    ElMessage.warning('订单待人工审核：管理员确认收款后即可激活，请稍后再试')
    return
  }
  activating.value = true
  try {
    const code = await activationApi.activate({
      productId: form.productId,
      machineCode: machineCode.value,
      orderNo: form.orderNo || undefined,
    })
    ElMessage.success(`激活码签发成功：${code.code}`)
    await loadRecords()
    await loadOrderInfo()
  } catch (e: any) {
    ElMessage.error(e?.message || '激活码签发失败')
  } finally {
    activating.value = false
  }
}

/** 状态展示：0未激活 1已激活 2已吊销 3已过期 */
function statusOf(row: ActivationCode) {
  switch (row.status) {
    case 1:
      return { type: 'success' as const, text: '已激活' }
    case 2:
      return { type: 'danger' as const, text: '已吊销' }
    case 3:
      return { type: 'info' as const, text: '已过期' }
    default:
      return { type: 'warning' as const, text: '未激活' }
  }
}
</script>

<template>
  <div class="page-container">
    <h2 class="section-title">激活码</h2>
    <p class="section-subtitle">点击按钮自动获取本机机器码并申请激活码</p>

    <el-card shadow="never" class="mb">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="产品" prop="productId">
          <el-select v-model="form.productId" placeholder="选择产品">
            <el-option
              v-for="p in store.products"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.orderNo" label="关联订单">
          <div class="order-line">
            <el-input :model-value="form.orderNo" readonly class="order-input" />
            <el-tag
              v-if="orderInfo"
              :type="orderStatusOf(orderInfo.status).type"
              :loading="orderLoading"
            >
              {{ orderStatusOf(orderInfo.status).text }}
            </el-tag>
            <el-button v-else size="small" text type="primary" @click="loadOrderInfo">
              刷新状态
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="activating" @click="activate">
            生成激活码
          </el-button>
          <span class="form-tip">机器码由系统自动识别，无需手动填写</span>
        </el-form-item>
        <el-form-item label="机器码存储">
          <el-tag :type="storageMode === 'shared' ? 'success' : 'info'" class="mr">
            {{ storageMode === 'shared' ? 'COC 共享文件夹' : storageMode === 'local' ? '浏览器本地' : '未生成' }}
          </el-tag>
          <el-button size="small" :loading="authorizing" @click="authorizeFolder">
            授权 COC 文件夹（跨浏览器共用机器码）
          </el-button>
          <div class="form-tip">授权一次后，Chrome/Edge 等浏览器共用同一机器码；不授权则仅当前浏览器有效</div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>激活记录</template>
      <el-table :data="records" empty-text="暂无激活记录">
        <el-table-column prop="code" label="激活码" min-width="180" />
        <el-table-column prop="productName" label="产品" min-width="120" />
        <el-table-column prop="machineCode" label="机器码" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusOf(row).type">{{ statusOf(row).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="签发时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.mb {
  margin-bottom: 20px;
}

.mr {
  margin-right: 8px;
}

.form-tip {
  color: var(--text-secondary);
  font-size: 13px;
  margin-left: 12px;
}

.order-line {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.order-input {
  max-width: 260px;
}
</style>
