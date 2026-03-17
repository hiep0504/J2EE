<<<<<<< HEAD
import { Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import Footer from './components/Footer';
import HomePage from './pages/HomePage';

function App() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Header />
      <div style={{ flex: 1 }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
        </Routes>
      </div>
      <Footer />
    </div>
  );
=======
﻿import { BrowserRouter, Navigate, Routes, Route } from 'react-router-dom'
import './App.css'
import ProductReview from './View/Product/ProductReview'
import OrderCreate from './View/Order/OrderCreate'
import OrderHistory from './View/Order/OrderHistory'
import OrderDetail from './View/Order/OrderDetail'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/product/review" element={<ProductReview />} />
        <Route path="/order/create" element={<OrderCreate />} />
        <Route path="/order/history" element={<OrderHistory />} />
        <Route path="/order/detail/:orderId" element={<OrderDetail />} />
        <Route path="/order/workflow" element={<Navigate to="/order/create" replace />} />
      </Routes>
    </BrowserRouter>
  )
>>>>>>> origin/Review-the-product-and-Oder
}

export default App;
