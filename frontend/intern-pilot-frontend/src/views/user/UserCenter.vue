<template>
  <PageContainer title="个人中心" description="管理账号资料、安全信息和登录凭证。">
    <section class="account-hero">
      <div class="account-identity">
        <div class="profile-avatar large">
          <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像">
          <span v-else>{{ avatarText }}</span>
        </div>
        <div class="identity-copy">
          <h3>{{ profile?.nickname || profile?.username || '当前用户' }}</h3>
          <p>{{ profile?.email || '-' }}</p>
          <div class="identity-tags">
            <el-tag v-if="profile?.emailVerified" type="success" effect="plain">邮箱已验证</el-tag>
            <el-tag v-else type="warning" effect="plain">邮箱未验证</el-tag>
            <el-tag v-for="role in profile?.roles || []" :key="role" effect="plain">{{ role }}</el-tag>
          </div>
        </div>
      </div>

      <div class="hero-actions">
        <el-upload
          accept="image/jpeg,image/png,image/webp"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="uploadAvatar"
        >
          <el-button :icon="Upload" :loading="avatarUploading">更换头像</el-button>
        </el-upload>
      </div>
    </section>

    <section class="settings-shell">
      <aside class="account-summary">
        <div class="summary-row">
          <span>账号邮箱</span>
          <strong>{{ profile?.email || '-' }}</strong>
        </div>
        <div class="summary-row">
          <span>最近登录</span>
          <strong>{{ formatTime(profile?.lastLoginTime) }}</strong>
        </div>
        <div class="summary-row">
          <span>创建时间</span>
          <strong>{{ formatTime(profile?.createdAt) }}</strong>
        </div>
        <div class="summary-row">
          <span>更新时间</span>
          <strong>{{ formatTime(profile?.updatedAt) }}</strong>
        </div>
      </aside>

      <div class="settings-panel">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="资料设置" name="profile">
            <div class="section-title">
              <h3>基础资料</h3>
              <span>用于系统内展示的公开账号信息</span>
            </div>
            <el-form class="settings-form" :model="profileForm" label-position="top">
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
          </el-tab-pane>

          <el-tab-pane label="求职偏好" name="career">
            <div class="section-title">
              <h3>求职偏好</h3>
              <span>用于岗位推荐和后续 AI 分析的默认偏好</span>
            </div>
            <el-form class="settings-form" :model="profileForm" label-position="top">
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
              <el-form-item label="默认简历">
                <el-select
                  v-model="selectedDefaultResumeId"
                  clearable
                  filterable
                  placeholder="请选择默认简历"
                  :loading="resumeLoading"
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
              <div class="form-actions">
                <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存偏好</el-button>
              </div>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="安全设置" name="security">
            <div class="section-title">
              <h3>修改密码</h3>
              <span>更新后下次登录请使用新密码</span>
            </div>
            <el-form class="settings-form" :model="passwordForm" label-position="top">
              <el-form-item label="原密码">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="passwordForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
              </el-form-item>
              <div class="form-actions">
                <el-button type="primary" :loading="passwordSaving" @click="changePassword">修改密码</el-button>
              </div>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { changePasswordApi, getUserProfileApi, updateUserProfileApi, uploadUserAvatarApi } from '@/api/user'
import { getResumeListApi, setDefaultResumeApi } from '@/api/resume'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const profile = ref<any>(null)
const profileSaving = ref(false)
const avatarUploading = ref(false)
const passwordSaving = ref(false)
const resumeLoading = ref(false)
const activeTab = ref('profile')
const resumes = ref<any[]>([])
const selectedDefaultResumeId = ref<number | undefined>()

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
    return
  }
  await setDefaultResumeApi(value)
  resumes.value = resumes.value.map((resume) => ({
    ...resume,
    isDefault: resume.resumeId === value
  }))
  const selected = resumes.value.find((resume) => resume.resumeId === value)
  if (profile.value) {
    profile.value.defaultResumeId = value
    profile.value.defaultResumeName = selected?.resumeName || selected?.originalFileName || ''
  }
  ElMessage.success('默认简历已更新')
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

onMounted(async () => {
  await Promise.all([loadProfile(), loadResumes()])
})
</script>

<style scoped>
.account-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
  padding: 24px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #ffffff;
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

.identity-copy h3 {
  margin: 0 0 8px;
  font-size: 24px;
  line-height: 1.25;
}

.identity-copy p {
  margin: 0 0 12px;
  color: #667085;
}

.identity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-actions {
  flex: 0 0 auto;
}

.settings-shell {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.account-summary,
.settings-panel {
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #ffffff;
}

.account-summary {
  padding: 6px 0;
}

.summary-row {
  display: grid;
  gap: 6px;
  padding: 15px 18px;
  border-bottom: 1px solid #edf2f7;
}

.summary-row:last-child {
  border-bottom: 0;
}

.summary-row span {
  color: #667085;
  font-size: 13px;
}

.summary-row strong {
  overflow-wrap: anywhere;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.settings-panel {
  min-width: 0;
  padding: 6px 22px 22px;
}

.section-title {
  margin: 8px 0 18px;
}

.section-title h3 {
  margin: 0 0 6px;
  font-size: 18px;
}

.section-title span {
  color: #667085;
  font-size: 13px;
}

.settings-form {
  max-width: 560px;
}

.form-actions {
  margin-top: 8px;
}

.profile-avatar {
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  border-radius: 8px;
  background: #2563eb;
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

@media (max-width: 900px) {
  .account-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .settings-shell {
    grid-template-columns: 1fr;
  }
}
</style>
