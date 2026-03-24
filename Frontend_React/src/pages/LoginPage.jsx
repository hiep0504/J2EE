import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../services/authService'

function LoginPage({ onLoggedIn }) {
  const navigate = useNavigate()
  const [usernameOrEmail, setUsernameOrEmail] = useState('')
  const [password, setPassword] = useState('')
  const [status, setStatus] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setStatus('')

    if (!usernameOrEmail.trim() || !password) {
      setStatus('Vui lòng nhập đầy đủ thông tin')
      return
    }

    setSubmitting(true)
    try {
      await login({ usernameOrEmail, password })
      await onLoggedIn?.()
      navigate('/account')
    } catch (error) {
      const message = error?.response?.data?.message || 'Đăng nhập thất bại'
      setStatus(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="container py-4" style={{ maxWidth: 520 }}>
      <h5 className="mb-3">Đăng nhập</h5>
      <form onSubmit={handleSubmit} className="card card-body">
        <label className="form-label">
          Username hoặc Email
          <input
            className="form-control"
            value={usernameOrEmail}
            onChange={(e) => setUsernameOrEmail(e.target.value)}
            placeholder="user1 hoặc user1@gmail.com"
          />
        </label>

        <label className="form-label mt-2">
          Mật khẩu
          <input
            type="password"
            className="form-control"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Nhập mật khẩu"
          />
        </label>

        <button className="btn btn-dark mt-3" type="submit" disabled={submitting}>
          {submitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>

        <div className="mt-3">
          <small>
            Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
          </small>
        </div>

        {status && <div className="alert alert-warning mt-3 mb-0">{status}</div>}
      </form>
    </main>
  )
}

export default LoginPage
