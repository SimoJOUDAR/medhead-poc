import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from './AuthProvider'
import { LoginForm } from './LoginForm'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function renderLoginForm() {
  return render(
    <AuthProvider>
      <LoginForm />
    </AuthProvider>,
  )
}

describe('LoginForm', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    window.sessionStorage.clear()
  })

  it('renders accessible labelled inputs and a sign-in button', () => {
    renderLoginForm()

    expect(screen.getByLabelText(/username/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('keeps the submit button disabled until both fields are filled', async () => {
    renderLoginForm()
    const user = userEvent.setup()
    const submit = screen.getByRole('button', { name: /sign in/i })

    expect(submit).toBeDisabled()

    await user.type(screen.getByLabelText(/username/i), 'demo')
    expect(submit).toBeDisabled()

    await user.type(screen.getByLabelText(/password/i), 'demo')
    expect(submit).toBeEnabled()
  })

  it('calls the login endpoint on submit and stores the returned token', async () => {
    const fetchMock = vi.fn(async () =>
      jsonResponse(200, {
        token: 'jwt-token',
        issuedAt: '2026-04-23T00:00:00Z',
        expiresAt: '2026-04-23T01:00:00Z',
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    renderLoginForm()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'demo')
    await user.type(screen.getByLabelText(/password/i), 'demo')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/auth/login',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ username: 'demo', password: 'demo' }),
      }),
    )
    expect(window.sessionStorage.getItem('medhead.token')).toBe('jwt-token')
  })

  it('surfaces the server-provided message on a 401 response', async () => {
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

    renderLoginForm()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'demo')
    await user.type(screen.getByLabelText(/password/i), 'wrong')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent(/invalid username or password/i)
    expect(window.sessionStorage.getItem('medhead.token')).toBeNull()
  })

  it('surfaces a network-error message when fetch rejects', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('Failed to fetch')
      }),
    )

    renderLoginForm()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/username/i), 'demo')
    await user.type(screen.getByLabelText(/password/i), 'demo')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent(/could not reach the backend/i)
  })
})
