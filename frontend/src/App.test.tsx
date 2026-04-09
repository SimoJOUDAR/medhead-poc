import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import App from './App'

beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn(() =>
      Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ message: 'pong' }),
      } as Response),
    ),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

it('renders the MedHead PoC heading', () => {
  render(<App />)

  expect(
    screen.getByRole('heading', { level: 1, name: /MedHead PoC/i }),
  ).toBeInTheDocument()
})
