<template>
  <section class="auth-page auth-page--career">
    <div class="auth-panel auth-panel-register">
      <aside class="auth-copy">
        <div class="auth-brand">
          <img class="auth-brand-logo" :src="brandLogo" alt="InternPilot logo">
          <div>
            <span class="eyebrow">InternPilot</span>
            <strong>职业工作室</strong>
          </div>
        </div>
        <h2>先建立工作区，再把目标拆成行动</h2>
        <p>创建账号后即可集中管理简历、目标岗位、AI 分析与每一次投递进展。</p>

        <div class="auth-flow">
          <div v-for="item in flowItems" :key="item.label" class="auth-flow-item">
            <el-icon><component :is="item.icon" /></el-icon>
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.description }}</span>
            </div>
          </div>
        </div>

        <div class="auth-feature-list">
          <span>邮箱验证码</span>
          <span>求职资料</span>
          <span>AI 分析</span>
          <span>安全账号</span>
        </div>
      </aside>

      <main class="auth-form" aria-labelledby="auth-title">
        <div class="auth-form__inner">
          <div class="auth-form-title">
            <span>开始建立求职节奏</span>
            <h1 id="auth-title">创建职业工作室账号</h1>
            <p>先完成账号信息；学校、专业等求职资料可以稍后补充。</p>
          </div>

          <el-form ref="formRef" class="auth-form__fields" :model="form" :rules="rules" label-position="top">
            <div class="auth-section-title">
              <strong>账号信息</strong>
              <span>必填</span>
            </div>
            <el-form-item label="邮箱" prop="account">
              <el-input v-model.trim="form.account" :prefix-icon="Message" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="验证码" prop="captchaCode">
              <div class="captcha-row">
                <el-input v-model.trim="form.captchaCode" :prefix-icon="Key" placeholder="请输入验证码" />
                <el-button :disabled="captchaCountdown > 0" :loading="captchaSending" @click="sendCaptcha">
                  {{ captchaCountdown > 0 ? captchaCountdown + 's' : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" :prefix-icon="Lock" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" :prefix-icon="Lock" type="password" show-password placeholder="再次输入密码" />
            </el-form-item>

            <div class="auth-section-title optional">
              <strong>求职资料</strong>
              <span>可选</span>
            </div>
            <div class="auth-optional-box">
              <div class="form-grid two">
                <el-form-item label="学校">
                  <el-input v-model="form.school" :prefix-icon="User" placeholder="例如：西南大学" />
                </el-form-item>
                <el-form-item label="专业">
                  <el-input v-model="form.major" :prefix-icon="Briefcase" placeholder="例如：软件工程" />
                </el-form-item>
              </div>
              <div class="form-grid two">
                <el-form-item label="年级">
                  <el-input v-model="form.grade" placeholder="例如：大二 / 2024级" />
                </el-form-item>
                <el-form-item label="用户名">
                  <el-input v-model.trim="form.username" :prefix-icon="User" placeholder="可不填，系统自动生成" />
                </el-form-item>
              </div>
            </div>

            <el-button type="primary" :loading="loading" @click="handleRegister">创建账号</el-button>
            <p class="auth-switch">已有账号？<router-link to="/login">去登录</router-link></p>
          </el-form>
        </div>
      </main>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Briefcase, Document, Key, List, Lock, MagicStick, Message, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { registerApi, sendRegisterCaptchaApi } from '@/api/auth'
import brandLogo from '@/assets/brand-logo-optimized.png'

const router = useRouter()
const loading = ref(false)
const captchaSending = ref(false)
const captchaCountdown = ref(0)
const formRef = ref<FormInstance>()
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

const flowItems = [
  { label: '邮箱注册', description: '验证码确认账号归属', icon: Message },
  { label: '完善资料', description: '学校、专业和求职背景', icon: Document },
  { label: 'AI 分析', description: '生成岗位匹配和优化建议', icon: MagicStick },
  { label: '投递闭环', description: '跟进每一次申请进度', icon: List }
]

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
    return
  }
  callback()
}

const rules: FormRules = {
  account: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱', trigger: ['blur', 'change'] }
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

async function sendCaptcha() {
  const validEmail = await formRef.value?.validateField('account').catch(() => false)
  if (!validEmail) return

  captchaSending.value = true
  try {
    await sendRegisterCaptchaApi({ target: form.account, type: 'EMAIL' })
    ElMessage.success('验证码已发送，请查看邮箱')
    captchaCountdown.value = 60
    countdownTimer = setInterval(() => {
      captchaCountdown.value -= 1
      if (captchaCountdown.value <= 0) {
        if (countdownTimer) clearInterval(countdownTimer)
        captchaCountdown.value = 0
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error?.message || '验证码发送失败，请稍后重试')
  } finally {
    captchaSending.value = false
  }
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await registerApi({ ...form, accountType: 'EMAIL' })
    ElMessage.success('注册成功，请登录工作台')
    router.push('/login')
  } catch (error: any) {
    ElMessage.error(error?.message || '注册失败，请检查邮箱、验证码和密码')
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>
