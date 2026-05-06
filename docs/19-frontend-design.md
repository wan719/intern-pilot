# InternPilot 前端页面与可视化设计文档

## 1. 文档说明

本文档用于描述 InternPilot 前端页面与可视化设计方案，包括前端技术选型、项目结构、路由设计、页面设计、组件设计、Axios 封装、Token 管理、权限路由、接口对接、数据看板和前端开发顺序。

InternPilot 前端的第一阶段目标不是做复杂炫酷 UI，而是围绕后端接口完成一个可演示、可联调、可截图、可写进 README 的完整业务闭环。

---

## 2. 前端阶段目标

前端第一阶段需要完成以下目标：

1. 用户可以注册；
2. 用户可以登录；
3. 登录后可以保存 Token；
4. 用户可以上传简历；
5. 用户可以查看简历列表；
6. 用户可以创建岗位 JD；
7. 用户可以查看岗位列表；
8. 用户可以选择简历和岗位进行 AI 分析；
9. 用户可以查看 AI 分析报告；
10. 用户可以创建投递记录；
11. 用户可以跟踪投递状态；
12. 用户可以查看简单数据看板；
13. 前端可以正常调用后端接口；
14. 页面适合演示和截图。

---

## 3. 前端技术选型

### 3.1 推荐技术栈

| 技术 | 用途 |
| --- | --- |
| Vue 3 | 前端框架 |
| Vite | 项目构建工具 |
| TypeScript | 类型约束 |
| Element Plus | UI 组件库 |
| Vue Router | 路由管理 |
| Pinia | 状态管理 |
| Axios | HTTP 请求 |
| ECharts | 数据可视化 |
| Day.js | 时间格式化 |
| Sass / CSS | 样式 |

### 3.2 为什么选择 Vue 3

选择 Vue 3 的原因：

1. 上手快；
2. 适合后台管理类项目；
3. 和 Element Plus 搭配成熟；
4. 页面开发效率高；
5. 适合学生项目快速做出可演示效果。

### 3.3 为什么选择 Element Plus

Element Plus 提供大量现成组件：

```text
表单
表格
分页
弹窗
上传
卡片
标签
菜单
进度条
统计卡片
时间选择器
```

这些正好适合 InternPilot 的前端需求。

## 4. 前端项目创建

### 4.1 项目目录位置

建议放在项目根目录：

```text
InternPilot
├── backend
├── frontend
├── docs
├── deploy
└── README.md
```

前端工程路径：

```text
frontend/intern-pilot-frontend
```

### 4.2 创建 Vue 项目

进入 `frontend` 目录：

```powershell
cd frontend
npm create vite@latest intern-pilot-frontend
```

选择：

```text
Vue
TypeScript
```

进入项目：

```powershell
cd intern-pilot-frontend
npm install
```

### 4.3 安装依赖

```powershell
npm install element-plus
npm install @element-plus/icons-vue
npm install axios
npm install pinia
npm install vue-router
npm install echarts
npm install dayjs
```

如果使用 Sass：

```powershell
npm install -D sass
```

### 4.4 启动前端

```powershell
npm run dev
```

默认访问：

```text
http://localhost:5173
```

## 5. 前端目录结构设计

推荐目录：

```text
intern-pilot-frontend
├── public
├── src
│   ├── api
│   │   ├── auth.ts
│   │   ├── user.ts
│   │   ├── resume.ts
│   │   ├── job.ts
│   │   ├── analysis.ts
│   │   └── application.ts
│   ├── assets
│   ├── components
│   │   ├── layout
│   │   │   ├── AppLayout.vue
│   │   │   ├── AppSidebar.vue
│   │   │   └── AppHeader.vue
│   │   ├── common
│   │   │   ├── PageContainer.vue
│   │   │   ├── StatCard.vue
│   │   │   └── EmptyState.vue
│   │   └── business
│   │       ├── ResumeSelect.vue
│   │       ├── JobSelect.vue
│   │       └── MatchScoreCard.vue
│   ├── router
│   │   └── index.ts
│   ├── stores
│   │   └── auth.ts
│   ├── styles
│   │   ├── index.scss
│   │   └── variables.scss
│   ├── utils
│   │   ├── request.ts
│   │   ├── token.ts
│   │   └── format.ts
│   ├── views
│   │   ├── auth
│   │   │   ├── Login.vue
│   │   │   └── Register.vue
│   │   ├── dashboard
│   │   │   └── Dashboard.vue
│   │   ├── resume
│   │   │   ├── ResumeList.vue
│   │   │   └── ResumeDetail.vue
│   │   ├── job
│   │   │   ├── JobList.vue
│   │   │   └── JobDetail.vue
│   │   ├── analysis
│   │   │   ├── AnalysisMatch.vue
│   │   │   ├── AnalysisReportList.vue
│   │   │   └── AnalysisReportDetail.vue
│   │   └── application
│   │       ├── ApplicationList.vue
│   │       └── ApplicationDetail.vue
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── vite.config.ts
└── tsconfig.json
```

