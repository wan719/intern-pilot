<template>
  <div class="auth-page">
    <section class="auth-panel">
      <div class="auth-copy">
        <span class="eyebrow">InternPilot</span>
        <h1>AI 实习投递与简历优化平台</h1>
        <p>登录后可以上传简历、管理岗位 JD、生成 AI 匹配报告，并跟踪每一次投递进展。</p>
        <p class="auth-demo-hint">
          演示账号：admin@internpilot.local / 123456 或 demo@internpilot.local / 123456
        </p>
      </div>
      <el-form class="auth-form" :model="form" label-position="top" @keyup.enter="handleLogin">
        <h2>登录</h2>
        <el-form-item label="手机号 / 邮箱">
          <el-input v-model="form.account" placeholder="请输入手机号或邮箱" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="handleLogin">登录工作台</el-button>
        <p class="auth-switch">还没有账号？<router-link to="/register">去注册</router-link></p>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ account: '', password: '' })

async function handleLogin() {
  if (!form.account || !form.password) {
    ElMessage.warning('请输入手机号/邮箱和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await loginApi(form)
    auth.setLogin(res.token, res.user)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
  } finally {
    loading.value = false
  }
}
</script>