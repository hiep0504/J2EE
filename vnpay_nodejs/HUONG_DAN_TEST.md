# HƯỚNG DẪN TEST THANH TOÁN VNPAY SANDBOX

## Thông tin thẻ test VNPay Sandbox

### 1. Thẻ ATM Nội địa (VNBANK)
```
Ngân hàng: NCB (Ngân hàng Quốc dân)
Số thẻ: 9704198526191432198
Tên chủ thẻ: NGUYEN VAN A
Ngày phát hành: 07/15
Mật khẩu OTP: 123456
```

### 2. Thẻ Quốc tế (INTCARD)
```
Số thẻ: 4456530000001005
Tên chủ thẻ: NGUYEN VAN A
Ngày hết hạn: 12/25
CVV: 123
```

### 3. Ví điện tử / QR Code
- Chọn phương thức QR Code
- Quét mã QR bằng app ngân hàng test (nếu có)
- Hoặc chọn "Thanh toán test" nếu có nút này

## Các bước thanh toán

1. Tại trang http://localhost:8888 - Nhập số tiền
2. Click "Thanh toán"
3. **Trên trang VNPay:**
   - Chọn phương thức thanh toán (ATM, Thẻ quốc tế, QR...)
   - Nhập thông tin thẻ test ở trên
   - Click "Thanh toán"
4. Nhập mã OTP: **123456**
5. Xác nhận giao dịch

## Lưu ý
- Môi trường Sandbox không thu tiền thật
- Sử dụng thông tin thẻ test được cung cấp
- Nếu không có thẻ test, liên hệ VNPAY support để được cấp

## Liên hệ VNPAY
- Email: support@vnpay.vn
- Hotline: 1900 55 55 77
