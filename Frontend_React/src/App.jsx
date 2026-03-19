import { Routes, Route, Navigate } from 'react-router-dom';
import './App.css';

// Layout
import Header from './components/Header';
import Footer from './components/Footer';

// Pages
import HomePage from './pages/HomePage';

// Features
import ProductReview from './View/Product/ProductReview';
import OrderCreate from './View/Order/OrderCreate';
import OrderHistory from './View/Order/OrderHistory';
import OrderDetail from './View/Order/OrderDetail';
import OrderPaymentResult from './View/Order/OrderPaymentResult';

function App() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>

      <Header />

      <div style={{ flex: 1 }}>
        <Routes>
          {/* Home */}
          <Route path="/" element={<HomePage />} />

          {/* Product */}
          <Route path="/product/review" element={<ProductReview />} />

          {/* Order */}
          <Route path="/order/create" element={<OrderCreate />} />
          <Route path="/order/history" element={<OrderHistory />} />
          <Route path="/order/detail/:orderId" element={<OrderDetail />} />
          <Route path="/order/payment-result" element={<OrderPaymentResult />} />
          <Route path="/order/workflow" element={<Navigate to="/order/create" replace />} />
        </Routes>
      </div>

      <Footer />

    </div>
  );
}

export default App;