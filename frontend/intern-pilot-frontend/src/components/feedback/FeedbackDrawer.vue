<template>
  <el-drawer
    v-model="drawerVisible"
    title="意见反馈"
    direction="rtl"
    :size="drawerSize"
    :close-on-press-escape="true"
    class="feedback-drawer"
  >
    <section v-if="submitState === 'success'" class="feedback-success" role="status" aria-live="polite">
      <div class="success-mark" aria-hidden="true">✓</div>
      <span class="success-kicker">已收到</span>
      <h3>反馈已提交</h3>
      <p>感谢你帮助 InternPilot 变得更好。我们已经记录页面与环境信息，便于后续定位。</p>
      <div class="success-actions">
        <el-button @click="store.closeDrawer">关闭</el-button>
        <el-button data-feedback-another type="primary" @click="startAnotherFeedback">继续反馈</el-button>
      </div>
    </section>

    <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top" class="feedback-form">
      <el-alert
        class="feedback-tip"
        type="info"
        :closable="false"
        show-icon
        title="反馈会自动记录当前页面路径，方便管理员定位问题。"
      />

      <div v-if="submitError" class="feedback-error" role="alert">
        <strong>提交失败</strong>
        <span>{{ submitError }}</span>
        <span>你的输入已保留，可以直接重试。</span>
      </div>

      <el-form-item label="反馈类型" prop="type">
        <el-select v-model="form.type" placeholder="请选择反馈类型" class="full-control">
          <el-option label="功能异常" value="BUG" />
          <el-option label="使用建议" value="SUGGESTION" />
          <el-option label="页面体验问题" value="UI_UX" />
          <el-option label="AI 结果不准确" value="AI_RESULT" />
          <el-option label="系统响应较慢" value="PERFORMANCE" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>

      <el-form-item label="反馈标题" prop="title">
        <el-input v-model.trim="form.title" maxlength="120" show-word-limit placeholder="请简要描述问题或建议" />
      </el-form-item>

      <el-form-item label="反馈内容" prop="content">
        <el-input
          v-model.trim="form.content"
          type="textarea"
          :rows="7"
          maxlength="2000"
          show-word-limit
          placeholder="请描述你遇到的页面、操作步骤、期望结果或 AI 结果问题。"
        />
      </el-form-item>

      <el-form-item label="联系方式">
        <el-input v-model.trim="form.contact" placeholder="邮箱或手机号，可选" />
      </el-form-item>

      <el-form-item>
        <el-checkbox v-model="form.allowContact">允许我们联系你了解更多细节</el-checkbox>
      </el-form-item>

      <div class="page-url">
        <span>当前页面</span>
        <strong>{{ form.pageUrl }}</strong>
      </div>

      <div class="form-actions">
        <el-button @click="store.closeDrawer">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交反馈</el-button>
      </div>
    </el-form>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useFeedbackStore } from '@/stores/feedback'
import { useResponsiveSize } from '@/utils/useResponsiveSize'

const store = useFeedbackStore()
const route = useRoute()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const submitState = ref<'form' | 'success'>('form')
const submitError = ref('')
const { responsiveDrawerSize } = useResponsiveSize({ tabletBreakpoint: 900 })
const drawerSize = responsiveDrawerSize('430px', '78%')

const drawerVisible = computed({
  get: () => store.drawerVisible,
  set: (value) => {
    if (!value) store.closeDrawer()
  }
})

const form = reactive({
  type: '',
  title: '',
  content: '',
  contact: '',
  allowContact: false,
  pageUrl: route.fullPath,
  browserInfo: navigator.userAgent
})

const rules: FormRules = {
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入反馈标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入反馈内容', trigger: 'blur' }]
}

