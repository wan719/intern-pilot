<template>
  <PageContainer title="简历管理" description="上传 PDF / DOCX 简历，查看解析结果，并设置默认简历。">
    <template #actions>
      <el-button type="primary" :icon="Upload" @click="uploadVisible = true">上传简历</el-button>
    </template>

    <section class="panel">
      <el-table v-loading="loading" :data="resumes">
        <el-table-column prop="resumeName" label="简历名称" min-width="180" />
        <el-table-column prop="originalFileName" label="原文件" min-width="180" />
        <el-table-column prop="fileType" label="类型" width="90" />
        <el-table-column prop="parseStatus" label="解析状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.parseStatus === 'SUCCESS' ? 'success' : 'warning'">{{ row.parseStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="primary">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.resumeId)">详情</el-button>
            <el-button link type="primary" @click="setDefault(row.resumeId)">设默认</el-button>
            <el-button link type="danger" @click="removeResume(row.resumeId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="uploadVisible" title="上传简历" width="460px">
      <el-form label-position="top">
        <el-form-item label="简历名称">
          <el-input v-model="uploadForm.resumeName" placeholder="例如：Java 后端实习简历" />
        </el-form-item>
        <el-form-item label="文件">
          <el-upload drag :auto-upload="false" :limit="1" :on-change="onFileChange" :on-remove="onFileRemove">
            <el-icon><UploadFilled /></el-icon>
            <div>拖拽 PDF / DOCX 到这里，或点击选择</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="简历详情" size="48%">
      <div v-if="detail" class="detail-stack">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">{{ detail.resumeName }}</el-descriptions-item>
          <el-descriptions-item label="文件">{{ detail.originalFileName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.fileType }}</el-descriptions-item>
          <el-descriptions-item label="大小">{{ detail.fileSize }}</el-descriptions-item>
        </el-descriptions>
        <div class="text-preview">{{ detail.parsedText }}</div>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { deleteResumeApi, getResumeDetailApi, getResumeListApi, setDefaultResumeApi, uploadResumeApi } from '@/api/resume'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const uploading = ref(false)
const uploadVisible = ref(false)
const detailVisible = ref(false)
const resumes = ref<any[]>([])
const detail = ref<any>(null)
const selectedFile = ref<File>()
const uploadForm = reactive({ resumeName: '' })

async function loadResumes() {
  loading.value = true
  try {
    const res: any = await getResumeListApi({ pageNum: 1, pageSize: 100 })
    resumes.value = res.records || []
  } finally {
    loading.value = false
  }
}

function onFileChange(file: UploadFile) {
  selectedFile.value = file.raw
}

function onFileRemove() {
  selectedFile.value = undefined
}

async function submitUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请选择简历文件')
    return
  }
  const data = new FormData()
  data.append('file', selectedFile.value)
  data.append('resumeName', uploadForm.resumeName || selectedFile.value.name)
  uploading.value = true
  try {
    await uploadResumeApi(data)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    selectedFile.value = undefined
    uploadForm.resumeName = ''
    loadResumes()
  } finally {
    uploading.value = false
  }
}

async function openDetail(id: number) {
  detail.value = await getResumeDetailApi(id)
  detailVisible.value = true
}

async function setDefault(id: number) {
  await setDefaultResumeApi(id)
  ElMessage.success('已设置默认简历')
  loadResumes()
}

async function removeResume(id: number) {
  await ElMessageBox.confirm('确认删除这份简历吗？', '删除简历', { type: 'warning' })
  await deleteResumeApi(id)
  ElMessage.success('已删除')
  loadResumes()
}

onMounted(loadResumes)
</script>
