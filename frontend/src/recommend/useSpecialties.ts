import { useEffect, useState } from 'react'
import { ApiError } from '../auth/ApiError'
import { apiFetch } from '../auth/apiClient'
import { useAuth } from '../auth/authContext'

export interface SpecialtyGroupOption {
  id: number
  name: string
}

export interface SpecialtyOption {
  id: number
  name: string
  group: SpecialtyGroupOption
}

export interface UseSpecialtiesResult {
  specialties: SpecialtyOption[]
  loading: boolean
  error: ApiError | null
}

const SPECIALTIES_URL = '/api/v1/specialties'

export function useSpecialties(): UseSpecialtiesResult {
  const { token, logout } = useAuth()
  const [specialties, setSpecialties] = useState<SpecialtyOption[]>([])
  const [loading, setLoading] = useState<boolean>(token !== null)
  const [error, setError] = useState<ApiError | null>(null)

  useEffect(() => {
    if (!token) {
      return
    }

    const controller = new AbortController()

    apiFetch<SpecialtyOption[]>(SPECIALTIES_URL, {
      token,
      onUnauthorized: logout,
      signal: controller.signal,
    })
      .then((data) => {
        if (controller.signal.aborted) return
        setSpecialties(data)
        setLoading(false)
      })
      .catch((thrown) => {
        if (controller.signal.aborted) return
        const apiError = thrown instanceof ApiError ? thrown : ApiError.network()
        setError(apiError)
        setLoading(false)
      })

    return () => {
      controller.abort()
    }
  }, [token, logout])

  return { specialties, loading, error }
}