## 6. 页面设计总览

### 6.1 第一阶段页面

第一阶段建议做 7 个页面：

1. 登录页
2. 注册页
3. 首页 / 数据看板页
4. 简历管理页
5. 岗位 JD 管理页
6. AI 分析页
7. 投递记录页

### 6.2 第二阶段页面

后续可以扩展：

1. 简历详情页
2. 岗位详情页
3. 分析报告详情页
4. 投递详情页
5. 个人中心页
6. 系统设置页
7. AI 面试题页面
8. 数据统计页面

## 7. 路由设计

### 7.1 路由表

| 路径 | 页面 | 是否需要登录 |
| --- | --- | --- |
| `/login` | 登录页 | 否 |
| `/register` | 注册页 | 否 |
| `/` | 首页重定向 | 是 |
| `/dashboard` | 数据看板 | 是 |
| `/resumes` | 简历管理 | 是 |
| `/resumes/:id` | 简历详情 | 是 |
| `/jobs` | 岗位管理 | 是 |
| `/jobs/:id` | 岗位详情 | 是 |
| `/analysis/match` | AI 分析 | 是 |
| `/analysis/reports` | 分析报告列表 | 是 |
| `/analysis/reports/:id` | 分析报告详情 | 是 |
| `/applications` | 投递记录 | 是 |
| `/applications/:id` | 投递详情 | 是 |

### 7.2 router/index.ts

```ts
import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/token'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'resumes',
        component: () => import('@/views/resume/ResumeList.vue'),
        meta: { title: '简历管理' }
      },
      {
        path: 'resumes/:id',
        component: () => import('@/views/resume/ResumeDetail.vue'),
        meta: { title: '简历详情' }
      },
      {
        path: 'jobs',
        component: () => import('@/views/job/JobList.vue'),
        meta: { title: '岗位管理' }
      },
      {
        path: 'jobs/:id',
        component: () => import('@/views/job/JobDetail.vue'),
        meta: { title: '岗位详情' }
      },
      {
        path: 'analysis/match',
        component: () => import('@/views/analysis/AnalysisMatch.vue'),
        meta: { title: 'AI 匹配分析' }
      },
      {
        path: 'analysis/reports',
        component: () => import('@/views/analysis/AnalysisReportList.vue'),
        meta: { title: '分析报告' }
      },
      {
        path: 'analysis/reports/:id',
        component: () => import('@/views/analysis/AnalysisReportDetail.vue'),
        meta: { title: '报告详情' }
      },
      {
        path: 'applications',
        component: () => import('@/views/application/ApplicationList.vue'),
        meta: { title: '投递记录' }
      },
      {
        path: 'applications/:id',
        component: () => import('@/views/application/ApplicationDetail.vue'),
        meta: { title: '投递详情' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }

  const token = getToken()
  if (!token) {
    return '/login'
  }

  return true
})

export default router
```

## 8. 布局设计

### 8.1 整体布局

InternPilot 前端采用后台管理式布局：

```text
左侧菜单栏
  ↓
顶部用户栏
  ↓
右侧内容区
```

### 8.2 AppLayout.vue

页面结构：

```text
AppLayout
├── AppSidebar
├── AppHeader
└── RouterView
```

### 8.3 左侧菜单设计

菜单项：

1. 数据看板
2. 简历管理
3. 岗位管理
4. AI 匹配分析
5. 分析报告
6. 投递记录

### 8.4 AppSidebar.vue 示例

```vue
<template>
  <el-menu
    router
    :default-active="$route.path"
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

    <el-menu-item index="/analysis/match">
      <el-icon><MagicStick /></el-icon>
      <span>AI 匹配分析</span>
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
</template>

<script setup lang="ts">
import {
  DataBoard,
  Document,
  Briefcase,
  MagicStick,
  Tickets,
  List
} from '@element-plus/icons-vue'
</script>
```

