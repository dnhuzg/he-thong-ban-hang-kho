import { useState } from 'react'
import { loginUser } from '../services/api'

function LoginPage({ onLogin }) {
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsLoading(true)

    try {
      onLogin(await loginUser({ identifier, password }))
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <section className="login-page" aria-labelledby="login-title">
      <div className="login-heading">
        <p className="eyebrow">Tài khoản</p>
        <h1 id="login-title">Đăng nhập</h1>
        <p>Truy cập hệ thống quản lý bán hàng và kho.</p>
      </div>

      <form className="login-panel" onSubmit={handleSubmit}>
        <div className="login-field">
          <label htmlFor="login-identifier">Tài khoản hoặc email</label>
          <input
            autoComplete="username"
            id="login-identifier"
            maxLength={255}
            name="identifier"
            onChange={(event) => setIdentifier(event.target.value)}
            required
            value={identifier}
          />
        </div>

        <div className="login-field">
          <label htmlFor="login-password">Mật khẩu</label>
          <input
            autoComplete="current-password"
            id="login-password"
            maxLength={128}
            name="password"
            onChange={(event) => setPassword(event.target.value)}
            required
            type="password"
            value={password}
          />
        </div>

        {error && <p className="login-error" role="alert">{error}</p>}

        <button className="login-submit" disabled={isLoading} type="submit">
          {isLoading ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>
    </section>
  )
}

export default LoginPage