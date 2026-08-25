<template>
  <PageContainer title="个人中心" description="管理账号身份、教育与求职档案，以及当前登录会话。" width="wide">
    <section class="account-section identity-section" data-account-section="identity" aria-labelledby="identity-title">
      <div class="account-hero">
        <div class="account-identity">
          <div class="profile-avatar large">
            <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像">
            <span v-else>{{ avatarText }}</span>
          </div>
          <div class="identity-copy">
            <span class="section-kicker">账号身份</span>
            <h2 id="identity-title">{{ profile?.nickname || profile?.username || '当前用户' }}</h2>
            <p>{{ profile?.email || '-' }}</p>
            <div class="identity-tags">
              <el-tag v-if="profile?.emailVerified" type="success" effect="plain">邮箱已验证</el-tag>
              <el-tag v-else type="warning" effect="plain">邮箱未验证</el-tag>
              <el-tag v-for="role in profile?.roles || []" :key="role" effect="plain">{{ role }}</el-tag>
            </div>
          </div>
        </div>

        <el-upload
          accept="image/jpeg,image/png,image/webp"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="uploadAvatar"
        >
          <el-button :icon="Upload" :loading="avatarUploading">更换头像</el-button>
        </el-upload>
      </div>

      <el-form class="settings-form identity-form" :model="profileForm" label-position="top">
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input :model-value="profile?.email || ''" disabled />
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
        </div>
      </el-form>
    </section>

    <div class="settings-grid">
      <section class="account-section education-section" data-account-section="education" aria-labelledby="education-title">
        <div class="section-title">
          <div>
            <span class="section-kicker">教育与求职档案</span>
            <h2 id="education-title">让推荐更贴近你的目标</h2>
          </div>
          <p>学校和专业沿用注册资料；求职偏好用于岗位推荐与 AI 分析。</p>
        </div>

        <dl class="education-facts">
          <div><dt>学校</dt><dd>{{ auth.user?.school || '未填写' }}</dd></div>
          <div><dt>专业</dt><dd>{{ auth.user?.major || '未填写' }}</dd></div>
        </dl>

        <el-form class="settings-form career-form" :model="profileForm" label-position="top">
          <el-form-item label="期望岗位">
            <el-input v-model="profileForm.preferredJobTitle" placeholder="例如：Java 后端实习生" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="期望城市">
            <el-input v-model="profileForm.preferredCity" placeholder="例如：重庆、成都、远程" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="期望薪资">
            <el-input v-model="profileForm.expectedSalary" placeholder="例如：150-200/天 或 8k-12k" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="求职类型">
            <el-select v-model="profileForm.employmentType" clearable placeholder="请选择">
              <el-option label="实习" value="实习" />
              <el-option label="全职" value="全职" />
              <el-option label="校招" value="校招" />
              <el-option label="远程" value="远程" />
            </el-select>
          </el-form-item>
          <el-form-item label="默认简历" class="career-form__wide">
            <el-select
              v-model="selectedDefaultResumeId"
              clearable
              filterable
              placeholder="请选择默认简历"
              :loading="resumeLoading || defaultResumeSaving"
              :disabled="resumeLoading || defaultResumeSaving"
              @change="changeDefaultResume"
            >
              <el-option
                v-for="resume in resumes"
                :key="resume.resumeId"
                :label="resume.resumeName || resume.originalFileName"
                :value="resume.resumeId"
              />
            </el-select>
          </el-form-item>
          <div class="form-actions career-form__wide">
            <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存求职档案</el-button>
          </div>
        </el-form>
      </section>

      <section class="account-section" data-account-section="security" aria-labelledby="security-title">
        <div class="section-title compact">
          <div>
            <span class="section-kicker">安全设置</span>
            <h2 id="security-title">修改密码</h2>
          </div>
          <p>更新后下次登录请使用新密码。</p>
        </div>
        <el-form class="settings-form" :model="passwordForm" label-position="top">
          <el-form-item label="原密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <div class="form-actions">
            <el-button type="primary" :loading="passwordSaving" @click="changePassword">修改密码</el-button>
          </div>
        </el-form>
      </section>

      <section class="account-section session-section" data-account-section="session" aria-labelledby="session-title">
        <div class="section-title compact">
          <div>
            <span class="section-kicker">会话与登录</span>
            <h2 id="session-title">当前账号活动</h2>
          </div>
          <p>查看账号时间信息，或安全结束当前会话。</p>
        </div>
        <dl class="session-facts">
          <div><dt>最近登录</dt><dd>{{ formatTime(profile?.lastLoginTime) }}</dd></div>
          <div><dt>创建时间</dt><dd>{{ formatTime(profile?.createdAt) }}</dd></div>
          <div><dt>资料更新</dt><dd>{{ formatTime(profile?.updatedAt) }}</dd></div>
        </dl>
        <div class="session-action">
          <div>
            <strong>结束当前会话</strong>
            <span>退出后需要重新验证账号。</span>
          </div>
          <el-button data-session-logout type="danger" plain @click="logoutCurrentSession">退出当前账号</el-button>
        </div>
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { changePasswordApi, getUserProfileApi, updateUserProfileApi, uploadUserAvatarApi } from '@/api/user'
import { getResumeListApi, setDefaultResumeApi } from '@/api/resume'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const profile = ref<any>(null)
const profileSaving = ref(false)
const avatarUploading = ref(false)
const passwordSaving = ref(false)
const resumeLoading = ref(false)
const defaultResumeSaving = ref(false)
const resumes = ref<any[]>([])
const selectedDefaultResumeId = ref<number | undefined>()
const confirmedDefaultResumeId = ref<number | undefined>()
const pendingDefaultResumeId = ref<number | undefined>()

