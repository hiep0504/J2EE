package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.payment.VnPayCreatePaymentRequest;
import com.example.Backend_J2EE.dto.payment.VnPayCreatePaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class VnPayService {

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.pay-url}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public VnPayCreatePaymentResponse createPayment(VnPayCreatePaymentRequest request, HttpServletRequest httpRequest) {
        long amount = request.getAmount() != null ? request.getAmount() : 0L;

        String bankCode = request.getBankCode();
        String language = (request.getLanguage() == null || request.getLanguage().isBlank()) ? "vn" : request.getLanguage();

        LocalDateTime now = LocalDateTime.now();
        String createDate = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String orderId = now.format(DateTimeFormatter.ofPattern("ddHHmmss"));

        String ipAddr = getIpAddress(httpRequest);

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

        Map<String, String> encodedSorted = encodeAndSort(vnpParams);
        String signData = joinQueryParams(encodedSorted);

        String vnpSecureHash = hmacSHA512(vnpHashSecret, signData);
        encodedSorted.put(URLEncoder.encode("vnp_SecureHash", StandardCharsets.UTF_8), vnpSecureHash);

        String paymentUrl = vnpPayUrl + "?" + joinQueryParams(encodedSorted);
        return new VnPayCreatePaymentResponse(paymentUrl);
    }

    public String buildReturnRedirectUrl(Map<String, String> allParams, String originalQuery) {
        Map<String, String> rawParams = new HashMap<>(allParams);
        String secureHash = rawParams.remove("vnp_SecureHash");
        rawParams.remove("vnp_SecureHashType");

        Map<String, String> encodedSorted = encodeAndSort(rawParams);
        String signData = joinQueryParams(encodedSorted);

        String expectedHash = hmacSHA512(vnpHashSecret, signData);
        boolean success = expectedHash.equals(secureHash) && "00".equals(rawParams.get("vnp_ResponseCode"));

        String statusParam = "status=" + (success ? "success" : "fail");
        String queryWithStatus;
        if (originalQuery == null || originalQuery.isBlank()) {
            queryWithStatus = statusParam;
        } else {
            queryWithStatus = originalQuery + "&" + statusParam;
        }

        return frontendBaseUrl + "/order/payment-result?" + queryWithStatus;
    }

    private static Map<String, String> encodeAndSort(Map<String, String> params) {
        Map<String, String> encodedSorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            encodedSorted.put(encodedKey, encodedValue);
        }
        return encodedSorted;
    }

    private static String joinQueryParams(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
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
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot sign data", e);
        }
    }
}
