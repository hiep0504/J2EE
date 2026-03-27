import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { checkoutCart, getCart } from '../services/cartService'
import { createVnPayPayment } from '../services/paymentService'

function formatVnd(value) {
  const number = Number(value || 0)
  return `${number.toLocaleString('vi-VN')}đ`
}

function normalizeSelectedItems(input) {
  if (!Array.isArray(input)) return []

  return input
    .filter((item) => item && item.productId && item.sizeId)
    .map((item) => ({
      productId: Number(item.productId),
      sizeId: Number(item.sizeId),
    }))
}

function CartCheckoutPage() {
  const navigate = useNavigate()
  const location = useLocation()

  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('COD')
  const [submitting, setSubmitting] = useState(false)

  const selectedItemsInput = useMemo(() => {
    const fromState = normalizeSelectedItems(location.state?.selectedItems)
    if (fromState.length > 0) return fromState

    try {
      const raw = sessionStorage.getItem('cartCheckoutSelection')
      return normalizeSelectedItems(raw ? JSON.parse(raw) : [])
    } catch {
      return []
    }
  }, [location.state])

  useEffect(() => {
    async function loadCart() {
      setError('')
      try {
        const res = await getCart()
        setCart(res.data)
      } catch {
        setError('Không thể tải giỏ hàng.')
      } finally {
        setLoading(false)
      }
    }

    loadCart()
  }, [])

  const selectedKeys = useMemo(
    () => new Set(selectedItemsInput.map((item) => `${item.productId}-${item.sizeId}`)),
    [selectedItemsInput]
  )

  const selectedItems = useMemo(() => {
    const allItems = cart?.items || []
    return allItems.filter((item) => selectedKeys.has(`${item.productId}-${item.sizeId}`))
  }, [cart, selectedKeys])

  const selectedTotal = useMemo(
    () => selectedItems.reduce((sum, item) => sum + Number(item.lineTotal || 0), 0),
    [selectedItems]
  )

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    if (selectedItems.length === 0) {
      setError('Không có sản phẩm nào được chọn để thanh toán.')
      return
    }
    if (!address.trim()) {
      setError('Vui lòng nhập địa chỉ giao hàng.')
      return
    }
    if (!phone.trim()) {
      setError('Vui lòng nhập số điện thoại.')
      return
    }

    const payloadItems = selectedItems.map((item) => ({
      productId: item.productId,
      sizeId: item.sizeId,
    }))

    try {
      setSubmitting(true)
      const checkoutRes = await checkoutCart({
        address,
        phone,
        paymentMethod,
        items: payloadItems,
      })

      const order = checkoutRes.data
      localStorage.setItem('lastOrderId', String(order.id))
      localStorage.setItem('lastOrderAccountId', String(order.accountId || ''))
      localStorage.setItem('lastOrderTotal', String(order.totalPrice || 0))
      localStorage.setItem('lastOrderAddress', order.address || address)
      localStorage.setItem('lastOrderPhone', order.phone || phone)
      localStorage.setItem('lastOrderPaymentMethod', paymentMethod)
      if (order.orderDate) {
        localStorage.setItem('lastOrderDate', order.orderDate)
      }
      sessionStorage.removeItem('cartCheckoutSelection')

      if (paymentMethod === 'VNPAY') {
        const paymentRes = await createVnPayPayment(Number(order.totalPrice || 0), '', 'vn')
        const paymentUrl = paymentRes?.data?.paymentUrl
        if (!paymentUrl) {
          throw new Error('Không thể tạo URL thanh toán VNPay.')
        }
        window.location.href = paymentUrl
        return
      }

      navigate('/order/payment-result?status=cod')
    } catch (err) {
      const message = err?.response?.data?.message || err?.message || 'Không thể thanh toán.'
      setError(message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <p className="text-center py-5 text-muted">Đang tải...</p>
  }

  return (
    <main className="container py-4">
      <h5 className="mb-4">Xác nhận thanh toán</h5>

      {error ? <div className="alert alert-danger">{error}</div> : null}

      {selectedItems.length === 0 ? (
        <div className="card border-0 shadow-sm">
          <div className="card-body">
            <p className="mb-3">Không có sản phẩm được chọn.</p>
            <Link to="/cart" className="btn btn-outline-secondary">Quay lại giỏ hàng</Link>
          </div>
        </div>
      ) : (
        <div className="row g-3">
          <div className="col-12 col-lg-7">
            <div className="card border-0 shadow-sm">
              <div className="card-body">
                <h6 className="mb-3">Thông tin giao hàng</h6>
                <form onSubmit={handleSubmit}>
                  <div className="mb-3">
                    <label className="form-label">Số điện thoại</label>
                    <input
                      type="text"
                      className="form-control"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      placeholder="Nhập số điện thoại nhận hàng"
                    />
                  </div>

                  <div className="mb-3">
                    <label className="form-label">Địa chỉ giao hàng</label>
                    <input
                      type="text"
                      className="form-control"
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                      placeholder="Số nhà, đường, quận/huyện, tỉnh/thành..."
                    />
                  </div>

                  <div className="mb-4">
                    <label className="form-label">Phương thức thanh toán</label>
                    <select
                      className="form-select"
                      value={paymentMethod}
                      onChange={(e) => setPaymentMethod(e.target.value)}
                    >
                      <option value="COD">Thanh toán khi nhận hàng (COD)</option>
                      <option value="VNPAY">Thanh toán online qua VNPay</option>
                    </select>
                  </div>

                  <div className="d-flex gap-2">
                    <button type="submit" className="btn btn-success" disabled={submitting}>
                      {submitting ? 'Đang xử lý...' : paymentMethod === 'VNPAY' ? 'Tiếp tục đến VNPay' : 'Tạo đơn hàng'}
                    </button>
                    <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/cart')}>
                      Quay lại giỏ hàng
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>

          <div className="col-12 col-lg-5">
            <div className="card border-0 shadow-sm">
              <div className="card-body">
                <h6 className="mb-3">Sản phẩm đã chọn</h6>
                <div className="list-group list-group-flush">
                  {selectedItems.map((item) => (
                    <div key={`${item.productId}-${item.sizeId}`} className="list-group-item px-0 d-flex justify-content-between">
                      <div>
                        <div className="fw-semibold">{item.name}</div>
                        <small className="text-muted">Size: {item.sizeName} • SL: {item.quantity}</small>
                      </div>
                      <div className="fw-semibold">{formatVnd(item.lineTotal)}</div>
                    </div>
                  ))}
                </div>
                <hr />
                <div className="d-flex justify-content-between">
                  <span className="fw-semibold">Tổng thanh toán</span>
                  <span className="text-danger fw-bold">{formatVnd(selectedTotal)}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}

export default CartCheckoutPage
