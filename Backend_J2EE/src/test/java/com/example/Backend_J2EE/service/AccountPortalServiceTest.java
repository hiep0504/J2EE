package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.account.AccountOwnedReviewResponse;
import com.example.Backend_J2EE.dto.account.AccountPurchasedProductResponse;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.dto.order.OrderItemResponse;
import com.example.Backend_J2EE.dto.order.OrderSummaryResponse;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.Order;
import com.example.Backend_J2EE.entity.OrderDetail;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.entity.ProductImage;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.entity.Review;
import com.example.Backend_J2EE.entity.ReviewMedia;
import com.example.Backend_J2EE.repository.OrderRepository;
import com.example.Backend_J2EE.repository.ProductImageRepository;
import com.example.Backend_J2EE.repository.ProductRepository;
import com.example.Backend_J2EE.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountPortalServiceTest {

    @Mock private AuthService authService;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private FileStorageService fileStorageService;

    private AccountPortalService service;

    @BeforeEach
    void setUp() {
        service = new AccountPortalService(authService, orderRepository, productRepository, productImageRepository, reviewRepository, fileStorageService);
    }

    @Test
    void getMyOrdersMapsOrderSummary() {
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(orderRepository.findByAccount_IdOrderByOrderDateDesc(1)).thenReturn(List.of(
                Order.builder().id(10).totalPrice(new BigDecimal("200")).status(Order.OrderStatus.completed).address("addr").phone("0909").orderDate(LocalDateTime.of(2024, 1, 1, 10, 0)).build()
        ));

        List<OrderSummaryResponse> response = service.getMyOrders(1);

        assertEquals(1, response.size());
        assertEquals(10, response.get(0).getId());
        assertEquals("completed", response.get(0).getStatus());
    }

    @Test
    void getMyOrderDetailRejectsForeignOrder() {
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(orderRepository.findById(10)).thenReturn(Optional.of(Order.builder().id(10).account(Account.builder().id(2).build()).build()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getMyOrderDetail(1, 10));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Ban khong co quyen xem don hang nay"));
    }

    @Test
    void getMyOrderDetailReturnsNotFoundWhenMissingOrder() {
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(orderRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getMyOrderDetail(1, 999));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getMyOrderDetailMapsItemsAndLineTotal() {
        Product product = Product.builder().id(100).name("Shoe").image("thumb.png").build();
        ProductSize size = ProductSize.builder().id(5).product(product).build();
        OrderDetail detail = OrderDetail.builder()
                .id(11)
                .productSize(size)
                .quantity(2)
                .price(new BigDecimal("30"))
                .build();
        Order order = Order.builder()
                .id(10)
                .account(Account.builder().id(1).build())
                .status(Order.OrderStatus.completed)
                .orderDetails(List.of(detail))
                .build();

        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));

        OrderDetailResponse response = service.getMyOrderDetail(1, 10);

        assertEquals(10, response.getId());
        assertEquals(1, response.getItems().size());
        OrderItemResponse item = response.getItems().get(0);
        assertEquals("Shoe", item.getProductName());
        assertEquals("thumb.png", item.getProductImage());
        assertEquals(new BigDecimal("60"), item.getLineTotal());
    }

    @Test
    void getMyPurchasedProductsDedupesAndFlagsReviewed() {
        Account account = Account.builder().id(1).build();
        Product product = Product.builder().id(100).name("Shoe").price(new BigDecimal("100")).build();
        when(authService.getAccountOrThrow(1)).thenReturn(account);
        when(orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(1, Order.OrderStatus.completed)).thenReturn(List.of(
                Order.builder()
                        .id(10)
                        .orderDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                        .status(Order.OrderStatus.completed)
                        .orderDetails(List.of(OrderDetail.builder().productSize(ProductSize.builder().product(product).build()).build()))
                        .build()
        ));
        Review review = Review.builder().id(7).product(product).account(account).rating(5).comment("good").createdAt(LocalDateTime.of(2023, 1, 1, 10, 0)).build();
        when(reviewRepository.findByAccount_IdAndProduct_IdIn(1, List.of(100))).thenReturn(List.of(review));
        when(productImageRepository.findByProduct_IdAndIsMainTrue(100)).thenReturn(Optional.of(ProductImage.builder().imageUrl("img").build()));

        List<AccountPurchasedProductResponse> response = service.getMyPurchasedProducts(1);

        assertEquals(1, response.size());
        assertEquals(100, response.get(0).getProductId());
        assertTrue(response.get(0).isCanReview());
        assertEquals(7, response.get(0).getReview().getId());
    }

        @Test
        void getMyPurchasedProductsReturnsEmptyWhenNoCompletedOrders() {
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(1, Order.OrderStatus.completed)).thenReturn(List.of());

        List<AccountPurchasedProductResponse> response = service.getMyPurchasedProducts(1);

        assertTrue(response.isEmpty());
        verify(reviewRepository, never()).findByAccount_IdAndProduct_IdIn(any(), any());
        }

        @Test
        void getMyPurchasedProductsSetsCanReviewFalseWhenNoRepurchaseAfterReview() {
        Account account = Account.builder().id(1).build();
        Product product = Product.builder().id(100).name("Shoe").price(new BigDecimal("100")).image("shoe.png").build();
        Order completedOrder = Order.builder()
            .id(10)
            .orderDate(LocalDateTime.of(2024, 1, 1, 10, 0))
            .status(Order.OrderStatus.completed)
            .orderDetails(List.of(OrderDetail.builder().productSize(ProductSize.builder().product(product).build()).build()))
            .build();
        Review review = Review.builder()
            .id(7)
            .product(product)
            .account(account)
            .rating(5)
            .comment("good")
            .createdAt(LocalDateTime.of(2024, 2, 1, 10, 0))
            .build();

        when(authService.getAccountOrThrow(1)).thenReturn(account);
        when(orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(1, Order.OrderStatus.completed))
            .thenReturn(List.of(completedOrder));
        when(reviewRepository.findByAccount_IdAndProduct_IdIn(1, List.of(100))).thenReturn(List.of(review));

        List<AccountPurchasedProductResponse> response = service.getMyPurchasedProducts(1);

        assertEquals(1, response.size());
        assertFalse(response.get(0).isCanReview());
        }

        @Test
        void getMyPurchasedProductsUsesFirstNonBlankImageFallback() {
        Product product = Product.builder().id(100).name("Shoe").price(new BigDecimal("100")).image("  ").build();
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(1, Order.OrderStatus.completed)).thenReturn(List.of(
            Order.builder()
                .id(10)
                .orderDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                .status(Order.OrderStatus.completed)
                .orderDetails(List.of(OrderDetail.builder().productSize(ProductSize.builder().product(product).build()).build()))
                .build()
        ));
        when(reviewRepository.findByAccount_IdAndProduct_IdIn(1, List.of(100))).thenReturn(List.of());
        when(productImageRepository.findByProduct_IdAndIsMainTrue(100)).thenReturn(Optional.empty());
        when(productImageRepository.findByProduct_Id(100)).thenReturn(List.of(ProductImage.builder().imageUrl("fallback.png").build()));

        List<AccountPurchasedProductResponse> response = service.getMyPurchasedProducts(1);

        assertEquals(1, response.size());
        assertEquals("fallback.png", response.get(0).getImageUrl());
        assertTrue(response.get(0).isCanReview());
        assertNull(response.get(0).getReview());
        }

    @Test
    void createMyReviewRequiresCompletedPurchase() {
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(1, Order.OrderStatus.completed)).thenReturn(List.of());

        assertThrows(ResponseStatusException.class,
                () -> service.createMyReview(1, 100, 5, "ok", List.<MultipartFile>of(), null));
    }

        @Test
        void createMyReviewRejectsInvalidRating() {
        Product product = Product.builder().id(100).name("Shoe").build();
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(1, Order.OrderStatus.completed)).thenReturn(List.of(
            completedOrderWithProduct(10, LocalDateTime.of(2024, 1, 1, 10, 0), product)
        ));
        when(reviewRepository.findByAccount_IdAndProduct_IdOrderByCreatedAtDesc(1, 100)).thenReturn(List.of());
        when(productRepository.findById(100)).thenReturn(Optional.of(product));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.createMyReview(1, 100, 0, "ok", List.of(), null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void createMyReviewRejectsWhenAlreadyReviewedAndNoRepurchase() {
        Account account = Account.builder().id(1).build();
        Product product = Product.builder().id(100).name("Shoe").build();
        Review latestReview = Review.builder()
            .id(9)
            .account(account)
            .product(product)
            .createdAt(LocalDateTime.of(2024, 2, 1, 10, 0))
            .build();

        when(authService.getAccountOrThrow(1)).thenReturn(account);
        when(orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(1, Order.OrderStatus.completed)).thenReturn(List.of(
            completedOrderWithProduct(10, LocalDateTime.of(2024, 1, 1, 10, 0), product)
        ));
        when(reviewRepository.findByAccount_IdAndProduct_IdOrderByCreatedAtDesc(1, 100)).thenReturn(List.of(latestReview));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.createMyReview(1, 100, 5, "ok", List.of(), null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        void createMyReviewAllowsWhenRepurchasedAfterLatestReview() {
        Account account = Account.builder().id(1).build();
        Product product = Product.builder().id(100).name("Shoe").build();
        Review latestReview = Review.builder()
            .id(9)
            .account(account)
            .product(product)
            .createdAt(LocalDateTime.of(2024, 2, 1, 10, 0))
            .build();

        when(authService.getAccountOrThrow(1)).thenReturn(account);
        when(orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(1, Order.OrderStatus.completed)).thenReturn(List.of(
            completedOrderWithProduct(10, LocalDateTime.of(2024, 3, 1, 10, 0), product)
        ));
        when(reviewRepository.findByAccount_IdAndProduct_IdOrderByCreatedAtDesc(1, 100)).thenReturn(List.of(latestReview));
        when(productRepository.findById(100)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(20);
            return saved;
        });

        AccountOwnedReviewResponse response = service.createMyReview(1, 100, 5, "  ok  ", List.of(), null);

        assertNotNull(response);
        assertEquals(20, response.getId());
        assertEquals(5, response.getRating());
        assertEquals("ok", response.getComment());
        }

        @Test
        void createMyReviewReturnsNotFoundWhenProductMissing() {
        Product purchasedProduct = Product.builder().id(100).name("Shoe").build();
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(1, Order.OrderStatus.completed)).thenReturn(List.of(
            completedOrderWithProduct(10, LocalDateTime.of(2024, 3, 1, 10, 0), purchasedProduct)
        ));
        when(reviewRepository.findByAccount_IdAndProduct_IdOrderByCreatedAtDesc(1, 100)).thenReturn(List.of());
        when(productRepository.findById(100)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.createMyReview(1, 100, 5, "ok", List.of(), null));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void updateMyReviewReturnsNotFoundWhenMissingReview() {
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(reviewRepository.findById(77)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.updateMyReview(1, 77, 5, "ok", List.of(), null, false));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void updateMyReviewRejectsForeignReview() {
        Review review = Review.builder().id(11).account(Account.builder().id(2).build()).reviewMediaList(new ArrayList<>()).build();
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(reviewRepository.findById(11)).thenReturn(Optional.of(review));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.updateMyReview(1, 11, 5, "ok", List.of(), null, false));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void updateMyReviewReplaceMediaClearsExistingMedia() {
        ReviewMedia existingMedia = ReviewMedia.builder()
            .id(1)
            .mediaType(ReviewMedia.MediaType.image)
            .mediaUrl("old.png")
            .build();
        Review review = Review.builder()
            .id(11)
            .account(Account.builder().id(1).build())
            .reviewMediaList(new ArrayList<>(List.of(existingMedia)))
            .build();

        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(reviewRepository.findById(11)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountOwnedReviewResponse response = service.updateMyReview(1, 11, 4, "updated", List.of(), null, true);

        assertNotNull(response);
        assertEquals(0, response.getMedia().size());
        assertEquals("updated", response.getComment());
        }

        @Test
        void updateMyReviewRejectsWhenImageLimitExceeded() {
        List<ReviewMedia> media = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            media.add(ReviewMedia.builder().mediaType(ReviewMedia.MediaType.image).mediaUrl("img" + i + ".png").build());
        }
        Review review = Review.builder()
            .id(11)
            .account(Account.builder().id(1).build())
            .reviewMediaList(media)
            .build();
        MultipartFile newImage = org.mockito.Mockito.mock(MultipartFile.class);
        when(newImage.isEmpty()).thenReturn(false);

        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(reviewRepository.findById(11)).thenReturn(Optional.of(review));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.updateMyReview(1, 11, 4, "updated", List.of(newImage), null, false));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        void deleteMyReviewRejectsForeignReview() {
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(reviewRepository.findById(11)).thenReturn(Optional.of(
            Review.builder().id(11).account(Account.builder().id(2).build()).build()
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.deleteMyReview(1, 11));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void deleteMyReviewDeletesOwnedReview() {
        Review review = Review.builder().id(11).account(Account.builder().id(1).build()).build();
        when(authService.getAccountOrThrow(1)).thenReturn(Account.builder().id(1).build());
        when(reviewRepository.findById(11)).thenReturn(Optional.of(review));

        service.deleteMyReview(1, 11);

        verify(reviewRepository).delete(review);
        }

        private Order completedOrderWithProduct(Integer orderId, LocalDateTime orderDate, Product product) {
        return Order.builder()
            .id(orderId)
            .status(Order.OrderStatus.completed)
            .orderDate(orderDate)
            .orderDetails(List.of(
                OrderDetail.builder()
                    .id(orderId + 1)
                    .productSize(ProductSize.builder().id(orderId + 10).product(product).build())
                    .quantity(1)
                    .price(new BigDecimal("10"))
                    .build()
            ))
            .build();
        }
}