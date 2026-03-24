import { useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { toCurrency } from './orderApi'

function useQuery() {
  const { search } = useLocation()
  return useMemo(() => new URLSearchParams(search), [search])
}

function OrderPaymentResult() {
  const navigate = useNavigate()
  const query = useQuery()

  const status = query.get('status')
  const responseCode = query.get('vnp_ResponseCode')
  const isSuccess = (status === 'success' && responseCode === '00') || status === 'cod'

  const amount = query.get('vnp_Amount')
  const lastOrderTotal = typeof window !== 'undefined' ? localStorage.getItem('lastOrderTotal') : null
  const amountValue = amount
    ? Number(amount) / 100
    : lastOrderTotal
      ? Number(lastOrderTotal)
      : 0
  const bankCode = query.get('vnp_BankCode')
  const orderInfo = query.get('vnp_OrderInfo')
  const payDate = query.get('vnp_PayDate')
  const transactionNo = query.get('vnp_TransactionNo')

  const formattedPayDate = useMemo(() => {
    if (!payDate) return null
    if (payDate.length !== 14) return payDate

    const year = payDate.substring(0, 4)
    const month = payDate.substring(4, 6)
    const day = payDate.substring(6, 8)
    const hour = payDate.substring(8, 10)
    const minute = payDate.substring(10, 12)
    const second = payDate.substring(12, 14)

    return `${day}/${month}/${year} ${hour}:${minute}:${second}`
  }, [payDate])

  const lastOrderId = typeof window !== 'undefined' ? localStorage.getItem('lastOrderId') : null
  const lastOrderAccountId = typeof window !== 'undefined'
    ? localStorage.getItem('lastOrderAccountId') || '2'
    : '2'

  const lastOrderAddress = typeof window !== 'undefined' ? localStorage.getItem('lastOrderAddress') : null
  const lastOrderPhone = typeof window !== 'undefined' ? localStorage.getItem('lastOrderPhone') : null
  const lastOrderPaymentMethod = typeof window !== 'undefined'
    ? localStorage.getItem('lastOrderPaymentMethod')
    : null
  const lastOrderDate = typeof window !== 'undefined' ? localStorage.getItem('lastOrderDate') : null

  const formattedOrderDate = useMemo(() => {
    if (!lastOrderDate) return null

    const dateObj = new Date(lastOrderDate)
    if (Number.isNaN(dateObj.getTime())) {
      return lastOrderDate
    }

    const day = String(dateObj.getDate()).padStart(2, '0')
    const month = String(dateObj.getMonth() + 1).padStart(2, '0')
    const year = dateObj.getFullYear()
    const hour = String(dateObj.getHours()).padStart(2, '0')
    const minute = String(dateObj.getMinutes()).padStart(2, '0')

    return `${day}/${month}/${year} ${hour}:${minute}`
  }, [lastOrderDate])

  function goHome() {
    navigate('/')
  }

  function goHistory() {
    navigate(`/order/history?accountId=${lastOrderAccountId}`)
  }

  function goDetail() {
    if (!lastOrderId) return
    navigate(`/order/detail/${lastOrderId}?accountId=${lastOrderAccountId}`)
  }

  return (
    <main className="page">
      <section className="card shadow-lg border-0">
        <div className="todo-header mb-3">
          <h1 className="h3 mb-0">Kết quả thanh toán </h1>
        </div>

        <div className="review-list">
          <article className="review-item border rounded-3 p-3 shadow-sm">
            <p className={`status mb-3 ${isSuccess ? 'alert alert-success' : 'alert alert-danger'}`}>
              {isSuccess
                ? lastOrderPaymentMethod === 'VNPAY'
                  ? 'Thanh toán VNPay thành công'
                  : 'Đặt hàng thành công (COD)'
                : 'Thanh toán VNPay không thành công'}
            </p>
            <p><strong>Số tiền:</strong> {toCurrency(amountValue)}</p>
            {formattedOrderDate && (
              <p><strong>Ngày đặt hàng:</strong> {formattedOrderDate}</p>
            )}
            {bankCode && <p><strong>Ngân hàng:</strong> {bankCode}</p>}
            {orderInfo && <p><strong>Nội dung:</strong> {orderInfo}</p>}
            {formattedPayDate && (
              <p><strong>Thời gian thanh toán:</strong> {formattedPayDate}</p>
            )}
            {transactionNo && <p><strong>Mã giao dịch:</strong> {transactionNo}</p>}
            {lastOrderAddress && (
              <p><strong>Địa chỉ giao hàng:</strong> {lastOrderAddress}</p>
            )}
            {lastOrderPhone && (
              <p><strong>Số điện thoại:</strong> {lastOrderPhone}</p>
            )}
            {lastOrderPaymentMethod && (
              <p>
                <strong>Phương thức thanh toán:</strong>{' '}
                {lastOrderPaymentMethod === 'VNPAY'
                  ? 'Thanh toán online qua VNPay'
                  : 'Thanh toán khi nhận hàng (COD)'}
              </p>
            )}
            {lastOrderId && (
              <p><strong>Mã đơn hàng hệ thống:</strong> {lastOrderId}</p>
            )}
          </article>
        </div>

        <div className="todo-header" style={{ marginTop: '16px' }}>
          <button type="button" className="outline-button btn btn-outline-secondary" onClick={goHome}>
            Về trang chủ
          </button>
          <button type="button" className="outline-button btn btn-outline-primary" onClick={goHistory}>
            Xem lịch sử đơn hàng
          </button>
          {lastOrderId && (
            <button type="button" className="primary-button btn btn-primary" onClick={goDetail}>
              Xem chi tiết đơn hàng
            </button>
          )}
        </div>
      </section>
    </main>
  )
}

export default OrderPaymentResult
