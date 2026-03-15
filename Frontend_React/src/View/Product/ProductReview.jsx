import { useEffect, useMemo, useState } from 'react'

const API_BASE = 'http://localhost:8080/api'
const SELECTED_PRODUCT_STORAGE_KEY = 'product-review-selected-product-id'

function toCurrency(value) {
  if (value === null || value === undefined) return '0 đ'
  return Number(value).toLocaleString('vi-VN') + ' đ'
}

function renderStars(count) {
  return '★'.repeat(count) + '☆'.repeat(5 - count)
}

function ProductReview() {
  const [products, setProducts] = useState([])
  const [selectedProductId, setSelectedProductId] = useState('')
  const [reviews, setReviews] = useState([])
  const [accountId, setAccountId] = useState('2')
  const [rating, setRating] = useState(0)
  const [hoveredRating, setHoveredRating] = useState(0)
  const [comment, setComment] = useState('')
  const [imageFiles, setImageFiles] = useState([])
  const [videoFile, setVideoFile] = useState(null)
  const [status, setStatus] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const selectedProduct = useMemo(
    () => products.find((item) => String(item.id) === selectedProductId),
    [products, selectedProductId]
  )

  const filteredReviews = useMemo(
    () => reviews.filter((review) => String(review.productId) === selectedProductId),
    [reviews, selectedProductId]
  )

  const imageCount = imageFiles.length

  useEffect(() => {
    loadProducts()
  }, [])

  useEffect(() => {
    if (!selectedProductId) return
    localStorage.setItem(SELECTED_PRODUCT_STORAGE_KEY, selectedProductId)
  }, [selectedProductId])

  useEffect(() => {
    if (!selectedProductId) return
    loadReviews(selectedProductId)
  }, [selectedProductId])

  async function loadProducts() {
    setStatus('Đang tải danh sách sản phẩm...')
    try {
      const response = await fetch(`${API_BASE}/products`)
      if (!response.ok) throw new Error('Không thể tải sản phẩm')
      const data = await response.json()

      const fullProducts = Array.isArray(data) ? data : []
      setProducts(fullProducts)

      const rememberedProductId = localStorage.getItem(SELECTED_PRODUCT_STORAGE_KEY)
      const hasRememberedProduct = fullProducts.some(
        (item) => String(item.id) === rememberedProductId
      )

      if (hasRememberedProduct) {
        setSelectedProductId(rememberedProductId)
      } else if (fullProducts.length > 0) {
        setSelectedProductId(String(fullProducts[0].id))
      } else {
        setSelectedProductId('')
      }

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
    if (!selectedProductId) { setStatus('Vui lòng chọn sản phẩm'); return }
    if (!accountId.trim()) { setStatus('Vui lòng nhập accountId'); return }
    if (rating < 1 || rating > 5) { setStatus('Vui lòng chọn số sao từ 1 đến 5'); return }

    setIsSubmitting(true)
    setStatus('Đang gửi đánh giá...')

    const formData = new FormData()
    formData.append('productId', selectedProductId)
    formData.append('accountId', accountId)
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
      setStatus('Tạo review thành công!')
      await loadReviews(selectedProductId)
    } catch (error) {
      setStatus(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="page">
      <section className="card">
        <h1>Trang đánh giá sản phẩm</h1>
        <form onSubmit={submitReview} className="form-grid">
          <label>
            Sản phẩm
            <select
              value={selectedProductId}
              onChange={(event) => setSelectedProductId(event.target.value)}
            >
              {products.map((product) => (
                <option key={product.id} value={String(product.id)}>
                  {product.name} - {toCurrency(product.price)}
                </option>
              ))}
            </select>
          </label>

          <label>
            Account ID
            <input
              type="number"
              value={accountId}
              onChange={(event) => setAccountId(event.target.value)}
              min="1"
            />
          </label>

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
            {isSubmitting ? 'Đang gửi...' : 'Gửi đánh giá'}
          </button>
        </form>
        {status && <p className="status">{status}</p>}
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
                    <video key={media.id} src={`http://localhost:8080${media.mediaUrl}`} controls />
                  ) : (
                    <img key={media.id} src={`http://localhost:8080${media.mediaUrl}`} alt="review" />
                  )
                )}
              </div>
            </article>
          ))}
        </div>
      </section>
    </main>
  )
}

export default ProductReview