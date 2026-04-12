import { useState } from 'react';
import { Link } from 'react-router-dom';
import './ProductCard.css';
import { addToCart } from '../services/cartService';
import { toMediaUrl } from '../utils/mediaUrl';

function ProductCard({ product }) {
  const [adding, setAdding] = useState(false);
  const [message, setMessage] = useState('');
  const [showSizes, setShowSizes] = useState(false);
  const [sizes, setSizes] = useState([]);
  const [selectedSize, setSelectedSize] = useState('');
  const [loadingSizes, setLoadingSizes] = useState(false);

  async function loadSizes() {
    if (sizes.length > 0) return;
    setLoadingSizes(true);
    try {
      const res = await fetch(`https://busticket.ink/api/products/${product.id}/sizes`);
      if (res.ok) {
        const data = await res.json();
        setSizes(Array.isArray(data) ? data : []);
        if (data.length > 0) {
          setSelectedSize(data[0].sizeId);
        }
      }
    } catch {
      setMessage('✗ Không thể tải size');
    } finally {
      setLoadingSizes(false);
    }
  }

  async function handleAddToCart(e) {
    e.preventDefault();
    e.stopPropagation();

    if (!selectedSize) {
      setMessage('✗ Vui lòng chọn size');
      return;
    }

    setAdding(true);
    setMessage('');
    try {
      await addToCart(product.id, parseInt(selectedSize), 1);
      setMessage('✓ Đã thêm vào giỏ');
      setTimeout(() => {
        setMessage('');
        setShowSizes(false);
      }, 2000);
    } catch (err) {
      const errorMsg = err?.response?.data?.message || err?.message || 'Thất bại. Vui lòng thử lại.';
      setMessage('✗ ' + errorMsg);
      console.error('Add to cart failed', err);
    } finally {
      setAdding(false);
    }
  }

  const handleShowSizes = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setShowSizes(true);
    loadSizes();
  };

  return (
    <div className="card h-100 product-card">
      <Link to={`/products/${product.id}`} className="product-card__link text-decoration-none text-dark">
        <img
          src={toMediaUrl(product.image || 'https://placehold.co/300x300?text=No+Image')}
          alt={product.name}
          className="card-img-top product-card__image"
        />
        <div className="card-body">
          <p className="text-muted small mb-1">{product.categoryName}</p>
          <h6 className="card-title mb-2">{product.name}</h6>
          <p className="text-danger fw-bold mb-0">
            {Number(product.price).toLocaleString('vi-VN')}đ
          </p>
        </div>
      </Link>

      <div className="card-body pt-0">
        {!showSizes ? (
          <button
            type="button"
            className="btn btn-sm btn-outline-dark mt-3 w-100"
            onClick={handleShowSizes}
            disabled={adding}
          >
            {adding ? 'Đang thêm...' : 'Thêm vào giỏ'}
          </button>
        ) : (
          <div className="mt-3">
            <div className="mb-2">
              <label className="form-label small">Chọn size:</label>
              {loadingSizes ? (
                <p className="small text-muted">Đang tải size...</p>
              ) : sizes.length === 0 ? (
                <p className="small text-danger">Không có size</p>
              ) : (
                <select
                  className="form-select form-select-sm"
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
            <div className="d-grid gap-1">
              <button
                type="button"
                className="btn btn-sm btn-dark"
                onClick={handleAddToCart}
                disabled={adding || sizes.length === 0}
              >
                {adding ? 'Đang thêm...' : 'Xác nhận'}
              </button>
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  setShowSizes(false);
                  setMessage('');
                }}
              >
                Hủy
              </button>
            </div>
          </div>
        )}

        {message && (
          <p className={`small mt-2 mb-0 ${message.startsWith('✓') ? 'text-success' : 'text-danger'}`}>
            {message}
          </p>
        )}
      </div>
    </div>
  );
}

export default ProductCard;

