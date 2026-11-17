const prefix = 'internpilot:admin-work-tabs:v1:'

export function readAdminTabSession(account: number | null): string[] {
  if (account == null) return []
  try {
    const paths: unknown = JSON.parse(sessionStorage.getItem(prefix + account) ?? '[]')
    return Array.isArray(paths) ? paths.filter((path): path is string => typeof path === 'string') : []
  } catch {
    return []
  }
}

export function saveAdminTabSession(account: number | null, paths: string[]) {
  if (account == null) return
  try {
    if (paths.length) sessionStorage.setItem(prefix + account, JSON.stringify(paths))
    else sessionStorage.removeItem(prefix + account)
  } catch {
    // The live tab list still works when session storage is unavailable.
  }
}
