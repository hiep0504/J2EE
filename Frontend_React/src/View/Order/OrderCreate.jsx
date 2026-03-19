import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { API_BASE, createVnpayPayment, createEmptyItem, toCurrency } from './orderApi'

function OrderCreate() {
  const navigate = useNavigate()
  const [accountId, setAccountId] = useState('2')
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [options, setOptions] = useState([])
  const [items, setItems] = useState([createEmptyItem()])
  const [createdOrder, setCreatedOrder] = useState(null)
  const [paymentMethod, setPaymentMethod] = useState('cod')
  const [status, setStatus] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    loadOrderOptions()
  }, [])

  async function loadOrderOptions() {
    try {
      const response = await fetch(`${API_BASE}/orders/options`)
      if (!response.ok) throw new Error('Không thể tải danh sách sản phẩm đặt hàng')
      const data = await response.json()
      setOptions(data)
    } catch (error) {
      setStatus(error.message)
    }
  }

  function addItemRow() {
    setItems((prev) => [...prev, createEmptyItem()])
  }

  function removeItemRow(index) {
    setItems((prev) => {
      if (prev.length === 1) return [createEmptyItem()]
      return prev.filter((_, idx) => idx !== index)
    })
  }

  function updateItem(index, field, value) {
    setItems((prev) =>
      prev.map((item, idx) => (idx === index ? { ...item, [field]: value } : item))
    )
  }

  async function createOrder(event) {
    event.preventDefault()

    if (!accountId.trim()) {
      setStatus('Vui lòng nhập accountId')
      return
    }
    if (!address.trim()) {
      setStatus('Vui lòng nhập địa chỉ giao hàng')
      return
    }
    if (!phone.trim()) {
      setStatus('Vui lòng nhập số điện thoại')
      return
    }

    const normalizedItems = items
      .filter((item) => item.productSizeId && Number(item.quantity) > 0)
      .map((item) => ({
        productSizeId: Number(item.productSizeId),
        quantity: Number(item.quantity),
      }))

    if (normalizedItems.length === 0) {
      setStatus('Vui lòng chọn ít nhất 1 sản phẩm')
      return
    }

    setIsSubmitting(true)
    setStatus('Đang tạo đơn hàng...')

    try {
      const response = await fetch(`${API_BASE}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          accountId: Number(accountId),
          address,
          phone,
          items: normalizedItems,
        }),
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || 'Tạo đơn hàng thất bại')
      }

      const data = await response.json()
      setCreatedOrder(data)

      if (paymentMethod === 'vnpay') {
        setStatus(`Đơn hàng #${data.id} đã tạo. Đang chuyển sang VNPay...`)
        const paymentUrl = await createVnpayPayment({
          orderId: data.id,
          accountId: Number(accountId),
          orderInfo: `Thanh toan don hang #${data.id}`,
        })
        window.location.assign(paymentUrl)
        return
      }

      setStatus(`Tạo đơn hàng thành công #${data.id}`)
    } catch (error) {
      setStatus(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  function goHistory() {
    navigate(`/order/history?accountId=${accountId}`)
  }

  function goDetail(orderId) {
    navigate(`/order/detail/${orderId}?accountId=${accountId}`)
  }

  return (
    <main className="page">
      <section className="card">
        <div className="todo-header">
          <h1>Tạo đơn hàng</h1>
          <button type="button" className="outline-button" onClick={goHistory}>
            Xem lịch sử đơn hàng
          </button>
        </div>

        <form onSubmit={createOrder} className="form-grid">
          <label>
            Account ID
            <input
              type="number"
              min="1"
              value={accountId}
              onChange={(event) => setAccountId(event.target.value)}
            />
          </label>

          <label>
            Địa chỉ giao hàng
            <input
              type="text"
              value={address}
              onChange={(event) => setAddress(event.target.value)}
              placeholder="Nhập địa chỉ"
            />
          </label>

          <label>
            Số điện thoại
            <input
              type="text"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              placeholder="Nhập số điện thoại"
            />
          </label>

          <fieldset className="payment-methods">
            <legend>Phương thức thanh toán</legend>
            <label className="payment-choice">
              <input
                type="radio"
                name="paymentMethod"
                value="cod"
                checked={paymentMethod === 'cod'}
                onChange={(event) => setPaymentMethod(event.target.value)}
              />
              <span>Thanh toán khi nhận hàng</span>
            </label>
            <label className="payment-choice">
              <input
                type="radio"
                name="paymentMethod"
                value="vnpay"
                checked={paymentMethod === 'vnpay'}
                onChange={(event) => setPaymentMethod(event.target.value)}
              />
              <span>Thanh toán online với VNPay</span>
            </label>
          </fieldset>

          <div>
            <p className="media-title">Sản phẩm đặt mua</p>
            {items.map((item, index) => (
              <div key={`order-item-${index}`} className="media-row order-item-row">
                <select
                  value={item.productSizeId}
                  onChange={(event) => updateItem(index, 'productSizeId', event.target.value)}
                >
                  <option value="">-- Chọn sản phẩm/size --</option>
                  {options.map((opt) => (
                    <option key={opt.productSizeId} value={opt.productSizeId}>
                      {opt.productName} - Size {opt.sizeName} - {toCurrency(opt.price)} (Tồn {opt.stock})
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  min="1"
                  value={item.quantity}
                  onChange={(event) => updateItem(index, 'quantity', event.target.value)}
                />
                <button type="button" className="subtle-button" onClick={() => removeItemRow(index)}>
                  Xóa
                </button>
              </div>
            ))}
            <button type="button" className="outline-button" onClick={addItemRow}>
              Thêm sản phẩm
            </button>
          </div>

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Đang xử lý...' : paymentMethod === 'vnpay' ? 'Tạo đơn và thanh toán VNPay' : 'Tạo đơn hàng'}
          </button>
        </form>

        {createdOrder && (
          <div className="review-list">
            <p className="status">Đơn #{createdOrder.id} - Tổng tiền: {toCurrency(createdOrder.totalPrice)}</p>
            <div className="todo-header">
              <button type="button" className="outline-button" onClick={goHistory}>
                Xem lịch sử đơn hàng
              </button>
              <button type="button" className="primary-button" onClick={() => goDetail(createdOrder.id)}>
                Xem chi tiết đơn hàng
              </button>
            </div>
          </div>
        )}

        {status && <p className="status">{status}</p>}
      </section>
    </main>
  )
}

export default OrderCreate
