import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import ProductCard from '../components/ProductCard';
import { getAllCategories, getAllProducts } from '../services/productService';
import './HomePage.css';

function HomePage({ user }) {
  const [searchParams] = useSearchParams();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [keyword, setKeyword] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [sortMode, setSortMode] = useState('newest');

  async function loadProducts() {
    setLoading(true);
    setError('');

    try {
      const res = await getAllProducts();
      setProducts(Array.isArray(res.data) ? res.data : []);
    } catch {
      setError('Không thể tải sản phẩm. Vui lòng kiểm tra backend và thử lại.');
    } finally {
      setLoading(false);
    }
  }

  async function loadCategories() {
    try {
      const res = await getAllCategories();
      setCategories(Array.isArray(res.data) ? res.data : []);
    } catch {
      setCategories([]);
    }
  }

  useEffect(() => {
    loadProducts();
    loadCategories();
  }, []);

  useEffect(() => {
    setKeyword(searchParams.get('q') || '');
    setSelectedCategoryId(searchParams.get('categoryId') || '');
  }, [searchParams]);

  const visibleProducts = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();

    let list = products.filter((product) => {
      const categoryMatched = !selectedCategoryId
        || String(product.categoryId || '') === String(selectedCategoryId);

      if (!categoryMatched) {
        return false;
      }

      if (!normalizedKeyword) {
        return true;
      }

      const name = String(product.name || '').toLowerCase();
      const category = String(product.categoryName || '').toLowerCase();

      return name.includes(normalizedKeyword) || category.includes(normalizedKeyword);
    });

    if (sortMode === 'price-asc') {
      list = [...list].sort((a, b) => Number(a.price) - Number(b.price));
    } else if (sortMode === 'price-desc') {
      list = [...list].sort((a, b) => Number(b.price) - Number(a.price));
    } else {
      list = [...list].sort((a, b) => Number(b.id) - Number(a.id));
    }

    return list;
  }, [products, keyword, selectedCategoryId, sortMode]);

  const highlightProducts = visibleProducts.slice(0, 4);

  return (
    <main className="home-page container py-4 py-lg-5">
      <section className="home-hero mb-4 mb-lg-5">
        <div>
          <p className="home-hero__label mb-2">BST Mua Sắm 2026</p>
          <h1 className="home-hero__title mb-3">Phong cách mới cho tủ đồ hàng ngày</h1>
          <p className="home-hero__desc mb-0">
            Chọn nhanh sản phẩm hot, tối ưu theo giá và đặt hàng chỉ trong vài bước.
          </p>
          {String(user?.role || '').toLowerCase() === 'admin' && (
            <Link to="/admin/dashboard" className="home-hero__admin-btn">
              Vào Admin Dashboard
            </Link>
          )}
        </div>
        <div className="home-hero__stats">
          <span>Tổng sản phẩm</span>
          <strong>{products.length}</strong>
        </div>
      </section>

      <section className="home-toolbar mb-4">
        <div className="row g-2 align-items-center">
          <div className="col-12 col-md-4">
            <select
              className="form-select home-toolbar__select"
              value={selectedCategoryId}
              onChange={(event) => setSelectedCategoryId(event.target.value)}
            >
              <option value="">Tất cả danh mục</option>
              {categories.map((category) => (
                <option key={category.id} value={String(category.id)}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>
          <div className="col-7 col-md-4">
            <select
              className="form-select home-toolbar__select"
              value={sortMode}
              onChange={(event) => setSortMode(event.target.value)}
            >
              <option value="newest">Mới nhất</option>
              <option value="price-asc">Giá tăng dần</option>
              <option value="price-desc">Giá giảm dần</option>
            </select>
          </div>
          <div className="col-5 col-md-2 d-grid">
            <button type="button" className="btn home-toolbar__reload" onClick={loadProducts}>
              Tải lại
            </button>
          </div>
        </div>
      </section>

      {loading && <p className="text-center py-5 text-muted">Đang tải sản phẩm...</p>}

      {!loading && error && (
        <div className="home-empty text-center py-5">
          <p className="text-danger mb-3">{error}</p>
          <button type="button" className="btn btn-dark" onClick={loadProducts}>
            Thử lại
          </button>
        </div>
      )}

      {!loading && !error && visibleProducts.length === 0 && (
        <p className="text-center py-5 text-muted">Không tìm thấy sản phẩm phù hợp.</p>
      )}

      {!loading && !error && visibleProducts.length > 0 && (
        <>
          <section className="home-highlight mb-4 mb-lg-5">
            <h2 className="home-section-title">Gợi ý nổi bật</h2>
            <div className="row row-cols-1 row-cols-sm-2 row-cols-lg-4 g-3">
              {highlightProducts.map((product) => (
                <div className="col" key={product.id}>
                  <ProductCard product={product} />
                </div>
              ))}
            </div>
          </section>

          <section>
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h2 className="home-section-title mb-0">Tất cả sản phẩm</h2>
              <span className="home-count">{visibleProducts.length} sản phẩm</span>
            </div>

            <div className="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-3">
              {visibleProducts.map((product) => (
                <div className="col" key={product.id}>
                  <ProductCard product={product} />
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </main>
  );
}

export default HomePage;
