export type InterviewGenerationKind = 'INTERVIEW_QUESTION' | 'INTERVIEW_REGENERATE'

export type InterviewGenerationOutcome =
  | { ok: true; reportId: number }
  | { ok: false; error: unknown }

export interface ActiveInterviewGeneration {
  kind: InterviewGenerationKind
  signature: string
  localTaskId: string
  reportId?: number
  promise: Promise<InterviewGenerationOutcome>
}

const activeGenerations = new Map<string, ActiveInterviewGeneration>()

export function activeInterviewGenerations(kind?: InterviewGenerationKind): ActiveInterviewGeneration[] {
  return Array.from(activeGenerations.values()).filter((entry) => !kind || entry.kind === kind)
}

export function findActiveInterviewGeneration(kind: InterviewGenerationKind, signature?: string) {
  return activeInterviewGenerations(kind).find((entry) => signature === undefined || entry.signature === signature)
}

export function registerInterviewGeneration(options: {
  kind: InterviewGenerationKind
  signature: string
  localTaskId: string
  reportId?: number
  run: () => Promise<InterviewGenerationOutcome>
}): ActiveInterviewGeneration {
  const key = generationKey(options.kind, options.signature)
  const existing = activeGenerations.get(key)
  if (existing) return existing

  const entry = {
    kind: options.kind,
    signature: options.signature,
    localTaskId: options.localTaskId,
    reportId: options.reportId,
    promise: Promise.resolve().then(options.run)
  } satisfies ActiveInterviewGeneration
  activeGenerations.set(key, entry)
  void entry.promise.finally(() => {
    if (activeGenerations.get(key) === entry) activeGenerations.delete(key)
  })
  return entry
}

function generationKey(kind: InterviewGenerationKind, signature: string) {
  return `${kind}:${signature}`
}
