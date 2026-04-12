export interface ApiFieldError {
  field: string
  message: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: ApiFieldError[]

  constructor(status: number, code: string, message: string, details: ApiFieldError[] = []) {
    super(message || `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }

  static async fromResponse(response: Response): Promise<ApiError> {
    const payload = await readJsonBody(response)
    const code = stringField(payload, 'code') ?? stringField(payload, 'error') ?? ''
    const message = stringField(payload, 'message') ?? `HTTP ${response.status}`
    const details = Array.isArray((payload as { details?: unknown } | null)?.details)
      ? ((payload as { details: unknown[] }).details.filter(isFieldError))
      : []
    return new ApiError(response.status, code, message, details)
  }

  static network(message = 'Could not reach the backend. Check your connection.'): ApiError {
    return new ApiError(0, 'NETWORK_ERROR', message)
  }
}

async function readJsonBody(response: Response): Promise<unknown> {
  try {
    const text = await response.text()
    return text ? JSON.parse(text) : null
  } catch {
    return null
  }
}

function stringField(payload: unknown, key: string): string | null {
  if (payload && typeof payload === 'object' && key in payload) {
    const value = (payload as Record<string, unknown>)[key]
    return typeof value === 'string' ? value : null
  }
  return null
}

function isFieldError(candidate: unknown): candidate is ApiFieldError {
  return (
    typeof candidate === 'object' &&
    candidate !== null &&
    typeof (candidate as ApiFieldError).field === 'string' &&
    typeof (candidate as ApiFieldError).message === 'string'
  )
}
