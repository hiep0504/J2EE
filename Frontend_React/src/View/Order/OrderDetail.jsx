import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { API_BASE, formatDate, toCurrency } from './orderApi'

function OrderDetail({ user, authChecked }) {
  const navigate = useNavigate()
  const { orderId } = useParams()
  const [orderDetail, setOrderDetail] = useState(null)
  const [status, setStatus] = useState('')
  const accountId = user?.id

  useEffect(() => {
    if (orderId && accountId) {
      viewOrderDetail(orderId, accountId)
    }
  }, [orderId, accountId])

  async function viewOrderDetail(inputOrderId, inputAccountId) {
    const orderValue = String(inputOrderId || orderId || '').trim()
    const accountValue = String(inputAccountId || accountId || '').trim()

    if (!orderValue || !accountValue) {
      setStatus('Thiếu orderId hoặc accountId')
      return
    }

    try {
      const response = await fetch(`${API_BASE}/orders/${orderValue}?accountId=${accountValue}`)
      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || 'Không thể tải chi tiết đơn hàng')
      }
      const data = await response.json()
      setOrderDetail(data)
      setStatus(`Đang xem chi tiết đơn #${orderValue}`)
    } catch (error) {
      setStatus(error.message)
    }
  }

  function goHistory() {
    navigate('/order/history')
  }

  function goCreate() {
    navigate('/order/create')
  }

  if (authChecked && !user) {
    return (
      <main className="page">
        <section className="card shadow-lg border-0">
          <h1 className="h4 mb-3">Chi tiết đơn hàng</h1>
          <p className="text-muted mb-3">Vui lòng đăng nhập để xem chi tiết đơn hàng.</p>
          <button type="button" className="primary-button btn btn-primary" onClick={() => navigate('/login')}>
            Đi đến đăng nhập
          </button>
        </section>
      </main>
    )
  }

  return (
    <main className="page">
      <section className="card shadow-lg border-0">
        <div className="todo-header mb-3">
          <h1 className="h3 mb-0">Chi tiết đơn hàng</h1>
          <div className="todo-header">
            <button type="button" className="outline-button btn btn-outline-secondary" onClick={goHistory}>
              Xem lịch sử đơn hàng
            </button>
            <button type="button" className="subtle-button btn btn-light" onClick={goCreate}>
              Tạo đơn hàng
            </button>
          </div>
        </div>

        <div className="media-row order-item-row mb-3">
          {user && (
            <p className="text-muted mb-0">
              Tài khoản: <strong>{user.username}</strong>
            </p>
          )}
          <button type="button" className="primary-button btn btn-primary" onClick={() => viewOrderDetail(orderId, accountId)}>
            Tải lại chi tiết
          </button>
        </div>

        {!orderDetail && <p className="text-muted">Không có dữ liệu chi tiết đơn hàng.</p>}

        {orderDetail && (
          <div className="review-list">
            <article className="review-item border rounded-3 p-3 shadow-sm">
              <div className="review-top">
                <strong>Đơn #{orderDetail.id}</strong>
                <span className="stars-text">{orderDetail.status}</span>
              </div>
              <p>Ngày đặt: {formatDate(orderDetail.orderDate)}</p>
              <p>Địa chỉ: {orderDetail.address}</p>
              <p>SĐT: {orderDetail.phone}</p>
              <p>Tổng tiền: {toCurrency(orderDetail.totalPrice)}</p>
              <div className="media-grid mt-3">
                {(orderDetail.items || []).map((item) => (
                  <div className="review-item border rounded-3 p-2" key={item.orderDetailId}>
                    <strong>{item.productName} - Size {item.sizeName}</strong>
                    <p>Số lượng: {item.quantity}</p>
                    <p>Đơn giá: {toCurrency(item.unitPrice)}</p>
                    <p>Thành tiền: {toCurrency(item.lineTotal)}</p>
                  </div>
                ))}
              </div>
            </article>
          </div>
        )}

        {status && <p className="status mt-3 text-muted">{status}</p>}
      </section>
    </main>
  )
}

export default OrderDetail
