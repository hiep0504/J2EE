import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMe, updateMe } from '../services/accountService'

function AccountPage({ authChecked, user, onRefreshed }) {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [status, setStatus] = useState('')
  const [saving, setSaving] = useState(false)

  const canShowForm = useMemo(() => !!user, [user])

  useEffect(() => {
    if (authChecked && !user) {
      navigate('/login')
    }
  }, [authChecked, user, navigate])

  useEffect(() => {
    if (user) {
      setEmail(user.email || '')
      setPhone(user.phone || '')
    }
  }, [user])

  async function refresh() {
    try {
      const res = await getMe()
      await onRefreshed?.(res.data)
    } catch {
      await onRefreshed?.(null)
    }
  }

  async function handleSave(event) {
    event.preventDefault()
    setStatus('')

    setSaving(true)
    try {
      await updateMe({ email, phone })
      setStatus('Cập nhật thành công')
      await refresh()
    } catch (error) {
      const message = error?.response?.data?.message || 'Cập nhật thất bại'
      setStatus(message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="container py-4" style={{ maxWidth: 720 }}>
      <h5 className="mb-3">Thông tin cá nhân</h5>

      {!authChecked && <p className="text-muted">Đang kiểm tra đăng nhập...</p>}

      {canShowForm && (
        <div className="row g-3">
          <div className="col-12 col-md-5">
            <div className="card card-body">
              <p className="mb-1"><strong>Username:</strong> {user.username}</p>
              <p className="mb-1"><strong>Role:</strong> {user.role}</p>
              <p className="mb-0"><strong>Created:</strong> {user.createdAt ? new Date(user.createdAt).toLocaleString('vi-VN') : '-'}</p>
            </div>
          </div>

          <div className="col-12 col-md-7">
            <form onSubmit={handleSave} className="card card-body">
              <label className="form-label">
                Email
                <input className="form-control" value={email} onChange={(e) => setEmail(e.target.value)} />
              </label>

              <label className="form-label mt-2">
                Số điện thoại
                <input className="form-control" value={phone} onChange={(e) => setPhone(e.target.value)} />
              </label>

              <button className="btn btn-dark mt-3" type="submit" disabled={saving}>
                {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
              </button>

              {status && <div className="alert alert-info mt-3 mb-0">{status}</div>}
            </form>
          </div>
        </div>
      )}
    </main>
  )
}

export default AccountPage
