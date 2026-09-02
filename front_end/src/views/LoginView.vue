<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { LoginParams } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<LoginParams>({
  username: '',
  password: '',
  remember: true,
})

const rules: FormRules<LoginParams> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '长度 3-32 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '长度 6-64 个字符', trigger: 'blur' },
  ],
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success(`欢迎回来，${auth.userInfo?.nickname || auth.userInfo?.username}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch {
    // 错误提示由 http 拦截器统一处理（如：用户名或密码错误）
  } finally {
    loading.value = false
  }
}

function onForgot() {
  ElMessage.info('请联系管理员重置密码')
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="brand">
        <el-icon :size="30" class="brand-icon"><MagicStick /></el-icon>
        <span class="brand-name">造物集</span>
      </div>
      <p class="brand-slogan">让每个创造都被认真对待</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="onSubmit"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
            clearable
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item class="form-row-item">
          <div class="form-row">
            <el-checkbox v-model="form.remember">记住我</el-checkbox>
            <el-link type="primary" :underline="false" @click="onForgot">忘记密码？</el-link>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="extra">
        <el-link :underline="false" @click="$router.push('/')">返回首页</el-link>
        <span class="tip">账号由管理员开通</span>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #4f6ef7 0%, #3a55d8 55%, #2b3fa0 100%);
}

.login-card {
  width: 100%;
  max-width: 400px;
  border-radius: 12px;
  padding: 12px 8px 4px;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 4px;
}

.brand-icon {
  color: var(--brand-color);
}

.brand-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
}

.brand-slogan {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 28px;
}

.form-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.submit-btn {
  width: 100%;
  letter-spacing: 8px;
  font-weight: 600;
}

.extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2px;
}

.tip {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
