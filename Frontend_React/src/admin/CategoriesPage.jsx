import { useEffect, useState } from 'react'
import {
  createAdminCategory,
  deleteAdminCategory,
  getAdminCategories,
  updateAdminCategory,
} from '../services/adminService'
import './AdminPages.css'

function CategoriesPage() {
  const [items, setItems] = useState([])
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [size] = useState(8)
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ name: '', description: '' })

  async function loadData(nextPage = page) {
    const res = await getAdminCategories({ keyword, page: nextPage, size })
    setItems(res.data.items || [])
    setTotal(res.data.total || 0)
    setTotalPages(res.data.totalPages || 0)
    setPage(res.data.page || 0)
  }

  useEffect(() => {
    loadData(0)
  }, [keyword])

  function openCreate() {
    setEditing(null)
    setForm({ name: '', description: '' })
    setShowModal(true)
  }

  function openEdit(item) {
    setEditing(item)
    setForm({ name: item.name || '', description: item.description || '' })
    setShowModal(true)
  }

  async function submitForm(event) {
    event.preventDefault()
    if (editing) {
      await updateAdminCategory(editing.id, form)
    } else {
      await createAdminCategory(form)
    }
    setShowModal(false)
    loadData(0)
  }

  async function removeItem(id) {
    if (!window.confirm('Xóa danh mục này?')) return
    await deleteAdminCategory(id)
    loadData(page)
  }

  return (
    <div className="admin-panel">
      <div className="admin-panel__header">
        <h2 className="admin-panel__title">Quản lý danh mục</h2>
        <div className="admin-toolbar admin-toolbar--categories">
          <input
            className="admin-input"
            placeholder="Tìm danh mục"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <button type="button" className="admin-btn admin-btn--primary" onClick={openCreate}>
            Thêm danh mục
          </button>
        </div>
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên</th>
              <th>Mô tả</th>
              <th>Sản phẩm</th>
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.id}</td>
                <td>{item.name}</td>
                <td>{item.description || '-'}</td>
                <td>{item.productCount}</td>
                <td>
                  <button type="button" className="admin-btn admin-btn--ghost" onClick={() => openEdit(item)}>
                    Sửa
                  </button>{' '}
                  <button type="button" className="admin-btn admin-btn--danger" onClick={() => removeItem(item.id)}>
                    Xóa
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
          <button type="button" className="admin-btn admin-btn--ghost" disabled={page <= 0} onClick={() => loadData(page - 1)}>
            Trước
          </button>
          <span>Trang {page + 1}/{Math.max(totalPages, 1)}</span>
          <button
            type="button"
            className="admin-btn admin-btn--ghost"
            disabled={page + 1 >= totalPages}
            onClick={() => loadData(page + 1)}
          >
            Sau
          </button>
        </div>
      </div>

      {showModal && (
        <div className="admin-modal" onClick={() => setShowModal(false)}>
          <div className="admin-modal__content" onClick={(e) => e.stopPropagation()}>
            <h3>{editing ? 'Cập nhật danh mục' : 'Thêm danh mục'}</h3>
            <form onSubmit={submitForm} className="admin-grid">
              <label>
                Tên
                <input className="admin-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </label>
              <label className="span-2">
                Mô tả
                <textarea className="admin-input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </label>
              <div className="span-2 admin-toolbar">
                <button type="submit" className="admin-btn admin-btn--primary">Lưu</button>
                <button type="button" className="admin-btn admin-btn--ghost" onClick={() => setShowModal(false)}>Đóng</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default CategoriesPage
