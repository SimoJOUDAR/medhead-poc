import { useEffect, useState, type FormEvent } from 'react'
import { AuthProvider } from './auth/AuthProvider'
import { useAuth } from './auth/authContext'
import { LoginForm } from './auth/LoginForm'
import { apiFetch } from './auth/apiClient'
import { SpecialtyDropdown } from './recommend/SpecialtyDropdown'
import { useSpecialties } from './recommend/useSpecialties'

type PingResponse = { message: string }

function AuthenticatedPanel() {
  const { token, logout } = useAuth()
  const [message, setMessage] = useState<string>('Connecting…')
  const [specialtyId, setSpecialtyId] = useState<number | null>(null)
  const { specialties, loading, error } = useSpecialties()

  useEffect(() => {
    if (!token) return
    let cancelled = false
    apiFetch<PingResponse>('/api/v1/ping', { token, onUnauthorized: logout })
      .then((data) => {
        if (!cancelled) setMessage(data.message)
      })
      .catch(() => {
        if (!cancelled) setMessage('Unable to reach backend.')
      })
    return () => {
      cancelled = true
    }
  }, [token, logout])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
  }

  return (
    <section aria-label="Signed in">
      <form aria-label="Hospital recommendation" onSubmit={handleSubmit} noValidate>
        <SpecialtyDropdown
          specialties={specialties}
          loading={loading}
          error={error}
          value={specialtyId}
          onChange={setSpecialtyId}
        />
        <button type="submit" disabled={specialtyId === null}>
          Find hospital
        </button>
      </form>
      <p>Backend says: {message}</p>
      <button type="button" onClick={logout}>
        Log out
      </button>
    </section>
  )
}

function AppShell() {
  const { token } = useAuth()
  return (
    <main>
      <h1>MedHead PoC</h1>
      {token ? <AuthenticatedPanel /> : <LoginForm />}
    </main>
  )
}

function App() {
  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  )
}

export default App
