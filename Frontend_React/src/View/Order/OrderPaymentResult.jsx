import { useNavigate, useSearchParams } from 'react-router-dom'
import { getVnpayResultMessage, toCurrency } from './orderApi'

function OrderPaymentResult() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const code = searchParams.get('code') || '99'
  const paymentResult = {
    code,
    status: searchParams.get('status') || (code === '00' ? 'success' : 'failed'),
    orderId: searchParams.get('orderId') || '',
    accountId: searchParams.get('accountId') || '',
    bankCode: searchParams.get('bankCode') || '-',
    transactionNo: searchParams.get('transactionNo') || '-',
    transactionStatus: searchParams.get('transactionStatus') || '-',
    payDate: searchParams.get('payDate') || '-',
    amount: Number(searchParams.get('amount') || 0),
    message: getVnpayResultMessage(code),
  }

  function goDetail() {
    if (!paymentResult.orderId || !paymentResult.accountId) return
    navigate(`/order/detail/${paymentResult.orderId}?accountId=${paymentResult.accountId}`)
  }

  function goHistory() {
    if (!paymentResult.accountId) {
      navigate('/order/create')
      return
    }

    navigate(`/order/history?accountId=${paymentResult.accountId}`)
  }

  return (
    <main className="page">
      <section className="card">
        <div className="todo-header">
          <h1>Kết quả thanh toán VNPay</h1>
          <span className={`payment-badge ${paymentResult.status === 'success' ? 'payment-success' : 'payment-failed'}`}>
            {paymentResult.status === 'success' ? 'Thành công' : 'Thất bại'}
          </span>
        </div>

        <p className="status payment-status-text">{paymentResult.message}</p>

        <div className="review-list payment-result-grid">
          <article className="review-item">
            <p><strong>Mã đơn hàng:</strong> #{paymentResult.orderId || '-'}</p>
            <p><strong>Số tiền:</strong> {toCurrency(paymentResult.amount)}</p>
            <p><strong>Mã phản hồi:</strong> {paymentResult.code}</p>
            <p><strong>Mã ngân hàng:</strong> {paymentResult.bankCode}</p>
          </article>

          <article className="review-item">
            <p><strong>Mã giao dịch VNPay:</strong> {paymentResult.transactionNo}</p>
            <p><strong>Trạng thái giao dịch:</strong> {paymentResult.transactionStatus}</p>
            <p><strong>Thời gian thanh toán:</strong> {paymentResult.payDate}</p>
            <p><strong>Account ID:</strong> {paymentResult.accountId || '-'}</p>
          </article>
        </div>

        <div className="todo-header payment-actions">
          <button type="button" className="outline-button" onClick={goHistory}>
            Xem lịch sử đơn hàng
          </button>
          <button
            type="button"
            className="primary-button"
            onClick={goDetail}
            disabled={!paymentResult.orderId || !paymentResult.accountId}
          >
            Xem chi tiết đơn hàng
          </button>
        </div>
      </section>
    </main>
  )
}

export default OrderPaymentResult