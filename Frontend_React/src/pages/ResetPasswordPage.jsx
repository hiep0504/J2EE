import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../services/authService'

function ResetPasswordPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [status, setStatus] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setStatus('')

    if (!token) {
      setStatus('Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn')
      return
    }

    if (!password || !confirmPassword) {
      setStatus('Vui lòng nhập đầy đủ mật khẩu mới')
      return
    }

    if (password !== confirmPassword) {
      setStatus('Mật khẩu xác nhận không khớp')
      return
    }

    setSubmitting(true)
    try {
      const response = await resetPassword({ token, password })
      setStatus(response?.data?.message || 'Đặt lại mật khẩu thành công')
      setTimeout(() => navigate('/login'), 1200)
    } catch (error) {
      const message = error?.response?.data?.message || 'Đặt lại mật khẩu thất bại'
      setStatus(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="container py-4" style={{ maxWidth: 520 }}>
      <h5 className="mb-3">Đặt lại mật khẩu</h5>
      <form onSubmit={handleSubmit} className="card card-body">
        <p className="text-muted mb-3">
          Tạo mật khẩu mới cho tài khoản của bạn.
        </p>

        <label className="form-label">
          Mật khẩu mới
          <input
            type="password"
            className="form-control"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Nhập mật khẩu mới"
            autoComplete="new-password"
          />
        </label>

        <label className="form-label mt-2">
          Xác nhận mật khẩu
          <input
            type="password"
            className="form-control"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Nhập lại mật khẩu mới"
            autoComplete="new-password"
          />
        </label>

        <button className="btn btn-dark mt-3" type="submit" disabled={submitting}>
          {submitting ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
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

export default ResetPasswordPage