const closed = new Set(['REJECTED', 'GIVEN_UP'])

function dateValue(value?: string | null) {
  return value ? new Date(value).getTime() : Number.NaN
}

export function upcomingInterviews<T extends { interviewDate?: string | null; status?: string }>(items: T[], now: number) {
  return items
    .map(item => ({ item, time: dateValue(item.interviewDate) }))
    .filter(({ item, time }) => time >= now && !closed.has(item.status ?? ''))
    .sort((a, b) => a.time - b.time)
    .slice(0, 3)
    .map(({ item }) => item)
}

export function newestWorkspaceItem<T extends { createdAt?: string | null }>(items: T[]): T | undefined {
  let newest: T | undefined
  let latest = Number.NEGATIVE_INFINITY
  for (const item of items) {
    const time = dateValue(item.createdAt)
    if (time > latest) {
      latest = time
      newest = item
    }
  }
  return newest
}
