import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { getMyPurchasedProducts } from '../../services/accountPortalService'
import { toMediaUrl } from '../../utils/mediaUrl'

const API_BASE = 'http://localhost:8080/api'

function toCurrency(value) {
  if (value === null || value === undefined) return '0 đ'
  return Number(value).toLocaleString('vi-VN') + ' đ'
}

function renderStars(count) {
  return '★'.repeat(count) + '☆'.repeat(5 - count)
}

function renderReviewSkeleton() {
  return (
    <main className="page">
      <section className="card skeleton-card">
        <div className="skeleton skeleton-line skeleton-line--lg" style={{ width: '240px', marginBottom: '12px' }} />
        <div className="skeleton skeleton-line" style={{ width: '180px', marginBottom: '18px' }} />
        <div className="skeleton" style={{ width: '100%', height: '44px', borderRadius: '12px', marginBottom: '14px' }} />
        <div className="skeleton" style={{ width: '100%', height: '120px', borderRadius: '16px', marginBottom: '12px' }} />
        <div className="row g-2">
          <div className="col-6">
            <div className="skeleton" style={{ width: '100%', height: '44px', borderRadius: '12px' }} />
          </div>
          <div className="col-6">
            <div className="skeleton" style={{ width: '100%', height: '44px', borderRadius: '12px' }} />
          </div>
        </div>
      </section>
    </main>
  )
}

