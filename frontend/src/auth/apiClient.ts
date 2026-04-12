import { ApiError } from './ApiError'

export interface ApiFetchOptions {
  method?: string
  body?: unknown
  token: string | null
  onUnauthorized: () => void
  signal?: AbortSignal
}

export async function apiFetch<T>(url: string, options: ApiFetchOptions): Promise<T> {
  const { method = 'GET', body, token, onUnauthorized, signal } = options

  const headers: Record<string, string> = { Accept: 'application/json' }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const init: RequestInit = { method, headers, signal }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(body)
  }

  let response: Response
  try {
    response = await fetch(url, init)
  } catch {
    throw ApiError.network()
  }

  if (!response.ok) {
    const error = await ApiError.fromResponse(response)
    if (response.status === 401) {
      onUnauthorized()
    }
    throw error
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
