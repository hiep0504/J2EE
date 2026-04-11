import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import './App.css';

// Layout
import Header from './components/Header';
import Footer from './components/Footer';

// Pages
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AccountPage from './pages/AccountPage';
import CartPage from './pages/CartPage';
import CartCheckoutPage from './pages/CartCheckoutPage';
import ProductDetailPage from './pages/ProductDetailPage';
import SupportChatPage from './pages/SupportChatPage';

// Features
import ProductReview from './View/Product/ProductReview';
import OrderCreate from './View/Order/OrderCreate';
import OrderHistory from './View/Order/OrderHistory';
import OrderDetail from './View/Order/OrderDetail';
import OrderPaymentResult from './View/Order/OrderPaymentResult';

// Admin
import RequireAdmin from './admin/RequireAdmin';
import AdminLayout from './admin/AdminLayout';
import DashboardPage from './admin/DashboardPage';
import ProductsPage from './admin/ProductsPage';
import CategoriesPage from './admin/CategoriesPage';
import OrdersPage from './admin/OrdersPage';
import RevenuePage from './admin/RevenuePage';
import UsersPage from './admin/UsersPage';
import ReviewsPage from './admin/ReviewsPage';
import ChatSupportPage from './admin/ChatSupportPage';

import { getMe } from './services/accountService';
import { logout } from './services/authService';

function App() {
  const navigate = useNavigate();
  const [authChecked, setAuthChecked] = useState(false);
  const [user, setUser] = useState(null);

  async function refreshMe() {
    try {
      const res = await getMe();
      setUser(res.data);
    } catch {
      setUser(null);
    } finally {
      setAuthChecked(true);
    }
  }

  useEffect(() => {
    refreshMe();
  }, []);

  async function handleLogout() {
    try {
      await logout();
    } finally {
      setUser(null);
      navigate('/');
    }
  }

  function handleSearch(keyword) {
    const query = keyword.trim();
    const search = query ? `?q=${encodeURIComponent(query)}` : '';
    navigate(`/${search}`);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Header user={user} onLogout={handleLogout} onSearch={handleSearch} />

      <div style={{ flex: 1 }}>
        <Routes>
          {/* Home */}
          <Route path="/" element={<HomePage user={user} />} />

          {/* Auth */}
          <Route path="/login" element={<LoginPage onLoggedIn={refreshMe} />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/account"
            element={<AccountPage authChecked={authChecked} user={user} onRefreshed={setUser} />}
          />

          {/* Product */}
          <Route path="/products/:productId" element={<ProductDetailPage user={user} authChecked={authChecked} />} />
          <Route path="/support-chat" element={<SupportChatPage />} />
          <Route path="/product/review" element={<ProductReview user={user} authChecked={authChecked} />} />

          {/* Order */}
          <Route path="/order/create" element={<OrderCreate user={user} authChecked={authChecked} />} />
          <Route path="/order/history" element={<OrderHistory user={user} authChecked={authChecked} />} />
          <Route path="/order/detail/:orderId" element={<OrderDetail user={user} authChecked={authChecked} />} />
          <Route path="/order/payment-result" element={<OrderPaymentResult />} />
          <Route path="/order/workflow" element={<Navigate to="/order/create" replace />} />

          {/* Cart */}
          <Route path="/cart" element={<CartPage />} />
          <Route path="/cart/checkout" element={<CartCheckoutPage />} />

          {/* Admin */}
          <Route
            path="/admin"
            element={(
              <RequireAdmin user={user} authChecked={authChecked}>
                <AdminLayout user={user} />
              </RequireAdmin>
            )}
          >
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="products" element={<ProductsPage />} />
            <Route path="categories" element={<CategoriesPage />} />
            <Route path="orders" element={<OrdersPage />} />
            <Route path="revenue" element={<RevenuePage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="reviews" element={<ReviewsPage />} />
            <Route path="chat" element={<ChatSupportPage />} />
          </Route>
        </Routes>
      </div>

      <Footer />
    </div>
  );
}

export default App;