<template>
  <div class="auth-page">
    <section class="auth-panel">
      <div class="auth-copy">
        <span class="eyebrow">InternPilot</span>
        <h1>创建求职工作台账号</h1>
        <p>准备好一份简历和目标岗位，就可以跑完整的 AI 匹配与投递闭环。</p>
      </div>
      <el-form class="auth-form" :model="form" label-position="top">
        <h2>注册</h2>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" show-password />
        </el-form-item>
        <div class="form-grid two">
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
          <el-form-item label="年级"><el-input v-model="form.grade" /></el-form-item>
        </div>
        <div class="form-grid two">
          <el-form-item label="学校"><el-input v-model="form.school" /></el-form-item>
          <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
        </div>
        <el-button type="primary" :loading="loading" @click="handleRegister">注册账号</el-button>
        <p class="auth-switch">已有账号？<router-link to="/login">去登录</router-link></p>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  school: '',
  major: '',
  grade: ''
})

async function handleRegister() {
  if (!form.username || !form.password || !form.confirmPassword) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await registerApi(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // Error message is already shown by the request interceptor.
  } finally {
    loading.value = false
  }
}
</script>