function ProductReview({ user, authChecked }) {
  const navigate = useNavigate()
  const { productId } = useParams()
  const [searchParams] = useSearchParams()
  const [products, setProducts] = useState([])
  const [reviews, setReviews] = useState([])
  const [rating, setRating] = useState(0)
  const [hoveredRating, setHoveredRating] = useState(0)
  const [comment, setComment] = useState('')
  const [imageFiles, setImageFiles] = useState([])
  const [videoFile, setVideoFile] = useState(null)
  const [status, setStatus] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [purchasedProducts, setPurchasedProducts] = useState([])
  const [purchaseLoading, setPurchaseLoading] = useState(false)

  const routeProductId = String(productId || searchParams.get('productId') || '')

  const selectedProduct = useMemo(
    () => products.find((item) => String(item.id) === routeProductId),
    [products, routeProductId]
  )

  const filteredReviews = useMemo(
    () => reviews.filter((review) => String(review.productId) === routeProductId),
    [reviews, routeProductId]
  )

  const imageCount = imageFiles.length
  const accountId = user?.id

  const purchaseRecord = useMemo(
    () => purchasedProducts.find((item) => String(item.productId) === routeProductId),
    [purchasedProducts, routeProductId]
  )
  const canReview = !!purchaseRecord?.canReview
  const alreadyReviewed = !!purchaseRecord?.review

  useEffect(() => {
    loadProducts()
  }, [routeProductId])

  useEffect(() => {
    if (!routeProductId) return
    loadReviews(routeProductId)
  }, [routeProductId])

  useEffect(() => {
    async function loadPurchasedProducts() {
      if (!authChecked || !user) {
        setPurchasedProducts([])
        return
      }

      setPurchaseLoading(true)
      try {
        const res = await getMyPurchasedProducts()
        setPurchasedProducts(Array.isArray(res.data) ? res.data : [])
      } catch {
        setPurchasedProducts([])
      } finally {
        setPurchaseLoading(false)
      }
    }

    loadPurchasedProducts()
  }, [authChecked, user])

  async function loadProducts() {
    setStatus('Đang tải danh sách sản phẩm...')
    try {
      const response = await fetch(`${API_BASE}/products`)
      if (!response.ok) throw new Error('Không thể tải sản phẩm')
      const data = await response.json()

      const fullProducts = Array.isArray(data) ? data : []
      setProducts(fullProducts)

      setStatus('')
    } catch (error) {
      setStatus(error.message)
    }
  }

  async function loadReviews(productId) {
    try {
      const response = await fetch(`${API_BASE}/reviews?productId=${productId}`)
      if (!response.ok) throw new Error('Không thể tải đánh giá')
      const data = await response.json()
      setReviews(Array.isArray(data) ? data : [])
    } catch (error) {
      setStatus(error.message)
    }
  }

  function handleImageChange(event) {
    const selected = Array.from(event.target.files)
    const merged = [...imageFiles, ...selected].slice(0, 5)
    setImageFiles(merged)
    event.target.value = ''
  }

  function removeImage(index) {
    setImageFiles(imageFiles.filter((_, idx) => idx !== index))
  }

  function handleVideoChange(event) {
    const file = event.target.files[0] || null
    setVideoFile(file)
  }

  async function submitReview(event) {
    event.preventDefault()
    if (!routeProductId) { setStatus('Không xác định được sản phẩm cần đánh giá'); return }
    if (!accountId) { setStatus('Vui lòng đăng nhập để gửi đánh giá'); return }
    if (!purchaseRecord) { setStatus('Bạn chỉ có thể đánh giá sản phẩm đã mua và đơn hàng đã hoàn tất'); return }
    if (!canReview) { setStatus('Bạn đã đánh giá cho lần mua hiện tại rồi. Hãy mua lại để đánh giá tiếp.'); return }
    if (rating < 1 || rating > 5) { setStatus('Vui lòng chọn số sao từ 1 đến 5'); return }

    setIsSubmitting(true)
    setStatus('Đang gửi đánh giá...')

    const formData = new FormData()
    formData.append('productId', routeProductId)
    formData.append('accountId', String(accountId))
    formData.append('rating', String(rating))
    formData.append('comment', comment)
    imageFiles.forEach((file) => formData.append('images', file))
    if (videoFile) formData.append('video', videoFile)

    try {
      const response = await fetch(`${API_BASE}/reviews`, {
        method: 'POST',
        body: formData,
      })
      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || 'Không thể tạo review')
      }
      setComment('')
      setRating(0)
      setVideoFile(null)
      setImageFiles([])
      setStatus(alreadyReviewed ? 'Viết đánh giá mới thành công!' : 'Tạo review thành công!')
      await loadReviews(routeProductId)
    } catch (error) {
      setStatus(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    !authChecked ? (
      renderReviewSkeleton()
    ) : (
    <main className="page">
      <section className="card">
        {authChecked && !user ? (
          <>
            <h1>Trang đánh giá sản phẩm</h1>
            <p className="status">Vui lòng đăng nhập để gửi đánh giá.</p>
            <button type="button" className="primary-button" onClick={() => navigate('/login')}>
              Đi đến đăng nhập
            </button>
          </>
        ) : (
          <>
        <h1>Trang đánh giá sản phẩm</h1>
        <p className="status" style={{ marginTop: 0 }}>
          {user && <span>Đang đánh giá bằng tài khoản: <strong>{user.username}</strong></span>}
        </p>

        {selectedProduct ? (
          <>
            <p className="media-title" style={{ marginTop: 0 }}>Sản phẩm đang đánh giá</p>
            <div className="media-row" style={{ justifyContent: 'space-between' }}>
              <strong>{selectedProduct.name}</strong>
              <span>{toCurrency(selectedProduct.price)}</span>
            </div>

            {purchaseLoading ? (
              <p className="status">Đang kiểm tra trạng thái mua hàng...</p>
            ) : !purchaseRecord ? (
              <p className="status">Bạn chỉ có thể đánh giá sản phẩm đã mua và đơn hàng đã hoàn tất.</p>
            ) : canReview ? (
              <form onSubmit={submitReview} className="form-grid">
                {alreadyReviewed && (
                  <p className="status" style={{ marginTop: 0 }}>
                    Bạn đã mua lại sản phẩm này, có thể viết đánh giá mới.
                  </p>
                )}

                <fieldset className="stars-field">
                  <legend>Đánh giá sao (1-5)</legend>
                  <div className="stars">
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
                    <span className="star-value">{rating || 0}/5</span>
                  </div>
                </fieldset>

                <label>
                  Nội dung review
                  <textarea
                    rows="4"
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                    placeholder="Nhập cảm nhận của bạn về sản phẩm..."
                  />
                </label>

                <div>
                  <p className="media-title">Ảnh review ({imageCount}/5)</p>
                  <div className="image-preview-list">
                    {imageFiles.map((file, index) => (
                      <div className="image-preview-item" key={index}>
                        <img src={URL.createObjectURL(file)} alt={file.name} />
                        <button type="button" className="subtle-button" onClick={() => removeImage(index)}>
                          Xóa
                        </button>
                      </div>
                    ))}
                  </div>
                  {imageFiles.length < 5 && (
                    <label className="outline-button file-label">
                      Chọn ảnh
                      <input
                        type="file"
                        accept="image/*"
                        multiple
                        style={{ display: 'none' }}
                        onChange={handleImageChange}
                      />
                    </label>
                  )}
                </div>

                <div>
                  <p className="media-title">Video review (tối đa 1)</p>
                  {videoFile ? (
                    <div className="media-row">
                      <span>{videoFile.name}</span>
                      <button type="button" className="subtle-button" onClick={() => setVideoFile(null)}>
                        Xóa
                      </button>
                    </div>
                  ) : (
                    <label className="outline-button file-label">
                      Chọn video
                      <input
                        type="file"
                        accept="video/*"
                        style={{ display: 'none' }}
                        onChange={handleVideoChange}
                      />
                    </label>
                  )}
                </div>

                <button className="primary-button" type="submit" disabled={isSubmitting}>
                  {isSubmitting ? 'Đang gửi...' : alreadyReviewed ? 'Gửi đánh giá mới' : 'Gửi đánh giá'}
                </button>
              </form>
            ) : (
              <p className="status">Bạn đã đánh giá cho lần mua hiện tại rồi. Hãy mua lại để đánh giá tiếp.</p>
            )}
          </>
        ) : (
          <p className="status">Không xác định được sản phẩm cần đánh giá.</p>
        )}
        {status && <p className="status">{status}</p>}
          </>
        )}
      </section>

      <section className="card">
        <h2>
          Danh sách đánh giá {selectedProduct ? `- ${selectedProduct.name}` : ''}
        </h2>
        <div className="review-list">
          {filteredReviews.length === 0 && <p>Chưa có đánh giá nào cho sản phẩm này.</p>}
          {filteredReviews.map((review) => (
            <article key={review.id} className="review-item">
              <div className="review-top">
                <strong>{review.accountUsername || `User #${review.accountId}`}</strong>
                <span className="stars-text">{renderStars(review.rating || 0)}</span>
              </div>
              <p>{review.comment || 'Không có nội dung'}</p>
              <div className="media-grid">
                {(review.media || []).map((media) =>
                  media.mediaType === 'video' ? (
                    <video key={media.id} src={toMediaUrl(media.mediaUrl)} controls />
                  ) : (
                    <img key={media.id} src={toMediaUrl(media.mediaUrl)} alt="review" />
                  )
                )}
              </div>
            </article>
          ))}
        </div>
      </section>
    </main>
    )
  )
}

export default ProductReview