## 9. Axios 请求封装

### 9.1 utils/request.ts

```ts
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/token'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 60000
})

request.interceptors.request.use((config) => {
  const token = getToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')

      if (res.code === 401) {
        removeToken()
        router.push('/login')
      }

      return Promise.reject(res)
    }

    return res.data
  },
  (error) => {
    ElMessage.error(error.response?.data?.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
```

### 9.2 utils/token.ts

```ts
const TOKEN_KEY = 'internpilot_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
```

### 9.3 .env.development

路径：

```text
frontend/intern-pilot-frontend/.env.development
```

内容：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 10. API 模块封装

### 10.1 api/auth.ts

```ts
import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  email?: string
  school?: string
  major?: string
  grade?: string
}

export function loginApi(data: LoginRequest) {
  return request.post('/api/auth/login', data)
}

export function registerApi(data: RegisterRequest) {
  return request.post('/api/auth/register', data)
}
```

### 10.2 api/resume.ts

```ts
import request from '@/utils/request'

export function uploadResumeApi(formData: FormData) {
  return request.post('/api/resumes/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getResumeListApi(params: any) {
  return request.get('/api/resumes', { params })
}

export function getResumeDetailApi(id: number) {
  return request.get(`/api/resumes/${id}`)
}

export function deleteResumeApi(id: number) {
  return request.delete(`/api/resumes/${id}`)
}

export function setDefaultResumeApi(id: number) {
  return request.put(`/api/resumes/${id}/default`)
}
```

### 10.3 api/job.ts

```ts
import request from '@/utils/request'

export function createJobApi(data: any) {
  return request.post('/api/jobs', data)
}

export function getJobListApi(params: any) {
  return request.get('/api/jobs', { params })
}

export function getJobDetailApi(id: number) {
  return request.get(`/api/jobs/${id}`)
}

export function updateJobApi(id: number, data: any) {
  return request.put(`/api/jobs/${id}`, data)
}

export function deleteJobApi(id: number) {
  return request.delete(`/api/jobs/${id}`)
}
```

### 10.4 api/analysis.ts

```ts
import request from '@/utils/request'

export function matchAnalysisApi(data: any) {
  return request.post('/api/analysis/match', data)
}

export function getAnalysisReportsApi(params: any) {
  return request.get('/api/analysis/reports', { params })
}

export function getAnalysisReportDetailApi(id: number) {
  return request.get(`/api/analysis/reports/${id}`)
}
```

### 10.5 api/application.ts

```ts
import request from '@/utils/request'

export function createApplicationApi(data: any) {
  return request.post('/api/applications', data)
}

export function getApplicationListApi(params: any) {
  return request.get('/api/applications', { params })
}

export function getApplicationDetailApi(id: number) {
  return request.get(`/api/applications/${id}`)
}

export function updateApplicationStatusApi(id: number, data: any) {
  return request.put(`/api/applications/${id}/status`, data)
}

export function updateApplicationNoteApi(id: number, data: any) {
  return request.put(`/api/applications/${id}/note`, data)
}

export function deleteApplicationApi(id: number) {
  return request.delete(`/api/applications/${id}`)
}
```

## 11. 登录页设计

### 11.1 页面目标

登录页需要完成：

1. 输入用户名；
2. 输入密码；
3. 调用登录接口；
4. 保存 Token；
5. 跳转数据看板；
6. 提供跳转注册入口。

### 11.2 页面布局

```text
InternPilot Logo / 标题
用户名输入框
密码输入框
登录按钮
注册链接
```

### 11.3 Login.vue 示例

```vue
<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>InternPilot</h2>
      <p class="subtitle">AI 实习投递与简历优化平台</p>

      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-button type="primary" class="full-button" @click="handleLogin">
          登录
        </el-button>

        <div class="auth-link">
          没有账号？
          <router-link to="/register">去注册</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { loginApi } from '@/api/auth'
import { setToken } from '@/utils/token'
import router from '@/router'

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  const res: any = await loginApi(form)
  setToken(res.token)
  ElMessage.success('登录成功')
  router.push('/dashboard')
}
</script>

<style scoped>
.auth-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fb;
}

.auth-card {
  width: 380px;
}

.subtitle {
  color: #666;
  margin-bottom: 24px;
}

.full-button {
  width: 100%;
}

.auth-link {
  margin-top: 16px;
  text-align: center;
}
</style>
```

