import { render, screen, waitFor } from '@testing-library/react'
import { axe, toHaveNoViolations } from 'jest-axe'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { TOKEN_STORAGE_KEY } from '../auth/authContext'
import type { SpecialtyOption } from '../recommend/useSpecialties'

expect.extend(toHaveNoViolations)

const WCAG_21_AA = {
  runOnly: {
    type: 'tag' as const,
    values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'],
  },
}

const CARDIOLOGY: SpecialtyOption = {
  id: 7,
  name: 'Cardiology',
  group: { id: 10, name: 'Medicine' },
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
  window.sessionStorage.clear()
})

describe('accessibility -- WCAG 2.1 AA', () => {
  it('reports zero violations on the unauthenticated login state', async () => {
    const { container } = render(<App />)

    await screen.findByRole('button', { name: /sign in/i })

    const results = await axe(container, WCAG_21_AA)
    expect(results).toHaveNoViolations()
  })

  describe('authenticated recommendation state', () => {
    beforeEach(() => {
      window.sessionStorage.setItem(TOKEN_STORAGE_KEY, 'jwt-token')
      vi.stubGlobal(
        'fetch',
        vi.fn<(input: RequestInfo | URL, init?: RequestInit) => Promise<Response>>(
          async (input) => {
            const url = typeof input === 'string' ? input : input.toString()
            if (url === '/api/v1/specialties') {
              return jsonResponse(200, [CARDIOLOGY])
            }
            throw new Error(`Unexpected fetch: ${url}`)
          },
        ),
      )
    })

    it('reports zero violations', async () => {
      const { container } = render(<App />)

      await waitFor(() =>
        expect(screen.getByRole('combobox', { name: /specialty/i })).toBeEnabled(),
      )

      const results = await axe(container, WCAG_21_AA)
      expect(results).toHaveNoViolations()
    })
  })
})
