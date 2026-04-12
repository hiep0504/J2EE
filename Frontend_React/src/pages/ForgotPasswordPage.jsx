import { useState } from 'react'
import { Link } from 'react-router-dom'
import { requestPasswordReset } from '../services/authService'

function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [status, setStatus] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setStatus('')

    if (!email.trim()) {
      setStatus('Vui lòng nhập email của bạn')
      return
    }

    setSubmitting(true)
    try {
      const response = await requestPasswordReset({ email })
      setStatus(response?.data?.message || 'Nếu email tồn tại, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu.')
    } catch (error) {
      const message = error?.response?.data?.message || 'Không thể gửi email đặt lại mật khẩu'
      setStatus(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="container py-4" style={{ maxWidth: 520 }}>
      <h5 className="mb-3">Quên mật khẩu</h5>
      <form onSubmit={handleSubmit} className="card card-body">
        <p className="text-muted mb-3">
          Nhập email đã đăng ký. Chúng tôi sẽ gửi link đặt lại mật khẩu qua Gmail.
        </p>

        <label className="form-label">
          Email
          <input
            className="form-control"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="user@gmail.com"
            autoComplete="email"
          />
        </label>

        <button className="btn btn-dark mt-3" type="submit" disabled={submitting}>
          {submitting ? 'Đang gửi...' : 'Gửi link đặt lại mật khẩu'}
        </button>

        <div className="mt-3">
          <small>
            Quay lại <Link to="/login">đăng nhập</Link>
          </small>
        </div>

        {status && <div className="alert alert-info mt-3 mb-0">{status}</div>}
      </form>
    </main>
  )
}

export default ForgotPasswordPage