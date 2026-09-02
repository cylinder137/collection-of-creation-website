<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAdminStore } from '@/stores'

/**
 * 管理员登录页（隐藏入口）
 *
 * 官网无任何指向此页的链接/按钮，只有管理员手动访问 http://<域名>/admin 才会到这里。
 * 登录成功后换取无状态令牌，缓存在前端，后续每个管理接口都会携带，后端逐次核验。
 */
const adminStore = useAdminStore()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await adminStore.login(form.username.trim(), form.password)
    ElMessage.success('登录成功')
    router.replace('/admin/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg" />

    <div class="login-card">
      <div class="login-head">
        <span class="login-mark">造</span>
        <h1 class="login-title">造物集管理后台</h1>
        <p class="login-sub">请使用管理员账号登录，登录状态有效期 12 小时</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleSubmit"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="管理员账号"
            :prefix-icon="'User'"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="'Lock'"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-button
          class="login-submit"
          type="primary"
          size="large"
          :loading="loading"
          @click="handleSubmit"
        >
          登 录
        </el-button>
      </el-form>

      <router-link class="login-back" to="/">← 返回造物集官网</router-link>
    </div>

    <p class="login-foot">
      © 2026 大连造物集有限公司 · 本页为内部管理入口，请勿外传
    </p>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(900px 500px at 20% 10%, rgba(79, 110, 247, 0.18), transparent 60%),
    radial-gradient(800px 460px at 85% 85%, rgba(168, 85, 247, 0.16), transparent 62%),
    #f4f5f9;
  z-index: 0;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
  padding: 40px 36px 28px;
}

.login-head {
  text-align: center;
  margin-bottom: 30px;
}

.login-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  box-shadow: 0 6px 18px rgba(79, 110, 247, 0.3);
  margin-bottom: 16px;
}

.login-title {
  font-size: 21px;
  font-weight: 700;
}

.login-sub {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-top: 6px;
}

.login-submit {
  width: 100%;
  margin-top: 4px;
}

.login-back {
  display: block;
  text-align: center;
  margin-top: 18px;
  font-size: 13px;
  color: var(--text-tertiary);
  transition: color 0.18s;
}

.login-back:hover {
  color: var(--brand-color);
}

.login-foot {
  position: relative;
  z-index: 1;
  margin-top: 24px;
  font-size: 12.5px;
  color: var(--text-tertiary);
  text-align: center;
}
</style>
