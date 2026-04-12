import { useId, useRef, useState, type FormEvent } from 'react'
import { ApiError } from './ApiError'
import { useAuth } from './authContext'

export function LoginForm() {
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  const usernameId = useId()
  const passwordId = useId()
  const errorId = useId()
  const usernameRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)

  const canSubmit = username.trim().length > 0 && password.length > 0 && !submitting
  const fieldErrors = new Map((error?.details ?? []).map((detail) => [detail.field, detail.message]))

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canSubmit) return

    setSubmitting(true)
    setError(null)
    try {
      await login(username, password)
    } catch (thrown) {
      const apiError =
        thrown instanceof ApiError
          ? thrown
          : new ApiError(0, 'UNKNOWN', 'Unexpected error while signing in.')
      setError(apiError)
      const firstFieldError = apiError.details[0]?.field
      if (firstFieldError === 'password') {
        passwordRef.current?.focus()
      } else {
        usernameRef.current?.focus()
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} aria-label="Sign in" noValidate>
      <div>
        <label htmlFor={usernameId}>Username</label>
        <input
          id={usernameId}
          ref={usernameRef}
          name="username"
          type="text"
          autoComplete="username"
          required
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          aria-invalid={fieldErrors.has('username') ? 'true' : undefined}
          aria-describedby={error ? errorId : undefined}
        />
      </div>
      <div>
        <label htmlFor={passwordId}>Password</label>
        <input
          id={passwordId}
          ref={passwordRef}
          name="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          aria-invalid={fieldErrors.has('password') ? 'true' : undefined}
          aria-describedby={error ? errorId : undefined}
        />
      </div>
      <button type="submit" disabled={!canSubmit}>
        {submitting ? 'Signing in…' : 'Sign in'}
      </button>
      {error && (
        <p id={errorId} role="alert">
          {error.message}
        </p>
      )}
    </form>
  )
}
