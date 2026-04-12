import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { ApiError } from './ApiError'
import { AuthContext, TOKEN_STORAGE_KEY, type AuthContextValue } from './authContext'

const LOGIN_URL = '/api/v1/auth/login'

function readStoredToken(): string | null {
  if (typeof window === 'undefined') return null
  return window.sessionStorage.getItem(TOKEN_STORAGE_KEY)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(readStoredToken)

  const logout = useCallback(() => {
    window.sessionStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    let response: Response
    try {
      response = await fetch(LOGIN_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ username, password }),
      })
    } catch {
      throw ApiError.network()
    }

    if (!response.ok) {
      throw await ApiError.fromResponse(response)
    }

    const body = (await response.json()) as { token: string }
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, body.token)
    setToken(body.token)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ token, login, logout }),
    [token, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
