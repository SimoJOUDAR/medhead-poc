import { renderHook, act, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import { AuthProvider } from '../auth/AuthProvider'
import { TOKEN_STORAGE_KEY } from '../auth/authContext'
import { useRecommendation, type RecommendationResponse } from './useRecommendation'

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const FRED_BROOKS: RecommendationResponse = {
  hospital: {
    id: 1,
    name: 'Fred Brooks Hospital',
    latitude: 51.5237,
    longitude: -0.1311,
    address: '12 Bloomsbury Way, London',
    availableBeds: 1,
    distanceKm: 0.12,
    estimatedTravelTimeMinutes: 2.4,
  },
  specialty: { id: 7, name: 'Cardiology', group: 'Medicine' },
  requestedSpecialty: { id: 7, name: 'Cardiology', group: 'Medicine' },
  bedReserved: true,
  fallback: false,
  timestamp: '2026-04-24T10:00:00Z',
}

describe('useRecommendation', () => {
  beforeEach(() => {
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, 'jwt-token')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    window.sessionStorage.clear()
  })

  it('POSTs the request body to /api/v1/emergency/recommend with the bearer token', async () => {
    const fetchMock = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>(
      async () => jsonResponse(200, FRED_BROOKS),
    )
    vi.stubGlobal('fetch', fetchMock)

    const { result } = renderHook(() => useRecommendation(), { wrapper })

    await act(async () => {
      await result.current.submit({ specialtyId: 7, latitude: 51.523, longitude: -0.131 })
    })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/v1/emergency/recommend')
    expect((init as RequestInit).method).toBe('POST')
    const headers = (init as RequestInit).headers as Record<string, string>
    expect(headers.Authorization).toBe('Bearer jwt-token')
    expect(headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(String((init as RequestInit).body))).toEqual({
      specialtyId: 7,
      latitude: 51.523,
      longitude: -0.131,
    })
  })

  it('populates result on a 200 response', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse(200, FRED_BROOKS)))

    const { result } = renderHook(() => useRecommendation(), { wrapper })

    await act(async () => {
      await result.current.submit({ specialtyId: 7, latitude: 51.523, longitude: -0.131 })
    })

    expect(result.current.result).toEqual(FRED_BROOKS)
    expect(result.current.error).toBeNull()
  })

  it('toggles loading true while the request is in flight and false once it resolves', async () => {
    let resolveFetch: ((response: Response) => void) | undefined
    const pending = new Promise<Response>((resolve) => {
      resolveFetch = resolve
    })
    vi.stubGlobal('fetch', vi.fn(() => pending))

    const { result } = renderHook(() => useRecommendation(), { wrapper })

    expect(result.current.loading).toBe(false)

    let submitPromise: Promise<void>
    act(() => {
      submitPromise = result.current.submit({
        specialtyId: 7,
        latitude: 51.523,
        longitude: -0.131,
      })
    })

    await waitFor(() => expect(result.current.loading).toBe(true))

    await act(async () => {
      resolveFetch?.(jsonResponse(200, FRED_BROOKS))
      await submitPromise
    })

    expect(result.current.loading).toBe(false)
    expect(result.current.result).toEqual(FRED_BROOKS)
  })
})
