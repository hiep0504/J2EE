import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { API_BASE, formatDate, toCurrency } from './orderApi'

function OrderDetail() {
  const navigate = useNavigate()
  const { orderId } = useParams()
  const [searchParams] = useSearchParams()
  const [accountId, setAccountId] = useState(searchParams.get('accountId') || '2')
  const [orderDetail, setOrderDetail] = useState(null)
  const [status, setStatus] = useState('')

  useEffect(() => {
    if (orderId && accountId) {
      viewOrderDetail(orderId, accountId)
    }
  }, [orderId])

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
    navigate(`/order/history?accountId=${accountId}`)
  }

  function goCreate() {
    navigate('/order/create')
  }

  return (
    <main className="page">
      <section className="card">
        <div className="todo-header">
          <h1>Chi tiết đơn hàng</h1>
          <div className="todo-header">
            <button type="button" className="outline-button" onClick={goHistory}>
              Xem lịch sử đơn hàng
            </button>
            <button type="button" className="subtle-button" onClick={goCreate}>
              Tạo đơn hàng
            </button>
          </div>
        </div>

        <div className="media-row order-item-row">
          <input
            type="number"
            min="1"
            value={accountId}
            onChange={(event) => setAccountId(event.target.value)}
            placeholder="Nhập accountId"
          />
          <button type="button" className="primary-button" onClick={() => viewOrderDetail(orderId, accountId)}>
            Tải lại chi tiết
          </button>
        </div>

        {!orderDetail && <p>Không có dữ liệu chi tiết đơn hàng.</p>}

        {orderDetail && (
          <div className="review-list">
            <article className="review-item">
              <div className="review-top">
                <strong>Đơn #{orderDetail.id}</strong>
                <span className="stars-text">{orderDetail.status}</span>
              </div>
              <p>Ngày đặt: {formatDate(orderDetail.orderDate)}</p>
              <p>Địa chỉ: {orderDetail.address}</p>
              <p>SĐT: {orderDetail.phone}</p>
              <p>Tổng tiền: {toCurrency(orderDetail.totalPrice)}</p>
              <div className="media-grid">
                {(orderDetail.items || []).map((item) => (
                  <div className="review-item" key={item.orderDetailId}>
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

        {status && <p className="status">{status}</p>}
      </section>
    </main>
  )
}

export default OrderDetail
