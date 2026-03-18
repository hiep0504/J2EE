import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProductById } from '../services/productService';

function ProductDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedImage, setSelectedImage] = useState(null);
  const [selectedSize, setSelectedSize] = useState(null);

  useEffect(() => {
    getProductById(id)
      .then(res => {
        setProduct(res);
        setSelectedImage(res.image);
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p className="text-center py-5 text-muted">Đang tải...</p>;
  if (error) return <p className="text-center py-5 text-danger">{error}</p>;

  const mainImages = product.images?.length > 0
    ? product.images
    : [{ imageUrl: product.image, isMain: true }];

  return (
    <main className="container py-4">
      <button className="btn btn-link text-muted ps-0 mb-3" onClick={() => navigate(-1)}>
        ← Quay lại
      </button>

      <div className="row g-4">
        {/* Ảnh sản phẩm */}
        <div className="col-md-6">
          <img
            src={selectedImage || 'https://placehold.co/600x600?text=No+Image'}
            alt={product.name}
            className="img-fluid rounded w-100"
            style={{ aspectRatio: '1', objectFit: 'cover' }}
          />
          {mainImages.length > 1 && (
            <div className="d-flex gap-2 mt-2 flex-wrap">
              {mainImages.map((img, index) => (
                <img
                  key={index}
                  src={img.imageUrl}
                  alt={`${product.name} ${index + 1}`}
                  onClick={() => setSelectedImage(img.imageUrl)}
                  className="rounded"
                  style={{
                    width: 64,
                    height: 64,
                    objectFit: 'cover',
                    cursor: 'pointer',
                    border: selectedImage === img.imageUrl ? '2px solid #dc3545' : '2px solid transparent',
                  }}
                />
              ))}
            </div>
          )}
        </div>

        {/* Thông tin sản phẩm */}
        <div className="col-md-6">
          <p className="text-muted small mb-1">{product.categoryName}</p>
          <h4 className="fw-bold mb-2">{product.name}</h4>
          <h5 className="text-danger fw-bold mb-3">
            {Number(product.price).toLocaleString('vi-VN')}đ
          </h5>

          {/* Chọn size */}
          {product.sizes?.length > 0 && (
            <div className="mb-3">
              <p className="mb-2 fw-semibold">Kích thước:</p>
              <div className="d-flex gap-2 flex-wrap">
                {product.sizes.map(s => (
                  <button
                    key={s.id}
                    className={`btn btn-sm ${selectedSize?.id === s.id ? 'btn-dark' : 'btn-outline-dark'}`}
                    disabled={s.quantity === 0}
                    onClick={() => setSelectedSize(s)}
                  >
                    {s.sizeName}
                    {s.quantity === 0 && ' (hết)'}
                  </button>
                ))}
              </div>
              {selectedSize && (
                <p className="text-muted small mt-1">Còn lại: {selectedSize.quantity} sản phẩm</p>
              )}
            </div>
          )}

          <button className="btn btn-danger w-100 mb-2">Thêm vào giỏ hàng</button>
          <button className="btn btn-outline-danger w-100">Mua ngay</button>

          {/* Mô tả */}
          {product.description && (
            <div className="mt-4">
              <p className="fw-semibold mb-1">Mô tả sản phẩm:</p>
              <p className="text-muted">{product.description}</p>
            </div>
          )}
        </div>
      </div>
    </main>
  );
}

export default ProductDetailPage;