const profileForm = reactive({
  nickname: '',
  preferredJobTitle: '',
  preferredCity: '',
  expectedSalary: '',
  employmentType: ''
})
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const avatarText = computed(() => {
  const name = profile.value?.nickname || profile.value?.username || 'U'
  return String(name).slice(0, 1).toUpperCase()
})
const avatarUrl = computed(() => resolveAvatarUrl(profile.value?.avatarUrl))

function resolveAvatarUrl(url?: string) {
  if (!url) {
    return ''
  }
  if (/^https?:\/\//i.test(url)) {
    return url
  }
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  return `${baseUrl}${url}`
}

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function syncAuthUser() {
  if (!profile.value) {
    return
  }
  auth.setUser({
    ...(auth.user || {}),
    username: profile.value.username,
    nickname: profile.value.nickname,
    avatarUrl: profile.value.avatarUrl,
    email: profile.value.email,
    roles: profile.value.roles,
    permissions: profile.value.permissions
  })
}

async function loadProfile() {
  profile.value = await getUserProfileApi()
  profileForm.nickname = profile.value?.nickname || ''
  profileForm.preferredJobTitle = profile.value?.preferredJobTitle || ''
  profileForm.preferredCity = profile.value?.preferredCity || ''
  profileForm.expectedSalary = profile.value?.expectedSalary || ''
  profileForm.employmentType = profile.value?.employmentType || ''
  selectedDefaultResumeId.value = profile.value?.defaultResumeId
  confirmedDefaultResumeId.value = profile.value?.defaultResumeId
  syncAuthUser()
}

async function saveProfile() {
  profileSaving.value = true
  try {
    profile.value = await updateUserProfileApi({ ...profileForm })
    syncAuthUser()
    ElMessage.success('资料已保存')
  } finally {
    profileSaving.value = false
  }
}

async function uploadAvatar(file: any) {
  const raw = file?.raw
  if (!raw) {
    return
  }
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(raw.type)) {
    ElMessage.warning('头像仅支持 JPG、PNG、WEBP 格式')
    return
  }
  if (raw.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 2MB')
    return
  }

  const data = new FormData()
  data.append('file', raw)
  avatarUploading.value = true
  try {
    profile.value = await uploadUserAvatarApi(data)
    syncAuthUser()
    ElMessage.success('头像已更新')
  } finally {
    avatarUploading.value = false
  }
}

async function loadResumes() {
  resumeLoading.value = true
  try {
    const res: any = await getResumeListApi({ pageNum: 1, pageSize: 100 })
    resumes.value = res?.records || []
  } finally {
    resumeLoading.value = false
  }
}