watch(drawerVisible, (visible) => {
  if (visible) {
    form.pageUrl = route.fullPath
    form.browserInfo = navigator.userAgent
    submitError.value = ''
  } else {
    submitState.value = 'form'
    submitError.value = ''
    resetForm()
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  submitError.value = ''
  try {
    await store.submitFeedback({ ...form })
    ElMessage.success('反馈提交成功，感谢你的建议')
    resetForm()
    submitState.value = 'success'
  } catch (error: any) {
    submitError.value = getSubmitErrorMessage(error)
    ElMessage.error(submitError.value)
  } finally {
    submitting.value = false
  }
}

function getSubmitErrorMessage(error: any) {
  const responseMessage = error?.response?.data?.message || error?.response?.data?.data?.message
  return (typeof responseMessage === 'string' && responseMessage) || error?.message || '反馈提交失败，请稍后重试'
}

async function startAnotherFeedback() {
  submitState.value = 'form'
  submitError.value = ''
  await nextTick()
  formRef.value?.clearValidate()
}

function resetForm() {
  form.type = ''
  form.title = ''
  form.content = ''
  form.contact = ''
  form.allowContact = false
}
</script>

<style scoped>
.feedback-form {
  padding-bottom: 20px;
}

.feedback-error {
  display: grid;
  gap: var(--space-1);
  margin-bottom: var(--space-4);
  padding: var(--space-3) var(--space-4);
  border: 1px solid color-mix(in srgb, var(--color-danger) 32%, var(--color-border));
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-danger) 7%, var(--color-surface));
  color: var(--color-danger);
  font-size: 13px;
}

.feedback-error span:last-child {
  color: var(--color-text-muted);
}

.feedback-success {
  display: grid;
  min-height: min(520px, 70vh);
  place-items: center;
  align-content: center;
  padding: var(--space-6);
  text-align: center;
}

.success-mark {
  display: grid;
  width: 64px;
  height: 64px;
  margin-bottom: var(--space-4);
  place-items: center;
  border-radius: 50%;
  background: color-mix(in srgb, var(--color-success) 12%, var(--color-surface));
  color: var(--color-success);
  font-size: 30px;
  font-weight: 800;
}

.success-kicker {
  color: var(--color-success);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.feedback-success h3 {
  margin: var(--space-2) 0;
  font-size: 22px;
}

.feedback-success p {
  max-width: 340px;
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.success-actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.full-control {
  width: 100%;
}

.feedback-tip {
  margin-bottom: 16px;
}

.page-url {
  display: grid;
  gap: 4px;
  margin-bottom: 18px;
  padding: 10px 12px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.page-url span {
  color: var(--color-text-soft);
  font-size: 12px;
}

.page-url strong {
  overflow-wrap: anywhere;
  color: var(--color-text-muted);
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-soft);
}

@media (max-width: 640px) {
  :global(.feedback-drawer.el-drawer) {
    top: 0;
    bottom: calc(72px + env(safe-area-inset-bottom));
    height: auto !important;
  }

  :deep(.feedback-drawer .el-drawer__header) {
    margin-bottom: 12px;
    padding: 16px 16px 10px;
  }

  :deep(.feedback-drawer .el-drawer__body) {
    padding: 0 16px 16px;
  }

  .feedback-form {
    min-height: 100%;
    padding-bottom: 84px;
  }

  .feedback-tip {
    margin-bottom: 12px;
  }

  .form-actions {
    position: sticky;
    bottom: -16px;
    z-index: 2;
    display: grid;
    grid-template-columns: 1fr 1fr;
    margin: 0 -16px;
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
    background: var(--color-surface);
    box-shadow: 0 -8px 20px rgba(15, 23, 42, 0.08);
  }

  .form-actions .el-button {
    width: 100%;
    margin-left: 0;
  }

  .feedback-success {
    min-height: 60vh;
    padding: var(--space-4);
  }

  .success-actions {
    width: 100%;
    flex-direction: column-reverse;
  }

  .success-actions .el-button {
    width: 100%;
    margin-left: 0;
  }
}
</style>
