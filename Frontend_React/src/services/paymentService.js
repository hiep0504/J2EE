import apiClient from './apiClient'

export function createVnPayPayment(amount, bankCode = '', language = 'vn') {
  return apiClient.post('/vnpay/create-payment', {
    amount,
    bankCode,
    language,
  })
}
