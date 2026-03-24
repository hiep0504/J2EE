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

// Features
import ProductReview from './View/Product/ProductReview';
import OrderCreate from './View/Order/OrderCreate';
import OrderHistory from './View/Order/OrderHistory';
import OrderDetail from './View/Order/OrderDetail';
import OrderPaymentResult from './View/Order/OrderPaymentResult';

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

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Header user={user} onLogout={handleLogout} />

      <div style={{ flex: 1 }}>
        <Routes>
          {/* Home */}
          <Route path="/" element={<HomePage />} />

          {/* Auth */}
          <Route path="/login" element={<LoginPage onLoggedIn={refreshMe} />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/account"
            element={<AccountPage authChecked={authChecked} user={user} onRefreshed={setUser} />}
          />

          {/* Product */}
          <Route path="/product/review" element={<ProductReview />} />

          {/* Order */}
          <Route path="/order/create" element={<OrderCreate />} />
          <Route path="/order/history" element={<OrderHistory />} />
          <Route path="/order/detail/:orderId" element={<OrderDetail />} />
          <Route path="/order/payment-result" element={<OrderPaymentResult />} />
          <Route path="/order/workflow" element={<Navigate to="/order/create" replace />} />

          {/* Cart */}
          <Route path="/cart" element={<CartPage />} />
        </Routes>
      </div>

      <Footer />
    </div>
  );
}

export default App;