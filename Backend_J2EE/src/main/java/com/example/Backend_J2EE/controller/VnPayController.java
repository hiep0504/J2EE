package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.payment.VnPayCreatePaymentRequest;
import com.example.Backend_J2EE.dto.payment.VnPayCreatePaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vnpay")
@CrossOrigin(origins = "*")
public class VnPayController {

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.pay-url}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    /**
     * Tao URL thanh toan VNPay va tra ve cho frontend.
     */
    @PostMapping("/create-payment")
    public ResponseEntity<VnPayCreatePaymentResponse> createPayment(@RequestBody VnPayCreatePaymentRequest request,
                                                                    HttpServletRequest httpRequest) {
        long amount = request.getAmount() != null ? request.getAmount() : 0L;
        if (amount <= 0) {
            return ResponseEntity.badRequest().build();
        }

        String bankCode = request.getBankCode();
        String language = (request.getLanguage() == null || request.getLanguage().isBlank()) ? "vn" : request.getLanguage();

        LocalDateTime now = LocalDateTime.now();
        String createDate = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String orderId = now.format(DateTimeFormatter.ofPattern("ddHHmmss"));

        String ipAddr = getIpAddress(httpRequest);

        // Tham so goc (chua encode)
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Locale", language);
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", "Thanh toan cho ma GD:" + orderId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Amount", String.valueOf(amount * 100));
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", ipAddr);
        vnpParams.put("vnp_CreateDate", createDate);
        if (bankCode != null && !bankCode.isBlank()) {
            vnpParams.put("vnp_BankCode", bankCode);
        }

        // Giong sortObject trong sample Node: encode key & value truoc, sau do sort
        Map<String, String> encodedSorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            encodedSorted.put(encodedKey, encodedValue);
        }

        // signData la chuoi encodedKey=encodedValue noi bang '&', encode:false giong qs.stringify(..., {encode:false})
        String signData = encodedSorted.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));

        String vnpSecureHash = hmacSHA512(vnpHashSecret, signData);
        encodedSorted.put(URLEncoder.encode("vnp_SecureHash", StandardCharsets.UTF_8), vnpSecureHash);

        // Query string gui sang VNPay dung y chuoi da encode o tren (encode:false)
        String queryUrl = encodedSorted.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));

        String paymentUrl = vnpPayUrl + "?" + queryUrl;

        return ResponseEntity.ok(new VnPayCreatePaymentResponse(paymentUrl));
    }

    /**
     * Endpoint nhan ket qua redirect tu VNPay.
     * Hien tai chi tra ve code don gian de test; co the mo rong luu DB, cap nhat don hang.
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> allParams,
                                            HttpServletRequest httpRequest) {
        Map<String, String> rawParams = new HashMap<>(allParams);
        String secureHash = rawParams.remove("vnp_SecureHash");
        rawParams.remove("vnp_SecureHashType");

        // Encode + sort giong sortObject ben Node
        Map<String, String> encodedSorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : rawParams.entrySet()) {
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            encodedSorted.put(encodedKey, encodedValue);
        }

        String signData = encodedSorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        String expectedHash = hmacSHA512(vnpHashSecret, signData);
        boolean success = expectedHash.equals(secureHash) && "00".equals(rawParams.get("vnp_ResponseCode"));

        // Giữ nguyên toàn bộ query string để frontend hiển thị, thêm status=success|fail
        String originalQuery = httpRequest.getQueryString();
        String statusParam = "status=" + (success ? "success" : "fail");
        String queryWithStatus;
        if (originalQuery == null || originalQuery.isBlank()) {
            queryWithStatus = statusParam;
        } else {
            queryWithStatus = originalQuery + "&" + statusParam;
        }

        String redirectUrl = "http://localhost:5173/order/payment-result" + "?" + queryWithStatus;

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .build();
    }

    private static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            // Truong hop co nhieu IP, lay IP dau tien
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hash.append('0');
                hash.append(hex);
            }
            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot sign data", e);
        }
    }
}
