/**
 * Script test gọi VNPay API để lấy danh sách giao dịch
 */

const request = require('request');
const moment = require('moment');
const crypto = require("crypto");
const config = require('config');

process.env.TZ = 'Asia/Ho_Chi_Minh';

// Lấy thông tin config
const vnp_TmnCode = config.get('vnp_TmnCode');
const secretKey = config.get('vnp_HashSecret');
const vnp_Api = config.get('vnp_Api');

console.log('=== TEST VNPAY API ===');
console.log('Terminal Code:', vnp_TmnCode);
console.log('API URL:', vnp_Api);
console.log('');

// Test query một giao dịch cụ thể
function testQueryTransaction(txnRef, transactionDate) {
    let date = new Date();
    let vnp_RequestId = moment(date).format('HHmmss');
    let vnp_Version = '2.1.0';
    let vnp_Command = 'querydr';
    let vnp_TxnRef = txnRef;
    let vnp_OrderInfo = 'Truy van GD ma:' + txnRef;
    let vnp_TransactionDate = transactionDate; // Format: DD/MM/YYYY HH:mm:ss
    let vnp_CreateDate = moment(date).format('YYYYMMDDHHmmss');
    let vnp_IpAddr = '127.0.0.1';
    
    // Tạo chuỗi ký theo thứ tự
    let data = vnp_RequestId + "|" + vnp_Version + "|" + vnp_Command + "|" + vnp_TmnCode + "|" + vnp_TxnRef + "|" + vnp_TransactionDate + "|" + vnp_CreateDate + "|" + vnp_IpAddr + "|" + vnp_OrderInfo;
    
    let hmac = crypto.createHmac("sha512", secretKey);
    let vnp_SecureHash = hmac.update(Buffer.from(data, 'utf-8')).digest("hex");
    
    let dataObj = {
        'vnp_RequestId': vnp_RequestId,
        'vnp_Version': vnp_Version,
        'vnp_Command': vnp_Command,
        'vnp_TmnCode': vnp_TmnCode,
        'vnp_TxnRef': vnp_TxnRef,
        'vnp_OrderInfo': vnp_OrderInfo,
        'vnp_TransactionDate': vnp_TransactionDate,
        'vnp_CreateDate': vnp_CreateDate,
        'vnp_IpAddr': vnp_IpAddr,
        'vnp_SecureHash': vnp_SecureHash
    };
    
    console.log('Request Data:', JSON.stringify(dataObj, null, 2));
    console.log('');
    
    request({
        url: vnp_Api,
        method: "POST",
        json: true,
        body: dataObj
    }, function (error, response, body) {
        if (error) {
            console.error('Error:', error);
            return;
        }
        
        console.log('Response:', JSON.stringify(body, null, 2));
        console.log('');
        
        if (body.vnp_ResponseCode === '00') {
            console.log('✅ Giao dịch tìm thấy!');
            console.log('- Mã GD:', body.vnp_TransactionNo);
            console.log('- Số tiền:', body.vnp_Amount);
            console.log('- Ngân hàng:', body.vnp_BankCode);
            console.log('- Trạng thái:', body.vnp_TransactionStatus);
        } else {
            console.log('❌ Lỗi:', body.vnp_Message || 'Không tìm thấy giao dịch');
        }
    });
}

// Lấy thông tin từ VNPay Portal
// Thay đổi các giá trị này theo giao dịch thực tế của bạn
const testTxnRef = '13161051';  // Mã đơn hàng từ web VNPay
const testTransactionDate = '13/11/2025 16:11:29';  // Ngày tạo từ web VNPay

console.log('Đang truy vấn giao dịch:', testTxnRef);
console.log('Ngày giao dịch:', testTransactionDate);
console.log('');

testQueryTransaction(testTxnRef, testTransactionDate);
