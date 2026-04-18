package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.payment.VnPayCreatePaymentRequest;
import com.example.Backend_J2EE.dto.payment.VnPayCreatePaymentResponse;
import com.example.Backend_J2EE.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vnpay")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class VnPayController {

    private final VnPayService vnPayService;

    public VnPayController(VnPayService vnPayService) {
        this.vnPayService = vnPayService;
    }

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

        return ResponseEntity.ok(vnPayService.createPayment(request, httpRequest));
    }

    /**
     * Endpoint nhan ket qua redirect tu VNPay.
     * Hien tai chi tra ve code don gian de test; co the mo rong luu DB, cap nhat don hang.
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> allParams,
                                            HttpServletRequest httpRequest) {
        String redirectUrl = vnPayService.buildReturnRedirectUrl(allParams, httpRequest.getQueryString());

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .build();
    }
}
