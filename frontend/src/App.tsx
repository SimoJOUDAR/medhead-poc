import { useEffect, useState } from 'react'
import { AuthProvider } from './auth/AuthProvider'
import { useAuth } from './auth/authContext'
import { LoginForm } from './auth/LoginForm'
import { apiFetch } from './auth/apiClient'
import { RecommendForm } from './recommend/RecommendForm'

type PingResponse = { message: string }

function AuthenticatedPanel() {
  const { token, logout } = useAuth()
  const [message, setMessage] = useState<string>('Connecting…')

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

  return (
    <section aria-label="Signed in">
      <RecommendForm />
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
