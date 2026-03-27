import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMe, updateMe } from '../services/accountService'
import {
  createMyReview,
  deleteMyReview,
  getMyOrderDetail,
  getMyOrders,
  getMyPurchasedProducts,
  updateMyReview,
} from '../services/accountPortalService'
import { toMediaUrl } from '../utils/mediaUrl'
import './AccountPage.css'

function AccountPage({ authChecked, user, onRefreshed }) {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('profile')

  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [profileStatus, setProfileStatus] = useState('')
  const [saving, setSaving] = useState(false)

  const [orders, setOrders] = useState([])
  const [ordersLoading, setOrdersLoading] = useState(false)
  const [ordersStatus, setOrdersStatus] = useState('')
  const [expandedOrderId, setExpandedOrderId] = useState(null)
  const [orderDetailById, setOrderDetailById] = useState({})

  const [purchasedProducts, setPurchasedProducts] = useState([])
  const [productsLoading, setProductsLoading] = useState(false)
  const [productsStatus, setProductsStatus] = useState('')

  const [reviewEditorOpen, setReviewEditorOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [editingReview, setEditingReview] = useState(null)
  const [rating, setRating] = useState(0)
  const [hoveredRating, setHoveredRating] = useState(0)
  const [comment, setComment] = useState('')
  const [imageFiles, setImageFiles] = useState([])
  const [videoFile, setVideoFile] = useState(null)
  const [replaceMedia, setReplaceMedia] = useState(false)
  const [reviewSaving, setReviewSaving] = useState(false)
  const [reviewStatus, setReviewStatus] = useState('')

  const canShowForm = useMemo(() => !!user, [user])
  const imagePreviewUrls = useMemo(
    () => imageFiles.map((file) => URL.createObjectURL(file)),
    [imageFiles]
  )

  useEffect(() => {
    if (authChecked && !user) {
      navigate('/login')
    }
  }, [authChecked, user, navigate])

  useEffect(() => {
    if (user) {
      setEmail(user.email || '')
      setPhone(user.phone || '')
    }
  }, [user])

  useEffect(() => {
    if (!user) return
    loadOrders()
    loadPurchasedProducts()
  }, [user])

  useEffect(() => {
    return () => {
      imagePreviewUrls.forEach((url) => URL.revokeObjectURL(url))
    }
  }, [imagePreviewUrls])

  function toCurrency(value) {
    if (value === null || value === undefined) return '0 đ'
    return Number(value).toLocaleString('vi-VN') + ' đ'
  }

  function toDate(value) {
    if (!value) return '-'
    return new Date(value).toLocaleString('vi-VN')
  }

  function normalizeStatus(status) {
    const value = (status || '').toLowerCase()
    if (value === 'cancelled') return 'canceled'
    return value || '-'
  }

  async function refresh() {
    try {
      const res = await getMe()
      await onRefreshed?.(res.data)
    } catch {
      await onRefreshed?.(null)
    }
  }

  async function handleSave(event) {
    event.preventDefault()
    setProfileStatus('')

    setSaving(true)
    try {
      await updateMe({ email, phone })
      setProfileStatus('Cập nhật thành công')
      await refresh()
    } catch (error) {
      const message = error?.response?.data?.message || 'Cập nhật thất bại'
      setProfileStatus(message)
    } finally {
      setSaving(false)
    }
  }

  async function loadOrders() {
    setOrdersLoading(true)
    setOrdersStatus('')
    try {
      const res = await getMyOrders()
      setOrders(Array.isArray(res.data) ? res.data : [])
    } catch (error) {
      setOrdersStatus(error?.response?.data?.message || 'Không thể tải đơn hàng')
    } finally {
      setOrdersLoading(false)
    }
  }

  async function loadPurchasedProducts() {
    setProductsLoading(true)
    setProductsStatus('')
    try {
      const res = await getMyPurchasedProducts()
      setPurchasedProducts(Array.isArray(res.data) ? res.data : [])
    } catch (error) {
      setProductsStatus(error?.response?.data?.message || 'Không thể tải sản phẩm đã mua')
    } finally {
      setProductsLoading(false)
    }
  }

  async function toggleOrderDetail(orderId) {
    if (expandedOrderId === orderId) {
      setExpandedOrderId(null)
      return
    }

    setExpandedOrderId(orderId)
    if (orderDetailById[orderId]) {
      return
    }

    try {
      const res = await getMyOrderDetail(orderId)
      setOrderDetailById((prev) => ({ ...prev, [orderId]: res.data }))
    } catch (error) {
      setOrdersStatus(error?.response?.data?.message || 'Không thể tải chi tiết đơn hàng')
    }
  }

  function openCreateReview(product) {
    setEditingProduct(product)
    setEditingReview(null)
    setRating(0)
    setHoveredRating(0)
    setComment('')
    setImageFiles([])
    setVideoFile(null)
    setReplaceMedia(false)
    setReviewStatus('')
    setReviewEditorOpen(true)
  }

  function openEditReview(product) {
    const review = product.review
    setEditingProduct(product)
    setEditingReview(review)
    setRating(review?.rating || 0)
    setHoveredRating(0)
    setComment(review?.comment || '')
    setImageFiles([])
    setVideoFile(null)
    setReplaceMedia(false)
    setReviewStatus('')
    setReviewEditorOpen(true)
  }

  function closeReviewEditor() {
    setReviewEditorOpen(false)
    setEditingProduct(null)
    setEditingReview(null)
    setImageFiles([])
    setVideoFile(null)
    setReviewStatus('')
  }

  function handleImageChange(event) {
    const files = Array.from(event.target.files || [])
    if (files.length === 0) return

    const merged = [...imageFiles, ...files]
    if (merged.length > 5) {
      setReviewStatus('Bạn chỉ được upload tối đa 5 ảnh')
      event.target.value = ''
      return
    }

    setImageFiles(merged)
    setReviewStatus('')
    event.target.value = ''
  }

  function handleVideoChange(event) {
    const file = event.target.files?.[0] || null
    if (!file) return
    setVideoFile(file)
    setReviewStatus('')
  }

  function removeImage(index) {
    setImageFiles((prev) => prev.filter((_, idx) => idx !== index))
  }

  async function handleSubmitReview(event) {
    event.preventDefault()

    if (!editingProduct?.productId) {
      setReviewStatus('Không xác định được sản phẩm cần đánh giá')
      return
    }
    if (rating < 1 || rating > 5) {
      setReviewStatus('Vui lòng chọn số sao từ 1 đến 5')
      return
    }
    if (imageFiles.length > 5) {
      setReviewStatus('Bạn chỉ được upload tối đa 5 ảnh')
      return
    }

    const payload = new FormData()
    payload.append('rating', String(rating))
    payload.append('comment', comment || '')
    payload.append('replaceMedia', String(replaceMedia))

    imageFiles.forEach((file) => payload.append('images', file))
    if (videoFile) {
      payload.append('video', videoFile)
    }

    setReviewSaving(true)
    setReviewStatus('')
    try {
      if (editingReview?.id) {
        await updateMyReview(editingReview.id, payload)
      } else {
        await createMyReview(editingProduct.productId, payload)
      }
      await loadPurchasedProducts()
      closeReviewEditor()
    } catch (error) {
      setReviewStatus(error?.response?.data?.message || 'Không thể lưu đánh giá')
    } finally {
      setReviewSaving(false)
    }
  }

  async function handleDeleteReview(reviewId) {
    const ok = window.confirm('Bạn có chắc muốn xóa đánh giá này không?')
    if (!ok) return

    try {
      await deleteMyReview(reviewId)
      await loadPurchasedProducts()
    } catch (error) {
      setProductsStatus(error?.response?.data?.message || 'Không thể xóa đánh giá')
    }
  }

  function renderProfileTab() {
    return (
      <section className="account-panel">
        <header className="panel-header">
          <h2>Thông tin cá nhân</h2>
          <p>Quản lý hồ sơ, email và số điện thoại của bạn.</p>
        </header>

        <div className="profile-grid">
          <div className="profile-card">
            <p><strong>Username:</strong> {user.username}</p>
            <p><strong>Role:</strong> {user.role}</p>
            <p><strong>Ngày tạo:</strong> {toDate(user.createdAt)}</p>
          </div>

          <form onSubmit={handleSave} className="profile-form">
            <label>
              Email
              <input
                className="account-input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>

            <label>
              Số điện thoại
              <input
                className="account-input"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </label>

            <button className="account-btn account-btn-primary" type="submit" disabled={saving}>
              {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>

            {profileStatus && <div className="account-alert">{profileStatus}</div>}
          </form>
        </div>
      </section>
    )
  }

  function renderOrdersTab() {
    return (
      <section className="account-panel">
        <header className="panel-header">
          <h2>Đơn hàng</h2>
          <p>Theo dõi trạng thái và xem chi tiết từng đơn đã đặt.</p>
        </header>

        {ordersLoading && <p className="account-muted">Đang tải đơn hàng...</p>}
        {ordersStatus && <div className="account-alert">{ordersStatus}</div>}
        {!ordersLoading && orders.length === 0 && (
          <p className="account-muted">Bạn chưa có đơn hàng nào.</p>
        )}

        <div className="order-list">
          {orders.map((order) => {
            const detail = orderDetailById[order.id]
            const expanded = expandedOrderId === order.id
            return (
              <article className="order-item" key={order.id}>
                <div className="order-summary-row">
                  <div>
                    <p className="order-id">Mã đơn: #{order.id}</p>
                    <p className="account-muted">Ngày đặt: {toDate(order.orderDate)}</p>
                  </div>
                  <div className="order-right">
                    <span className={`order-status status-${normalizeStatus(order.status)}`}>
                      {normalizeStatus(order.status)}
                    </span>
                    <p className="order-price">{toCurrency(order.totalPrice)}</p>
                    <button
                      className="account-btn"
                      onClick={() => toggleOrderDetail(order.id)}
                      type="button"
                    >
                      {expanded ? 'Ẩn chi tiết' : 'Xem chi tiết'}
                    </button>
                  </div>
                </div>

                {expanded && (
                  <div className="order-detail">
                    {!detail && <p className="account-muted">Đang tải chi tiết...</p>}
                    {detail && (
                      <>
                        <p><strong>Địa chỉ:</strong> {detail.address || '-'}</p>
                        <p><strong>Số điện thoại:</strong> {detail.phone || '-'}</p>
                        <div className="order-detail-table-wrap">
                          <table className="order-detail-table">
                            <thead>
                              <tr>
                                <th>Ảnh</th>
                                <th>Sản phẩm</th>
                                <th>Size</th>
                                <th>SL</th>
                                <th>Đơn giá</th>
                                <th>Thành tiền</th>
                              </tr>
                            </thead>
                            <tbody>
                              {(detail.items || []).map((item) => (
                                <tr key={item.orderDetailId}>
                                  <td>
                                    <img
                                      className="order-product-thumb"
                                      src={toMediaUrl(item.productImage || '/uploads/images/placeholder.png')}
                                      alt={item.productName || 'product'}
                                    />
                                  </td>
                                  <td>{item.productName}</td>
                                  <td>{item.sizeName || '-'}</td>
                                  <td>{item.quantity}</td>
                                  <td>{toCurrency(item.unitPrice)}</td>
                                  <td>{toCurrency(item.lineTotal)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </>
                    )}
                  </div>
                )}
              </article>
            )
          })}
        </div>
      </section>
    )
  }

  function renderReviewSection(product) {
    if (!product.review) {
      return (
        <button
          className="account-btn account-btn-primary"
          type="button"
          onClick={() => openCreateReview(product)}
        >
          Đánh giá
        </button>
      )
    }

    const review = product.review
    return (
      <div className="review-box">
        <div className="review-header">
          <span className="review-badge">Đã đánh giá</span>
          <span className="review-stars">{'★'.repeat(review.rating || 0)}{'☆'.repeat(5 - (review.rating || 0))}</span>
        </div>

        <p className="review-comment">{review.comment || 'Không có nội dung'}</p>

        <div className="review-media-grid">
          {(review.media || []).map((media) => (
            media.mediaType === 'video' ? (
              <video key={media.id} controls src={toMediaUrl(media.mediaUrl)} />
            ) : (
              <img key={media.id} src={toMediaUrl(media.mediaUrl)} alt="review" />
            )
          ))}
        </div>

        <div className="review-action-row">
          <button className="account-btn" type="button" onClick={() => openEditReview(product)}>
            Sửa đánh giá
          </button>
          <button className="account-btn account-btn-danger" type="button" onClick={() => handleDeleteReview(review.id)}>
            Xóa đánh giá
          </button>
        </div>
      </div>
    )
  }

  function renderPurchasedProductsTab() {
    return (
      <section className="account-panel">
        <header className="panel-header">
          <h2>Sản phẩm đã mua</h2>
          <p>Chỉ hiển thị sản phẩm thuộc các đơn hàng đã hoàn tất (completed).</p>
        </header>

        {productsLoading && <p className="account-muted">Đang tải sản phẩm đã mua...</p>}
        {productsStatus && <div className="account-alert">{productsStatus}</div>}
        {!productsLoading && purchasedProducts.length === 0 && (
          <p className="account-muted">Bạn chưa có sản phẩm nào đủ điều kiện đánh giá.</p>
        )}

        <div className="purchased-grid">
          {purchasedProducts.map((product) => (
            <article key={product.productId} className="purchased-item">
              <img
                className="purchased-image"
                src={toMediaUrl(product.imageUrl || '/uploads/images/placeholder.png')}
                alt={product.productName}
              />
              <div className="purchased-content">
                <h3>{product.productName}</h3>
                <p className="purchased-price">{toCurrency(product.price)}</p>
                <p className="account-muted">Mua gần nhất: {toDate(product.lastPurchasedAt)}</p>

                {renderReviewSection(product)}
              </div>
            </article>
          ))}
        </div>
      </section>
    )
  }

  function renderCurrentTab() {
    if (activeTab === 'orders') return renderOrdersTab()
    if (activeTab === 'purchased') return renderPurchasedProductsTab()
    return renderProfileTab()
  }

  function renderReviewEditor() {
    if (!reviewEditorOpen || !editingProduct) {
      return null
    }

    return (
      <div className="review-modal-backdrop" role="presentation" onClick={closeReviewEditor}>
        <div className="review-modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
          <h3>{editingReview ? 'Sửa đánh giá' : 'Đánh giá sản phẩm'}</h3>
          <p className="account-muted">{editingProduct.productName}</p>

          <form onSubmit={handleSubmitReview} className="review-form">
            <div className="star-row">
              {[1, 2, 3, 4, 5].map((value) => (
                <button
                  key={value}
                  type="button"
                  className={(hoveredRating || rating) >= value ? 'star active' : 'star'}
                  onMouseEnter={() => setHoveredRating(value)}
                  onMouseLeave={() => setHoveredRating(0)}
                  onClick={() => setRating(value)}
                >
                  ★
                </button>
              ))}
              <span>{rating || 0}/5</span>
            </div>

            <label>
              Nội dung đánh giá
              <textarea
                rows="4"
                value={comment}
                onChange={(event) => setComment(event.target.value)}
                placeholder="Nhập cảm nhận của bạn về sản phẩm..."
              />
            </label>

            <div>
              <p className="account-muted">Ảnh ({imageFiles.length}/5)</p>
              <div className="new-media-grid">
                {imagePreviewUrls.map((url, index) => (
                  <div key={url} className="new-media-item">
                    <img src={url} alt={`preview-${index}`} />
                    <button type="button" className="account-btn" onClick={() => removeImage(index)}>
                      Xóa
                    </button>
                  </div>
                ))}
              </div>

              {imageFiles.length < 5 && (
                <label className="account-btn account-btn-ghost file-picker" htmlFor="review-images">
                  Chọn ảnh
                  <input
                    id="review-images"
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={handleImageChange}
                  />
                </label>
              )}
            </div>

            <div>
              <p className="account-muted">Video (tối đa 1)</p>
              {videoFile ? (
                <div className="video-file-row">
                  <span>{videoFile.name}</span>
                  <button type="button" className="account-btn" onClick={() => setVideoFile(null)}>
                    Xóa
                  </button>
                </div>
              ) : (
                <label className="account-btn account-btn-ghost file-picker" htmlFor="review-video">
                  Chọn video
                  <input
                    id="review-video"
                    type="file"
                    accept="video/*"
                    onChange={handleVideoChange}
                  />
                </label>
              )}
            </div>

            {editingReview && (
              <label className="check-row">
                <input
                  type="checkbox"
                  checked={replaceMedia}
                  onChange={(event) => setReplaceMedia(event.target.checked)}
                />
                Thay thế toàn bộ ảnh/video cũ bằng file mới
              </label>
            )}

            {reviewStatus && <div className="account-alert">{reviewStatus}</div>}

            <div className="modal-action-row">
              <button className="account-btn" type="button" onClick={closeReviewEditor}>Hủy</button>
              <button className="account-btn account-btn-primary" type="submit" disabled={reviewSaving}>
                {reviewSaving ? 'Đang lưu...' : 'Lưu đánh giá'}
              </button>
            </div>
          </form>
        </div>
      </div>
    )
  }

  return (
    <main className="account-page-wrap">
      {!authChecked && <p className="account-muted">Đang kiểm tra đăng nhập...</p>}

      {canShowForm && (
        <div className="account-layout">
          <aside className="account-sidebar">
            <button
              type="button"
              className={activeTab === 'profile' ? 'sidebar-item active' : 'sidebar-item'}
              onClick={() => setActiveTab('profile')}
            >
              Thông tin cá nhân
            </button>
            <button
              type="button"
              className={activeTab === 'orders' ? 'sidebar-item active' : 'sidebar-item'}
              onClick={() => setActiveTab('orders')}
            >
              Đơn hàng
            </button>
            <button
              type="button"
              className={activeTab === 'purchased' ? 'sidebar-item active' : 'sidebar-item'}
              onClick={() => setActiveTab('purchased')}
            >
              Sản phẩm đã mua
            </button>
          </aside>

          <section className="account-content">
            {renderCurrentTab()}
          </section>
        </div>
      )}

      {renderReviewEditor()}
    </main>
  )
}

export default AccountPage
