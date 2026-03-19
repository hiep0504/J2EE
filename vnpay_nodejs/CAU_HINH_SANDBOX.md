# CÁCH KÍCH HOẠT PHƯƠNG THỨC THANH TOÁN VNPAY SANDBOX

## Vấn đề hiện tại
Lỗi: "Ngân hàng thanh toán không được hỗ trợ" (Error code: 76)
- Tài khoản sandbox của bạn chưa được cấu hình đầy đủ các phương thức thanh toán

## Giải pháp

### Bước 1: Đăng nhập vào VNPAY Merchant Portal
1. Truy cập: https://sandbox.vnpayment.vn/merchantv2/
2. Đăng nhập bằng tài khoản đã đăng ký (Terminal: AFHY5UKO)

### Bước 2: Kích hoạt phương thức thanh toán
1. Vào menu **"Cấu hình"** hoặc **"Settings"**
2. Tìm mục **"Phương thức thanh toán"** hoặc **"Payment Methods"**
3. **BẬT** các phương thức:
   - ✅ VNPAYQR (QR Code)
   - ✅ ATM Card (Thẻ nội địa)
   - ✅ International Card (Thẻ quốc tế)
   - ✅ Các ví điện tử (nếu muốn)

### Bước 3: Kiểm tra cấu hình API
1. Vào **"API Configuration"**
2. Đảm bảo:
   - API Version: **2.1.0**
   - Hash Type: **SHA512**
   - Return URL: `http://localhost:8888/order/vnpay_return`

### Bước 4: Lưu và đợi kích hoạt
- Lưu cấu hình
- Đợi vài phút để hệ thống cập nhật
- Thử lại giao dịch

## Nếu vẫn không được

### Giải pháp 1: Liên hệ VNPAY Support
```
Email: support@vnpay.vn
Hotline: 1900 55 55 77
Nội dung: "Xin hỗ trợ kích hoạt tài khoản sandbox Terminal ID: AFHY5UKO
           để test tất cả các phương thức thanh toán"
```

### Giải pháp 2: Sử dụng tài khoản demo public
VNPAY thường cung cấp tài khoản demo chung với đầy đủ phương thức thanh toán.
Tìm kiếm trong tài liệu hoặc hỏi support.

### Giải pháp 3: Test với phương thức đơn giản nhất
Trong khi chờ kích hoạt, thử thanh toán không chỉ định bankCode:
- Chọn: "Cổng thanh toán VNPAYQR" (không chọn gì cả)
- Để VNPay tự động hiển thị các phương thức có sẵn

## Kiểm tra trạng thái tài khoản
Dashboard: https://sandbox.vnpayment.vn/merchantv2/
- Xem có thông báo "Pending Approval" không
- Kiểm tra email xem có yêu cầu xác thực không
