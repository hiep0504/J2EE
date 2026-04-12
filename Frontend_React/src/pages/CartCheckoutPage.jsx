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
  const [detailAddress, setDetailAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('COD')
  const [submitting, setSubmitting] = useState(false)

  const [provinces, setProvinces] = useState([])
  const [districts, setDistricts] = useState([])
  const [wards, setWards] = useState([])
  const [selectedProvinceCode, setSelectedProvinceCode] = useState('')
  const [selectedDistrictCode, setSelectedDistrictCode] = useState('')
  const [selectedWardCode, setSelectedWardCode] = useState('')

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

  useEffect(() => {
    let mounted = true

    async function loadProvinces() {
      try {
        const res = await fetch('https://provinces.open-api.vn/api/p/')
        if (!res.ok) throw new Error('Không thể tải tỉnh/thành')
        const data = await res.json()
        if (mounted) {
          setProvinces(Array.isArray(data) ? data : [])
        }
      } catch {
        if (mounted) {
          setProvinces([])
        }
      }
    }

    loadProvinces()

    return () => {
      mounted = false
    }
  }, [])

  useEffect(() => {
    let mounted = true

    async function loadDistricts() {
      if (!selectedProvinceCode) {
        setDistricts([])
        setWards([])
        setSelectedDistrictCode('')
        setSelectedWardCode('')
        return
      }

      try {
        const res = await fetch(`https://provinces.open-api.vn/api/p/${selectedProvinceCode}?depth=2`)
        if (!res.ok) throw new Error('Không thể tải quận/huyện')
        const data = await res.json()
        if (mounted) {
          setDistricts(Array.isArray(data?.districts) ? data.districts : [])
          setWards([])
          setSelectedDistrictCode('')
          setSelectedWardCode('')
        }
      } catch {
        if (mounted) {
          setDistricts([])
          setWards([])
          setSelectedDistrictCode('')
          setSelectedWardCode('')
        }
      }
    }

    loadDistricts()

    return () => {
      mounted = false
    }
  }, [selectedProvinceCode])

  useEffect(() => {
    let mounted = true

    async function loadWards() {
      if (!selectedDistrictCode) {
        setWards([])
        setSelectedWardCode('')
        return
      }

      try {
        const res = await fetch(`https://provinces.open-api.vn/api/d/${selectedDistrictCode}?depth=2`)
        if (!res.ok) throw new Error('Không thể tải phường/xã')
        const data = await res.json()
        if (mounted) {
          setWards(Array.isArray(data?.wards) ? data.wards : [])
          setSelectedWardCode('')
        }
      } catch {
        if (mounted) {
          setWards([])
          setSelectedWardCode('')
        }
      }
    }

    loadWards()

    return () => {
      mounted = false
    }
  }, [selectedDistrictCode])

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

  const selectedProvinceName = useMemo(
    () => provinces.find((p) => String(p.code) === String(selectedProvinceCode))?.name || '',
    [provinces, selectedProvinceCode]
  )

  const selectedDistrictName = useMemo(
    () => districts.find((d) => String(d.code) === String(selectedDistrictCode))?.name || '',
    [districts, selectedDistrictCode]
  )

  const selectedWardName = useMemo(
    () => wards.find((w) => String(w.code) === String(selectedWardCode))?.name || '',
    [wards, selectedWardCode]
  )

  const fullAddress = useMemo(() => {
    const parts = [detailAddress.trim(), selectedWardName, selectedDistrictName, selectedProvinceName]
      .filter(Boolean)
    return parts.join(', ')
  }, [detailAddress, selectedWardName, selectedDistrictName, selectedProvinceName])

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    if (selectedItems.length === 0) {
      setError('Không có sản phẩm nào được chọn để thanh toán.')
      return
    }
    if (!selectedProvinceCode || !selectedDistrictCode || !selectedWardCode) {
      setError('Vui lòng chọn đầy đủ Tỉnh/Thành, Quận/Huyện và Phường/Xã.')
      return
    }
    if (!detailAddress.trim()) {
      setError('Vui lòng nhập số nhà, tên đường.')
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
        address: fullAddress,
        phone,
        paymentMethod,
        items: payloadItems,
      })

      const order = checkoutRes.data
      localStorage.setItem('lastOrderId', String(order.id))
      localStorage.setItem('lastOrderAccountId', String(order.accountId || ''))
      localStorage.setItem('lastOrderTotal', String(order.totalPrice || 0))
      localStorage.setItem('lastOrderAddress', order.address || fullAddress)
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
                    <label className="form-label">Tỉnh/Thành phố</label>
                    <select
                      className="form-select"
                      value={selectedProvinceCode}
                      onChange={(e) => setSelectedProvinceCode(e.target.value)}
                    >
                      <option value="">Chọn Tỉnh/Thành phố</option>
                      {provinces.map((province) => (
                        <option key={province.code} value={String(province.code)}>
                          {province.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="mb-3">
                    <label className="form-label">Quận/Huyện</label>
                    <select
                      className="form-select"
                      value={selectedDistrictCode}
                      onChange={(e) => setSelectedDistrictCode(e.target.value)}
                      disabled={!selectedProvinceCode}
                    >
                      <option value="">Chọn Quận/Huyện</option>
                      {districts.map((district) => (
                        <option key={district.code} value={String(district.code)}>
                          {district.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="mb-3">
                    <label className="form-label">Phường/Xã</label>
                    <select
                      className="form-select"
                      value={selectedWardCode}
                      onChange={(e) => setSelectedWardCode(e.target.value)}
                      disabled={!selectedDistrictCode}
                    >
                      <option value="">Chọn Phường/Xã</option>
                      {wards.map((ward) => (
                        <option key={ward.code} value={String(ward.code)}>
                          {ward.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="mb-3">
                    <label className="form-label">Số nhà, tên đường</label>
                    <input
                      type="text"
                      className="form-control"
                      value={detailAddress}
                      onChange={(e) => setDetailAddress(e.target.value)}
                      placeholder="Ví dụ: 123 Nguyễn Trãi"
                    />
                  </div>

                  {fullAddress ? (
                    <div className="alert alert-light border mb-4">
                      <strong>Địa chỉ giao hàng:</strong> {fullAddress}
                    </div>
                  ) : null}

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
