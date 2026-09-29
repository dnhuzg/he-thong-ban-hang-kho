import AppLayout from './layouts/AppLayout'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import { useState } from 'react'
import './App.css'

function App() {
  const [session, setSession] = useState(null)

  if (!session) {
    return (
      <AppLayout>
        <LoginPage onLogin={setSession} />
      </AppLayout>
    )
  }

  return (
    <AppLayout user={session.user}>
      <HomePage />
    </AppLayout>
  )
}

export default App
