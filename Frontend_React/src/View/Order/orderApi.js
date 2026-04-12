export const API_BASE = 'https://busticket.ink/api'

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
