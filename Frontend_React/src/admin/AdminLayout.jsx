import { Link, NavLink, Outlet } from 'react-router-dom'
import './AdminLayout.css'

const navItems = [
  { to: '/admin/dashboard', label: 'Dashboard' },
  { to: '/admin/products', label: 'Sản phẩm' },
  { to: '/admin/categories', label: 'Danh mục' },
  { to: '/admin/orders', label: 'Đơn hàng' },
  { to: '/admin/users', label: 'Người dùng' },
  { to: '/admin/reviews', label: 'Đánh giá' },
]

function AdminLayout({ user }) {
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <Link to="/" className="admin-brand">ShopApp Admin</Link>
        <nav className="admin-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? 'admin-nav__item active' : 'admin-nav__item')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="admin-main">
        <header className="admin-topbar">
          <div>
            <h1>Admin Dashboard</h1>
            <p>Quản lý sản phẩm, đơn hàng, người dùng và nội dung hệ thống.</p>
          </div>
          <div className="admin-user-chip">
            <span>{user?.username || 'Ẩn danh'}</span>
            <strong>{user?.role || 'user'}</strong>
          </div>
        </header>

        <section className="admin-content">
          <Outlet />
        </section>
      </div>
    </div>
  )
}

export default AdminLayout
