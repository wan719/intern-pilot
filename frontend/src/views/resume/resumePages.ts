export interface ResumeDetails {
  resumeId: number
  resumeName?: string
  originalFileName?: string
  fileType: 'PDF' | 'DOCX'
  fileSize?: number
  parsedText?: string
  parseStatus?: string
  isDefault?: boolean
}

export interface ResumeVersionSummary {
  versionId: number
  versionName: string
  isCurrent: number
}

export interface ResumeVersionContent extends ResumeVersionSummary {
  content: string
  sourceVersionId?: number | null
}

export function positiveId(value: unknown): number | undefined {
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) return
  const id = Number(value)
  return Number.isSafeInteger(id) ? id : undefined
}

export function resumeTitle(resume: ResumeDetails) {
  return resume.resumeName || resume.originalFileName || '未命名简历'
}