## 12. 注册页设计

### 12.1 页面目标

注册页需要完成：

1. 输入用户名；
2. 输入密码；
3. 输入确认密码；
4. 输入邮箱、学校、专业、年级；
5. 调用注册接口；
6. 注册成功跳转登录页。

### 12.2 字段

- username
- password
- confirmPassword
- email
- school
- major
- grade

## 13. 数据看板页设计

### 13.1 页面目标

第一阶段后端可能还没有专门 dashboard 接口，所以前端可以先基于已有列表接口做简单统计。

看板展示：

1. 简历数量
2. 岗位数量
3. 分析报告数量
4. 投递记录数量
5. 已投递数量
6. Offer 数量
7. 平均匹配分

### 13.2 页面布局

```text
顶部欢迎语
四个统计卡片
投递状态饼图
匹配分数柱状图
最近分析报告
最近投递记录
```

### 13.3 数据来源

| 数据 | 来源 |
| --- | --- |
| 简历数量 | `GET /api/resumes` |
| 岗位数量 | `GET /api/jobs` |
| 报告数量 | `GET /api/analysis/reports` |
| 投递数量 | `GET /api/applications` |
| 投递状态 | `GET /api/applications` |
| 匹配分数 | `GET /api/analysis/reports` |

### 13.4 后续建议新增接口

后续后端可以新增：

```http
GET /api/dashboard/summary
```

返回：

```json
{
  "totalResumes": 3,
  "totalJobs": 12,
  "totalReports": 8,
  "totalApplications": 10,
  "appliedCount": 5,
  "offerCount": 1,
  "averageMatchScore": 76.5
}
```

## 14. 简历管理页设计

### 14.1 页面目标

简历管理页需要完成：

1. 上传简历；
2. 查询简历列表；
3. 查看简历详情；
4. 设置默认简历；
5. 删除简历。

### 14.2 页面布局

```text
页面标题：简历管理
上传区域
简历列表表格
详情弹窗
```

### 14.3 表格字段

| 字段 | 说明 |
| --- | --- |
| resumeName | 简历名称 |
| originalFileName | 原始文件名 |
| fileType | 文件类型 |
| fileSize | 文件大小 |
| parseStatus | 解析状态 |
| isDefault | 是否默认 |
| createdAt | 上传时间 |
| 操作 | 详情 / 设为默认 / 删除 |

### 14.4 上传交互

上传前端使用：

```text
el-upload
```

注意：

1. 限制文件类型 `.pdf,.docx`；
2. 上传时携带 Token；
3. 上传成功后刷新列表；
4. 上传失败显示错误信息。

### 14.5 ResumeList.vue 核心逻辑

```vue
<template>
  <div>
    <el-card>
      <template #header>
        <div class="page-header">
          <span>简历管理</span>
        </div>
      </template>

      <el-upload
        :http-request="handleUpload"
        accept=".pdf,.docx"
        :show-file-list="false"
      >
        <el-button type="primary">上传简历</el-button>
      </el-upload>

      <el-table :data="list" style="margin-top: 20px">
        <el-table-column prop="resumeName" label="简历名称" />
        <el-table-column prop="originalFileName" label="文件名" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column prop="parseStatus" label="解析状态" width="120" />
        <el-table-column label="默认" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button size="small" @click="setDefault(row)">默认</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  uploadResumeApi,
  getResumeListApi,
  deleteResumeApi,
  setDefaultResumeApi
} from '@/api/resume'

const list = ref<any[]>([])

async function loadList() {
  const res: any = await getResumeListApi({
    pageNum: 1,
    pageSize: 10
  })
  list.value = res.records
}

async function handleUpload(options: any) {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('resumeName', options.file.name)

  await uploadResumeApi(formData)
  ElMessage.success('上传成功')
  loadList()
}

function viewDetail(row: any) {
  // 后续可跳转详情页或打开弹窗
}

async function setDefault(row: any) {
  await setDefaultResumeApi(row.resumeId)
  ElMessage.success('设置成功')
  loadList()
}

async function remove(row: any) {
  await deleteResumeApi(row.resumeId)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>
```

## 15. 岗位 JD 管理页设计

### 15.1 页面目标

岗位管理页需要完成：

1. 创建岗位 JD；
2. 查询岗位列表；
3. 关键词搜索；
4. 岗位类型筛选；
5. 查看岗位详情；
6. 修改岗位；
7. 删除岗位。

