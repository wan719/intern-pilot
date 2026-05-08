<template>
  <aside class="app-sidebar">
    <div class="brand">
      <div class="brand-mark">IP</div>
      <div>
        <strong>InternPilot</strong>
        <span>AI 求职工作台</span>
      </div>
    </div>

    <el-menu
      :default-active="route.path"
      :default-openeds="['analysis-group']"
      class="sidebar-menu"
    >
      <el-menu-item index="/dashboard" @click="go('/dashboard')">
        <el-icon><DataBoard /></el-icon>
        <span>数据看板</span>
      </el-menu-item>
      <el-menu-item index="/resumes" @click="go('/resumes')">
        <el-icon><Document /></el-icon>
        <span>简历管理</span>
      </el-menu-item>
      <el-menu-item index="/jobs" @click="go('/jobs')">
        <el-icon><Briefcase /></el-icon>
        <span>岗位管理</span>
      </el-menu-item>

      <el-sub-menu index="analysis-group">
        <template #title>
          <div class="analysis-title">
            <el-icon><MagicStick /></el-icon>
            <span>AI 分析</span>
          </div>
        </template>
        <el-menu-item index="/analysis/match" @click="go('/analysis/match')">
          <el-icon><MagicStick /></el-icon>
          <span>匹配分析</span>
        </el-menu-item>
        <el-menu-item index="/analysis/reports" @click="go('/analysis/reports')">
          <el-icon><Tickets /></el-icon>
          <span>分析报告</span>
        </el-menu-item>
        <el-menu-item index="/interview-questions" @click="go('/interview-questions')">
          <el-icon><QuestionFilled /></el-icon>
          <span>AI 面试题</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/applications" @click="go('/applications')">
        <el-icon><List /></el-icon>
        <span>投递记录</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { Briefcase, DataBoard, Document, List, MagicStick, QuestionFilled, Tickets } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

function go(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<style scoped>
.analysis-title {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}
</style>
