import { useEffect, useState } from 'react';
import ProductCard from '../components/ProductCard';
import { getAllProducts } from '../services/productService';

function HomePage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    getAllProducts()
      .then(res => setProducts(res.data))
      .catch(() => setError('Không thể tải sản phẩm.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="text-center py-5 text-muted">Đang tải...</p>;
  if (error) return <p className="text-center py-5 text-danger">{error}</p>;

  return (
    <main className="container py-4">
      <h5 className="mb-4">Sản phẩm</h5>
      <div className="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-3">
        {products.map(product => (
          <div className="col" key={product.id}>
            <ProductCard product={product} />
          </div>
        ))}
      </div>
    </main>
  );
}

export default HomePage;
