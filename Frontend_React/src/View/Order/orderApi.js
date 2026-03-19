export const API_BASE = 'http://localhost:8080/api'

export function toCurrency(value) {
  if (value === null || value === undefined) return '0 đ'
  return Number(value).toLocaleString('vi-VN') + ' đ'
}

export function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('vi-VN')
}

export function createEmptyItem() {
  return { productSizeId: '', quantity: 1 }
}

export async function createVnpayPayment({ orderId, accountId, orderInfo }) {
  const response = await fetch(`${API_BASE}/payment/vnpay/url`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      orderId,
      accountId,
      orderInfo: orderInfo || `Thanh toan don hang #${orderId}`,
      locale: 'vn',
    }),
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || 'Khong the tao URL thanh toan VNPay')
  }

  const data = await response.json()
  if (!data.paymentUrl) {
    throw new Error('Phan hoi VNPay khong hop le')
  }

  return data.paymentUrl
}

export function getVnpayResultMessage(code) {
  const messages = {
    '00': 'Thanh toán VNPay thành công.',
    '07': 'Giao dịch bị nghi ngờ gian lận.',
    '09': 'Thẻ hoặc tài khoản chưa đăng ký Internet Banking.',
    '10': 'Xác thực thông tin thẻ hoặc tài khoản không đúng quá 3 lần.',
    '11': 'Giao dịch hết hạn chờ thanh toán.',
    '12': 'Thẻ hoặc tài khoản bị khóa.',
    '13': 'Sai mật khẩu xác thực giao dịch.',
    '24': 'Khách hàng đã hủy giao dịch.',
    '51': 'Tài khoản không đủ số dư để thanh toán.',
    '65': 'Tài khoản vượt hạn mức giao dịch trong ngày.',
    '75': 'Ngân hàng thanh toán đang bảo trì.',
    '79': 'Sai mật khẩu thanh toán quá số lần quy định.',
    '97': 'Chữ ký không hợp lệ.',
    '99': 'Không xác định được kết quả thanh toán.',
  }

  return messages[code] || 'Thanh toán chưa hoàn tất hoặc đã xảy ra lỗi.'
}
