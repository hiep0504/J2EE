package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.admin.AdminCategoryRequest;
import com.example.Backend_J2EE.dto.admin.AdminCategoryResponse;
import com.example.Backend_J2EE.dto.admin.AdminOrderDetailResponse;
import com.example.Backend_J2EE.dto.admin.AdminOrderResponse;
import com.example.Backend_J2EE.dto.admin.AdminPageResponse;
import com.example.Backend_J2EE.dto.admin.AdminProductRequest;
import com.example.Backend_J2EE.dto.admin.AdminProductResponse;
import com.example.Backend_J2EE.dto.admin.AdminRevenueSummaryResponse;
import com.example.Backend_J2EE.dto.admin.AdminReviewResponse;
import com.example.Backend_J2EE.dto.admin.AdminSizeResponse;
import com.example.Backend_J2EE.dto.admin.AdminUpdateOrderStatusRequest;
import com.example.Backend_J2EE.dto.admin.AdminUpdateUserLockRequest;
import com.example.Backend_J2EE.dto.admin.AdminUpdateUserRoleRequest;
import com.example.Backend_J2EE.dto.admin.AdminUserResponse;
import com.example.Backend_J2EE.service.AdminService;
import com.example.Backend_J2EE.service.AdminUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private AdminUploadService adminUploadService;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(adminService, adminUploadService);
    }

    @Test
    void uploadImageDelegatesToUploadService() {
        MockMultipartFile file = new MockMultipartFile("file", "shoe.png", "image/png", new byte[] {1, 2, 3});
        when(adminUploadService.uploadImage(file)).thenReturn(Map.of("url", "/uploads/images/mock.png"));

        Map<String, String> response = controller.uploadImage(file);

        assertEquals("/uploads/images/mock.png", response.get("url"));
        verify(adminUploadService).uploadImage(file);
    }

    @Test
    void uploadImageRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);
        when(adminUploadService.uploadImage(file)).thenThrow(new IllegalArgumentException("File la bat buoc"));

        assertThrows(IllegalArgumentException.class, () -> controller.uploadImage(file));
    }

    @Test
    void allEndpointsDelegateToService() {
        AdminProductResponse productResponse = new AdminProductResponse(1, "Product", BigDecimal.TEN, "desc", "img", 2, "Shoes", LocalDateTime.now(), List.of(), List.of());
        AdminCategoryResponse categoryResponse = new AdminCategoryResponse(1, "Shoes", "desc", 3L);
        AdminOrderDetailResponse orderDetailResponse = new AdminOrderDetailResponse(1, 2, "alice", BigDecimal.TEN, "pending", "addr", "phone", LocalDateTime.now(), List.of());
        AdminOrderResponse orderResponse = new AdminOrderResponse(1, 2, "alice", BigDecimal.TEN, "pending", "addr", "phone", LocalDateTime.now());
        AdminRevenueSummaryResponse revenueResponse = new AdminRevenueSummaryResponse(6, BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());
        AdminUserResponse userResponse = new AdminUserResponse(1, "alice", "alice@example.com", "0900", "user", false, LocalDateTime.now());
        AdminReviewResponse reviewResponse = new AdminReviewResponse(1, 1, "Product", 2, "alice", 5, "good", LocalDateTime.now(), List.of());

        when(adminService.getProducts(null, null, 0, 10)).thenReturn(new AdminPageResponse<>(List.of(productResponse), 1, 0, 10, 1));
        when(adminService.createProduct(any())).thenReturn(productResponse);
        when(adminService.updateProduct(eq(1), any())).thenReturn(productResponse);
        when(adminService.getCategories(null, 0, 10)).thenReturn(new AdminPageResponse<>(List.of(categoryResponse), 1, 0, 10, 1));
        when(adminService.getSizes()).thenReturn(List.of(new AdminSizeResponse(1, "42")));
        when(adminService.createCategory(any())).thenReturn(categoryResponse);
        when(adminService.updateCategory(eq(2), any())).thenReturn(categoryResponse);
        when(adminService.getOrders(null, null, 0, 10)).thenReturn(new AdminPageResponse<>(List.of(orderResponse), 1, 0, 10, 1));
        when(adminService.getOrderDetail(3)).thenReturn(orderDetailResponse);
        when(adminService.updateOrderStatus(eq(4), any())).thenReturn(orderResponse);
        when(adminService.getRevenueSummary(6, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))).thenReturn(revenueResponse);
        when(adminService.getUsers(null, null, 0, 10)).thenReturn(new AdminPageResponse<>(List.of(userResponse), 1, 0, 10, 1));
        when(adminService.updateUserRole(eq(5), any())).thenReturn(userResponse);
        when(adminService.updateUserLock(eq(6), any())).thenReturn(userResponse);
        when(adminService.getReviews(null, null, 0, 10)).thenReturn(new AdminPageResponse<>(List.of(reviewResponse), 1, 0, 10, 1));

        assertEquals(1, controller.getProducts(null, null, 0, 10).getItems().size());
        assertEquals(1, controller.getCategories(null, 0, 10).getItems().size());
        assertEquals(1, controller.getSizes().size());
        assertEquals(1, controller.getOrders(null, null, 0, 10).getItems().size());
        assertEquals(1, controller.getUsers(null, null, 0, 10).getItems().size());
        assertEquals(1, controller.getReviews(null, null, 0, 10).getItems().size());
        assertEquals(6, controller.getRevenueSummary(6, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)).getMonths());
        assertEquals(1, controller.getOrderDetail(3).getId());

        controller.createProduct(new AdminProductRequest());
        controller.updateProduct(1, new AdminProductRequest());
        controller.deleteProduct(1);
        controller.createCategory(new AdminCategoryRequest());
        controller.updateCategory(2, new AdminCategoryRequest());
        controller.deleteCategory(2);
        controller.updateOrderStatus(4, new AdminUpdateOrderStatusRequest());
        controller.updateUserRole(5, new AdminUpdateUserRoleRequest());
        controller.updateUserLock(6, new AdminUpdateUserLockRequest());
        controller.deleteReview(7);

        verify(adminService).getProducts(null, null, 0, 10);
        verify(adminService).createProduct(any());
        verify(adminService).updateProduct(eq(1), any());
        verify(adminService).deleteProduct(1);
        verify(adminService).getCategories(null, 0, 10);
        verify(adminService).getSizes();
        verify(adminService).createCategory(any());
        verify(adminService).updateCategory(eq(2), any());
        verify(adminService).deleteCategory(2);
        verify(adminService).getOrders(null, null, 0, 10);
        verify(adminService).getOrderDetail(3);
        verify(adminService).updateOrderStatus(eq(4), any());
        verify(adminService).getRevenueSummary(6, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        verify(adminService).getUsers(null, null, 0, 10);
        verify(adminService).updateUserRole(eq(5), any());
        verify(adminService).updateUserLock(eq(6), any());
        verify(adminService).getReviews(null, null, 0, 10);
        verify(adminService).deleteReview(7);
    }
}
