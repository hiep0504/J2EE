import { Link } from 'react-router-dom';
import './ProductCard.css';
import { addToCart } from '../services/cartService';

function ProductCard({ product }) {
  async function handleAddToCart(e) {
    e.preventDefault();
    e.stopPropagation();
    try {
      await addToCart(product.id, 1);
    } catch (err) {
      // keep UI minimal; log for debugging
      console.error('Add to cart failed', err);
    }
  }

  return (
    <Link to={`/products/${product.id}`} className="text-decoration-none text-dark">
      <div className="card h-100 product-card">
        <img
          src={product.image || 'https://placehold.co/300x300?text=No+Image'}
          alt={product.name}
          className="card-img-top product-card__image"
        />
        <div className="card-body">
          <p className="text-muted small mb-1">{product.categoryName}</p>
          <h6 className="card-title mb-2">{product.name}</h6>
          <p className="text-danger fw-bold mb-0">
            {Number(product.price).toLocaleString('vi-VN')}đ
          </p>

          <button
            type="button"
            className="btn btn-sm btn-outline-dark mt-3 w-100"
            onClick={handleAddToCart}
          >
            Thêm vào giỏ
          </button>
        </div>
      </div>
    </Link>
  );
}

export default ProductCard;
