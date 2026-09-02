<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores'
import { orderApi } from '@/api'
import type { Order } from '@/types'

/** 收款码图片（人工收款码，放入 front_end/public/image/ 后此处改为实际文件名） */
const PAY_QR_SRC = '/image/wechat-qr.png'

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const submitting = ref(false)
const createdOrder = ref<Order | null>(null)

const product = computed(
  () => store.products.find((p) => p.id === Number(route.params.productId)) ?? null,
)

const form = reactive({
  remark: '',
})

async function submit() {
  if (!product.value) return
  submitting.value = true
  try {
    const order = await orderApi.create({
      productId: product.value.id,
      // 人工核验模式：无需收集手机号/邮箱
      remark: form.remark,
    })
    ElMessage.success('订单已创建，请扫码完成付款')
    // 记录最近订单号，激活页自动带上以便后端校验并绑定
    sessionStorage.setItem('zwj_last_order_no', order.orderNo)
    createdOrder.value = order
    // TODO: 微信支付 API 开通后替换为 Native 下单 → 二维码
  } catch (e: any) {
    ElMessage.error(e?.message || '订单创建失败')
  } finally {
    submitting.value = false
  }
}

function goActivation() {
  router.push('/activation')
}
</script>

<template>
  <div class="page-container">
    <el-page-header content="购买" @back="$router.back()" class="mb" />

    <el-empty v-if="!product" description="未找到该产品" />

    <!-- 下单表单 -->
    <el-row v-else-if="!createdOrder" :gutter="32">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <h2 class="product-name">{{ product.name }}</h2>
          <p class="product-version">v{{ product.version ?? '1.0' }} · {{ product.code }}</p>
          <p class="product-desc">{{ product.description }}</p>
          <el-divider />
          <p class="product-price">
            <span class="price-symbol">¥</span>
            <span class="price-num">{{ product.price }}</span>
            <span class="price-unit">/ 永久授权</span>
          </p>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>提交订单（人工收款核验）</template>
          <el-form :model="form" label-width="90px">
            <el-form-item label="产品">
              <el-input :model-value="product.name" disabled />
            </el-form-item>
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="3"
                placeholder="选填，如您的联系方式 / 转账备注"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="submitting" @click="submit">
                提交订单
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert
            title="支付方式：微信扫码转账（人工核验）"
            description="提交订单后展示收款码，请用微信扫码按金额转账；管理员确认收款后订单自动放行，即可激活。"
            type="info"
            show-icon
            :closable="false"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 下单成功：收款码 + 订单信息 -->
    <el-row v-else :gutter="32">
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="qr-card">
          <template #header>微信扫码付款</template>
          <div class="qr-wrap">
            <img :src="PAY_QR_SRC" alt="收款码" class="qr-img" />
          </div>
          <p class="qr-tip">请使用微信「扫一扫」扫码，按下方金额转账</p>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>订单信息（待人工审核）</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="订单号">
              <span class="order-no">{{ createdOrder.orderNo }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="产品">{{ createdOrder.productName }}</el-descriptions-item>
            <el-descriptions-item label="应付金额">
              <span class="price-num">¥{{ createdOrder.amount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag type="warning">待人工核验</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-steps :active="1" align-center class="steps" finish-status="process">
            <el-step title="扫码转账" description="微信扫码支付对应金额" />
            <el-step title="管理员核验" description="人工确认收款（一般数分钟内）" />
            <el-step title="签发激活码" description="核验通过后前往激活页" />
          </el-steps>

          <div class="actions">
            <el-button type="primary" size="large" @click="goActivation">
              前往激活页（核验通过后签发激活码）
            </el-button>
            <el-button size="large" @click="createdOrder = null">再下一单</el-button>
          </div>
          <el-alert
            class="mt"
            title="提示"
            description="付款后请耐心等待管理员核验（订单状态变为「已支付」即可激活）。如长时间未通过，可在备注中留下您的联系方式。"
            type="info"
            show-icon
            :closable="false"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.mb {
  margin-bottom: 20px;
}

.product-name {
  font-size: 24px;
  font-weight: 600;
}

.product-version {
  color: var(--brand-color);
  margin: 6px 0 12px;
}

.product-desc {
  color: var(--text-secondary);
  line-height: 1.8;
}

.product-price {
  margin-bottom: 12px;
}

.price-symbol {
  font-size: 18px;
  color: #f56c6c;
}

.price-num {
  font-size: 30px;
  font-weight: 700;
  color: #f56c6c;
}

.price-unit {
  color: var(--text-secondary);
  font-size: 13px;
}

.qr-card {
  text-align: center;
}

.qr-wrap {
  padding: 12px;
  border: 1px dashed var(--border-color, #dcdfe6);
  border-radius: 8px;
  display: inline-block;
  background: #fff;
}

.qr-img {
  width: 220px;
  height: 220px;
  display: block;
  object-fit: contain;
}

.qr-tip {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 10px;
}

.order-no {
  font-family: Consolas, monospace;
  user-select: all;
}

.steps {
  margin: 20px 0;
}

.actions {
  margin: 8px 0 16px;
}

.mt {
  margin-top: 8px;
}
</style>
