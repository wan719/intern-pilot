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
      router
      :default-active="$route.path"
      :default-openeds="['analysis-group']"
      class="sidebar-menu"
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataBoard /></el-icon>
        <span>数据看板</span>
      </el-menu-item>
      <el-menu-item index="/resumes">
        <el-icon><Document /></el-icon>
        <span>简历管理</span>
      </el-menu-item>
      <el-menu-item index="/jobs">
        <el-icon><Briefcase /></el-icon>
        <span>岗位管理</span>
      </el-menu-item>

      <el-sub-menu index="analysis-group">
        <template #title>
          <div class="analysis-title" @click.stop="goAnalysisMatch">
            <el-icon><MagicStick /></el-icon>
            <span>AI 分析</span>
          </div>
        </template>
        <el-menu-item index="/analysis/match">
          <el-icon><MagicStick /></el-icon>
          <span>匹配分析</span>
        </el-menu-item>
        <el-menu-item index="/analysis/reports">
          <el-icon><Tickets /></el-icon>
          <span>分析报告</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/applications">
        <el-icon><List /></el-icon>
        <span>投递记录</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Briefcase, DataBoard, Document, List, MagicStick, Tickets } from '@element-plus/icons-vue'

const router = useRouter()

function goAnalysisMatch() {
  router.push('/analysis/match')
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
