import { useEffect, useState } from 'react'

type PingResponse = { message: string }

function App() {
  const [message, setMessage] = useState<string>('Connecting…')

  useEffect(() => {
    fetch('/api/v1/ping')
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }
        return response.json() as Promise<PingResponse>
      })
      .then((data) => setMessage(data.message))
      .catch(() => setMessage('Unable to reach backend.'))
  }, [])

  return (
    <main>
      <h1>MedHead PoC</h1>
      <p>Backend says: {message}</p>
    </main>
  )
}

export default App
