package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.payment.VnPayCreatePaymentRequest;
import com.example.Backend_J2EE.service.VnPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VnPayControllerTest {

    private VnPayController controller;
    private VnPayService service;

    @BeforeEach
    void setUp() {
        service = new VnPayService();
        controller = new VnPayController(service);
        ReflectionTestUtils.setField(service, "vnpTmnCode", "TMN123");
        ReflectionTestUtils.setField(service, "vnpHashSecret", "secret");
        ReflectionTestUtils.setField(service, "vnpPayUrl", "https://pay.example.com");
        ReflectionTestUtils.setField(service, "vnpReturnUrl", "http://frontend/return");
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173");
    }

    @Test
    void createPaymentBuildsPaymentUrl() {
        VnPayCreatePaymentRequest request = new VnPayCreatePaymentRequest();
        request.setAmount(100000L);
        request.setBankCode("NCB");
        request.setLanguage("vn");

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");

        var response = controller.createPayment(request, servletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getPaymentUrl().startsWith("https://pay.example.com?"));
        assertTrue(response.getBody().getPaymentUrl().contains("vnp_TmnCode=TMN123"));
        assertTrue(response.getBody().getPaymentUrl().contains("vnp_Amount=10000000"));
    }

    @Test
    void createPaymentRejectsZeroAmount() {
        VnPayCreatePaymentRequest request = new VnPayCreatePaymentRequest();
        request.setAmount(0L);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        assertEquals(HttpStatus.BAD_REQUEST, controller.createPayment(request, servletRequest).getStatusCode());
    }

    @Test
    void createPaymentDefaultsLanguageAndUsesRemoteAddressWhenForwardHeaderMissing() {
        VnPayCreatePaymentRequest request = new VnPayCreatePaymentRequest();
        request.setAmount(25000L);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("10.0.0.5");

        var response = controller.createPayment(request, servletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getPaymentUrl().contains("vnp_Locale=vn"));
        assertTrue(response.getBody().getPaymentUrl().contains("vnp_IpAddr=10.0.0.5"));
    }

    @Test
    void returnRedirectsWithSuccessStatusWhenHashMatches() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setQueryString("vnp_Amount=10000000&vnp_ResponseCode=00");

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_ResponseCode", "00");

        String secureHash = sign(params);
        params.put("vnp_SecureHash", secureHash);

        var response = controller.vnpayReturn(params, servletRequest);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst("Location").contains("status=success"));
    }

    @Test
    void returnRedirectsWithFailStatusWhenHashOrCodeDoNotMatch() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setQueryString("vnp_Amount=10000000&vnp_ResponseCode=24");

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_ResponseCode", "24");
        params.put("vnp_SecureHash", "wrong");

        var response = controller.vnpayReturn(params, servletRequest);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst("Location").contains("status=fail"));
    }

    private String sign(Map<String, String> params) {
        Map<String, String> encodedSorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            encodedSorted.put(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8), URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        String signData = encodedSorted.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        try {
            javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(signData.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}