package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.order.CreateOrderRequest;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.dto.order.OrderProductSizeOptionResponse;
import com.example.Backend_J2EE.dto.order.OrderSummaryResponse;
import com.example.Backend_J2EE.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/options")
    public List<OrderProductSizeOptionResponse> getOrderOptions() {
        return orderService.getOrderOptions();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/history")
    public List<OrderSummaryResponse> getOrderHistory(@RequestParam Integer accountId) {
        return orderService.getOrderHistory(accountId);
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrderDetail(
            @PathVariable Integer orderId,
            @RequestParam Integer accountId
    ) {
        return orderService.getOrderDetail(orderId, accountId);
    }
}
