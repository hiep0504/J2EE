import { useEffect, useState } from 'react'
import { deleteAdminReview, getAdminReviews } from '../services/adminService'
import './AdminPages.css'

function ReviewsPage() {
  const [items, setItems] = useState([])
  const [keyword, setKeyword] = useState('')
  const [productId, setProductId] = useState('')
  const [page, setPage] = useState(0)
  const [size] = useState(8)
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  async function loadData(nextPage = page) {
    const params = { keyword, page: nextPage, size }
    if (productId) params.productId = Number(productId)

    const res = await getAdminReviews(params)
    setItems(res.data.items || [])
    setTotal(res.data.total || 0)
    setTotalPages(res.data.totalPages || 0)
    setPage(res.data.page || 0)
  }

  useEffect(() => {
    loadData(0)
  }, [keyword, productId])

  async function onDelete(id) {
    if (!window.confirm('Xóa review này?')) return
    await deleteAdminReview(id)
    loadData(page)
  }

  return (
    <div className="admin-panel">
      <div className="admin-panel__header">
        <h2 className="admin-panel__title">Quản lý đánh giá</h2>
        <div className="admin-toolbar admin-toolbar--reviews">
          <input className="admin-input" placeholder="Tìm theo user, sản phẩm, nội dung" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
          <input className="admin-input" type="number" placeholder="Lọc productId" value={productId} onChange={(e) => setProductId(e.target.value)} />
        </div>
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Product</th>
              <th>User</th>
              <th>Rating</th>
              <th>Comment</th>
              <th>Media</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.id}</td>
                <td>{item.productName} (#{item.productId})</td>
                <td>{item.username} (#{item.accountId})</td>
                <td>{item.rating}</td>
                <td>{item.comment || '-'}</td>
                <td>
                  {(item.media || []).map((mediaItem) => (
                    <div key={mediaItem.id}>
                      <a href={mediaItem.mediaUrl} target="_blank" rel="noreferrer">{mediaItem.mediaType}</a>
                    </div>
                  ))}
                </td>
                <td>
                  <button type="button" className="admin-btn admin-btn--danger" onClick={() => onDelete(item.id)}>
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
          <button type="button" className="admin-btn admin-btn--ghost" disabled={page <= 0} onClick={() => loadData(page - 1)}>Trước</button>
          <span>Trang {page + 1}/{Math.max(totalPages, 1)}</span>
          <button type="button" className="admin-btn admin-btn--ghost" disabled={page + 1 >= totalPages} onClick={() => loadData(page + 1)}>Sau</button>
        </div>
      </div>
    </div>
  )
}

export default ReviewsPage
