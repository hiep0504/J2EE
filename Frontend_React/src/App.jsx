import { BrowserRouter, Navigate, Routes, Route } from 'react-router-dom'
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
}

export default App