### 15.2 页面布局

```text
搜索区域
新增岗位按钮
岗位列表表格
新增 / 编辑弹窗
详情弹窗
```

### 15.3 搜索字段

- keyword
- jobType
- location

### 15.4 表格字段

| 字段 | 说明 |
| --- | --- |
| companyName | 公司名称 |
| jobTitle | 岗位名称 |
| jobType | 岗位类型 |
| location | 工作地点 |
| sourcePlatform | 来源平台 |
| salaryRange | 薪资范围 |
| createdAt | 创建时间 |
| 操作 | 详情 / 编辑 / 删除 |

## 16. AI 匹配分析页设计

### 16.1 页面目标

AI 分析页是前端核心展示页面。

需要完成：

1. 选择简历；
2. 选择岗位；
3. 点击开始分析；
4. 展示加载状态；
5. 展示匹配分数；
6. 展示匹配等级；
7. 展示优势；
8. 展示短板；
9. 展示缺失技能；
10. 展示优化建议；
11. 展示面试准备建议；
12. 支持强制重新分析；
13. 支持基于分析结果创建投递记录。

### 16.2 页面布局

```text
左侧：选择简历、选择岗位、forceRefresh、开始分析按钮
右侧：分析结果卡片
底部：建议列表、面试准备建议
```

### 16.3 结果展示设计

匹配分数可以用：

```text
el-progress
```

例如：

```vue
<el-progress
  type="dashboard"
  :percentage="result.matchScore"
/>
```

匹配等级用：

```text
el-tag
```

技能缺口用：

```text
el-tag 列表
```

建议用：

```text
el-card + el-timeline / el-list
```

### 16.4 AnalysisMatch.vue 核心逻辑

```vue
<template>
  <el-card>
    <template #header>AI 匹配分析</template>

    <el-form label-width="100px">
      <el-form-item label="选择简历">
        <el-select v-model="form.resumeId" placeholder="请选择简历" style="width: 300px">
          <el-option
            v-for="item in resumes"
            :key="item.resumeId"
            :label="item.resumeName || item.originalFileName"
            :value="item.resumeId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="选择岗位">
        <el-select v-model="form.jobId" placeholder="请选择岗位" style="width: 300px">
          <el-option
            v-for="item in jobs"
            :key="item.jobId"
            :label="`${item.companyName} - ${item.jobTitle}`"
            :value="item.jobId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="重新分析">
        <el-switch v-model="form.forceRefresh" />
      </el-form-item>

      <el-button type="primary" :loading="loading" @click="handleAnalyze">
        开始 AI 分析
      </el-button>
    </el-form>
  </el-card>

  <el-card v-if="result" style="margin-top: 20px">
    <template #header>
      <div class="result-header">
        <span>分析结果</span>
        <el-tag>{{ result.matchLevel }}</el-tag>
      </div>
    </template>

    <div class="score-box">
      <el-progress type="dashboard" :percentage="result.matchScore" />
      <div>匹配分数</div>
    </div>

    <h3>简历优势</h3>
    <el-tag
      v-for="item in result.strengths"
      :key="item"
      type="success"
      style="margin: 4px"
    >
      {{ item }}
    </el-tag>

    <h3>简历短板</h3>
    <el-tag
      v-for="item in result.weaknesses"
      :key="item"
      type="warning"
      style="margin: 4px"
    >
      {{ item }}
    </el-tag>

    <h3>缺失技能</h3>
    <el-tag
      v-for="item in result.missingSkills"
      :key="item"
      type="danger"
      style="margin: 4px"
    >
      {{ item }}
    </el-tag>

    <h3>优化建议</h3>
    <el-timeline>
      <el-timeline-item v-for="item in result.suggestions" :key="item">
        {{ item }}
      </el-timeline-item>
    </el-timeline>

    <h3>面试准备建议</h3>
    <el-timeline>
      <el-timeline-item v-for="item in result.interviewTips" :key="item">
        {{ item }}
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getResumeListApi } from '@/api/resume'
import { getJobListApi } from '@/api/job'
import { matchAnalysisApi } from '@/api/analysis'

const resumes = ref<any[]>([])
const jobs = ref<any[]>([])
const result = ref<any>(null)
const loading = ref(false)

const form = reactive({
  resumeId: undefined,
  jobId: undefined,
  forceRefresh: false
})

async function loadOptions() {
  const resumeRes: any = await getResumeListApi({ pageNum: 1, pageSize: 100 })
  resumes.value = resumeRes.records

  const jobRes: any = await getJobListApi({ pageNum: 1, pageSize: 100 })
  jobs.value = jobRes.records
}

async function handleAnalyze() {
  if (!form.resumeId || !form.jobId) {
    ElMessage.warning('请选择简历和岗位')
    return
  }

  loading.value = true
  try {
    result.value = await matchAnalysisApi(form)
    ElMessage.success('分析完成')
  } finally {
    loading.value = false
  }
}

onMounted(loadOptions)
</script>

<style scoped>
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score-box {
  text-align: center;
  margin-bottom: 24px;
}
</style>
```

