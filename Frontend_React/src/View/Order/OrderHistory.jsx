import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { API_BASE, formatDate, toCurrency } from './orderApi'

function OrderHistory({ user, authChecked }) {
  const navigate = useNavigate()
  const [orderHistory, setOrderHistory] = useState([])
  const [status, setStatus] = useState('')
  const accountId = user?.id

  useEffect(() => {
    if (accountId) {
      loadOrderHistory(accountId)
    }
  }, [accountId])

  async function loadOrderHistory(inputAccountId) {
    const value = String(inputAccountId ?? accountId ?? '').trim()
    if (!value) {
      setStatus('Vui lòng đăng nhập để xem lịch sử đơn hàng')
      return
    }

    try {
      const response = await fetch(`${API_BASE}/orders/history?accountId=${value}`)
      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || 'Không thể tải lịch sử đơn hàng')
      }
      const data = await response.json()
      setOrderHistory(data)
      setStatus('Đã tải lịch sử đơn hàng')
    } catch (error) {
      setStatus(error.message)
    }
  }

  function goCreate() {
    navigate('/order/create')
  }

  function goDetail(orderId) {
    navigate(`/order/detail/${orderId}`)
  }

  if (authChecked && !user) {
    return (
      <main className="page">
        <section className="card shadow-lg border-0">
          <h1 className="h4 mb-3">Lịch sử đơn hàng</h1>
          <p className="text-muted mb-3">Vui lòng đăng nhập để xem lịch sử đơn hàng.</p>
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
          <h1 className="h3 mb-0">Lịch sử đơn hàng</h1>
          <button type="button" className="outline-button btn btn-outline-secondary" onClick={goCreate}>
            Quay lại tạo đơn
          </button>
        </div>

        <div className="media-row order-item-row mb-3">
          {user && (
            <p className="text-muted mb-0">
              Tài khoản: <strong>{user.username}</strong>
            </p>
          )}
          <button type="button" className="primary-button btn btn-primary" onClick={() => loadOrderHistory(String(accountId || ''))}>
            Xem lịch sử đơn hàng
          </button>
        </div>

        <div className="review-list">
          {orderHistory.length === 0 && <p className="text-muted">Chưa có đơn hàng nào.</p>}
          {orderHistory.map((order) => (
            <article key={order.id} className="review-item border rounded-3 p-3 shadow-sm">
              <div className="review-top">
                <strong>Đơn #{order.id}</strong>
                <span className="stars-text">{order.status}</span>
              </div>
              <p>Ngày đặt: {formatDate(order.orderDate)}</p>
              <p>Tổng tiền: {toCurrency(order.totalPrice)}</p>
              <button type="button" className="outline-button btn btn-outline-primary" onClick={() => goDetail(order.id)}>
                Xem chi tiết đơn hàng
              </button>
            </article>
          ))}
        </div>

        {status && <p className="status mt-3 text-muted">{status}</p>}
      </section>
    </main>
  )
}

export default OrderHistory
