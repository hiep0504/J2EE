package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.config.VnPayConfig;
import com.example.Backend_J2EE.entity.Order;
import com.example.Backend_J2EE.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final OrderRepository orderRepository;
    private final VnPayConfig vnPayConfig;

    public PaymentController(OrderRepository orderRepository, VnPayConfig vnPayConfig) {
        this.orderRepository = orderRepository;
        this.vnPayConfig = vnPayConfig;
    }

    @PostMapping("/vnpay/url")
    public Map<String, String> createVnPayUrl(@RequestBody CreateVnPayPaymentRequest request,
                                              HttpServletRequest httpRequest) {
        if (request.orderId == null || request.accountId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId va accountId la bat buoc");
        }

        Order order = orderRepository.findById(request.orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay don hang"));

        Integer ownerId = order.getAccount() != null ? order.getAccount().getId() : null;
        if (ownerId == null || !ownerId.equals(request.accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen thanh toan don hang nay");
        }

        BigDecimal amount = order.getTotalPrice();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tong tien don hang khong hop le");
        }

        String ipAddr = vnPayConfig.getClientIp(httpRequest);
        String orderInfo = request.orderInfo != null && !request.orderInfo.isBlank()
                ? request.orderInfo
                : "Thanh toan don hang #" + order.getId();

        String paymentUrl = vnPayConfig.buildPaymentUrl(
                order.getId(),
                amount,
                ipAddr,
                request.bankCode,
                request.locale,
                orderInfo
        );

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        return response;
    }

    @GetMapping("/vnpay-return")
    public void handleVnPayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, String> vnpParams = new HashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                vnpParams.put(entry.getKey(), entry.getValue()[0]);
            }
        }

        String secureHash = vnpParams.get("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        boolean valid = vnPayConfig.verifySignature(vnpParams, secureHash);
        String responseCode = valid
                ? vnpParams.getOrDefault("vnp_ResponseCode", "99")
                : "97";

        String txnRef = vnpParams.get("vnp_TxnRef");
        Integer orderId = null;
        try {
            if (txnRef != null) {
                orderId = Integer.valueOf(txnRef);
            }
        } catch (NumberFormatException ignored) {
        }

        Order order = null;
        Integer accountId = null;
        BigDecimal amount = null;
        if (orderId != null) {
            order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                accountId = order.getAccount() != null ? order.getAccount().getId() : null;
                amount = order.getTotalPrice();
            }
        }

        String redirectUrl = vnPayConfig.buildFrontendReturnUrl(orderId, accountId, amount, vnpParams, responseCode);
        if (redirectUrl == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Khong the xac dinh URL tra ve frontend");
            return;
        }

        response.sendRedirect(redirectUrl);
    }

    public static class CreateVnPayPaymentRequest {
        public Integer orderId;
        public Integer accountId;
        public String bankCode;
        public String locale;
        public String orderInfo;
    }
}
