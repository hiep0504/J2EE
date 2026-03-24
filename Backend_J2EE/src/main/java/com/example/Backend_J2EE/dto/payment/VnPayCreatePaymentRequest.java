package com.example.Backend_J2EE.dto.payment;

import lombok.Data;

@Data
public class VnPayCreatePaymentRequest {

    private Long amount;      // so tien VND
    private String bankCode;  // ma ngan hang (co the de null)
    private String language;  // "vn" hoac "en", co the null
}
