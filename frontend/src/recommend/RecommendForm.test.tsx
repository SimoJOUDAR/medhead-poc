import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import App from '../App'
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

interface RecommendFetchMockOptions {
  recommend: () => Promise<Response>
}

function mockRecommendFetch({ recommend }: RecommendFetchMockOptions) {
  const fetchMock = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>(
    async (input) => {
      const url = typeof input === 'string' ? input : input.toString()
      if (url === '/api/v1/specialties') {
        return jsonResponse(200, [CARDIOLOGY])
      }
      if (url === '/api/v1/emergency/recommend') {
        return recommend()
      }
      throw new Error(`Unexpected fetch: ${url}`)
    },
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

async function submitValidRequest() {
  const user = userEvent.setup()
  await screen.findByRole('option', { name: 'Cardiology' })
  await user.selectOptions(screen.getByRole('combobox', { name: /specialty/i }), 'Cardiology')
  await user.type(screen.getByLabelText(/latitude/i), '51.523')
  await user.type(screen.getByLabelText(/longitude/i), '-0.131')
  await user.click(screen.getByRole('button', { name: /find hospital/i }))
  return user
}

describe('RecommendForm (error branches)', () => {
  beforeEach(() => {
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, 'jwt-token')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    window.sessionStorage.clear()
  })

  it('renders a field-list alert when the backend returns 400 VALIDATION_ERROR', async () => {
    mockRecommendFetch({
      recommend: async () =>
        jsonResponse(400, {
          code: 'VALIDATION_ERROR',
          message: 'Request body failed validation',
          details: [
            { field: 'latitude', message: 'must be between -90 and 90' },
            { field: 'longitude', message: 'must be between -180 and 180' },
          ],
        }),
    })

    render(<RecommendForm />, { wrapper: withAuth })
    await submitValidRequest()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Please check: latitude, longitude')
    expect(alert).toHaveAttribute('aria-live', 'assertive')
  })

  it('renders the dispatch-line copy for 404 NO_BEDS_AVAILABLE', async () => {
    mockRecommendFetch({
      recommend: async () =>
        jsonResponse(404, { code: 'NO_BEDS_AVAILABLE', message: 'No beds available anywhere' }),
    })

    render(<RecommendForm />, { wrapper: withAuth })
    await submitValidRequest()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /no hospitals have beds available right now/i,
    )
  })

  it('renders the pick-another-specialty copy for 404 SPECIALTY_NOT_FOUND', async () => {
    mockRecommendFetch({
      recommend: async () =>
        jsonResponse(404, { code: 'SPECIALTY_NOT_FOUND', message: 'Specialty 999 not found' }),
    })

    render(<RecommendForm />, { wrapper: withAuth })
    await submitValidRequest()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /selected specialty is no longer available/i,
    )
  })

  it('renders the overloaded copy for 503 responses', async () => {
    mockRecommendFetch({
      recommend: async () =>
        jsonResponse(503, {
          code: 'ROUTING_UNAVAILABLE',
          message: 'Routing service is unavailable',
        }),
    })

    render(<RecommendForm />, { wrapper: withAuth })
    await submitValidRequest()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      /system is under heavy load and could not secure a bed/i,
    )
  })

  it('renders the connection-check copy when fetch rejects (network failure)', async () => {
    const fetchMock = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>(
      async (input) => {
        const url = typeof input === 'string' ? input : input.toString()
        if (url === '/api/v1/specialties') {
          return jsonResponse(200, [CARDIOLOGY])
        }
        if (url === '/api/v1/emergency/recommend') {
          throw new TypeError('Network down')
        }
        throw new Error(`Unexpected fetch: ${url}`)
      },
    )
    vi.stubGlobal('fetch', fetchMock)

    render(<RecommendForm />, { wrapper: withAuth })
    await submitValidRequest()

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not reach the backend/i)
  })

  it('keeps the form interactive after an error so the user can retry', async () => {
    let call = 0
    mockRecommendFetch({
      recommend: async () => {
        call += 1
        if (call === 1) {
          return jsonResponse(503, {
            code: 'ROUTING_UNAVAILABLE',
            message: 'Routing service is unavailable',
          })
        }
        return jsonResponse(200, FRED_BROOKS)
      },
    })

    render(<RecommendForm />, { wrapper: withAuth })
    const user = await submitValidRequest()

    await screen.findByRole('alert')
    const submit = screen.getByRole('button', { name: /find hospital/i })
    expect(submit).toBeEnabled()

    await user.click(submit)

    expect(
      await screen.findByRole('heading', { level: 2, name: /fred brooks hospital/i }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})

describe('RecommendForm + App (401 returns the user to login)', () => {
  beforeEach(() => {
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, 'jwt-token')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    window.sessionStorage.clear()
  })

  it('clears the token and renders the login form when the backend returns 401', async () => {
    const fetchMock = vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>(
      async (input) => {
        const url = typeof input === 'string' ? input : input.toString()
        if (url === '/api/v1/specialties') {
          return jsonResponse(200, [CARDIOLOGY])
        }
        if (url === '/api/v1/emergency/recommend') {
          return jsonResponse(401, { code: 'UNAUTHORIZED', message: 'Token expired' })
        }
        throw new Error(`Unexpected fetch: ${url}`)
      },
    )
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)
    await submitValidRequest()

    await waitFor(() =>
      expect(window.sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull(),
    )
    expect(await screen.findByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })
})
