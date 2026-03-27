import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import SearchBar from './SearchBar';
import { getAllCategories } from '../services/productService';
import './Header.css';

function Header({ user, onLogout, onSearch }) {
  const location = useLocation();
  const navigate = useNavigate();
  const categoryRef = useRef(null);
  const userRef = useRef(null);
  const searchKeyword = new URLSearchParams(location.search).get('q') || '';
  const selectedCategoryId = new URLSearchParams(location.search).get('categoryId') || '';
  const [categories, setCategories] = useState([]);
  const [showCategoryMenu, setShowCategoryMenu] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);

  const isAdmin = String(user?.role || '').toLowerCase() === 'admin';

  useEffect(() => {
    let mounted = true;

    async function loadCategories() {
      try {
        const res = await getAllCategories();
        if (mounted) {
          setCategories(Array.isArray(res.data) ? res.data : []);
        }
      } catch {
        if (mounted) {
          setCategories([]);
        }
      }
    }

    loadCategories();

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    function handlePointerDown(event) {
      if (showCategoryMenu && categoryRef.current && !categoryRef.current.contains(event.target)) {
        setShowCategoryMenu(false);
      }

      if (showUserMenu && userRef.current && !userRef.current.contains(event.target)) {
        setShowUserMenu(false);
      }
    }

    function handleEscape(event) {
      if (event.key === 'Escape') {
        setShowCategoryMenu(false);
        setShowUserMenu(false);
      }
    }

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [showCategoryMenu, showUserMenu]);

  const selectedCategoryLabel = useMemo(() => {
    if (!selectedCategoryId) {
      return 'Danh mục';
    }

    const selected = categories.find((category) => String(category.id) === String(selectedCategoryId));
    return selected?.name || 'Danh mục';
  }, [categories, selectedCategoryId]);

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

  function handleSelectCategory(categoryId) {
    navigateWithQuery({ categoryId });
    setShowCategoryMenu(false);
  }

  function handleClearCategory() {
    navigateWithQuery({ categoryId: '' });
    setShowCategoryMenu(false);
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
            <div className="shop-header__dropdown" ref={categoryRef}>
              <button
                type="button"
                className="shop-header__link shop-header__dropdown-toggle"
                onClick={() => setShowCategoryMenu((prev) => !prev)}
                aria-expanded={showCategoryMenu}
              >
                {selectedCategoryLabel}
                <span className="shop-header__caret" aria-hidden="true">▾</span>
              </button>

              {showCategoryMenu && (
                <div className="shop-header__menu" role="menu">
                  <button type="button" className="shop-header__menu-item" onClick={handleClearCategory}>
                    Tất cả danh mục
                  </button>
                  {categories.map((category) => (
                    <button
                      key={category.id}
                      type="button"
                      className="shop-header__menu-item"
                      onClick={() => handleSelectCategory(String(category.id))}
                    >
                      {category.name}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <Link to="/cart" className="shop-header__link">Giỏ hàng</Link>
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
