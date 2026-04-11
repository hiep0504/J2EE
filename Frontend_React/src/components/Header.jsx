import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import SearchBar from './SearchBar';
import './Header.css';

function Header({ user, onLogout, onSearch }) {
  const location = useLocation();
  const navigate = useNavigate();
  const userRef = useRef(null);
  const searchKeyword = new URLSearchParams(location.search).get('q') || '';
  const [showUserMenu, setShowUserMenu] = useState(false);

  const isAdmin = String(user?.role || '').toLowerCase() === 'admin';

  useEffect(() => {
    function handlePointerDown(event) {
      if (showUserMenu && userRef.current && !userRef.current.contains(event.target)) {
        setShowUserMenu(false);
      }
    }

    function handleEscape(event) {
      if (event.key === 'Escape') {
        setShowUserMenu(false);
      }
    }

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [showUserMenu]);

  function navigateWithQuery(nextParams) {
    const params = new URLSearchParams(location.search);

    Object.entries(nextParams).forEach(([key, value]) => {
      if (value === null || value === undefined || value === '') {
        params.delete(key);
      } else {
        params.set(key, value);
      }
    });

    const query = params.toString();
    navigate(query ? `/?${query}` : '/');
  }

  return (
    <header className="shop-header">
      <div className="shop-header__inner">
        <div className="shop-header__left">
          <Link to="/" className="shop-header__brand">
            <span className="shop-header__logo">SA</span>
            <div>
              <strong>ShopApp</strong>
              <p>Streetwear Store</p>
            </div>
          </Link>
        </div>

        <div className="shop-header__center">
          <SearchBar
            value={searchKeyword}
            onSearch={onSearch}
            placeholder="Tìm sản phẩm..."
          />
        </div>

        <div className="shop-header__right">
          <nav className="shop-header__nav">
            <Link to="/cart" className="shop-header__link">Giỏ hàng</Link>

            <Link to="/support-chat" className="shop-header__icon-link" aria-label="Hỗ trợ chat" title="Hỗ trợ chat">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <path d="M12 4C16.97 4 21 7.58 21 12C21 16.42 16.97 20 12 20C10.76 20 9.58 19.78 8.5 19.38L4 20L5.06 16.74C3.77 15.38 3 13.75 3 12C3 7.58 7.03 4 12 4Z" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
                <circle cx="9" cy="12" r="1.1" fill="currentColor" />
                <circle cx="12" cy="12" r="1.1" fill="currentColor" />
                <circle cx="15" cy="12" r="1.1" fill="currentColor" />
              </svg>
            </Link>
          </nav>

          <div className="shop-header__actions">
            {user ? (
              <div className="shop-header__dropdown" ref={userRef}>
                <button
                  type="button"
                  className="shop-header__user-toggle"
                  onClick={() => setShowUserMenu((prev) => !prev)}
                  aria-expanded={showUserMenu}
                >
                  <span className="shop-header__user-icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <circle cx="12" cy="8" r="4" stroke="currentColor" strokeWidth="1.8" />
                      <path d="M4 20C5.5 16.5 8.4 14.8 12 14.8C15.6 14.8 18.5 16.5 20 20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                    </svg>
                  </span>
                  <span>{user.username}</span>
                  <span className="shop-header__caret" aria-hidden="true">▾</span>
                </button>

                {showUserMenu && (
                  <div className="shop-header__menu shop-header__menu--user" role="menu">
                    <Link to="/account" className="shop-header__menu-item" onClick={() => setShowUserMenu(false)}>
                      Tài khoản
                    </Link>

                    {isAdmin && (
                      <Link to="/admin/dashboard" className="shop-header__menu-item" onClick={() => setShowUserMenu(false)}>
                        Admin Dashboard
                      </Link>
                    )}

                    <button
                      type="button"
                      className="shop-header__menu-item shop-header__menu-item--danger"
                      onClick={() => {
                        setShowUserMenu(false);
                        onLogout();
                      }}
                    >
                      Logout
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <>
                <Link to="/login" className="shop-header__btn shop-header__btn--subtle">Đăng nhập</Link>
                <Link to="/register" className="shop-header__btn shop-header__btn--solid">Đăng ký</Link>
              </>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}

export default Header;
