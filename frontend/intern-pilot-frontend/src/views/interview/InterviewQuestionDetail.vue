<template>
  <PageContainer title="面试题练习" :description="detail?.title || '先看题目自测，再展开参考答案和答题要点。'">
    <template #actions>
      <el-button @click="router.push('/interview-questions')">返回列表</el-button>
    </template>

    <section v-if="loading" class="panel">
      <el-skeleton :rows="8" animated />
    </section>

    <section v-else-if="errorText" class="panel state-panel">
      <el-result icon="error" title="面试题加载失败" :sub-title="errorText">
        <template #extra>
          <el-button type="primary" @click="loadDetail">重试</el-button>
          <el-button @click="router.push('/interview-questions')">返回列表</el-button>
        </template>
      </el-result>
    </section>

    <section v-else-if="detail" class="panel">
      <div class="panel-header">
        <div>
          <h3>{{ detail.title }}</h3>
          <span>{{ detail.companyName }} - {{ detail.jobTitle }} · {{ detail.questionCount }} 道题</span>
        </div>
        <el-tag>{{ formatDateTime(detail.createdAt) }}</el-tag>
      </div>

      <el-empty v-if="groupedQuestions.length === 0" description="这份报告暂时没有题目" />

      <el-collapse v-else v-model="activeNames">
        <el-collapse-item
          v-for="group in groupedQuestions"
          :key="group.type"
          :name="group.type"
        >
          <template #title>
            <div class="group-title">
              <strong>{{ questionTypeLabel(group.type) }}</strong>
              <el-tag size="small" type="info">{{ group.items.length }} 题</el-tag>
            </div>
          </template>

          <div class="question-list">
            <article v-for="item in group.items" :key="item.questionId" class="question-item">
              <div class="question-heading">
                <h4>{{ item.sortOrder }}. {{ item.question }}</h4>
                <div class="question-actions">
                  <el-tag :type="difficultyTagType(item.difficulty)">
                    {{ difficultyLabel(item.difficulty) }}
                  </el-tag>
                  <el-button link type="primary" @click.stop="toggleAnswer(item.questionId)">
                    {{ isAnswerVisible(item.questionId) ? '隐藏答案' : '查看答案' }}
                  </el-button>
                </div>
              </div>

              <el-alert
                v-if="!isAnswerVisible(item.questionId)"
                class="practice-hint"
                type="info"
                :closable="false"
                title="先尝试口述 1-2 分钟，再展开参考答案。"
              />

              <template v-else>
                <p class="answer">{{ item.answer || '暂无参考答案' }}</p>

                <div class="tag-section">
                  <h4>答题要点</h4>
                  <el-tag v-for="point in item.answerPoints || []" :key="point" type="success">
                    {{ point }}
                  </el-tag>
                  <span v-if="!item.answerPoints?.length" class="empty-text">暂无要点</span>
                </div>

                <div class="tag-section">
                  <h4>相关技能</h4>
                  <el-tag v-for="skill in item.relatedSkills || []" :key="skill" type="primary">
                    {{ skill }}
                  </el-tag>
                  <span v-if="!item.relatedSkills?.length" class="empty-text">暂无技能标签</span>
                </div>
              </template>
            </article>
          </div>
        </el-collapse-item>
      </el-collapse>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import { getInterviewQuestionDetailApi } from '@/api/interviewQuestion'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const detail = ref<any>(null)
const loading = ref(false)
const errorText = ref('')
const activeNames = ref<string[]>([])
const visibleAnswerIds = ref<Set<number>>(new Set())

const groupedQuestions = computed(() => {
  const map = new Map<string, any[]>()
  for (const item of detail.value?.questions || []) {
    const type = item.questionType || 'JOB_SKILL'
    if (!map.has(type)) {
      map.set(type, [])
    }
    map.get(type)!.push(item)
  }

  return Array.from(map.entries()).map(([type, items]) => ({ type, items }))
})

function questionTypeLabel(type: string) {
  const labels: Record<string, string> = {
    JAVA_BASIC: 'Java 基础',
    SPRING_BOOT: 'Spring Boot',
    SPRING_SECURITY: 'Spring Security',
    MYSQL: 'MySQL',
    REDIS: 'Redis',
    PROJECT: '项目追问',
    ALGORITHM: '算法与数据结构',
    SYSTEM_DESIGN: '系统设计',
    HR: 'HR 面试',
    RESUME: '简历深挖',
    JOB_SKILL: '岗位技能专项'
  }
  return labels[type] || type
}

function difficultyLabel(difficulty: string) {
  const labels: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '较难'
  }
  return labels[difficulty] || difficulty
}

function difficultyTagType(difficulty: string) {
  if (difficulty === 'EASY') return 'success'
  if (difficulty === 'HARD') return 'danger'
  return 'warning'
}

function isAnswerVisible(questionId: number) {
  return visibleAnswerIds.value.has(questionId)
}

function toggleAnswer(questionId: number) {
  const next = new Set(visibleAnswerIds.value)
  if (next.has(questionId)) {
    next.delete(questionId)
  } else {
    next.add(questionId)
  }
  visibleAnswerIds.value = next
}

async function loadDetail() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    errorText.value = '报告 ID 不正确'
    return
  }

  loading.value = true
  errorText.value = ''
  detail.value = null
  visibleAnswerIds.value = new Set()

  try {
    detail.value = await getInterviewQuestionDetailApi(id)
    activeNames.value = groupedQuestions.value.map((group) => group.type)
  } catch {
    errorText.value = '报告不存在、已被删除，或当前账号没有访问权限。'
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.state-panel {
  min-height: 360px;
}

.group-title {
  display: flex;
  gap: 10px;
  align-items: center;
}

.question-list {
  display: grid;
  gap: 14px;
}

.question-item {
  padding: 14px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #f8fafc;
}

.question-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.question-heading h4 {
  margin: 0;
  line-height: 1.5;
}

.question-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
  align-items: center;
}

.practice-hint {
  margin-top: 12px;
}

.answer {
  margin: 12px 0 14px;
  color: #344054;
  line-height: 1.7;
  white-space: pre-wrap;
}

.empty-text {
  color: #98a2b3;
  font-size: 13px;
}

@media (max-width: 900px) {
  .question-heading {
    flex-direction: column;
  }
}
</style>
