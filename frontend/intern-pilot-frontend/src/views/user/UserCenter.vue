<template>
  <PageContainer title="个人中心" description="查看当前账号资料、邮箱验证状态和角色信息。">
    <div class="user-center-grid">
      <section class="panel profile-summary">
        <div class="profile-avatar">{{ avatarText }}</div>
        <div>
          <h3>{{ profile?.nickname || profile?.username || '当前用户' }}</h3>
          <p>{{ profile?.email || '-' }}</p>
          <div class="tag-section">
            <el-tag v-if="profile?.emailVerified" type="success" effect="plain">邮箱已验证</el-tag>
            <el-tag v-else type="warning" effect="plain">邮箱未验证</el-tag>
            <el-tag v-for="role in profile?.roles || []" :key="role" effect="plain">{{ role }}</el-tag>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h3>安全信息</h3>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="最近登录时间">{{ formatTime(profile?.lastLoginTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(profile?.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(profile?.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h3>修改资料</h3>
        </div>
        <el-form :model="profileForm" label-position="top">
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" maxlength="50" show-word-limit />
          </el-form-item>
          <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
        </el-form>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h3>修改密码</h3>
        </div>
        <el-form :model="passwordForm" label-position="top">
          <el-form-item label="原密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-button type="primary" :loading="passwordSaving" @click="changePassword">修改密码</el-button>
        </el-form>
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import { changePasswordApi, getUserProfileApi, updateUserProfileApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const profile = ref<any>(null)
const profileSaving = ref(false)
const passwordSaving = ref(false)

const profileForm = reactive({ nickname: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const avatarText = computed(() => {
  const name = profile.value?.nickname || profile.value?.username || 'U'
  return String(name).slice(0, 1).toUpperCase()
})

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
    email: profile.value.email,
    roles: profile.value.roles,
    permissions: profile.value.permissions
  })
}

async function loadProfile() {
  profile.value = await getUserProfileApi()
  profileForm.nickname = profile.value?.nickname || ''
  syncAuthUser()
}

async function saveProfile() {
  profileSaving.value = true
  try {
    profile.value = await updateUserProfileApi({ nickname: profileForm.nickname })
    syncAuthUser()
    ElMessage.success('资料已保存')
  } finally {
    profileSaving.value = false
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

onMounted(loadProfile)
</script>

<style scoped>
.user-center-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.profile-summary {
  display: flex;
  gap: 18px;
  align-items: center;
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
}

.profile-summary h3 {
  margin: 0 0 8px;
  font-size: 20px;
}

.profile-summary p {
  margin: 0 0 12px;
  color: #667085;
}

@media (max-width: 900px) {
  .user-center-grid {
    grid-template-columns: 1fr;
  }
}
</style>