async function changeDefaultResume(value?: number) {
  if (!value) {
    selectedDefaultResumeId.value = pendingDefaultResumeId.value ?? confirmedDefaultResumeId.value
    return
  }
  if (defaultResumeSaving.value) {
    selectedDefaultResumeId.value = pendingDefaultResumeId.value ?? confirmedDefaultResumeId.value
    return
  }
  const previousResumeId = confirmedDefaultResumeId.value
  const previousResumes = resumes.value
  const previousProfileResumeId = profile.value?.defaultResumeId
  const previousProfileResumeName = profile.value?.defaultResumeName
  pendingDefaultResumeId.value = value
  selectedDefaultResumeId.value = value
  defaultResumeSaving.value = true
  try {
    await setDefaultResumeApi(value)
    const selected = resumes.value.find((resume) => resume.resumeId === value)
    resumes.value = resumes.value.map((resume) => ({
      ...resume,
      isDefault: resume.resumeId === value
    }))
    confirmedDefaultResumeId.value = value
    selectedDefaultResumeId.value = value
    if (profile.value) {
      profile.value.defaultResumeId = value
      profile.value.defaultResumeName = selected?.resumeName || selected?.originalFileName || ''
    }
    ElMessage.success('默认简历已更新')
  } catch {
    confirmedDefaultResumeId.value = previousResumeId
    selectedDefaultResumeId.value = previousResumeId
    resumes.value = previousResumes
    if (profile.value) {
      profile.value.defaultResumeId = previousProfileResumeId
      profile.value.defaultResumeName = previousProfileResumeName
    }
  } finally {
    pendingDefaultResumeId.value = undefined
    defaultResumeSaving.value = false
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  passwordSaving.value = true
  try {
    await changePasswordApi(passwordForm)
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码已修改，请下次登录时使用新密码')
  } finally {
    passwordSaving.value = false
  }
}

function logoutCurrentSession() {
  auth.logout()
  router.push('/login')
}

onMounted(async () => {
  await Promise.all([loadProfile(), loadResumes()])
})
</script>

<style scoped>
.account-section {
  min-width: 0;
  padding: var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.identity-section {
  margin-bottom: var(--space-5);
}

.account-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border-soft);
}

.account-identity {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 18px;
}

.identity-copy {
  min-width: 0;
}

.identity-copy h2 {
  margin: 0 0 8px;
  font-size: 24px;
  line-height: 1.25;
}

.identity-copy p {
  margin: 0 0 12px;
  color: var(--color-text-muted);
}

.identity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
  align-items: start;
}

.education-section {
  grid-column: 1 / -1;
}

.section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}

.section-title.compact {
  display: block;
}

.section-title h2 {
  margin: var(--space-1) 0 0;
  font-size: 20px;
}

.section-title p {
  max-width: 420px;
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.section-title.compact p {
  margin-top: var(--space-2);
}

.section-kicker {
  color: var(--color-primary-hover);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.identity-form {
  display: grid;
  max-width: none;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 0 var(--space-4);
  align-items: end;
}

.career-form {
  display: grid;
  max-width: none;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--space-4);
}

.career-form__wide {
  grid-column: 1 / -1;
}

.settings-form {
  max-width: 560px;
}

.settings-form.career-form,
.settings-form.identity-form {
  max-width: none;
}

.form-actions {
  margin-top: var(--space-2);
  padding-bottom: 18px;
}

.profile-avatar {
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  border-radius: 8px;
  background: var(--color-primary);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  overflow: hidden;
}

.profile-avatar.large {
  width: 88px;
  height: 88px;
  font-size: 34px;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.education-facts,
.session-facts {
  display: grid;
  gap: var(--space-3);
  margin: 0 0 var(--space-5);
}

.education-facts {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.education-facts div,
.session-facts div {
  padding: var(--space-4);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.education-facts dt,
.session-facts dt {
  margin-bottom: var(--space-1);
  color: var(--color-text-soft);
  font-size: 12px;
}

.education-facts dd,
.session-facts dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--color-text);
  font-weight: 700;
}

.session-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-soft);
}

.session-action div {
  display: grid;
  gap: var(--space-1);
}

.session-action span {
  color: var(--color-text-muted);
  font-size: 13px;
}

@media (max-width: 900px) {
  .account-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .settings-grid,
  .identity-form,
  .career-form,
  .education-facts {
    grid-template-columns: 1fr;
  }

  .career-form__wide {
    grid-column: auto;
  }

  .section-title,
  .session-action {
    align-items: stretch;
    flex-direction: column;
  }

  .session-action .el-button {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .account-section {
    padding: var(--space-4);
  }

  .account-identity {
    align-items: flex-start;
  }

  .profile-avatar.large {
    width: 68px;
    height: 68px;
  }
}
</style>
