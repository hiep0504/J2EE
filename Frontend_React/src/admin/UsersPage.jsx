import { useEffect, useState } from 'react'
import { getAdminUsers, updateAdminUserLock, updateAdminUserRole } from '../services/adminService'
import './AdminPages.css'

function UsersPage() {
  const [items, setItems] = useState([])
  const [keyword, setKeyword] = useState('')
  const [role, setRole] = useState('')
  const [page, setPage] = useState(0)
  const [size] = useState(10)
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  async function loadData(nextPage = page) {
    const res = await getAdminUsers({ keyword, role, page: nextPage, size })
    setItems(res.data.items || [])
    setTotal(res.data.total || 0)
    setTotalPages(res.data.totalPages || 0)
    setPage(res.data.page || 0)
  }

  useEffect(() => {
    loadData(0)
  }, [keyword, role])

  async function onChangeRole(userId, nextRole) {
    await updateAdminUserRole(userId, { role: nextRole })
    loadData(page)
  }

  async function onToggleLock(userId, currentLock) {
    await updateAdminUserLock(userId, { locked: !currentLock })
    loadData(page)
  }

  return (
    <div className="admin-panel">
      <div className="admin-panel__header">
        <h2 className="admin-panel__title">Quản lý người dùng</h2>
        <div className="admin-toolbar admin-toolbar--users">
          <input className="admin-input" placeholder="Tìm username/email" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
          <select className="admin-select" value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="">Tất cả role</option>
            <option value="admin">admin</option>
            <option value="user">user</option>
          </select>
        </div>
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Email</th>
              <th>Phone</th>
              <th className="admin-users-role-col">Role</th>
              <th>Lock</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.id}</td>
                <td>{item.username}</td>
                <td>{item.email}</td>
                <td>{item.phone || '-'}</td>
                <td className="admin-users-role-col">
                  <select className="admin-select admin-select--user-role" value={item.role || 'user'} onChange={(e) => onChangeRole(item.id, e.target.value)}>
                    <option value="admin">admin</option>
                    <option value="user">user</option>
                  </select>
                </td>
                <td>
                  <button type="button" className="admin-btn admin-btn--ghost" onClick={() => onToggleLock(item.id, !!item.locked)}>
                    {item.locked ? 'Mở khóa' : 'Khóa'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {items.length === 0 && <div className="admin-empty">Không có dữ liệu</div>}

      <div className="admin-pagination">
        <span>Tổng: {total}</span>
        <div className="admin-toolbar">
          <button type="button" className="admin-btn admin-btn--ghost" disabled={page <= 0} onClick={() => loadData(page - 1)}>Trước</button>
          <span>Trang {page + 1}/{Math.max(totalPages, 1)}</span>
          <button type="button" className="admin-btn admin-btn--ghost" disabled={page + 1 >= totalPages} onClick={() => loadData(page + 1)}>Sau</button>
        </div>
      </div>
    </div>
  )
}

export default UsersPage
