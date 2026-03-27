import { useEffect, useState } from 'react'
import {
  getAdminProducts,
  getAdminCategories,
  getAdminOrders,
  getAdminUsers,
  getAdminReviews,
} from '../services/adminService'
import './AdminPages.css'

function DashboardPage() {
  const [stats, setStats] = useState({
    products: 0,
    categories: 0,
    orders: 0,
    users: 0,
    reviews: 0,
  })

  useEffect(() => {
    async function load() {
      try {
        const [products, categories, orders, users, reviews] = await Promise.all([
          getAdminProducts({ page: 0, size: 1 }),
          getAdminCategories({ page: 0, size: 1 }),
          getAdminOrders({ page: 0, size: 1 }),
          getAdminUsers({ page: 0, size: 1 }),
          getAdminReviews({ page: 0, size: 1 }),
        ])

        setStats({
          products: products.data.total || 0,
          categories: categories.data.total || 0,
          orders: orders.data.total || 0,
          users: users.data.total || 0,
          reviews: reviews.data.total || 0,
        })
      } catch {
        setStats({ products: 0, categories: 0, orders: 0, users: 0, reviews: 0 })
      }
    }

    load()
  }, [])

  const cards = [
    { label: 'Sản phẩm', value: stats.products },
    { label: 'Danh mục', value: stats.categories },
    { label: 'Đơn hàng', value: stats.orders },
    { label: 'Người dùng', value: stats.users },
    { label: 'Đánh giá', value: stats.reviews },
  ]

  return (
    <div className="admin-panel">
      <div className="admin-panel__header">
        <h2 className="admin-panel__title">Tổng quan hệ thống</h2>
      </div>
      <div className="admin-grid">
        {cards.map((card) => (
          <div key={card.label} className="admin-panel">
            <p style={{ margin: 0, color: '#5b7391' }}>{card.label}</p>
            <h3 style={{ margin: '6px 0 0', color: '#0f2848' }}>{card.value}</h3>
          </div>
        ))}
      </div>
    </div>
  )
}

export default DashboardPage
