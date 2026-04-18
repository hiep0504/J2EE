import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCart, removeCartItem, updateCartItem } from '../services/cartService'
import { toMediaUrl } from '../utils/mediaUrl'

function formatVnd(value) {
  const number = Number(value || 0)
  return `${number.toLocaleString('vi-VN')}đ`
}

function CartPage() {
  const navigate = useNavigate()
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedItems, setSelectedItems] = useState(new Set())

  async function refresh() {
    setError(null)
    try {
      const res = await getCart()
      setCart(res.data)
    } catch {
      setError('Không thể tải giỏ hàng.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refresh()
  }, [])

  const toggleItem = (productId, sizeId) => {
    const key = `${productId}-${sizeId}`
    const newSelected = new Set(selectedItems)
    if (newSelected.has(key)) {
      newSelected.delete(key)
    } else {
      newSelected.add(key)
    }
    setSelectedItems(newSelected)
  }

  const toggleAll = () => {
    if (selectedItems.size === cart?.items?.length) {
      setSelectedItems(new Set())
    } else {
      const newSelected = new Set()
      cart?.items?.forEach(item => {
        newSelected.add(`${item.productId}-${item.sizeId}`)
      })
      setSelectedItems(newSelected)
    }
  }

  async function handleRemove(productId, sizeId) {
    try {
      await removeCartItem(productId, sizeId)
      await refresh()
      const key = `${productId}-${sizeId}`
      setSelectedItems(prev => {
        const newSet = new Set(prev)
        newSet.delete(key)
        return newSet
      })
    } catch {
      setError('Không thể xoá sản phẩm khỏi giỏ.')
    }
  }

  async function handleQuantityChange(productId, sizeId, quantity) {
    const qty = Number(quantity)
    if (!Number.isFinite(qty) || qty < 1) return

    try {
      await updateCartItem(productId, sizeId, qty)
      await refresh()
    } catch {
      setError('Không thể cập nhật số lượng.')
    }
  }

  async function handleCheckout() {
    setError(null)
    if (selectedItems.size === 0) {
      setError('Vui lòng chọn ít nhất 1 sản phẩm để thanh toán.')
      return
    }

    const selectedPayload = items
      .filter((item) => selectedItems.has(`${item.productId}-${item.sizeId}`))
      .map((item) => ({
        productId: item.productId,
        sizeId: item.sizeId,
      }))

    sessionStorage.setItem('cartCheckoutSelection', JSON.stringify(selectedPayload))
    navigate('/cart/checkout', { state: { selectedItems: selectedPayload } })
  }

  if (loading) return <p className="text-center py-5 text-muted">Đang tải...</p>

  const items = cart?.items || []

  // Calculate total only from selected items
  const selectedTotal = items.reduce((sum, item) => {
    if (selectedItems.has(`${item.productId}-${item.sizeId}`)) {
      return sum + Number(item.lineTotal)
    }
    return sum
  }, 0)

  return (
    <main className="container py-4">
      <h5 className="mb-4">Giỏ hàng</h5>
      {error ? <div className="alert alert-danger">{error}</div> : null}

      {items.length === 0 ? (
        <p className="text-muted">Giỏ hàng trống.</p>
      ) : (
        <>
          <div className="table-responsive">
            <table className="table align-middle">
            <thead>
              <tr>
                <th style={{ width: 40 }}>
                  <input
                    type="checkbox"
                    className="form-check-input"
                    checked={selectedItems.size > 0 && selectedItems.size === items.length}
                    onChange={toggleAll}
                  />
                </th>
                <th style={{ width: 80 }}></th>
                <th>Sản phẩm</th>
                <th>Size</th>
                <th className="text-end">Đơn giá</th>
                <th style={{ width: 140 }}>Số lượng</th>
                <th className="text-end">Thành tiền</th>
                <th style={{ width: 90 }}></th>
              </tr>
            </thead>
            <tbody>
              {items.map((it) => {
                const key = `${it.productId}-${it.sizeId}`
                const isSelected = selectedItems.has(key)
                return (
                  <tr key={key}>
                    <td>
                      <input
                        type="checkbox"
                        className="form-check-input"
                        checked={isSelected}
                        onChange={() => toggleItem(it.productId, it.sizeId)}
                      />
                    </td>
                    <td>
                      <img
                        src={toMediaUrl(it.image || 'https://placehold.co/80x80?text=No+Image')}
                        alt={it.name}
                        style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 8 }}
                      />
                    </td>
                    <td>
                      <div className="fw-semibold">{it.name}</div>
                    </td>
                    <td>
                      <span className="badge bg-light text-dark">{it.sizeName}</span>
                    </td>
                    <td className="text-end">{formatVnd(it.price)}</td>
                    <td>
                      <input
                        type="number"
                        min={1}
                        className="form-control form-control-sm"
                        value={it.quantity}
                        onChange={(e) => handleQuantityChange(it.productId, it.sizeId, e.target.value)}
                      />
                    </td>
                    <td className="text-end fw-semibold" style={{ backgroundColor: isSelected ? '#e8f5e9' : '' }}>
                      {formatVnd(it.lineTotal)}
                    </td>
                    <td className="text-end">
                      <button
                        type="button"
                        className="btn btn-outline-danger btn-sm"
                        onClick={() => handleRemove(it.productId, it.sizeId)}
                      >
                        Xoá
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={6} className="text-end fw-semibold">
                  Tổng tiền ({selectedItems.size} items):
                </td>
                <td colSpan={2} className="text-end">
                  <span className="fw-bold" style={{ fontSize: '1.2em', color: '#d32f2f' }}>
                    {formatVnd(selectedTotal)}
                  </span>
                </td>
              </tr>
            </tfoot>
            </table>
          </div>

          <section className="card border-0 shadow-sm mt-3">
            <div className="card-body">
              <h6 className="mb-2">Bước tiếp theo</h6>
              <p className="text-muted mb-3">
                Sau khi bấm thanh toán, bạn sẽ chuyển sang trang chọn địa chỉ và phương thức thanh toán.
              </p>

              <div className="d-flex justify-content-between align-items-center mt-3">
                <div>
                  <div className="fw-semibold">Đã chọn: {selectedItems.size} sản phẩm</div>
                  <div className="text-danger fw-bold">Tổng thanh toán: {formatVnd(selectedTotal)}</div>
                </div>
                <button
                  type="button"
                  className="btn btn-success"
                  disabled={selectedItems.size === 0}
                  onClick={handleCheckout}
                >
                  Thanh toán
                </button>
              </div>
            </div>
          </section>
        </>
      )}
    </main>
  )
}

export default CartPage
