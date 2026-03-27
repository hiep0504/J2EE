import { useEffect, useState } from 'react'
import { getAdminOrderDetail, getAdminOrders, updateAdminOrderStatus } from '../services/adminService'
import './AdminPages.css'

const statusOptions = ['pending', 'confirmed', 'shipping', 'completed', 'cancelled']

function getStatusColor(status) {
  switch (String(status || '').toLowerCase()) {
    case 'pending':
      return 'admin-tag--pending'
    case 'confirmed':
      return 'admin-tag--confirmed'
    case 'shipping':
      return 'admin-tag--shipping'
    case 'completed':
      return 'admin-tag--completed'
    case 'cancelled':
      return 'admin-tag--cancelled'
    default:
      return 'admin-tag--pending'
  }
}

function getStatusSelectClass(status) {
  switch (String(status || '').toLowerCase()) {
    case 'pending':
      return 'admin-select--pending'
    case 'confirmed':
      return 'admin-select--confirmed'
    case 'shipping':
      return 'admin-select--shipping'
    case 'completed':
      return 'admin-select--completed'
    case 'cancelled':
      return 'admin-select--cancelled'
    default:
      return ''
  }
}

function OrdersPage() {
  const [items, setItems] = useState([])
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [size] = useState(8)
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [detail, setDetail] = useState(null)
  const [updatingOrderId, setUpdatingOrderId] = useState(null)

  async function loadData(nextPage = page) {
    const params = { keyword, status, page: nextPage, size }
    const res = await getAdminOrders(params)
    setItems(res.data.items || [])
    setTotal(res.data.total || 0)
    setTotalPages(res.data.totalPages || 0)
    setPage(res.data.page || 0)
  }

  useEffect(() => {
    loadData(0)
  }, [keyword, status])

  async function openDetail(id) {
    const res = await getAdminOrderDetail(id)
    setDetail(res.data)
  }

  async function handleChangeStatus(orderId, nextStatus) {
    setUpdatingOrderId(orderId)

    try {
      const res = await updateAdminOrderStatus(orderId, { status: nextStatus })
      const updatedOrder = res.data

      setItems((prev) => prev.map((item) => {
        if (item.id !== orderId) {
          return item
        }
        return {
          ...item,
          status: updatedOrder.status,
        }
      }))

      setDetail((prev) => {
        if (!prev || prev.id !== orderId) {
          return prev
        }
        return {
          ...prev,
          status: updatedOrder.status,
        }
      })
    } finally {
      setUpdatingOrderId(null)
    }
  }

  return (
    <div className="admin-panel">
      <div className="admin-panel__header">
        <h2 className="admin-panel__title">Quản lý đơn hàng</h2>
        <div className="admin-toolbar admin-toolbar--orders">
          <input className="admin-input" placeholder="Tìm theo mã, user, SĐT" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
          <select className="admin-select" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tất cả trạng thái</option>
            {statusOptions.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </div>
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Mã</th>
              <th>Khách hàng</th>
              <th>Tổng tiền</th>
              <th>Trạng thái</th>
              <th>Ngày</th>
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>#{item.id}</td>
                <td>{item.username || '-'} (ID: {item.accountId})</td>
                <td>{item.totalPrice}</td>
                <td>
                  <span className={`admin-tag ${getStatusColor(item.status)}`}>{item.status}</span>
                </td>
                <td>{item.orderDate ? new Date(item.orderDate).toLocaleString() : '-'}</td>
                <td className="admin-order-actions-cell">
                  <div className="admin-order-actions">
                    <button type="button" className="admin-btn admin-btn--ghost" onClick={() => openDetail(item.id)}>
                      Chi tiết
                    </button>
                    <select
                      className={`admin-select admin-select--status ${getStatusSelectClass(item.status)}`}
                      value={item.status || ''}
                      onChange={(e) => handleChangeStatus(item.id, e.target.value)}
                      disabled={updatingOrderId === item.id}
                    >
                      {statusOptions.map((statusItem) => <option key={statusItem} value={statusItem}>{statusItem}</option>)}
                    </select>
                  </div>
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

      {detail && (
        <div className="admin-modal" onClick={() => setDetail(null)}>
          <div className="admin-modal__content" onClick={(e) => e.stopPropagation()}>
            <h3>Đơn hàng #{detail.id}</h3>
            <p><b>Người mua:</b> {detail.username} (ID: {detail.accountId})</p>
            <p><b>Địa chỉ:</b> {detail.address}</p>
            <p><b>SĐT:</b> {detail.phone}</p>
            <p><b>Trạng thái:</b> <span className={`admin-tag ${getStatusColor(detail.status)}`}>{detail.status}</span></p>
            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Size</th>
                    <th>SL</th>
                    <th>Giá</th>
                    <th>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {(detail.items || []).map((item) => (
                    <tr key={item.id}>
                      <td>{item.productName}</td>
                      <td>{item.sizeName}</td>
                      <td>{item.quantity}</td>
                      <td>{item.price}</td>
                      <td>{item.lineTotal}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="admin-toolbar">
              <button type="button" className="admin-btn admin-btn--ghost" onClick={() => setDetail(null)}>Đóng</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default OrdersPage
