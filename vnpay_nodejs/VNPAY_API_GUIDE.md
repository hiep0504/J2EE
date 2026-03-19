# HƯỚNG DẪN SỬ DỤNG VNPAY API ĐỂ LẤY DANH SÁCH GIAO DỊCH

## Vấn đề

VNPay Sandbox API **không cung cấp** endpoint để lấy **danh sách tất cả giao dịch**. 
API `querydr` chỉ cho phép truy vấn **từng giao dịch cụ thể** dựa trên:
- Mã đơn hàng (vnp_TxnRef)
- Ngày giao dịch (vnp_TransactionDate)

## Giải pháp đã triển khai

### 1. Lưu giao dịch trong Memory (Hiện tại)
- Khi tạo giao dịch → Lưu vào bộ nhớ
- Khi VNPay callback → Cập nhật trạng thái
- Xem danh sách tại: http://localhost:8888/order

**Ưu điểm:**
- Đơn giản, nhanh
- Không cần database

**Nhược điểm:**
- Mất dữ liệu khi restart server
- Không đồng bộ với VNPay Portal

### 2. Test API Query giao dịch cụ thể

Chạy script test:
```bash
node test_vnpay_api.js
```

**Cách sử dụng:**
1. Vào VNPay Portal: https://sandbox.vnpayment.vn/merchantv2/
2. Tìm giao dịch thành công
3. Copy:
   - Số hóa đơn (vnp_TxnRef): ví dụ `13161051`
   - Ngày tạo: ví dụ `13/11/2025 16:11:29`
4. Sửa trong file `test_vnpay_api.js`:
   ```javascript
   const testTxnRef = '13161051';
   const testTransactionDate = '13/11/2025 16:11:29';
   ```
5. Chạy: `node test_vnpay_api.js`

## Giải pháp để lấy TOÀN BỘ giao dịch

### Option 1: Lưu vào Database (Khuyến nghị)
```
Tạo giao dịch → Lưu DB
VNPay callback → Cập nhật DB
Xem danh sách → Query từ DB
```

**Setup MongoDB:**
```bash
npm install mongoose
```

**Setup MySQL:**
```bash
npm install mysql2
```

### Option 2: Webhook/IPN từ VNPay
- Đăng ký IPN URL với VNPay
- VNPay tự động gửi kết quả về server
- Lưu vào database

### Option 3: Scraping VNPay Portal (Không khuyến nghị)
- Dùng Puppeteer/Selenium
- Login vào portal
- Crawl dữ liệu
- **Chú ý:** Vi phạm terms of service

## Cấu trúc API querydr

**Request:**
```json
{
  "vnp_RequestId": "161129",
  "vnp_Version": "2.1.0",
  "vnp_Command": "querydr",
  "vnp_TmnCode": "AFHY5UKO",
  "vnp_TxnRef": "13161051",
  "vnp_OrderInfo": "Truy van GD ma:13161051",
  "vnp_TransactionDate": "13/11/2025 16:11:29",
  "vnp_CreateDate": "20251113161129",
  "vnp_IpAddr": "127.0.0.1",
  "vnp_SecureHash": "..."
}
```

**Response thành công (00):**
```json
{
  "vnp_ResponseCode": "00",
  "vnp_Message": "Giao dịch thành công",
  "vnp_TransactionNo": "15259067",
  "vnp_Amount": "1000000",
  "vnp_BankCode": "NCB",
  "vnp_TransactionStatus": "00"
}
```

**Response lỗi:**
- `91`: Không tìm thấy giao dịch
- `94`: Request trùng lặp
- `97`: Checksum không hợp lệ

## Tính năng hiện tại

✅ Tạo giao dịch
✅ Lưu vào memory
✅ Cập nhật trạng thái tự động
✅ Hiển thị danh sách
✅ Lọc theo ngày (giao diện)
✅ Tính tổng tiền

## Nếu muốn kết nối Database

Cho tôi biết bạn muốn dùng:
- MongoDB
- MySQL  
- PostgreSQL
- SQLite

Tôi sẽ giúp setup!
