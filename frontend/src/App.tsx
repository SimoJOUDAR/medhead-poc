import { AuthProvider } from './auth/AuthProvider'
import { useAuth } from './auth/authContext'
import { LoginForm } from './auth/LoginForm'
import { RecommendForm } from './recommend/RecommendForm'

function AuthenticatedPanel() {
  const { logout } = useAuth()

  return (
    <section aria-label="Signed in">
      <RecommendForm />
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
