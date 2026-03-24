import { Link } from 'react-router-dom';

function Header({ user, onLogout }) {
  return (
    <nav className="navbar navbar-dark bg-dark px-4">
      <div className="d-flex align-items-center gap-3">
        <Link to="/" className="navbar-brand fw-bold mb-0">ShopApp</Link>
        <Link to="/" className="nav-link text-white-50">Sản phẩm</Link>
        <Link to="/cart" className="nav-link text-white-50">Giỏ hàng</Link>
      </div>

      <div className="d-flex align-items-center gap-3">
        {user ? (
          <>
            <span className="text-white-50 small">Xin chào, {user.username}</span>
            <Link to="/account" className="btn btn-outline-light btn-sm">Tài khoản</Link>
            <button type="button" className="btn btn-light btn-sm" onClick={onLogout}>
              Đăng xuất
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="btn btn-outline-light btn-sm">Đăng nhập</Link>
            <Link to="/register" className="btn btn-light btn-sm">Đăng ký</Link>
          </>
        )}
      </div>
    </nav>
  );
}

export default Header;
