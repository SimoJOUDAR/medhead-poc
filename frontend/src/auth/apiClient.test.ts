import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './apiClient'
import { ApiError } from './ApiError'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function emptyResponse(status: number): Response {
  return new Response('', { status })
}

describe('apiFetch', () => {
  const onUnauthorized = vi.fn()

  beforeEach(() => {
    onUnauthorized.mockReset()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function stubFetch(response: Response) {
    const fetchMock = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>(
      () => Promise.resolve(response),
    )
    vi.stubGlobal('fetch', fetchMock)
    return fetchMock
  }

  it('injects a Bearer header when a token is present', async () => {
    const fetchMock = stubFetch(jsonResponse(200, { ok: true }))

    await apiFetch<{ ok: boolean }>('/api/v1/specialties', {
      token: 'jwt-token',
      onUnauthorized,
    })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const init = fetchMock.mock.calls[0][1]!
    const headers = init.headers as Record<string, string>
    expect(headers.Authorization).toBe('Bearer jwt-token')
  })

  it('omits the Authorization header when no token is present', async () => {
    const fetchMock = stubFetch(jsonResponse(200, {}))

    await apiFetch('/api/v1/specialties', { token: null, onUnauthorized })

    const init = fetchMock.mock.calls[0][1]!
    const headers = init.headers as Record<string, string>
    expect(headers.Authorization).toBeUndefined()
  })

  it('serializes the body as JSON and sets Content-Type for POST', async () => {
    const fetchMock = stubFetch(jsonResponse(200, {}))

    await apiFetch('/api/v1/emergency/recommend', {
      method: 'POST',
      body: { specialtyId: 1, latitude: 51.5, longitude: -0.1 },
      token: 'jwt-token',
      onUnauthorized,
    })

    const init = fetchMock.mock.calls[0][1]!
    const headers = init.headers as Record<string, string>
    expect(headers['Content-Type']).toBe('application/json')
    expect(init.body).toBe(JSON.stringify({ specialtyId: 1, latitude: 51.5, longitude: -0.1 }))
  })

  it('throws an ApiError with details[] on a 400 validation failure', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        jsonResponse(400, {
          timestamp: '2026-04-23T00:00:00Z',
          status: 400,
          code: 'VALIDATION_ERROR',
          message: 'Request body failed validation',
          details: [{ field: 'latitude', message: 'must be between -90 and 90' }],
        }),
      ),
    )

    const error = await apiFetch('/api/v1/emergency/recommend', {
      method: 'POST',
      body: {},
      token: 'jwt-token',
      onUnauthorized,
    }).catch((thrown) => thrown)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).code).toBe('VALIDATION_ERROR')
    expect((error as ApiError).message).toBe('Request body failed validation')
    expect((error as ApiError).details).toEqual([
      { field: 'latitude', message: 'must be between -90 and 90' },
    ])
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('invokes onUnauthorized and throws ApiError on 401', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => emptyResponse(401)))

    const error = await apiFetch('/api/v1/specialties', {
      token: 'stale-token',
      onUnauthorized,
    }).catch((thrown) => thrown)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(401)
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it('exposes the stable code on a 404 NO_BEDS_AVAILABLE response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        jsonResponse(404, {
          timestamp: '2026-04-23T00:00:00Z',
          status: 404,
          code: 'NO_BEDS_AVAILABLE',
          message: 'No hospital has beds available for specialty Cardiology',
          details: null,
        }),
      ),
    )

    const error = await apiFetch('/api/v1/emergency/recommend', {
      method: 'POST',
      body: {},
      token: 'jwt-token',
      onUnauthorized,
    }).catch((thrown) => thrown)

    expect((error as ApiError).status).toBe(404)
    expect((error as ApiError).code).toBe('NO_BEDS_AVAILABLE')
    expect((error as ApiError).details).toEqual([])
  })

  it('surfaces the code on a 503 reservation-conflict response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        jsonResponse(503, {
          timestamp: '2026-04-23T00:00:00Z',
          status: 503,
          code: 'RESERVATION_CONFLICT',
          message: 'Bed reservation could not complete after retries -- retry later',
          details: null,
        }),
      ),
    )

    const error = await apiFetch('/api/v1/emergency/recommend', {
      method: 'POST',
      body: {},
      token: 'jwt-token',
      onUnauthorized,
    }).catch((thrown) => thrown)

    expect((error as ApiError).status).toBe(503)
    expect((error as ApiError).code).toBe('RESERVATION_CONFLICT')
  })

  it('wraps a network failure into an ApiError with NETWORK_ERROR code', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('Failed to fetch')
      }),
    )

    const error = await apiFetch('/api/v1/specialties', {
      token: 'jwt-token',
      onUnauthorized,
    }).catch((thrown) => thrown)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(0)
    expect((error as ApiError).code).toBe('NETWORK_ERROR')
    expect(onUnauthorized).not.toHaveBeenCalled()
  })
})
