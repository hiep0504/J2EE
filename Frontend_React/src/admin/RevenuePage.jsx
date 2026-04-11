import { useEffect, useMemo, useState } from 'react'
import { getAdminRevenueSummary } from '../services/adminService'
import './AdminPages.css'

const monthOptions = [3, 6, 12]

function buildCsv(rows) {
  const escaped = rows.map((row) => row.map((cell) => {
    const text = String(cell ?? '')
    return `"${text.replace(/"/g, '""')}"`
  }).join(','))
  return `\uFEFF${escaped.join('\n')}`
}

function downloadCsv(filename, rows) {
  const content = buildCsv(rows)
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function formatCurrency(value) {
  const numericValue = Number(value || 0)
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(numericValue)
}

function formatPercent(value) {
  const number = Number(value || 0)
  return `${number.toFixed(2)}%`
}

function calculateMovingAverage(points, windowSize = 3) {
  if (!Array.isArray(points) || points.length === 0) {
    return []
  }

  return points.map((point, index) => {
    const fromIndex = Math.max(0, index - windowSize + 1)
    const slice = points.slice(fromIndex, index + 1)
    const total = slice.reduce((sum, item) => sum + Number(item.revenue || 0), 0)
    const value = slice.length > 0 ? total / slice.length : 0
    return {
      label: point.label,
      value,
    }
  })
}

function RevenueChart({ points }) {
  const width = 860
  const height = 320
  const padding = { top: 18, right: 18, bottom: 52, left: 72 }
  const plotWidth = width - padding.left - padding.right
  const plotHeight = height - padding.top - padding.bottom

  const maxRevenue = Math.max(1, ...points.map((item) => Number(item.revenue || 0)))
  const movingAverage = calculateMovingAverage(points, 3)

  const revenueToY = (value) => {
    const ratio = Number(value || 0) / maxRevenue
    return padding.top + plotHeight - ratio * plotHeight
  }

  const barSlot = points.length > 0 ? plotWidth / points.length : plotWidth
  const barWidth = Math.max(16, Math.min(42, barSlot * 0.58))

  const averageLinePath = movingAverage
    .map((item, index) => {
      const x = padding.left + index * barSlot + barSlot / 2
      const y = revenueToY(item.value)
      return `${index === 0 ? 'M' : 'L'} ${x} ${y}`
    })
    .join(' ')

  const axisValues = Array.from({ length: 5 }, (_, index) => {
    const value = (maxRevenue / 4) * (4 - index)
    return value
  })

  return (
    <div className="admin-revenue-chart-wrap">
      <svg className="admin-revenue-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Biểu đồ doanh thu theo tháng">
        <defs>
          <linearGradient id="barGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#0d6b6f" stopOpacity="0.92" />
            <stop offset="100%" stopColor="#6fc6c9" stopOpacity="0.7" />
          </linearGradient>
        </defs>

        {axisValues.map((value) => {
          const y = revenueToY(value)
          return (
            <g key={value}>
              <line x1={padding.left} y1={y} x2={width - padding.right} y2={y} className="admin-revenue-chart__grid" />
              <text x={padding.left - 10} y={y + 4} textAnchor="end" className="admin-revenue-chart__axis-label">
                {(value / 1000000).toFixed(0)}M
              </text>
            </g>
          )
        })}

        {points.map((item, index) => {
          const revenue = Number(item.revenue || 0)
          const barHeight = Math.max(1, (revenue / maxRevenue) * plotHeight)
          const x = padding.left + index * barSlot + (barSlot - barWidth) / 2
          const y = padding.top + plotHeight - barHeight

          return (
            <g key={item.label}>
              <rect x={x} y={y} width={barWidth} height={barHeight} rx="8" className="admin-revenue-chart__bar" />
              <text x={x + barWidth / 2} y={height - 24} textAnchor="middle" className="admin-revenue-chart__month">
                {item.label}
              </text>
            </g>
          )
        })}

        <path d={averageLinePath} className="admin-revenue-chart__line" />
      </svg>

      <div className="admin-revenue-chart__legend">
        <span><i className="legend-dot legend-dot--bar" /> Doanh thu tháng (cột)</span>
        <span><i className="legend-dot legend-dot--line" /> Trung bình động MA(3) (đường)</span>
      </div>
    </div>
  )
}

function RevenuePage() {
  const [months, setMonths] = useState(6)
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [query, setQuery] = useState({ months: 6, fromDate: '', toDate: '' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [summary, setSummary] = useState({
    totalRevenue: 0,
    totalOrders: 0,
    averageRevenuePerMonth: 0,
    trendPercent: 0,
    points: [],
    topCustomers: [],
  })

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError('')

      try {
        const params = {
          months: query.months,
        }

        if (query.fromDate && query.toDate) {
          params.fromDate = query.fromDate
          params.toDate = query.toDate
        }

        const res = await getAdminRevenueSummary(params)
        setSummary({
          totalRevenue: res.data.totalRevenue || 0,
          totalOrders: res.data.totalOrders || 0,
          averageRevenuePerMonth: res.data.averageRevenuePerMonth || 0,
          trendPercent: res.data.trendPercent || 0,
          points: res.data.points || [],
          topCustomers: res.data.topCustomers || [],
        })
      } catch {
        setError('Không tải được dữ liệu doanh thu. Vui lòng thử lại.')
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [query])

  function applyFilters() {
    if ((fromDate && !toDate) || (!fromDate && toDate)) {
      setError('Vui lòng nhập đầy đủ Từ ngày và Đến ngày.')
      return
    }

    if (fromDate && toDate && fromDate > toDate) {
      setError('Từ ngày không được lớn hơn Đến ngày.')
      return
    }

    setError('')
    setQuery({ months, fromDate, toDate })
  }

  function resetDateFilter() {
    setFromDate('')
    setToDate('')
    setError('')
    setQuery({ months, fromDate: '', toDate: '' })
  }

  function handleExportMonthlyCsv() {
    const rows = [
      ['Thang', 'Doanh thu', 'So don hoan tat'],
      ...(summary.points || []).map((item) => [item.label, item.revenue, item.orderCount]),
    ]
    downloadCsv('doanh-thu-theo-thang.csv', rows)
  }

  function handleExportCustomerCsv() {
    const rows = [
      ['Top', 'Account ID', 'Username', 'Doanh thu', 'So don', 'Ty trong (%)'],
      ...(summary.topCustomers || []).map((item, index) => [
        index + 1,
        item.accountId || '',
        item.username || 'Khach le',
        item.revenue,
        item.orderCount,
        item.percentOfTotal,
      ]),
    ]
    downloadCsv('top-khach-hang-doanh-thu.csv', rows)
  }

  const topCustomerRevenue = useMemo(
    () => (summary.topCustomers || []).reduce((sum, item) => sum + Number(item.revenue || 0), 0),
    [summary.topCustomers],
  )

  return (
    <div className="admin-panel admin-revenue-shell">
      <div className="admin-panel__header">
        <h2 className="admin-panel__title">Doanh thu theo đơn hàng khách hàng</h2>
        <div className="admin-toolbar admin-toolbar--revenue">
          <label htmlFor="revenue-months">Khoảng thời gian</label>
          <select
            id="revenue-months"
            className="admin-select"
            value={months}
            onChange={(event) => setMonths(Number(event.target.value))}
          >
            {monthOptions.map((item) => (
              <option key={item} value={item}>{item} tháng gần nhất</option>
            ))}
          </select>
          <input
            type="date"
            className="admin-input"
            value={fromDate}
            onChange={(event) => setFromDate(event.target.value)}
            aria-label="Từ ngày"
          />
          <input
            type="date"
            className="admin-input"
            value={toDate}
            onChange={(event) => setToDate(event.target.value)}
            aria-label="Đến ngày"
          />
          <button type="button" className="admin-btn admin-btn--primary" onClick={applyFilters}>Áp dụng</button>
          <button type="button" className="admin-btn admin-btn--ghost" onClick={resetDateFilter}>Xóa lọc ngày</button>
        </div>
      </div>

      <div className="admin-revenue-cards">
        <div className="admin-revenue-card">
          <p>Tổng doanh thu</p>
          <h3>{formatCurrency(summary.totalRevenue)}</h3>
        </div>
        <div className="admin-revenue-card">
          <p>Đơn hàng hoàn tất</p>
          <h3>{summary.totalOrders}</h3>
        </div>
        <div className="admin-revenue-card">
          <p>Doanh thu TB/tháng</p>
          <h3>{formatCurrency(summary.averageRevenuePerMonth)}</h3>
        </div>
        <div className="admin-revenue-card">
          <p>Xu hướng nửa sau so với nửa đầu</p>
          <h3 className={Number(summary.trendPercent) >= 0 ? 'up' : 'down'}>{formatPercent(summary.trendPercent)}</h3>
        </div>
      </div>

      {loading && <div className="admin-empty">Đang tải dữ liệu doanh thu...</div>}
      {!loading && error && <div className="admin-empty admin-empty--error">{error}</div>}

      {!loading && !error && (
        <>
          <div className="admin-toolbar">
            <button type="button" className="admin-btn admin-btn--ghost" onClick={handleExportMonthlyCsv}>Export CSV doanh thu tháng</button>
            <button type="button" className="admin-btn admin-btn--ghost" onClick={handleExportCustomerCsv}>Export CSV top khách hàng</button>
          </div>

          <RevenueChart points={summary.points || []} />

          <div className="admin-panel">
            <div className="admin-panel__header">
              <h3 className="admin-panel__title">Top khách hàng theo doanh thu</h3>
              <span style={{ color: '#4a6788' }}>Đóng góp: {formatCurrency(topCustomerRevenue)}</span>
            </div>

            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Khách hàng</th>
                    <th>Doanh thu</th>
                    <th>Số đơn</th>
                    <th>Tỷ trọng</th>
                  </tr>
                </thead>
                <tbody>
                  {(summary.topCustomers || []).map((item, index) => (
                    <tr key={`${item.accountId || 'guest'}-${index}`}>
                      <td>{index + 1}</td>
                      <td>{item.username || 'Khách lẻ'} {item.accountId ? `(ID: ${item.accountId})` : ''}</td>
                      <td>{formatCurrency(item.revenue)}</td>
                      <td>{item.orderCount}</td>
                      <td>{formatPercent(item.percentOfTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {(summary.topCustomers || []).length === 0 && <div className="admin-empty">Chưa có dữ liệu khách hàng trong giai đoạn này</div>}
          </div>
        </>
      )}
    </div>
  )
}

export default RevenuePage
