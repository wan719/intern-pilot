<template>
  <div class="auth-page">
    <section class="auth-panel">
      <div class="auth-copy">
        <span class="eyebrow">InternPilot</span>
        <h1>AI 实习投递与简历优化平台</h1>
        <p>登录后可以上传简历、管理岗位 JD、生成 AI 匹配报告，并跟踪每一次投递进展。</p>
      </div>
      <el-form class="auth-form" :model="form" label-position="top" @keyup.enter="handleLogin">
        <h2>登录</h2>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
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
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await loginApi(form)
    auth.setLogin(res.token, res.user)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // Error message is already shown by the request interceptor.
  } finally {
    loading.value = false
  }
}
</script>
