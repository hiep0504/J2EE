package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.order.CreateOrderItemRequest;
import com.example.Backend_J2EE.dto.order.CreateOrderRequest;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.dto.order.OrderProductSizeOptionResponse;
import com.example.Backend_J2EE.dto.order.OrderSummaryResponse;
import com.example.Backend_J2EE.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void getOrderOptionsMapsProductSizes() {
        OrderProductSizeOptionResponse option = new OrderProductSizeOptionResponse(3, 1, "Running Shoe", "42", 7, new BigDecimal("99.99"));
        when(orderService.getOrderOptions()).thenReturn(List.of(option));

        List<OrderProductSizeOptionResponse> result = orderController.getOrderOptions();

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getProductSizeId());
        assertEquals(1, result.get(0).getProductId());
        assertEquals("Running Shoe", result.get(0).getProductName());
        assertEquals("42", result.get(0).getSizeName());
        assertEquals(7, result.get(0).getStock());
        verify(orderService).getOrderOptions();
    }

    @Test
    void createOrderDelegatesToService() {
        OrderDetailResponse serviceResponse = new OrderDetailResponse();
        serviceResponse.setId(100);
        serviceResponse.setAccountId(5);
        serviceResponse.setTotalPrice(new BigDecimal("200.00"));

        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductSizeId(9);
        item.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setAccountId(5);
        request.setAddress("123 Street");
        request.setPhone("0900000000");
        request.getItems().add(item);

        when(orderService.createOrder(request)).thenReturn(serviceResponse);

        OrderDetailResponse response = orderController.createOrder(request);

        assertSame(serviceResponse, response);
        verify(orderService).createOrder(request);
    }

    @Test
    void getOrderHistoryAndDetailDelegateToService() {
        OrderSummaryResponse summary = new OrderSummaryResponse();
        summary.setId(11);
        OrderDetailResponse detail = new OrderDetailResponse();
        detail.setId(11);

        when(orderService.getOrderHistory(5)).thenReturn(List.of(summary));
        when(orderService.getOrderDetail(11, 5)).thenReturn(detail);

        assertEquals(1, orderController.getOrderHistory(5).size());
        assertEquals(11, orderController.getOrderDetail(11, 5).getId());
        verify(orderService).getOrderHistory(5);
        verify(orderService).getOrderDetail(11, 5);
    }
}