## 17. 分析报告列表页设计

### 17.1 页面目标

分析报告列表页需要完成：

1. 展示历史分析报告；
2. 支持最低分筛选；
3. 支持跳转详情；
4. 支持查看岗位信息；
5. 支持查看匹配分数。

### 17.2 表格字段

| 字段 | 说明 |
| --- | --- |
| reportId | 报告 ID |
| companyName | 公司 |
| jobTitle | 岗位 |
| matchScore | 匹配分数 |
| matchLevel | 匹配等级 |
| cacheHit | 是否缓存 |
| createdAt | 创建时间 |
| 操作 | 查看详情 |

## 18. 分析报告详情页设计

### 18.1 页面目标

报告详情页需要完整展示 AI 分析结果：

1. 匹配分数
2. 匹配等级
3. 简历优势
4. 简历短板
5. 缺失技能
6. 优化建议
7. 面试准备建议
8. AI 模型信息

### 18.2 展示重点

为了截图好看，建议使用：

1. 匹配分数仪表盘
2. 技能标签
3. 建议卡片
4. 时间线

## 19. 投递记录页设计

### 19.1 页面目标

投递记录页需要完成：

1. 查询投递记录列表；
2. 按状态筛选；
3. 按关键词搜索；
4. 创建投递记录；
5. 修改投递状态；
6. 修改备注和复盘；
7. 删除投递记录。

### 19.2 投递状态展示

状态建议映射颜色：

| 状态 | 颜色 |
| --- | --- |
| TO_APPLY | info |
| APPLIED | primary |
| WRITTEN_TEST | warning |
| FIRST_INTERVIEW | warning |
| SECOND_INTERVIEW | warning |
| HR_INTERVIEW | warning |
| OFFER | success |
| REJECTED | danger |
| GIVEN_UP | info |

### 19.3 状态选项

```ts
export const applicationStatusOptions = [
  { label: '待投递', value: 'TO_APPLY' },
  { label: '已投递', value: 'APPLIED' },
  { label: '笔试中', value: 'WRITTEN_TEST' },
  { label: '一面', value: 'FIRST_INTERVIEW' },
  { label: '二面', value: 'SECOND_INTERVIEW' },
  { label: 'HR 面', value: 'HR_INTERVIEW' },
  { label: '已 Offer', value: 'OFFER' },
  { label: '被拒', value: 'REJECTED' },
  { label: '放弃', value: 'GIVEN_UP' }
]
```

## 20. 投递详情页设计

### 20.1 页面目标

投递详情页展示：

1. 公司名称
2. 岗位名称
3. 使用简历
4. AI 匹配分数
5. 匹配等级
6. 投递状态
7. 优先级
8. 投递日期
9. 面试时间
10. 备注
11. 复盘

### 20.2 后续增强

后续可以展示状态时间线：

```text
待投递
  ↓
已投递
  ↓
笔试中
  ↓
一面
  ↓
Offer
```

对应后端后续扩展：

```text
application_status_log
```

## 21. 组件设计

### 21.1 PageContainer.vue

用于统一页面容器。

```vue
<template>
  <div class="page-container">
    <div class="page-title">
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-title {
  margin-bottom: 20px;
}

.page-title p {
  color: #666;
}
</style>
```

### 21.2 StatCard.vue

用于数据看板统计卡片。

```vue
<template>
  <el-card>
    <div class="stat-card">
      <div>
        <div class="label">{{ label }}</div>
        <div class="value">{{ value }}</div>
      </div>
      <el-icon size="32">
        <component :is="icon" />
      </el-icon>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: number | string
  icon?: any
}>()
</script>

<style scoped>
.stat-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.label {
  color: #666;
}

.value {
  font-size: 28px;
  font-weight: bold;
  margin-top: 8px;
}
</style>
```

