import type { LocationQuery } from 'vue-router'
import { getAnalysisReportDetailApi } from '@/api/analysis'
import { getApplicationDetailApi } from '@/api/application'
import { getInterviewQuestionReportsApi } from '@/api/interviewQuestion'
import { getJobDetailApi } from '@/api/job'
import { getResumeDetailApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'

export interface PreparationReport {
  reportId: number
  jobId: number
  resumeId: number
  resumeVersionId?: number | null
  companyName?: string
  jobTitle?: string
  matchScore?: number
}

interface ResumeOption {
  resumeId: number
  resumeName?: string
  originalFileName?: string
}

interface JobOption {
  jobId: number
  companyName: string
  jobTitle: string
}

interface ResumeVersion {
  versionId: number
  versionName: string
  isCurrent: number
}

interface ApplicationSource {
  applicationId: number
  jobId: number
  resumeId?: number | null
  reportId?: number | null
}

export type PreparationSource = { kind: 'report' | 'application'; id: number }
export type PreparationResult =
  | { kind: 'practice'; id: number }
  | { kind: 'unavailable' }
  | {
      kind: 'form'
      job?: JobOption
      resume?: ResumeOption
      versions: ResumeVersion[]
      versionId?: number
      report?: PreparationReport
      notes: string[]
    }

const requestOptions = { silentError: true }

export function preparationSource(query: LocationQuery): PreparationSource | 'invalid' | undefined {
  const application = query.applicationId
  const report = query.reportId
  if (application === undefined && report === undefined) return
  if (application !== undefined && report !== undefined) return 'invalid'
  const value = application ?? report
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value) || !Number.isSafeInteger(Number(value))) return 'invalid'
  return { kind: application === undefined ? 'report' : 'application', id: Number(value) }
}

function isUnavailable(error: unknown) {
  const failure = error as { code?: number; message?: string; response?: { status?: number; data?: { message?: string } } } | null
  const code = Number(failure?.response?.status ?? failure?.code)
  if (code === 403 || code === 404 || code === 410) return true
  // Some detail endpoints return a business error for both missing and inaccessible records.
  return /不存在|无权限访问|已被删除/.test(failure?.response?.data?.message ?? failure?.message ?? '')
}

async function ifAvailable<T>(read: () => Promise<T>): Promise<T | undefined> {
  try {
    return await read()
  } catch (error) {
    if (!isUnavailable(error)) throw error
  }
}

async function findPractice(jobId: number, resumeId?: number) {
  const pageSize = 20
  let pageNum = 1
  for (;;) {
    const page = await getInterviewQuestionReportsApi({ jobId, resumeId, pageNum, pageSize }, requestOptions) as unknown as {
      total: number
      records: { reportId: number; jobId: number; resumeId?: number; questionCount: number; status?: string }[]
    }
    if (!Array.isArray(page?.records)) throw new Error('题单列表不可用')
    const match = page.records.find(item => item.jobId === jobId
      && (resumeId === undefined || item.resumeId === resumeId)
      && item.questionCount > 0 && (!item.status || item.status === 'COMPLETED')
      && Number.isSafeInteger(item.reportId) && item.reportId > 0)
    if (match) return match.reportId
    if (!page.records.length || pageNum * pageSize >= page.total || !Number.isFinite(page.total)) return
    pageNum++
  }
}

export function reportMatchesSelection(report: PreparationReport, resumeId?: number, jobId?: number, versionId?: number) {
  return report.resumeId === resumeId && report.jobId === jobId
    && (report.resumeVersionId == null || report.resumeVersionId === versionId)
}

export async function loadInterviewPreparation(
  source: PreparationSource,
  options: { jobs: JobOption[]; resumes: ResumeOption[] }
): Promise<PreparationResult> {
  const loadReport = (id: number) => ifAvailable(async () => await getAnalysisReportDetailApi(id, requestOptions) as unknown as PreparationReport)
  const application = source.kind === 'application'
    ? await ifAvailable(async () => await getApplicationDetailApi(source.id, requestOptions) as unknown as ApplicationSource)
    : undefined
  const sourceReport = source.kind === 'report' ? await loadReport(source.id) : undefined
  if (source.kind === 'application' ? application?.applicationId !== source.id : sourceReport?.reportId !== source.id) {
    return { kind: 'unavailable' }
  }

  const jobId = application?.jobId ?? sourceReport?.jobId
  const resumeId = application?.resumeId ?? sourceReport?.resumeId
  const [jobOption, resumeOption] = await Promise.all([
    jobId == null ? undefined : options.jobs.find(item => item.jobId === jobId)
      ?? ifAvailable(async () => await getJobDetailApi(jobId, requestOptions) as unknown as JobOption),
    resumeId == null ? undefined : options.resumes.find(item => item.resumeId === resumeId)
      ?? ifAvailable(async () => await getResumeDetailApi(resumeId, requestOptions) as unknown as ResumeOption)
  ])

  const job = jobOption?.jobId === jobId ? jobOption : undefined
  let resume = resumeOption?.resumeId === resumeId ? resumeOption : undefined
  if (application && job && (resumeId == null || resume)) {
    const id = await findPractice(job.jobId, resume?.resumeId)
    if (id !== undefined) return { kind: 'practice', id }
  }

  const verifiedResumeId = resume?.resumeId
  const loadedVersions = verifiedResumeId != null
    ? await ifAvailable(async () => await getResumeVersionListApi(verifiedResumeId, requestOptions) as unknown as ResumeVersion[])
    : undefined
  if (resume && !loadedVersions) resume = undefined
  const versions = loadedVersions ?? []
  const notes: string[] = []
  if (!job) notes.push('原岗位已不可用，请重新选择岗位。')
  if (!resume) notes.push(resumeId == null ? '这条投递未关联简历，请选择要用于面试准备的简历。' : '原简历已不可用，请重新选择简历。')

  const reportId = application?.reportId
  const candidate = sourceReport ?? (reportId != null && job && resume ? await loadReport(reportId) : undefined)
  const expectedReportId = sourceReport ? source.id : reportId
  const hasCandidate = candidate != null && candidate.reportId === expectedReportId
  const matchingMaterials = hasCandidate && !!job && !!resume
    && candidate.jobId === job.jobId && candidate.resumeId === resume.resumeId
  const pinnedVersion = matchingMaterials && candidate.resumeVersionId != null
    ? versions.find(item => item.versionId === candidate.resumeVersionId)
    : undefined
  const report = matchingMaterials && (pinnedVersion || (!application && candidate.resumeVersionId == null))
    ? candidate
    : undefined
  if ((sourceReport || reportId != null) && !report) {
    notes.push(matchingMaterials ? '报告对应的简历版本不可用，已取消关联报告。' : '原匹配报告无法与当前岗位和简历关联。')
  }

  return {
    kind: 'form', job, resume, versions, report, notes,
    versionId: pinnedVersion?.versionId ?? versions.find(item => item.isCurrent === 1)?.versionId
  }
}
