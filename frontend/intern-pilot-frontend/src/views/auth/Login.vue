<template>
  <div class="auth-page">
    <section class="auth-panel">
      <div class="auth-copy">
        <div class="auth-brand">
          <img class="auth-brand-logo" :src="brandLogo" alt="InternPilot logo">
          <span class="eyebrow">InternPilot</span>
        </div>
        <p>从简历、岗位 JD 到 AI 匹配分析和投递跟进，一站式管理你的实习求职流程。</p>

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
          <span>简历管理</span>
          <span>AI 匹配分析</span>
          <span>面试题生成</span>
          <span>投递跟进</span>
        </div>
      </div>

      <el-form
        ref="formRef"
        class="auth-form"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <div class="auth-form-title">
          <span>Welcome back</span>
          <h2>登录工作台</h2>
          <p>使用邮箱登录，继续管理你的求职流程。</p>
        </div>

        <el-form-item label="邮箱" prop="account">
          <el-input v-model.trim="form.account" :prefix-icon="Message" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" :prefix-icon="Lock" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="handleLogin">进入工作台</el-button>
        <p class="auth-switch">还没有账号？<router-link to="/register">创建账号</router-link></p>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Briefcase, Document, List, Lock, MagicStick, Message } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { loginApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import brandLogo from '@/assets/brand-logo.png'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ account: '', password: '' })

const flowItems = [
  { label: '简历上传', description: '沉淀可复用的求职资料', icon: Document },
  { label: 'JD 管理', description: '维护目标岗位和能力要求', icon: Briefcase },
  { label: 'AI 分析', description: '生成匹配报告和优化建议', icon: MagicStick },
  { label: '投递跟进', description: '记录进度、面试和复盘', icon: List }
]

const rules: FormRules = {
  account: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱', trigger: ['blur', 'change'] }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res: any = await loginApi(form)
    auth.setLogin(res.token, res.user)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    ElMessage.error(error?.message || '登录失败，请检查邮箱和密码')
  } finally {
    loading.value = false
  }
}
</script>
