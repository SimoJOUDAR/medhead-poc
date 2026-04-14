import { useCallback, useState } from 'react'
import { ApiError } from '../auth/ApiError'
import { apiFetch } from '../auth/apiClient'
import { useAuth } from '../auth/authContext'

const RECOMMEND_URL = '/api/v1/emergency/recommend'

export interface RecommendationRequest {
  specialtyId: number
  latitude: number
  longitude: number
}

export interface RecommendedHospital {
  id: number
  name: string
  latitude: number
  longitude: number
  address: string
  availableBeds: number
  distanceKm: number
  estimatedTravelTimeMinutes: number
}

export interface RecommendedSpecialty {
  id: number
  name: string
  group: string
}

export interface RecommendationResponse {
  hospital: RecommendedHospital
  specialty: RecommendedSpecialty
  requestedSpecialty: RecommendedSpecialty
  bedReserved: boolean
  fallback: boolean
  timestamp: string
}

export interface UseRecommendationResult {
  submit: (request: RecommendationRequest) => Promise<void>
  result: RecommendationResponse | null
  loading: boolean
  error: ApiError | null
}

export function useRecommendation(): UseRecommendationResult {
  const { token, logout } = useAuth()
  const [result, setResult] = useState<RecommendationResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  const submit = useCallback(
    async (request: RecommendationRequest) => {
      setLoading(true)
      setError(null)
      try {
        const response = await apiFetch<RecommendationResponse>(RECOMMEND_URL, {
          method: 'POST',
          body: request,
          token,
          onUnauthorized: logout,
        })
        setResult(response)
      } catch (thrown) {
        const apiError = thrown instanceof ApiError ? thrown : ApiError.network()
        setError(apiError)
        setResult(null)
      } finally {
        setLoading(false)
      }
    },
    [token, logout],
  )

  return { submit, result, loading, error }
}
