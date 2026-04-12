import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { API_BASE, createEmptyItem, toCurrency } from './orderApi'

function OrderCreate({ user, authChecked }) {
  const navigate = useNavigate()
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [provinces, setProvinces] = useState([])
  const [districts, setDistricts] = useState([])
  const [wards, setWards] = useState([])
  const [provinceCode, setProvinceCode] = useState('')
  const [districtCode, setDistrictCode] = useState('')
  const [wardCode, setWardCode] = useState('')
  const [options, setOptions] = useState([])
  const [items, setItems] = useState([createEmptyItem()])
  const [createdOrder, setCreatedOrder] = useState(null)
  const [status, setStatus] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState('COD') // 'COD' | 'VNPAY'
  const accountId = user?.id

  useEffect(() => {
    loadOrderOptions()
  }, [])

  useEffect(() => {
    // Load danh sách tỉnh/thành phố từ API hành chính Việt Nam
    async function loadProvinces() {
      try {
        const res = await fetch('https://provinces.open-api.vn/api/?depth=1')
        if (!res.ok) throw new Error('Không thể tải danh sách tỉnh/thành phố')
        const data = await res.json()
        setProvinces(data || [])
      } catch (error) {
        console.error(error)
      }
    }
    loadProvinces()
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

    if (!accountId) {
      setStatus('Vui lòng đăng nhập để tạo đơn hàng')
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

    if (!provinceCode) {
      setStatus('Vui lòng chọn Tỉnh / Thành phố')
      return
    }
    if (!districtCode) {
      setStatus('Vui lòng chọn Quận / Huyện')
      return
    }
    if (!wardCode) {
      setStatus('Vui lòng chọn Phường / Xã')
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

    const provinceName = provinces.find((p) => String(p.code) === String(provinceCode))?.name
    const districtName = districts.find((d) => String(d.code) === String(districtCode))?.name
    const wardName = wards.find((w) => String(w.code) === String(wardCode))?.name

    // Ghép địa chỉ theo đúng thứ tự hiển thị trên form:
    // Tỉnh / Thành phố, Quận / Huyện, Phường / Xã, Địa chỉ chi tiết
    const fullAddressParts = [provinceName, districtName, wardName, address.trim()].filter(Boolean)
    const fullAddress = fullAddressParts.join(', ')

    setIsSubmitting(true)
    setStatus('Đang tạo đơn hàng...')

    try {
      const response = await fetch(`${API_BASE}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          accountId: Number(accountId),
          address: fullAddress,
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

      // Lưu thông tin đơn hàng cho trang kết quả thanh toán
      localStorage.setItem('lastOrderId', String(data.id))
      localStorage.setItem('lastOrderAccountId', String(accountId))
      if (data.totalPrice !== undefined && data.totalPrice !== null) {
        localStorage.setItem('lastOrderTotal', String(data.totalPrice))
      }
      if (data.orderDate) {
        localStorage.setItem('lastOrderDate', data.orderDate)
      }
      localStorage.setItem('lastOrderAddress', fullAddress)
      localStorage.setItem('lastOrderPhone', phone)
      localStorage.setItem('lastOrderPaymentMethod', paymentMethod)

      if (paymentMethod === 'VNPAY') {
        setStatus(`Tạo đơn hàng thành công #${data.id} - Đang chuyển sang VNPay...`)
        // Sau khi set state, gọi thanh toán VNPay
        await payWithVnPay(data)
      } else {
        setStatus(`Tạo đơn hàng COD thành công #${data.id}`)
        // Chuyển sang trang kết quả đơn hàng (giống VNPay)
        navigate('/order/payment-result?status=cod')
      }
    } catch (error) {
      setStatus(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleProvinceChange(event) {
    const code = event.target.value
    setProvinceCode(code)
    setDistrictCode('')
    setWardCode('')
    setDistricts([])
    setWards([])

    if (!code) return
    try {
      const res = await fetch(`https://provinces.open-api.vn/api/p/${code}?depth=2`)
      if (!res.ok) throw new Error()
      const data = await res.json()
      setDistricts(data.districts || [])
    } catch {
      setDistricts([])
    }
  }

  async function handleDistrictChange(event) {
    const code = event.target.value
    setDistrictCode(code)
    setWardCode('')
    setWards([])

    if (!code) return
    try {
      const res = await fetch(`https://provinces.open-api.vn/api/d/${code}?depth=2`)
      if (!res.ok) throw new Error()
      const data = await res.json()
      setWards(data.wards || [])
    } catch {
      setWards([])
    }
  }

  function goHistory() {
    navigate('/order/history')
  }

  function goDetail(orderId) {
    navigate(`/order/detail/${orderId}`)
  }

  async function payWithVnPay(orderFromParam) {
    const order = orderFromParam || createdOrder
    if (!order) {
      setStatus('Vui lòng tạo đơn hàng trước khi thanh toán VNPay')
      return
    }

    try {
      // Luu lai thong tin don hang de trang ket qua co the dung
      localStorage.setItem('lastOrderId', String(order.id))
      localStorage.setItem('lastOrderAccountId', String(accountId))
      if (order.totalPrice !== undefined && order.totalPrice !== null) {
        localStorage.setItem('lastOrderTotal', String(order.totalPrice))
      }

      setStatus('Đang tạo URL thanh toán VNPay...')
      const response = await fetch(`${API_BASE}/vnpay/create-payment`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          amount: Number(order.totalPrice),
          bankCode: '',
          language: 'vn',
        }),
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || 'Không thể tạo URL thanh toán VNPay')
      }

      const data = await response.json()
      if (data && data.paymentUrl) {
        window.location.href = data.paymentUrl
      } else {
        throw new Error('Phản hồi VNPay không hợp lệ')
      }
    } catch (error) {
      setStatus(error.message)
    }
  }

  const selectedItems = items
    .filter((item) => item.productSizeId && Number(item.quantity) > 0)
    .map((item) => {
      const opt = options.find((o) => o.productSizeId === Number(item.productSizeId))
      if (!opt) return null
      const quantity = Number(item.quantity) || 0
      const lineTotal = (Number(opt.price) || 0) * quantity
      return {
        id: item.productSizeId,
        name: opt.productName,
        size: opt.sizeName,
        quantity,
        price: Number(opt.price) || 0,
        lineTotal,
      }
    })
    .filter(Boolean)

  const totalQuantity = selectedItems.reduce((sum, item) => sum + item.quantity, 0)
  const estimatedTotal = selectedItems.reduce((sum, item) => sum + item.lineTotal, 0)

  const submitLabel = paymentMethod === 'VNPAY' ? 'Thanh toán qua VNPay' : 'Đặt hàng (COD)'

  if (authChecked && !user) {
    return (
      <main className="page">
        <section className="card shadow-lg border-0">
          <h1 className="h4 mb-3">Tạo đơn hàng</h1>
          <p className="text-muted mb-3">Vui lòng đăng nhập để tạo đơn hàng.</p>
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
          <h1 className="h3 mb-0">Tạo đơn hàng</h1>
          <button type="button" className="outline-button btn btn-outline-secondary" onClick={goHistory}>
            Xem lịch sử đơn hàng
          </button>
        </div>

        <form onSubmit={createOrder} className="form-grid order-layout">
          <div className="order-main">
            {user && (
              <p className="text-muted mb-2">
                Đang đặt hàng với tài khoản: <strong>{user.username}</strong>
              </p>
            )}

            <label>
              Số điện thoại
              <input
                type="text"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                placeholder="Nhập số điện thoại"
              />
            </label>

            <div>
              <label>
                Tỉnh / Thành phố
                <select value={provinceCode} onChange={handleProvinceChange}>
                  <option value="">Chọn tỉnh / thành phố</option>
                  {provinces.map((p) => (
                    <option key={p.code} value={p.code}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Quận / Huyện
                <select value={districtCode} onChange={handleDistrictChange} disabled={!provinceCode}>
                  <option value="">Chọn quận / huyện</option>
                  {districts.map((d) => (
                    <option key={d.code} value={d.code}>
                      {d.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Phường / Xã
                <select
                  value={wardCode}
                  onChange={(event) => setWardCode(event.target.value)}
                  disabled={!districtCode}
                >
                  <option value="">Chọn phường / xã</option>
                  {wards.map((w) => (
                    <option key={w.code} value={w.code}>
                      {w.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <label>
              Địa chỉ giao hàng
              <input
                type="text"
                value={address}
                onChange={(event) => setAddress(event.target.value)}
                placeholder="Số nhà, đường..."
              />
            </label>

            <fieldset className="stars-field">
              <legend>Phương thức thanh toán</legend>
              <select
                value={paymentMethod}
                onChange={(event) => setPaymentMethod(event.target.value)}
              >
                <option value="COD">Thanh toán khi nhận hàng (COD)</option>
                <option value="VNPAY">Thanh toán online qua VNPay</option>
              </select>
            </fieldset>

            <button className="primary-button btn btn-success w-100" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang tạo...' : submitLabel}
            </button>
          </div>

          <div className="order-summary">
            <p className="media-title">Giỏ hàng</p>
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
            <button type="button" className="outline-button btn btn-outline-primary" onClick={addItemRow}>
              Thêm sản phẩm
            </button>

            <div className="order-summary-box">
              <p>
                <strong>Tổng số lượng:</strong> {totalQuantity}
              </p>
              <p>
                <strong>Tạm tính:</strong> {toCurrency(estimatedTotal)}
              </p>
            </div>
          </div>
        </form>

        {createdOrder && paymentMethod !== 'VNPAY' && (
          <div className="review-list mt-3">
            <p className="status alert alert-success mb-2">Đơn #{createdOrder.id} - Tổng tiền: {toCurrency(createdOrder.totalPrice)}</p>
            <div className="todo-header">
              <button type="button" className="outline-button btn btn-outline-secondary" onClick={goHistory}>
                Xem lịch sử đơn hàng
              </button>
              <button type="button" className="primary-button btn btn-primary" onClick={() => goDetail(createdOrder.id)}>
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
