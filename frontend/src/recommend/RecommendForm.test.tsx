import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import { AuthProvider } from '../auth/AuthProvider'
import { TOKEN_STORAGE_KEY } from '../auth/authContext'
import { RecommendForm } from './RecommendForm'
import type { RecommendationResponse } from './useRecommendation'
import type { SpecialtyOption } from './useSpecialties'

function withAuth({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>
}

const CARDIOLOGY: SpecialtyOption = {
  id: 7,
  name: 'Cardiology',
  group: { id: 10, name: 'Medicine' },
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

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('RecommendForm (happy-path behaviour)', () => {
  beforeEach(() => {
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, 'jwt-token')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    window.sessionStorage.clear()
  })

  it('lets the user pick cardiology, enter coordinates, submit, and see Fred Brooks Hospital', async () => {
    const fetchMock = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>(
      async (input) => {
        const url = typeof input === 'string' ? input : input.toString()
        if (url === '/api/v1/specialties') {
          return jsonResponse(200, [CARDIOLOGY])
        }
        if (url === '/api/v1/emergency/recommend') {
          return jsonResponse(200, FRED_BROOKS)
        }
        throw new Error(`Unexpected fetch: ${url}`)
      },
    )
    vi.stubGlobal('fetch', fetchMock)

    const user = userEvent.setup()
    render(<RecommendForm />, { wrapper: withAuth })

    const submit = await screen.findByRole('button', { name: /find hospital/i })
    expect(submit).toBeDisabled()

    await screen.findByRole('option', { name: 'Cardiology' })
    const specialty = screen.getByRole('combobox', { name: /specialty/i })
    await user.selectOptions(specialty, 'Cardiology')

    await user.type(screen.getByLabelText(/latitude/i), '51.523')
    await user.type(screen.getByLabelText(/longitude/i), '-0.131')

    expect(submit).toBeEnabled()
    await user.click(submit)

    expect(
      await screen.findByRole('heading', { level: 2, name: /fred brooks hospital/i }),
    ).toBeInTheDocument()

    const recommendCall = fetchMock.mock.calls.find(
      ([input]) => (typeof input === 'string' ? input : input.toString()) === '/api/v1/emergency/recommend',
    )
    expect(recommendCall).toBeDefined()
    const init = recommendCall?.[1] as RequestInit
    expect(init.method).toBe('POST')
    expect(JSON.parse(String(init.body))).toEqual({
      specialtyId: 7,
      latitude: 51.523,
      longitude: -0.131,
    })
  })
})