### 21.3 ResumeSelect.vue

用于 AI 分析页选择简历。

```vue
<template>
  <el-select
    :model-value="modelValue"
    placeholder="请选择简历"
    style="width: 100%"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-option
      v-for="item in resumes"
      :key="item.resumeId"
      :label="item.resumeName || item.originalFileName"
      :value="item.resumeId"
    />
  </el-select>
</template>

<script setup lang="ts">
defineProps<{
  modelValue?: number
  resumes: any[]
}>()

defineEmits(['update:modelValue'])
</script>
```

### 21.4 JobSelect.vue

用于 AI 分析页选择岗位。

```vue
<template>
  <el-select
    :model-value="modelValue"
    placeholder="请选择岗位"
    style="width: 100%"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-option
      v-for="item in jobs"
      :key="item.jobId"
      :label="`${item.companyName} - ${item.jobTitle}`"
      :value="item.jobId"
    />
  </el-select>
</template>

<script setup lang="ts">
defineProps<{
  modelValue?: number
  jobs: any[]
}>()

defineEmits(['update:modelValue'])
</script>
```

## 22. 状态管理设计

### 22.1 stores/auth.ts

```ts
import { defineStore } from 'pinia'
import { getToken, setToken, removeToken } from '@/utils/token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: null as any
  }),

  actions: {
    setLogin(token: string, user: any) {
      this.token = token
      this.user = user
      setToken(token)
    },

    logout() {
      this.token = null
      this.user = null
      removeToken()
    }
  }
})
```

## 23. main.ts 配置

```ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './styles/index.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
```

## 24. vite.config.ts 配置

### 24.1 路径别名

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173
  }
})
```

### 24.2 如果不想后端配 CORS，可以用代理

```ts
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

如果使用代理，Axios baseURL 可以设为空：

```env
VITE_API_BASE_URL=
```

## 25. 前后端接口对应关系

### 25.1 认证

| 页面 | 接口 |
| --- | --- |
| 登录页 | `POST /api/auth/login` |
| 注册页 | `POST /api/auth/register` |
| 顶部用户信息 | `GET /api/user/me` |

### 25.2 简历

| 页面功能 | 接口 |
| --- | --- |
| 上传简历 | `POST /api/resumes/upload` |
| 简历列表 | `GET /api/resumes` |
| 简历详情 | `GET /api/resumes/{id}` |
| 设置默认 | `PUT /api/resumes/{id}/default` |
| 删除简历 | `DELETE /api/resumes/{id}` |

### 25.3 岗位

| 页面功能 | 接口 |
| --- | --- |
| 创建岗位 | `POST /api/jobs` |
| 岗位列表 | `GET /api/jobs` |
| 岗位详情 | `GET /api/jobs/{id}` |
| 修改岗位 | `PUT /api/jobs/{id}` |
| 删除岗位 | `DELETE /api/jobs/{id}` |

### 25.4 AI 分析

| 页面功能 | 接口 |
| --- | --- |
| 发起分析 | `POST /api/analysis/match` |
| 报告列表 | `GET /api/analysis/reports` |
| 报告详情 | `GET /api/analysis/reports/{id}` |

### 25.5 投递记录

| 页面功能 | 接口 |
| --- | --- |
| 创建投递 | `POST /api/applications` |
| 投递列表 | `GET /api/applications` |
| 投递详情 | `GET /api/applications/{id}` |
| 修改状态 | `PUT /api/applications/{id}/status` |
| 修改备注 | `PUT /api/applications/{id}/note` |
| 删除投递 | `DELETE /api/applications/{id}` |

## 26. UI 风格建议

### 26.1 整体风格

建议使用：

1. 简洁
2. 浅色
3. 科技感
4. 蓝白配色
5. 卡片式布局
6. 少量图表

### 26.2 页面重点

最适合截图展示的页面：

1. AI 匹配分析页
2. 分析报告详情页
3. 数据看板页
4. 投递记录页
5. Swagger 文档页

### 26.3 不建议第一阶段做的内容

第一阶段不要过度追求：

1. 复杂动画
2. 暗黑模式
3. 移动端适配
4. 复杂权限菜单
5. 多语言
6. 复杂主题切换

先把业务闭环做出来。

## 27. 前端开发顺序

