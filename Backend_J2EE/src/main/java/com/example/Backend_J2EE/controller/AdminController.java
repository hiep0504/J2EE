package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.admin.*;
import com.example.Backend_J2EE.service.AdminService;
import com.example.Backend_J2EE.service.AdminUploadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AdminController {

    private final AdminService adminService;
    private final AdminUploadService adminUploadService;

    public AdminController(AdminService adminService, AdminUploadService adminUploadService) {
        this.adminService = adminService;
        this.adminUploadService = adminUploadService;
    }

    @PostMapping("/uploads/image")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        return adminUploadService.uploadImage(file);
    }

    @GetMapping("/products")
    public AdminPageResponse<AdminProductResponse> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getProducts(keyword, categoryId, page, size);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductResponse createProduct(@RequestBody AdminProductRequest request) {
        return adminService.createProduct(request);
    }

    @PutMapping("/products/{id}")
    public AdminProductResponse updateProduct(@PathVariable Integer id, @RequestBody AdminProductRequest request) {
        return adminService.updateProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Integer id) {
        adminService.deleteProduct(id);
    }

    @GetMapping("/categories")
    public AdminPageResponse<AdminCategoryResponse> getCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getCategories(keyword, page, size);
    }

    @GetMapping("/sizes")
    public java.util.List<AdminSizeResponse> getSizes() {
        return adminService.getSizes();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCategoryResponse createCategory(@RequestBody AdminCategoryRequest request) {
        return adminService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public AdminCategoryResponse updateCategory(@PathVariable Integer id, @RequestBody AdminCategoryRequest request) {
        return adminService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Integer id) {
        adminService.deleteCategory(id);
    }

    @GetMapping("/orders")
    public AdminPageResponse<AdminOrderResponse> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getOrders(keyword, status, page, size);
    }

    @GetMapping("/orders/{id}")
    public AdminOrderDetailResponse getOrderDetail(@PathVariable Integer id) {
        return adminService.getOrderDetail(id);
    }

    @PutMapping("/orders/{id}/status")
    public AdminOrderResponse updateOrderStatus(@PathVariable Integer id, @RequestBody AdminUpdateOrderStatusRequest request) {
        return adminService.updateOrderStatus(id, request);
    }

    @GetMapping("/revenue")
    public AdminRevenueSummaryResponse getRevenueSummary(
            @RequestParam(defaultValue = "6") Integer months,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return adminService.getRevenueSummary(months, fromDate, toDate);
    }

    @GetMapping("/users")
    public AdminPageResponse<AdminUserResponse> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getUsers(keyword, role, page, size);
    }

    @PutMapping("/users/{id}/role")
    public AdminUserResponse updateUserRole(@PathVariable Integer id, @RequestBody AdminUpdateUserRoleRequest request) {
        return adminService.updateUserRole(id, request);
    }

    @PutMapping("/users/{id}/lock")
    public AdminUserResponse updateUserLock(@PathVariable Integer id, @RequestBody AdminUpdateUserLockRequest request) {
        return adminService.updateUserLock(id, request);
    }

    @GetMapping("/reviews")
    public AdminPageResponse<AdminReviewResponse> getReviews(
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getReviews(productId, keyword, page, size);
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Integer id) {
        adminService.deleteReview(id);
    }
}
