import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../services/authService'

function RegisterPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [status, setStatus] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setStatus('')

    if (!username.trim() || !email.trim() || !password) {
      setStatus('Vui lòng nhập username, email và mật khẩu')
      return
    }

    setSubmitting(true)
    try {
      await register({ username, email, phone, password })
      navigate('/login')
    } catch (error) {
      const message = error?.response?.data?.message || 'Đăng ký thất bại'
      setStatus(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="container py-4" style={{ maxWidth: 520 }}>
      <h5 className="mb-3">Đăng ký</h5>
      <form onSubmit={handleSubmit} className="card card-body">
        <label className="form-label">
          Username
          <input
            className="form-control"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Nhập username"
          />
        </label>

        <label className="form-label mt-2">
          Email
          <input
            className="form-control"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Nhập email"
          />
        </label>

        <label className="form-label mt-2">
          Số điện thoại (tuỳ chọn)
          <input
            className="form-control"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="Nhập số điện thoại"
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
          {submitting ? 'Đang đăng ký...' : 'Đăng ký'}
        </button>

        <div className="mt-3">
          <small>
            Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
          </small>
        </div>

        {status && <div className="alert alert-warning mt-3 mb-0">{status}</div>}
      </form>
    </main>
  )
}

export default RegisterPage