推荐顺序：

1. 创建 Vue 3 + Vite 项目
2. 安装 Element Plus / Axios / Router / Pinia
3. 配置 Axios 请求封装
4. 配置 Token 存储
5. 配置路由守卫
6. 创建登录页
7. 创建注册页
8. 创建基础布局
9. 创建简历管理页
10. 创建岗位管理页
11. 创建 AI 分析页
12. 创建分析报告列表页
13. 创建投递记录页
14. 创建数据看板页
15. 做样式优化
16. 截图更新 README

## 28. 第一阶段验收标准

### 28.1 登录注册

- 可以注册用户；
- 可以登录用户；
- 登录后保存 Token；
- 未登录访问业务页跳转登录；
- Token 失效后跳转登录。

### 28.2 简历管理

- 可以上传 PDF 简历；
- 可以上传 DOCX 简历；
- 可以查看简历列表；
- 可以查看简历详情；
- 可以设置默认简历；
- 可以删除简历。

### 28.3 岗位管理

- 可以创建岗位；
- 可以查看岗位列表；
- 可以搜索岗位；
- 可以查看岗位详情；
- 可以修改岗位；
- 可以删除岗位。

### 28.4 AI 分析

- 可以选择简历；
- 可以选择岗位；
- 可以发起 AI 分析；
- 可以展示匹配分数；
- 可以展示优势、短板、缺失技能；
- 可以展示优化建议；
- 可以展示面试准备建议；
- 可以查看历史报告。

### 28.5 投递记录

- 可以创建投递记录；
- 可以查看投递列表；
- 可以筛选投递状态；
- 可以查看投递详情；
- 可以修改投递状态；
- 可以修改备注和复盘；
- 可以删除投递记录。

## 29. 后续增强方向

### 29.1 数据看板增强

后端新增：

```http
GET /api/dashboard/summary
```

前端展示：

1. 投递总数
2. 面试数量
3. Offer 数量
4. 平均匹配分
5. 投递状态分布
6. 岗位类型分布
7. 近 7 天投递趋势

### 29.2 AI 分析体验增强

可以增加：

1. 分析进度条
2. AI 分析加载动画
3. 分析历史对比
4. 一键创建投递记录
5. 导出分析报告 PDF

### 29.3 投递时间线

基于后端状态日志表：

```text
application_status_log
```

前端展示：

```text
待投递 → 已投递 → 笔试中 → 一面 → Offer
```

### 29.4 简历优化页面

后续可以新增：

1. AI 简历优化
2. AI 项目经历改写
3. AI 面试题生成
4. AI 自我介绍生成

## 30. README 截图建议

前端完成后，在 README 中加入：

1. 首页数据看板截图
2. 简历管理截图
3. 岗位管理截图
4. AI 分析结果截图
5. 投递记录截图
6. Swagger 接口文档截图

README 展示顺序：

1. 项目简介
2. 功能截图
3. 技术栈
4. 核心亮点
5. 启动方式
6. 接口文档
7. 项目结构
8. 后续规划

## 31. 面试讲解准备

前端部分面试可以这样讲：

前端第一阶段我采用 Vue 3 + Vite + Element Plus 实现，主要目标是完成后端接口的可视化展示和完整业务闭环演示。

页面包括登录注册、简历管理、岗位管理、AI 匹配分析、分析报告和投递记录。前端通过 Axios 统一封装请求，在请求拦截器中自动携带 JWT Token，在响应拦截器中统一处理 401 和业务异常。

路由方面使用 Vue Router，通过路由守卫判断是否登录，未登录访问业务页面会自动跳转到登录页。整体 UI 使用卡片、表格、标签、进度条和图表组件，让 AI 分析结果和投递状态更直观。

## 32. 前端设计结论

InternPilot 前端第一阶段的目标是：

```text
做出完整可演示闭环
而不是追求复杂 UI
```

最重要的是跑通：

```text
注册登录
  ↓
上传简历
  ↓
创建岗位
  ↓
AI 分析
  ↓
查看报告
  ↓
创建投递记录
  ↓
跟踪投递状态
```

推荐采用：

```text
Vue 3 + Vite + TypeScript + Element Plus + Axios + Pinia + Vue Router + ECharts
```

完成前端后，InternPilot 就可以从“后端项目”升级为“完整应用项目”，更适合 GitHub 展示、README 截图、课程汇报和暑期实习面试。
