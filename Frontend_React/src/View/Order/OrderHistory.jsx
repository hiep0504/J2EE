import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { API_BASE, formatDate, toCurrency } from './orderApi'

function OrderHistory() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [accountId, setAccountId] = useState(searchParams.get('accountId') || '2')
  const [orderHistory, setOrderHistory] = useState([])
  const [status, setStatus] = useState('')

  useEffect(() => {
    if (accountId) {
      loadOrderHistory(accountId)
    }
  }, [])

  async function loadOrderHistory(inputAccountId) {
    const value = (inputAccountId ?? accountId).trim()
    if (!value) {
      setStatus('Vui lòng nhập accountId để xem lịch sử')
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
    navigate(`/order/detail/${orderId}?accountId=${accountId}`)
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
          <input
            type="number"
            min="1"
            value={accountId}
            onChange={(event) => setAccountId(event.target.value)}
            placeholder="Nhập accountId"
          />
          <button type="button" className="primary-button btn btn-primary" onClick={() => loadOrderHistory(accountId)}>
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
