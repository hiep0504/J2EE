package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.order.CreateOrderItemRequest;
import com.example.Backend_J2EE.dto.order.CreateOrderRequest;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.dto.order.OrderItemResponse;
import com.example.Backend_J2EE.dto.order.OrderProductSizeOptionResponse;
import com.example.Backend_J2EE.dto.order.OrderSummaryResponse;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.Order;
import com.example.Backend_J2EE.entity.OrderDetail;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.repository.OrderRepository;
import com.example.Backend_J2EE.repository.ProductSizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final ProductSizeRepository productSizeRepository;

    public List<OrderProductSizeOptionResponse> getOrderOptions() {
        return productSizeRepository.findAll().stream().map(item -> new OrderProductSizeOptionResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getSize() != null ? item.getSize().getSizeName() : null,
                item.getQuantity(),
                item.getProduct() != null ? item.getProduct().getPrice() : null
        )).toList();
    }

    @Transactional
    public OrderDetailResponse createOrder(CreateOrderRequest request) {
        validateCreateOrderRequest(request);

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay tai khoan"));

        Order order = Order.builder()
                .account(account)
                .status(Order.OrderStatus.pending)
                .address(request.getAddress().trim())
                .phone(request.getPhone().trim())
                .totalPrice(BigDecimal.ZERO)
                .build();

        List<OrderDetail> orderDetails = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            ProductSize productSize = productSizeRepository.findById(itemRequest.getProductSizeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay bien the san pham"));

            Integer stock = productSize.getQuantity() == null ? 0 : productSize.getQuantity();
            Integer quantity = itemRequest.getQuantity();
            if (quantity == null || quantity <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So luong phai lon hon 0");
            }
            if (quantity > stock) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So luong vuot ton kho cho san pham " + productSize.getId());
            }

            BigDecimal unitPrice = productSize.getProduct() != null && productSize.getProduct().getPrice() != null
                    ? productSize.getProduct().getPrice()
                    : BigDecimal.ZERO;

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .productSize(productSize)
                    .quantity(quantity)
                    .price(unitPrice)
                    .build();
            orderDetails.add(detail);

            productSize.setQuantity(stock - quantity);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            total = total.add(lineTotal);
        }

        order.setOrderDetails(orderDetails);
        order.setTotalPrice(total);

        Order saved = orderRepository.save(order);
        return toOrderDetailResponse(saved);
    }

    public List<OrderSummaryResponse> getOrderHistory(Integer accountId) {
        return orderRepository.findByAccount_IdOrderByOrderDateDesc(accountId)
                .stream()
                .map(this::toOrderSummaryResponse)
                .toList();
    }

    public OrderDetailResponse getOrderDetail(Integer orderId, Integer accountId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay don hang"));

        Integer ownerId = order.getAccount() != null ? order.getAccount().getId() : null;
        if (ownerId == null || !ownerId.equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem don hang nay");
        }

        return toOrderDetailResponse(order);
    }

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request.getAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId la bat buoc");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia chi giao hang la bat buoc");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So dien thoai la bat buoc");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Don hang phai co it nhat 1 san pham");
        }

        long invalid = request.getItems().stream()
                .filter(item -> item.getProductSizeId() == null || item.getQuantity() == null)
                .count();
        if (invalid > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thong tin san pham trong don hang khong hop le");
        }
    }

    private OrderSummaryResponse toOrderSummaryResponse(Order order) {
        OrderSummaryResponse response = new OrderSummaryResponse();
        response.setId(order.getId());
        response.setAccountId(order.getAccount() != null ? order.getAccount().getId() : null);
        response.setOrderDate(order.getOrderDate());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        response.setAddress(order.getAddress());
        response.setPhone(order.getPhone());
        return response;
    }

    private OrderDetailResponse toOrderDetailResponse(Order order) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setId(order.getId());
        response.setAccountId(order.getAccount() != null ? order.getAccount().getId() : null);
        response.setOrderDate(order.getOrderDate());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        response.setAddress(order.getAddress());
        response.setPhone(order.getPhone());

        List<OrderItemResponse> items = order.getOrderDetails() == null
                ? List.of()
                : order.getOrderDetails().stream().map(detail -> {
                    ProductSize ps = detail.getProductSize();
                    BigDecimal lineTotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
                    return new OrderItemResponse(
                            detail.getId(),
                            ps != null ? ps.getId() : null,
                            ps != null && ps.getProduct() != null ? ps.getProduct().getName() : null,
                            ps != null && ps.getProduct() != null ? ps.getProduct().getImage() : "",
                            ps != null && ps.getSize() != null ? ps.getSize().getSizeName() : null,
                            detail.getQuantity(),
                            detail.getPrice(),
                            lineTotal
                    );
                }).toList();

        response.setItems(items);
        return response;
    }
}
