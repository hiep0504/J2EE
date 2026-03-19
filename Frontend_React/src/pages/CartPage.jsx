import { useEffect, useState } from 'react'
import { getCart, removeCartItem, updateCartItem } from '../services/cartService'

function formatVnd(value) {
  const number = Number(value || 0)
  return `${number.toLocaleString('vi-VN')}đ`
}

function CartPage() {
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

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

  async function handleRemove(productId) {
    try {
      const res = await removeCartItem(productId)
      setCart(res.data)
    } catch {
      setError('Không thể xoá sản phẩm khỏi giỏ.')
    }
  }

  async function handleQuantityChange(productId, quantity) {
    const qty = Number(quantity)
    if (!Number.isFinite(qty) || qty < 1) return

    try {
      const res = await updateCartItem(productId, qty)
      setCart(res.data)
    } catch {
      setError('Không thể cập nhật số lượng.')
    }
  }

  if (loading) return <p className="text-center py-5 text-muted">Đang tải...</p>
  if (error) return <p className="text-center py-5 text-danger">{error}</p>

  const items = cart?.items || []

  return (
    <main className="container py-4">
      <h5 className="mb-4">Giỏ hàng</h5>

      {items.length === 0 ? (
        <p className="text-muted">Giỏ hàng trống.</p>
      ) : (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th style={{ width: 80 }}></th>
                <th>Sản phẩm</th>
                <th className="text-end">Đơn giá</th>
                <th style={{ width: 140 }}>Số lượng</th>
                <th className="text-end">Thành tiền</th>
                <th style={{ width: 90 }}></th>
              </tr>
            </thead>
            <tbody>
              {items.map((it) => (
                <tr key={it.productId}>
                  <td>
                    <img
                      src={it.image || 'https://placehold.co/80x80?text=No+Image'}
                      alt={it.name}
                      style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 8 }}
                    />
                  </td>
                  <td>
                    <div className="fw-semibold">{it.name}</div>
                  </td>
                  <td className="text-end">{formatVnd(it.price)}</td>
                  <td>
                    <input
                      type="number"
                      min={1}
                      className="form-control form-control-sm"
                      value={it.quantity}
                      onChange={(e) => handleQuantityChange(it.productId, e.target.value)}
                    />
                  </td>
                  <td className="text-end fw-semibold">{formatVnd(it.lineTotal)}</td>
                  <td className="text-end">
                    <button
                      type="button"
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => handleRemove(it.productId)}
                    >
                      Xoá
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={4} className="text-end fw-semibold">Tổng tiền</td>
                <td className="text-end fw-bold">{formatVnd(cart?.total)}</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </main>
  )
}

export default CartPage
