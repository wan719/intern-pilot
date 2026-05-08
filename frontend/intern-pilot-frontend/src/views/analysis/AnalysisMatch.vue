<template>
  <PageContainer title="AI 匹配分析" description="选择简历和岗位，创建异步分析任务并实时查看执行进度。">
    <div class="analysis-grid">
      <section class="panel">
        <div class="panel-header">
          <h3>创建分析任务</h3>
        </div>

        <el-form :model="form" label-position="top">
          <el-form-item label="简历">
            <el-select v-model="form.resumeId" placeholder="请选择简历" filterable>
              <el-option
                v-for="item in resumes"
                :key="item.resumeId"
                :label="item.resumeName || item.originalFileName"
                :value="item.resumeId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="岗位">
            <el-select v-model="form.jobId" placeholder="请选择岗位" filterable>
              <el-option
                v-for="item in jobs"
                :key="item.jobId"
                :label="`${item.companyName} - ${item.jobTitle}`"
                :value="item.jobId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="强制重新分析">
            <el-switch v-model="form.forceRefresh" />
          </el-form-item>

          <el-button type="primary" :icon="MagicStick" :loading="running" @click="startTask">
            开始 AI 分析
          </el-button>
        </el-form>
      </section>

      <section class="panel result-panel">
        <div class="panel-header">
          <h3>任务进度</h3>
          <el-tag v-if="task.taskNo" :type="statusTagType">{{ task.status }}</el-tag>
        </div>

        <template v-if="task.taskNo">
          <el-steps :active="activeStep" finish-status="success" simple>
            <el-step title="读取资料" />
            <el-step title="构建 Prompt" />
            <el-step title="AI 分析" />
            <el-step title="生成报告" />
          </el-steps>

          <el-progress
            class="task-progress"
            :percentage="task.progress"
            :status="progressStatus"
            :stroke-width="10"
          />

          <p class="progress-message">{{ task.message }}</p>

          <el-alert
            v-if="task.status === 'FAILED'"
            type="error"
            :title="task.errorMessage || '分析失败'"
            show-icon
          />

          <div v-if="task.status === 'SUCCESS'" class="success-actions">
            <el-result icon="success" title="分析完成" :sub-title="`报告 ID：${task.reportId || '-'}`" />
            <el-button type="success" @click="goReports">查看分析报告</el-button>
          </div>
        </template>

        <el-empty v-else description="创建任务后将在这里显示实时进度" />
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import type { Client } from '@stomp/stompjs'
import PageContainer from '@/components/common/PageContainer.vue'
import router from '@/router'
import { createAnalysisTaskApi, getAnalysisTaskDetailApi } from '@/api/analysisTask'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import type { AnalysisProgressMessage } from '@/utils/analysisSocket'

const resumes = ref<any[]>([])
const jobs = ref<any[]>([])
const running = ref(false)
let stompClient: Client | null = null
let pollingTimer: number | undefined

const form = reactive({
  resumeId: undefined as number | undefined,
  jobId: undefined as number | undefined,
  forceRefresh: false
})

const task = reactive({
  taskNo: '',
  status: '',
  progress: 0,
  message: '',
  reportId: undefined as number | undefined,
  errorMessage: ''
})

const activeStep = computed(() => {
  if (task.progress < 30) return 1
  if (task.progress < 70) return 2
  if (task.progress < 95) return 3
  return 4
})

const progressStatus = computed(() => {
  if (task.status === 'FAILED') return 'exception'
  if (task.status === 'SUCCESS') return 'success'
  return undefined
})

const statusTagType = computed(() => {
  if (task.status === 'SUCCESS') return 'success'
  if (task.status === 'FAILED') return 'danger'
  if (task.status === 'RUNNING') return 'warning'
  return 'info'
})

async function loadOptions() {
  const [resumeRes, jobRes]: any[] = await Promise.all([
    getResumeListApi({ pageNum: 1, pageSize: 100 }),
    getJobListApi({ pageNum: 1, pageSize: 100 })
  ])
  resumes.value = resumeRes.records || []
  jobs.value = jobRes.records || []
}

async function startTask() {
  if (!form.resumeId || !form.jobId) {
    ElMessage.warning('请选择简历和岗位')
    return
  }

  cleanupTaskWatchers()
  running.value = true
  task.errorMessage = ''

  try {
    const res: any = await createAnalysisTaskApi(form)
    applyTaskMessage(res)
    connectSocket(res.taskNo)
    startPolling(res.taskNo)
  } catch {
    running.value = false
  }
}

function connectSocket(taskNo: string) {
  import('@/utils/analysisSocket')
    .then(({ createAnalysisSocket }) => {
      stompClient = createAnalysisSocket(
        taskNo,
        (message) => applyTaskMessage(message),
        () => {
          ElMessage.warning('WebSocket 连接异常，已使用轮询兜底')
        }
      )
    })
    .catch(() => {
      ElMessage.warning('WebSocket 初始化失败，已使用轮询兜底')
    })
}

function startPolling(taskNo: string) {
  pollingTimer = window.setInterval(async () => {
    const detail: any = await getAnalysisTaskDetailApi(taskNo)
    applyTaskMessage(detail)
  }, 3000)
}

function applyTaskMessage(message: AnalysisProgressMessage) {
  task.taskNo = message.taskNo
  task.status = message.status
  task.progress = message.progress || 0
  task.message = message.message || ''
  task.reportId = message.reportId
  task.errorMessage = message.errorMessage || ''

  if (message.status === 'SUCCESS') {
    running.value = false
    cleanupTaskWatchers()
    ElMessage.success('AI 分析完成')
  }

  if (message.status === 'FAILED') {
    running.value = false
    cleanupTaskWatchers()
    ElMessage.error(message.errorMessage || 'AI 分析失败')
  }
}

function cleanupTaskWatchers() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
    pollingTimer = undefined
  }
}

function goReports() {
  router.push('/analysis/reports')
}

onMounted(loadOptions)
onBeforeUnmount(cleanupTaskWatchers)
</script>

<style scoped>
.analysis-grid {
  display: grid;
  grid-template-columns: minmax(280px, 360px) 1fr;
  gap: 20px;
}

.task-progress {
  margin-top: 24px;
}

.progress-message {
  margin: 16px 0;
  color: #606266;
}

.success-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

@media (max-width: 900px) {
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}
</style>
