<template>
  <aside class="app-sidebar">
    <div class="brand">
      <div class="brand-mark">IP</div>
      <div>
        <strong>InternPilot</strong>
        <span>AI 求职工作台</span>
      </div>
    </div>

    <el-menu router :default-active="$route.path" class="sidebar-menu">
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
      <el-menu-item index="/analysis/match">
        <el-icon><MagicStick /></el-icon>
        <span>AI 分析</span>
      </el-menu-item>
      <el-menu-item index="/analysis/reports">
        <el-icon><Tickets /></el-icon>
        <span>分析报告</span>
      </el-menu-item>
      <el-menu-item index="/applications">
        <el-icon><List /></el-icon>
        <span>投递记录</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { Briefcase, DataBoard, Document, List, MagicStick, Tickets } from '@element-plus/icons-vue'
</script>
