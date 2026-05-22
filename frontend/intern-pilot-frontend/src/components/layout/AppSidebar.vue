<template>
  <aside class="app-sidebar">
    <div class="brand">
      <img class="brand-logo" :src="brandLogo" alt="InternPilot logo">
      <div>
        <strong>InternPilot</strong>
        <span>求职工作台</span>
      </div>
    </div>

    <el-menu :default-active="route.path" :default-openeds="defaultOpeneds" class="sidebar-menu">
      <div class="nav-section">
        <div class="nav-section-title">工作台</div>
        <el-menu-item index="/dashboard" @click="go('/dashboard')">
          <el-icon>
            <DataBoard />
          </el-icon>
          <span>数据看板</span>
        </el-menu-item>
      </div>

      <div class="nav-section">
        <div class="nav-section-title">求职资料</div>
        <el-menu-item index="/resumes" @click="go('/resumes')">
          <el-icon>
            <Document />
          </el-icon>
          <span>简历管理</span>
        </el-menu-item>
        <el-menu-item index="/jobs" @click="go('/jobs')">
          <el-icon>
            <Briefcase />
          </el-icon>
          <span>岗位管理</span>
        </el-menu-item>
      </div>

      <div class="nav-section">
        <div class="nav-section-title">AI 助手</div>
        <el-sub-menu index="analysis-group">
          <template #title>
            <div class="analysis-title">
              <el-icon>
                <MagicStick />
              </el-icon>
              <span>AI 分析</span>
            </div>
          </template>
          <el-menu-item index="/analysis/match" @click="go('/analysis/match')">
            <el-icon>
              <MagicStick />
            </el-icon>
            <span>匹配分析</span>
          </el-menu-item>
          <el-menu-item index="/analysis/reports" @click="go('/analysis/reports')">
            <el-icon>
              <Tickets />
            </el-icon>
            <span>分析报告</span>
          </el-menu-item>
          <el-menu-item index="/job-recommendations" @click="go('/job-recommendations')">
            <el-icon>
              <MagicStick />
            </el-icon>
            <span>岗位推荐</span>
          </el-menu-item>
          <el-menu-item index="/interview-questions" @click="go('/interview-questions')">
            <el-icon>
              <QuestionFilled />
            </el-icon>
            <span>AI 面试题</span>
          </el-menu-item>
        </el-sub-menu>
      </div>

      <div class="nav-section">
        <div class="nav-section-title">投递管理</div>
        <el-menu-item index="/applications" @click="go('/applications')">
          <el-icon>
            <List />
          </el-icon>
          <span>投递记录</span>
        </el-menu-item>
      </div>

    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Briefcase,
  DataBoard,
  Document,
  List,
  MagicStick,
  QuestionFilled,
  Tickets
} from '@element-plus/icons-vue'
import brandLogo from '@/assets/brand-logo-optimized.png'

const route = useRoute()
const router = useRouter()
const aiPaths = ['/analysis', '/job-recommendations', '/interview-questions']
const defaultOpeneds = computed(() => (aiPaths.some((path) => route.path.startsWith(path)) ? ['analysis-group'] : []))

function go(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<style scoped>
.nav-section {
  margin-bottom: 14px;
}

.nav-section-title {
  padding: 10px 12px 7px;
  color: var(--color-text-soft);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.analysis-title {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 42px;
  margin: 2px 0;
  border-radius: 8px;
  color: #344054;
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: var(--color-surface-muted);
  color: var(--color-primary-hover);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  position: relative;
  background: var(--color-primary-soft);
  color: var(--color-primary-hover);
}

.sidebar-menu :deep(.el-menu-item.is-active::before) {
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 4px;
  width: 3px;
  border-radius: 99px;
  background: var(--color-primary);
  content: "";
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  margin-left: 8px;
  padding-left: 38px !important;
}
</style>
