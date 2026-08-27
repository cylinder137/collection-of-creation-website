<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores'
import { activationApi } from '@/api'
import type { ActivationCode } from '@/types'

const store = useAppStore()
const records = ref<ActivationCode[]>([])
const activating = ref(false)

const form = reactive({
  productId: 1,
  machineCode: '',
})

const rules = {
  machineCode: [{ required: true, message: '请填写机器码', trigger: 'blur' }],
}

const formRef = ref()

async function loadRecords() {
  try {
    records.value = await activationApi.list()
  } catch {
    records.value = []
  }
}

onMounted(loadRecords)

async function activate() {
  await formRef.value.validate()
  activating.value = true
  try {
    const code = await activationApi.activate({
      productId: form.productId,
      machineCode: form.machineCode,
    })
    ElMessage.success(`激活码签发成功：${code.code}`)
    await loadRecords()
  } catch {
    ElMessage.warning('（演示）后端未就绪，激活码流程待联调')
  } finally {
    activating.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <h2 class="section-title">激活码</h2>
    <p class="section-subtitle">提交机器码，获取对应产品的激活码</p>

    <el-card shadow="never" class="mb">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
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
        <el-form-item label="机器码" prop="machineCode">
          <el-input v-model="form.machineCode" placeholder="请从软件客户端复制机器码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="activating" @click="activate">
            生成激活码
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>激活记录</template>
      <el-table :data="records" empty-text="暂无记录">
        <el-table-column prop="code" label="激活码" min-width="180" />
        <el-table-column prop="productName" label="产品" min-width="120" />
        <el-table-column prop="machineCode" label="机器码" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVATED' ? 'success' : 'info'">
              {{ row.status === 'ACTIVATED' ? '已激活' : '未使用' }}
            </el-tag>
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
</style>
