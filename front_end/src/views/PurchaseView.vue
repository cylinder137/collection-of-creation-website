<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores'
import { orderApi } from '@/api'

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const submitting = ref(false)

const product = computed(
  () => store.products.find((p) => p.id === Number(route.params.productId)) ?? null,
)

const form = reactive({
  contact: '',
  remark: '',
})

const rules = {
  contact: [{ required: true, message: '请填写手机号或邮箱', trigger: 'blur' }],
}

const formRef = ref()

async function submit() {
  if (!product.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const order = await orderApi.create({
      productId: product.value.id,
      contact: form.contact,
      remark: form.remark,
    })
    ElMessage.success(`订单创建成功：${order.orderNo}`)
    // TODO: 对接微信支付（企业收款），跳转支付
    router.push('/activation')
  } catch {
    // 后端未就绪时的演示流程
    ElMessage.success('（演示）订单创建成功，待后端联调微信支付')
    router.push('/activation')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <el-page-header content="购买" @back="$router.back()" class="mb" />

    <el-empty v-if="!product" description="未找到该产品" />

    <el-row v-else :gutter="32">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <h2 class="product-name">{{ product.name }}</h2>
          <p class="product-slogan">{{ product.slogan }}</p>
          <p class="product-desc">{{ product.description }}</p>
          <el-divider />
          <p class="product-price">
            <span class="price-symbol">¥</span>
            <span class="price-num">{{ product.price }}</span>
            <span class="price-unit">/ 永久授权</span>
          </p>
          <p class="product-meta">
            <el-tag v-for="t in product.tags" :key="t" size="small" class="tag">{{ t }}</el-tag>
            <span class="platform">支持平台：{{ (product.platforms ?? []).join(' / ') }}</span>
          </p>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>填写订单信息</template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="产品" prop="productId">
              <el-input :model-value="product.name" disabled />
            </el-form-item>
            <el-form-item label="联系方式" prop="contact">
              <el-input v-model="form.contact" placeholder="手机号或邮箱，用于接收激活码" />
            </el-form-item>
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="选填" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="submitting" @click="submit">
                提交订单
              </el-button>
              <span class="form-tip">支付成功后系统将自动签发激活码</span>
            </el-form-item>
          </el-form>
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

.product-slogan {
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
  font-size: 36px;
  font-weight: 700;
  color: #f56c6c;
}

.price-unit {
  color: var(--text-secondary);
  font-size: 13px;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.platform {
  color: var(--text-secondary);
  font-size: 13px;
  margin-left: 8px;
}

.form-tip {
  color: var(--text-secondary);
  font-size: 13px;
  margin-left: 12px;
}
</style>
