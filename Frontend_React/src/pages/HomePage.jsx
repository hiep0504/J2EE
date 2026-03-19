import { useEffect, useState } from 'react';
import ProductCard from '../components/ProductCard';
import { getAllProducts } from '../services/productService';
import { getAllCategories, getProductsByCategory } from '../services/categoryService';

function HomePage() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    getAllCategories()
      .then(res => setCategories(res))
      .catch(() => {});
  }, []);

  useEffect(() => {
    setLoading(true);
    setError(null);
    const fetchFn = selectedCategory
      ? () => getProductsByCategory(selectedCategory)
      : getAllProducts;
    fetchFn()
      .then(res => setProducts(res))
      .catch(() => setError('Không thể tải sản phẩm.'))
      .finally(() => setLoading(false));
  }, [selectedCategory]);

  return (
    <main className="container py-4">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <h5 className="mb-0">Sản phẩm</h5>
        <select
          className="form-select w-auto"
          value={selectedCategory ?? ''}
          onChange={e => setSelectedCategory(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">Tất cả danh mục</option>
          {categories.map(cat => (
            <option key={cat.id} value={cat.id}>{cat.name}</option>
          ))}
        </select>
      </div>

      {loading && <p className="text-center py-5 text-muted">Đang tải...</p>}
      {error && <p className="text-center py-5 text-danger">{error}</p>}
      {!loading && !error && (
        <div className="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-3">
          {products.map(product => (
            <div className="col" key={product.id}>
              <ProductCard product={product} />
            </div>
          ))}
          {products.length === 0 && (
            <p className="text-muted">Không có sản phẩm nào trong danh mục này.</p>
          )}
        </div>
      )}
    </main>
  );
}

export default HomePage;
