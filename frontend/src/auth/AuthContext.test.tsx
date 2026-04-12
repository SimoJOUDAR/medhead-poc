import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import { AuthProvider } from './AuthProvider'
import { TOKEN_STORAGE_KEY, useAuth } from './authContext'
import { ApiError } from './ApiError'

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('AuthProvider', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    window.sessionStorage.clear()
  })

  it('starts with no token when sessionStorage is empty', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    expect(result.current.token).toBeNull()
  })

  it('restores a token persisted in sessionStorage on mount', () => {
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, 'persisted-token')
    const { result } = renderHook(() => useAuth(), { wrapper })
    expect(result.current.token).toBe('persisted-token')
  })

  it('stores the token returned by /api/v1/auth/login on success', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        jsonResponse(200, {
          token: 'new-token',
          issuedAt: '2026-04-23T00:00:00Z',
          expiresAt: '2026-04-23T01:00:00Z',
        }),
      ),
    )

    const { result } = renderHook(() => useAuth(), { wrapper })

    await act(async () => {
      await result.current.login('demo', 'demo')
    })

    expect(result.current.token).toBe('new-token')
    expect(window.sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBe('new-token')
  })

  it('surfaces the server-provided ApiError on a 401 login failure', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        jsonResponse(401, {
          timestamp: '2026-04-23T00:00:00Z',
          status: 401,
          error: 'invalid_credentials',
          message: 'Invalid username or password',
        }),
      ),
    )

    const { result } = renderHook(() => useAuth(), { wrapper })

    const error = await act(async () =>
      result.current.login('demo', 'wrong').catch((thrown) => thrown as unknown),
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(401)
    expect((error as ApiError).code).toBe('invalid_credentials')
    expect((error as ApiError).message).toBe('Invalid username or password')
    expect(result.current.token).toBeNull()
    expect(window.sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
  })

  it('wraps a network failure as an ApiError with NETWORK_ERROR code', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('Failed to fetch')
      }),
    )

    const { result } = renderHook(() => useAuth(), { wrapper })

    const error = await act(async () =>
      result.current.login('demo', 'demo').catch((thrown) => thrown as unknown),
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe('NETWORK_ERROR')
    expect(result.current.token).toBeNull()
  })

  it('clears the token and sessionStorage entry on logout', async () => {
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, 'existing-token')
    const { result } = renderHook(() => useAuth(), { wrapper })
    expect(result.current.token).toBe('existing-token')

    act(() => {
      result.current.logout()
    })

    await waitFor(() => {
      expect(result.current.token).toBeNull()
    })
    expect(window.sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
  })
})
