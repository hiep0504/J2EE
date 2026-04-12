import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getMyPurchasedProducts } from '../services/accountPortalService';
import { addToCart } from '../services/cartService';
import { getAllProducts } from '../services/productService';
import { toMediaUrl } from '../utils/mediaUrl';
import './ProductDetailPage.css';

function toCurrency(value) {
  return Number(value || 0).toLocaleString('vi-VN') + 'đ';
}

function renderStars(value) {
  const score = Math.max(0, Math.min(5, Number(value || 0)));
  return '★'.repeat(Math.round(score)) + '☆'.repeat(5 - Math.round(score));
}

function renderDetailSkeleton() {
  return (
    <main className="product-detail container py-4 py-lg-5">
      <section className="product-detail__card">
        <div className="skeleton skeleton-product-image product-detail__skeleton-media" />

        <div className="skeleton-grid">
          <div className="skeleton skeleton-line skeleton-line--sm" style={{ width: '34%' }} />
          <div className="skeleton skeleton-line skeleton-line--lg" style={{ width: '72%' }} />
          <div className="skeleton skeleton-line" style={{ width: '28%' }} />
          <div className="skeleton skeleton-line" style={{ width: '96%' }} />
          <div className="skeleton skeleton-line" style={{ width: '84%' }} />
          <div className="skeleton skeleton-line" style={{ width: '66%', height: '44px', borderRadius: '12px' }} />
          <div className="skeleton" style={{ width: '100%', height: '48px', borderRadius: '12px' }} />
        </div>
      </section>

      <section className="product-detail__reviews mt-4 mt-lg-5">
        <div className="product-detail__reviews-header">
          <div className="skeleton skeleton-line skeleton-line--lg" style={{ width: '180px', marginBottom: '10px' }} />
          <div className="skeleton skeleton-line" style={{ width: '260px' }} />
        </div>

        <div className="skeleton-grid">
          {Array.from({ length: 3 }, (_, index) => (
            <div key={index} className="skeleton-product-card" style={{ padding: '14px' }}>
              <div className="skeleton skeleton-line" style={{ width: '42%', marginBottom: '10px' }} />
              <div className="skeleton skeleton-line" style={{ width: '84%', marginBottom: '8px' }} />
              <div className="skeleton skeleton-line" style={{ width: '70%', marginBottom: '14px' }} />
              <div className="row g-2">
                {Array.from({ length: 3 }, (_, thumbIndex) => (
                  <div className="col-4" key={thumbIndex}>
                    <div className="skeleton" style={{ width: '100%', aspectRatio: '1', borderRadius: '10px' }} />
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}

function ProductDetailPage({ user, authChecked }) {
  const { productId } = useParams();
  const [products, setProducts] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [purchasedProducts, setPurchasedProducts] = useState([]);
  const [purchaseLoading, setPurchaseLoading] = useState(false);
  const [reviewLoading, setReviewLoading] = useState(false);
  const [reviewError, setReviewError] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [adding, setAdding] = useState(false);
  const [cartMessage, setCartMessage] = useState('');
  const [sizes, setSizes] = useState([]);
  const [loadingSizes, setLoadingSizes] = useState(false);
  const [selectedSize, setSelectedSize] = useState('');

  useEffect(() => {
    async function loadProducts() {
      setLoading(true);
      setError('');
      try {
        const res = await getAllProducts();
        setProducts(Array.isArray(res.data) ? res.data : []);
      } catch {
        setError('Không thể tải chi tiết sản phẩm. Vui lòng thử lại.');
      } finally {
        setLoading(false);
      }
    }

    loadProducts();
  }, []);

  const product = useMemo(
    () => products.find((item) => String(item.id) === String(productId)),
    [products, productId]
  );

  const purchaseRecord = useMemo(
    () => purchasedProducts.find((item) => String(item.productId) === String(productId)),
    [purchasedProducts, productId]
  );

  const canWriteReview = !!user && !!purchaseRecord?.canReview;
  const hasReviewed = !!purchaseRecord?.review;

  useEffect(() => {
    async function loadSizes() {
      if (!product) return;
      setLoadingSizes(true);
      setSizes([]);
      setSelectedSize('');
      try {
        const res = await fetch(`https://busticket.ink/api/products/${product.id}/sizes`);
        if (res.ok) {
          const data = await res.json();
          setSizes(Array.isArray(data) ? data : []);
          if (data.length > 0) {
            setSelectedSize(data[0].sizeId);
          }
        }
      } catch (err) {
        console.error('Error loading sizes:', err);
      } finally {
        setLoadingSizes(false);
      }
    }

    loadSizes();
  }, [product]);

  const averageRating = useMemo(() => {
    if (reviews.length === 0) return 0;
    const total = reviews.reduce((sum, item) => sum + Number(item.rating || 0), 0);
    return total / reviews.length;
  }, [reviews]);

  useEffect(() => {
    async function loadReviews() {
      if (!productId) return;
      setReviewLoading(true);
      setReviewError('');
      try {
        const response = await fetch(`https://busticket.ink/api/reviews?productId=${productId}`);
        if (!response.ok) {
          throw new Error('Không thể tải danh sách đánh giá.');
        }
        const data = await response.json();
        setReviews(Array.isArray(data) ? data : []);
      } catch (err) {
        setReviewError(err.message || 'Không thể tải danh sách đánh giá.');
      } finally {
        setReviewLoading(false);
      }
    }

    loadReviews();
  }, [productId]);

  useEffect(() => {
    async function loadPurchasedProducts() {
      if (!authChecked || !user) {
        setPurchasedProducts([]);
        return;
      }

      setPurchaseLoading(true);
      try {
        const res = await getMyPurchasedProducts();
        setPurchasedProducts(Array.isArray(res.data) ? res.data : []);
      } catch {
        setPurchasedProducts([]);
      } finally {
        setPurchaseLoading(false);
      }
    }

    loadPurchasedProducts();
  }, [authChecked, user]);

  async function handleAddToCart() {
    if (!product) return;
    if (!selectedSize) {
      setCartMessage('✗ Vui lòng chọn size');
      return;
    }
    setAdding(true);
    setCartMessage('');
    try {
      await addToCart(product.id, parseInt(selectedSize), 1);
      setCartMessage('✓ Đã thêm vào giỏ hàng');
      setTimeout(() => setCartMessage(''), 3000);
    } catch (err) {
      const errorMsg = err?.response?.data?.message || err?.message || 'Thất bại. Vui lòng thử lại.';
      setCartMessage('✗ ' + errorMsg);
      console.error('Add to cart failed', err);
    } finally {
      setAdding(false);
    }
  }

  if (loading) {
    return renderDetailSkeleton();
  }

  if (error && !product) {
    return (
      <main className="product-detail container py-5">
        <div className="product-detail__empty">
          <p className="text-danger mb-3">{error}</p>
          <Link to="/" className="btn btn-dark">Về trang chủ</Link>
        </div>
      </main>
    );
  }

  if (!product) {
    return (
      <main className="product-detail container py-5">
        <div className="product-detail__empty">
          <p className="mb-3">Không tìm thấy sản phẩm.</p>
          <Link to="/" className="btn btn-dark">Về trang chủ</Link>
        </div>
      </main>
    );
  }

  return (
    <main className="product-detail container py-4 py-lg-5">
      <section className="product-detail__card">
        <div className="product-detail__media">
          <img
            src={toMediaUrl(product.image || 'https://placehold.co/700x700?text=No+Image')}
            alt={product.name}
          />
        </div>

        <div className="product-detail__content">
          <p className="product-detail__category mb-2">{product.categoryName || 'Sản phẩm'}</p>
          <h1 className="product-detail__title mb-2">{product.name}</h1>
          <p className="product-detail__price mb-3">{toCurrency(product.price)}</p>

          <p className="product-detail__desc mb-4">
            {product.description || 'Chưa có mô tả chi tiết cho sản phẩm này.'}
          </p>

          <div className="mb-3">
            <label className="form-label">Chọn size:</label>
            {loadingSizes ? (
              <p className="text-muted small">Đang tải size...</p>
            ) : sizes.length === 0 ? (
              <p className="text-danger small">Không có size</p>
            ) : (
              <select
                className="form-select"
                value={selectedSize}
                onChange={(e) => setSelectedSize(e.target.value)}
              >
                {sizes.map((s) => (
                  <option key={s.sizeId} value={s.sizeId}>
                    {s.sizeName}
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="product-detail__actions">
            <button
              type="button"
              className="btn btn-dark"
              disabled={adding}
              onClick={handleAddToCart}
            >
              {adding ? 'Đang thêm...' : 'Thêm vào giỏ'}
            </button>
            <Link to="/" className="btn btn-outline-dark">Tiếp tục mua sắm</Link>
            {authChecked && user && purchaseLoading && (
              <span className="btn btn-outline-primary disabled" aria-disabled="true">
                Đang kiểm tra mua hàng...
              </span>
            )}
            {authChecked && user && !purchaseLoading && canWriteReview && (
              <Link
                to={`/product/review?productId=${product.id}`}
                className="btn btn-outline-primary"
              >
                {hasReviewed ? 'Viết đánh giá lại' : 'Viết đánh giá'}
              </Link>
            )}
            {authChecked && user && !purchaseLoading && !canWriteReview && hasReviewed && (
              <span className="btn btn-outline-success disabled" aria-disabled="true">
                Đã đánh giá
              </span>
            )}
          </div>

          {cartMessage && (
            <p className={`mt-3 mb-0 ${cartMessage.startsWith('✓') ? 'text-success' : 'text-danger'}`}>
              {cartMessage}
            </p>
          )}
          {error && <p className="text-danger mt-3 mb-0">{error}</p>}
        </div>
      </section>

      <section className="product-detail__reviews mt-4 mt-lg-5">
        <div className="product-detail__reviews-header">
          <h2 className="mb-1">Đánh giá sản phẩm</h2>
          <p className="mb-0 text-muted">
            {reviews.length} đánh giá • {averageRating.toFixed(1)}/5 • {renderStars(averageRating)}
          </p>
        </div>

        {reviewLoading && (
          <div className="skeleton-grid">
            {Array.from({ length: 3 }, (_, index) => (
              <div key={index} className="product-detail__review-item">
                <div className="skeleton skeleton-line" style={{ width: '38%', marginBottom: '10px' }} />
                <div className="skeleton skeleton-line" style={{ width: '88%', marginBottom: '8px' }} />
                <div className="skeleton skeleton-line" style={{ width: '62%', marginBottom: '14px' }} />
                <div className="row g-2">
                  {Array.from({ length: 2 }, (_, thumbIndex) => (
                    <div className="col-6" key={thumbIndex}>
                      <div className="skeleton" style={{ width: '100%', aspectRatio: '1', borderRadius: '10px' }} />
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
        {!reviewLoading && reviewError && <p className="text-danger mb-0">{reviewError}</p>}

        {!reviewLoading && !reviewError && reviews.length === 0 && (
          <p className="text-muted mb-0">Sản phẩm này chưa có đánh giá nào.</p>
        )}

        {!reviewLoading && !reviewError && reviews.length > 0 && (
          <div className="product-detail__review-list">
            {reviews.map((review) => (
              <article className="product-detail__review-item" key={review.id}>
                <div className="product-detail__review-top">
                  <strong>{review.accountUsername || `User #${review.accountId}`}</strong>
                  <span>{renderStars(review.rating)}</span>
                </div>
                <p className="mb-2">{review.comment || 'Không có nội dung.'}</p>

                {(review.media || []).length > 0 && (
                  <div className="product-detail__media-grid">
                    {(review.media || []).map((media) => (
                      media.mediaType === 'video'
                        ? (
                          <video key={media.id} src={toMediaUrl(media.mediaUrl)} controls />
                        )
                        : (
                          <img key={media.id} src={toMediaUrl(media.mediaUrl)} alt="review" />
                        )
                    ))}
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default ProductDetailPage;
