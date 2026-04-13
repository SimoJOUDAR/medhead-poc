import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import { AuthProvider } from '../auth/AuthProvider'
import { ApiError } from '../auth/ApiError'
import { TOKEN_STORAGE_KEY } from '../auth/authContext'
import { useSpecialties, type SpecialtyOption } from './useSpecialties'

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function emptyResponse(status: number): Response {
  return new Response('', { status })
}

const CARDIOLOGY: SpecialtyOption = {
  id: 1,
  name: 'Cardiology',
  group: { id: 10, name: 'Medicine' },
}

const ONCOLOGY: SpecialtyOption = {
  id: 2,
  name: 'Oncology',
  group: { id: 10, name: 'Medicine' },
}

describe('useSpecialties', () => {
  beforeEach(() => {
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, 'jwt-token')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    window.sessionStorage.clear()
  })

  it('loads the catalogue and transitions from loading to populated', async () => {
    const fetchMock = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>(
      async () => jsonResponse(200, [CARDIOLOGY, ONCOLOGY]),
    )
    vi.stubGlobal('fetch', fetchMock)

    const { result } = renderHook(() => useSpecialties(), { wrapper })

    expect(result.current.loading).toBe(true)
    expect(result.current.specialties).toEqual([])
    expect(result.current.error).toBeNull()

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
    expect(result.current.specialties).toEqual([CARDIOLOGY, ONCOLOGY])
    expect(result.current.error).toBeNull()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/v1/specialties')
    const headers = (init as RequestInit).headers as Record<string, string>
    expect(headers.Authorization).toBe('Bearer jwt-token')
  })

  it('tolerates an empty catalogue without surfacing an error', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse(200, [])))

    const { result } = renderHook(() => useSpecialties(), { wrapper })

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.specialties).toEqual([])
    expect(result.current.error).toBeNull()
  })

  it('surfaces an ApiError when the server rejects the token', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => emptyResponse(401)))

    const { result } = renderHook(() => useSpecialties(), { wrapper })

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).toBeInstanceOf(ApiError)
    expect(result.current.error?.status).toBe(401)
    expect(result.current.specialties).toEqual([])
  })
})
