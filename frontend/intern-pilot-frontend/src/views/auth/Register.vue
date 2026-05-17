<template>
  <div class="auth-page">
    <section class="auth-panel">
      <div class="auth-copy">
        <span class="eyebrow">InternPilot</span>
        <h1>创建求职工作台账号</h1>
        <p>准备好一份简历和目标岗位，就可以跑完整的 AI 匹配与投递闭环。</p>
        <p class="auth-demo-hint">
          演示验证码固定为 123456
        </p>
      </div>
      <el-form class="auth-form" :model="form" label-position="top">
        <h2>注册</h2>
        <el-form-item label="账号类型">
          <el-radio-group v-model="form.accountType">
            <el-radio value="EMAIL">邮箱</el-radio>
            <el-radio value="PHONE">手机号</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.accountType === 'EMAIL' ? '邮箱' : '手机号'">
          <el-input v-model="form.account" :placeholder="form.accountType === 'EMAIL' ? '请输入邮箱' : '请输入手机号'" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" style="flex: 1" />
            <el-button :disabled="captchaCountdown > 0" :loading="captchaSending" @click="sendCaptcha" style="margin-left: 12px; white-space: nowrap;">
              {{ captchaCountdown > 0 ? captchaCountdown + 's' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" show-password />
        </el-form-item>
        <div class="form-grid two">
          <el-form-item label="学校"><el-input v-model="form.school" /></el-form-item>
          <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
        </div>
        <div class="form-grid two">
          <el-form-item label="年级"><el-input v-model="form.grade" /></el-form-item>
          <el-form-item label="用户名（可选）"><el-input v-model="form.username" placeholder="不填则自动生成" /></el-form-item>
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
import { registerApi, sendRegisterCaptchaApi } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const captchaSending = ref(false)
const captchaCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  account: '',
  accountType: 'EMAIL',
  password: '',
  confirmPassword: '',
  captchaCode: '',
  username: '',
  school: '',
  major: '',
  grade: ''
})

async function sendCaptcha() {
  if (!form.account) {
    ElMessage.warning('请先输入' + (form.accountType === 'EMAIL' ? '邮箱' : '手机号'))
    return
  }
  captchaSending.value = true
  try {
    await sendRegisterCaptchaApi({ target: form.account, type: form.accountType })
    ElMessage.success('验证码已发送')
    captchaCountdown.value = 60
    countdownTimer = setInterval(() => {
      captchaCountdown.value--
      if (captchaCountdown.value <= 0) {
        if (countdownTimer) clearInterval(countdownTimer)
        captchaCountdown.value = 0
      }
    }, 1000)
  } catch {
  } finally {
    captchaSending.value = false
  }
}

async function handleRegister() {
  if (!form.account || !form.accountType || !form.password || !form.confirmPassword || !form.captchaCode) {
    ElMessage.warning('请填写所有必填项')
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
  } finally {
    loading.value = false
  }
}
</script>