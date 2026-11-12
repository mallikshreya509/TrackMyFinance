import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { getErrorMessage } from '../api/errors'
import { useAuth } from '../auth/useAuth'

export default function RegisterPage() {
  const { user, register } = useAuth()

  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    if (password !== confirm) {
      setError('Passwords do not match.')
      return
    }
    setSubmitting(true)
    try {
      await register(email, password, displayName)
    } catch (err) {
      setError(getErrorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-card">
      <h1>TrackMyFinance</h1>
      <h2>Create account</h2>
      <form onSubmit={handleSubmit}>
        <label>
          Name
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)}
                 maxLength={100} autoComplete="name" />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                 required autoComplete="username" />
        </label>
        <label>
          Password (at least 8 characters)
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 required minLength={8} maxLength={72} autoComplete="new-password" />
        </label>
        <label>
          Confirm password
          <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)}
                 required autoComplete="new-password" />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Register'}
        </button>
      </form>
      <p>Already registered? <Link to="/login">Log in</Link></p>
    </div>
  )
}