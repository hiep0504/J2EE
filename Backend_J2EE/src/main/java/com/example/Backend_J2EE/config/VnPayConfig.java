package com.example.Backend_J2EE.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
public class VnPayConfig {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.frontend-return-url}")
    private String frontendReturnUrl;

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    public String buildPaymentUrl(Integer orderId,
                                  BigDecimal amount,
                                  String ipAddr,
                                  String bankCode,
                                  String locale,
                                  String orderInfo) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String createDate = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode);
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", String.valueOf(orderId));
        vnpParams.put("vnp_OrderInfo", orderInfo != null && !orderInfo.isBlank()
                ? orderInfo
                : "Thanh toan don hang #" + orderId);
        vnpParams.put("vnp_OrderType", "other");

        BigDecimal amountTimes100 = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);
        vnpParams.put("vnp_Amount", amountTimes100.toPlainString());

        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", ipAddr != null ? ipAddr : "127.0.0.1");
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_Locale", (locale != null && !locale.isBlank()) ? locale : "vn");

        if (bankCode != null && !bankCode.isBlank()) {
            vnpParams.put("vnp_BankCode", bankCode);
        }

        // Theo demo VNPay: cần ký trên giá trị đã được URL encode
        Map<String, String> encodedParams = encodeParams(vnpParams);
        String signData = buildSignData(encodedParams);
        String secureHash = hmacSHA512(hashSecret, signData);
        encodedParams.put("vnp_SecureHash", secureHash);

        String query = buildQueryString(encodedParams);
        return payUrl + "?" + query;
    }

    public boolean verifySignature(Map<String, String> vnpParams, String secureHash) {
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }
        Map<String, String> sorted = new TreeMap<>(vnpParams);
        Map<String, String> encoded = encodeParams(sorted);
        String signData = buildSignData(encoded);
        String calculated = hmacSHA512(hashSecret, signData);
        return secureHash.equalsIgnoreCase(calculated);
    }

    public String buildFrontendReturnUrl(Integer orderId,
                                         Integer accountId,
                                         BigDecimal amount,
                                         Map<String, String> vnpParams,
                                         String responseCode) {
        if (frontendReturnUrl == null || frontendReturnUrl.isBlank()) {
            return null;
        }
        String base = frontendReturnUrl;
        String separator = base.contains("?") ? "&" : "?";

        String code = responseCode != null ? responseCode : vnpParams.getOrDefault("vnp_ResponseCode", "99");
        String status = "00".equals(code) ? "success" : "failed";

        StringJoiner joiner = new StringJoiner("&");
        if (orderId != null) {
            joiner.add("orderId=" + urlEncode(orderId.toString()));
        }
        if (accountId != null) {
            joiner.add("accountId=" + urlEncode(accountId.toString()));
        }
        if (amount != null) {
            joiner.add("amount=" + urlEncode(amount.toPlainString()));
        }

        String bankCode = vnpParams.get("vnp_BankCode");
        if (bankCode != null) {
            joiner.add("bankCode=" + urlEncode(bankCode));
        }
        String transactionNo = vnpParams.get("vnp_TransactionNo");
        if (transactionNo != null) {
            joiner.add("transactionNo=" + urlEncode(transactionNo));
        }
        String transactionStatus = vnpParams.get("vnp_TransactionStatus");
        if (transactionStatus != null) {
            joiner.add("transactionStatus=" + urlEncode(transactionStatus));
        }
        String payDate = vnpParams.get("vnp_PayDate");
        if (payDate != null) {
            joiner.add("payDate=" + urlEncode(payDate));
        }

        joiner.add("code=" + urlEncode(code));
        joiner.add("status=" + urlEncode(status));

        return base + separator + joiner.toString();
    }

    private Map<String, String> encodeParams(Map<String, String> params) {
        Map<String, String> encoded = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            encoded.put(entry.getKey(), urlEncode(value));
        }
        return encoded;
    }

    private String buildSignData(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        return joiner.toString();
    }

    private String buildQueryString(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        return joiner.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create HMAC SHA512 signature", e);
        }
    }
}
