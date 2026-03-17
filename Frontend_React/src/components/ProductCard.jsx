import { Link } from 'react-router-dom';
import './ProductCard.css';

function ProductCard({ product }) {
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
        </div>
      </div>
    </Link>
  );
}

export default ProductCard;
