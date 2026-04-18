package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.admin.AdminCategoryRequest;
import com.example.Backend_J2EE.dto.admin.AdminCategoryResponse;
import com.example.Backend_J2EE.dto.admin.AdminPageResponse;
import com.example.Backend_J2EE.dto.admin.AdminProductImageRequest;
import com.example.Backend_J2EE.dto.admin.AdminProductRequest;
import com.example.Backend_J2EE.dto.admin.AdminProductResponse;
import com.example.Backend_J2EE.dto.admin.AdminReviewResponse;
import com.example.Backend_J2EE.dto.admin.AdminUpdateOrderStatusRequest;
import com.example.Backend_J2EE.dto.admin.AdminUpdateUserLockRequest;
import com.example.Backend_J2EE.dto.admin.AdminUpdateUserRoleRequest;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.Category;
import com.example.Backend_J2EE.entity.Order;
import com.example.Backend_J2EE.entity.OrderDetail;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.entity.ProductImage;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.entity.Review;
import com.example.Backend_J2EE.entity.Size;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.repository.CategoryRepository;
import com.example.Backend_J2EE.repository.OrderRepository;
import com.example.Backend_J2EE.repository.ProductImageRepository;
import com.example.Backend_J2EE.repository.ProductRepository;
import com.example.Backend_J2EE.repository.ProductSizeRepository;
import com.example.Backend_J2EE.repository.ReviewRepository;
import com.example.Backend_J2EE.repository.SizeRepository;
import com.example.Backend_J2EE.service.rag.RagIndexMaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductSizeRepository productSizeRepository;
    @Mock private SizeRepository sizeRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private RagIndexMaintenanceService ragIndexMaintenanceService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                productRepository,
                categoryRepository,
                productImageRepository,
                productSizeRepository,
                sizeRepository,
                orderRepository,
                accountRepository,
                reviewRepository,
                ragIndexMaintenanceService
        );
    }

    @Test
    void getCategoriesSortsNewestFirst() {
        Category first = Category.builder().id(1).name("First").description("A").build();
        Category second = Category.builder().id(2).name("Second").description("B").build();
        when(categoryRepository.findAll()).thenReturn(List.of(first, second));

        AdminPageResponse<AdminCategoryResponse> response = adminService.getCategories(null, 0, 10);

        assertEquals(2, response.getTotal());
        assertEquals(2, response.getItems().get(0).getId());
        assertEquals(1, response.getItems().get(1).getId());
    }

    @Test
    void createCategoryPersistsAndReturnsResponse() {
        AdminCategoryRequest request = new AdminCategoryRequest();
        request.setName("Boots");
        request.setDescription("Football boots");

        when(categoryRepository.existsByName("Boots")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(9);
            return category;
        });

        AdminCategoryResponse response = adminService.createCategory(request);

        assertEquals(9, response.getId());
        assertEquals("Boots", response.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        AdminCategoryRequest request = new AdminCategoryRequest();
        request.setName("Boots");

        when(categoryRepository.existsByName("Boots")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.createCategory(request));

        assertEquals(409, ex.getStatusCode().value());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createProductPersistsAndMarksRagDirty() {
        Category category = Category.builder().id(5).name("Shoes").build();
        AdminProductRequest request = new AdminProductRequest();
        request.setName("Shoe A");
        request.setPrice(new BigDecimal("199000"));
        request.setDescription("Desc");
        request.setImage("/img/main.png");
        request.setCategoryId(5);
        request.setImages(List.<AdminProductImageRequest>of());
        request.setSizes(List.of());

        when(categoryRepository.findById(5)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(20);
            product.setCategory(category);
            product.setProductImages(List.of());
            product.setProductSizes(List.of());
            return product;
        });
        when(productRepository.findById(20)).thenReturn(Optional.of(Product.builder().id(20).name("Shoe A").price(new BigDecimal("199000")).category(category).build()));

        AdminProductResponse response = adminService.createProduct(request);

        assertEquals(20, response.getId());
        assertEquals("Shoe A", response.getName());
        verify(ragIndexMaintenanceService).markDirty();
    }

    @Test
    void createProductRejectsMissingCategory() {
        AdminProductRequest request = baseProductRequest();
        when(categoryRepository.findById(5)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.createProduct(request));

        assertEquals(404, ex.getStatusCode().value());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductRejectsInvalidSizeId() {
        Category category = Category.builder().id(5).name("Shoes").build();
        AdminProductRequest request = baseProductRequest();
        com.example.Backend_J2EE.dto.admin.AdminProductSizeRequest sizeRequest = new com.example.Backend_J2EE.dto.admin.AdminProductSizeRequest();
        sizeRequest.setSizeId(999);
        sizeRequest.setQuantity(3);
        request.setSizes(List.of(sizeRequest));

        when(categoryRepository.findById(5)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(20);
            return product;
        });
        when(productSizeRepository.findByProduct_Id(20)).thenReturn(List.of());
        when(sizeRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.createProduct(request));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void updateProductRejectsNotFoundProduct() {
        AdminProductRequest request = baseProductRequest();
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.updateProduct(99, request));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void deleteProductDeletesAndMarksRagDirty() {
        Product product = Product.builder().id(50).build();
        when(productRepository.findById(50)).thenReturn(Optional.of(product));

        adminService.deleteProduct(50);

        verify(productRepository).delete(product);
        verify(ragIndexMaintenanceService).markDirty();
    }

    @Test
    void deleteProductRejectsWhenMissing() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.deleteProduct(999));

        assertEquals(404, ex.getStatusCode().value());
        verify(ragIndexMaintenanceService, never()).markDirty();
    }

    @Test
    void updateUserRoleRejectsInvalidRole() {
        when(accountRepository.findById(7)).thenReturn(Optional.of(Account.builder().id(7).build()));
        AdminUpdateUserRoleRequest request = new AdminUpdateUserRoleRequest();
        request.setRole("manager");

        assertThrows(ResponseStatusException.class, () -> adminService.updateUserRole(7, request));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateUserRoleAcceptsUppercaseRoleAndSaves() {
        Account account = Account.builder().id(7).role(Account.Role.user).build();
        when(accountRepository.findById(7)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminUpdateUserRoleRequest request = new AdminUpdateUserRoleRequest();
        request.setRole("ADMIN");

        assertEquals("admin", adminService.updateUserRole(7, request).getRole());
        assertEquals(Account.Role.admin, account.getRole());
    }

    @Test
    void updateUserRoleRejectsMissingRole() {
        AdminUpdateUserRoleRequest request = new AdminUpdateUserRoleRequest();
        request.setRole("   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.updateUserRole(1, request));

        assertEquals(400, ex.getStatusCode().value());
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void updateUserRoleRejectsUnknownUser() {
        when(accountRepository.findById(777)).thenReturn(Optional.empty());
        AdminUpdateUserRoleRequest request = new AdminUpdateUserRoleRequest();
        request.setRole("user");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.updateUserRole(777, request));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void updateUserLockPersistsChange() {
        Account account = Account.builder().id(8).username("locked").locked(false).build();
        when(accountRepository.findById(8)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUpdateUserLockRequest request = new AdminUpdateUserLockRequest();
        request.setLocked(true);

        adminService.updateUserLock(8, request);

        assertEquals(true, account.getLocked());
    }

    @Test
    void updateUserLockRejectsMissingLockedValue() {
        AdminUpdateUserLockRequest request = new AdminUpdateUserLockRequest();
        request.setLocked(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.updateUserLock(8, request));

        assertEquals(400, ex.getStatusCode().value());
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void getReviewsFiltersByProduct() {
        Review review = Review.builder()
                .id(1)
                .product(Product.builder().id(3).name("Ball").build())
                .account(Account.builder().id(4).username("alice").build())
                .rating(5)
                .comment("Great")
                .createdAt(LocalDateTime.now())
                .build();
        when(reviewRepository.findAll()).thenReturn(List.of(review));

        AdminPageResponse<AdminReviewResponse> response = adminService.getReviews(3, null, 0, 10);

        assertEquals(1, response.getTotal());
        assertEquals(1, response.getItems().size());
        assertEquals("Ball", response.getItems().get(0).getProductName());
    }

    @Test
    void deleteReviewDeletesExistingReview() {
        Review review = Review.builder().id(5).build();
        when(reviewRepository.findById(5)).thenReturn(Optional.of(review));

        adminService.deleteReview(5);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReviewRejectsWhenMissing() {
        when(reviewRepository.findById(404)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.deleteReview(404));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void updateOrderStatusChangesStatus() {
        Order order = Order.builder().id(50).status(Order.OrderStatus.pending).build();
        when(orderRepository.findById(50)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUpdateOrderStatusRequest request = new AdminUpdateOrderStatusRequest();
        request.setStatus("completed");

        assertEquals("completed", adminService.updateOrderStatus(50, request).getStatus());
        assertEquals(Order.OrderStatus.completed, order.getStatus());
    }

    @Test
    void updateOrderStatusRejectsBlankStatus() {
        AdminUpdateOrderStatusRequest request = new AdminUpdateOrderStatusRequest();
        request.setStatus("   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.updateOrderStatus(50, request));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void updateOrderStatusRejectsInvalidStatus() {
        Order order = Order.builder().id(50).status(Order.OrderStatus.pending).build();
        when(orderRepository.findById(50)).thenReturn(Optional.of(order));

        AdminUpdateOrderStatusRequest request = new AdminUpdateOrderStatusRequest();
        request.setStatus("done");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.updateOrderStatus(50, request));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void getOrderDetailMapsLineTotalAndFallbackImage() {
        Product product = Product.builder().id(10).name("Shoe X").image(" ").build();
        ProductSize productSize = ProductSize.builder().id(6).product(product).size(Size.builder().id(2).sizeName("42").build()).build();
        OrderDetail detail = OrderDetail.builder()
                .id(1)
                .productSize(productSize)
                .quantity(2)
                .price(new BigDecimal("150"))
                .build();
        Order order = Order.builder()
                .id(33)
                .status(Order.OrderStatus.confirmed)
                .orderDetails(List.of(detail))
                .build();
        when(orderRepository.findById(33)).thenReturn(Optional.of(order));
        when(productImageRepository.findByProduct_IdAndIsMainTrue(10)).thenReturn(Optional.of(
                ProductImage.builder().id(8).imageUrl("/img/fallback.png").isMain(true).build()
        ));

        var response = adminService.getOrderDetail(33);

        assertEquals(33, response.getId());
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("300"), response.getItems().get(0).getLineTotal());
        assertEquals("/img/fallback.png", response.getItems().get(0).getProductImage());
    }

    @Test
    void getOrderDetailRejectsMissingOrder() {
        when(orderRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.getOrderDetail(999));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void deleteCategoryRejectsWhenCategoryContainsProducts() {
        Category category = Category.builder().id(2).name("Shoes").products(List.of(Product.builder().id(1).build())).build();
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.deleteCategory(2));

        assertEquals(400, ex.getStatusCode().value());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategoryDeletesWhenNoProducts() {
        Category category = Category.builder().id(2).name("Shoes").products(new ArrayList<>()).build();
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));

        adminService.deleteCategory(2);

        verify(categoryRepository).delete(category);
    }

    @Test
    void updateCategoryRejectsDuplicateName() {
        Category category = Category.builder().id(2).name("Old").description("old").build();
        when(categoryRepository.findById(2)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("New")).thenReturn(true);

        AdminCategoryRequest request = new AdminCategoryRequest();
        request.setName("New");
        request.setDescription("desc");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> adminService.updateCategory(2, request));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void getUsersFiltersByRoleAndKeyword() {
        Account admin = Account.builder().id(1).username("alice").email("a@mail.com").role(Account.Role.admin).locked(false).build();
        Account user = Account.builder().id(2).username("bob").email("b@mail.com").role(Account.Role.user).locked(false).build();
        when(accountRepository.findAll()).thenReturn(List.of(admin, user));

        var response = adminService.getUsers("ali", "admin", 0, 10);

        assertEquals(1, response.getTotal());
        assertEquals("alice", response.getItems().get(0).getUsername());
        assertEquals("admin", response.getItems().get(0).getRole());
    }

    @Test
    void getProductsFiltersByKeywordAndCategory() {
        Category shoes = Category.builder().id(5).name("Shoes").build();
        Category balls = Category.builder().id(9).name("Balls").build();
        Product matched = Product.builder().id(1).name("Blue Shoe").description("Sport").category(shoes).build();
        Product ignored = Product.builder().id(2).name("Football").description("Ball").category(balls).build();
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(matched, ignored));

        var response = adminService.getProducts("shoe", 5, 0, 10);

        assertEquals(1, response.getTotal());
        assertEquals("Blue Shoe", response.getItems().get(0).getName());
    }

    private AdminProductRequest baseProductRequest() {
        AdminProductRequest request = new AdminProductRequest();
        request.setName("Shoe A");
        request.setPrice(new BigDecimal("199000"));
        request.setDescription("Desc");
        request.setImage("/img/main.png");
        request.setCategoryId(5);
        request.setImages(List.of());
        request.setSizes(List.of());
        return request;
    }
}