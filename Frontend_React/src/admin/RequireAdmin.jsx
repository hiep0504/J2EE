import { Navigate, useLocation } from 'react-router-dom'

function RequireAdmin({ user, authChecked, children }) {
  const location = useLocation()

  if (!authChecked) {
    return <div style={{ padding: 24 }}>Đang kiểm tra quyền truy cập...</div>
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (String(user.role || '').toLowerCase() !== 'admin') {
    return <Navigate to="/" replace />
  }

  return children
}

export default RequireAdmin
