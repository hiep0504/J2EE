package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.admin.*;
import com.example.Backend_J2EE.service.AdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AdminController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/uploads/image")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File la bat buoc");
        }
        return Map.of("url", saveImageFile(file));
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

    private String saveImageFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("images");
            Files.createDirectories(uploadPath);

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
            int extIndex = originalFilename.lastIndexOf('.');
            String ext = extIndex >= 0 ? originalFilename.substring(extIndex) : "";
            String filename = UUID.randomUUID() + ext;

            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/images/" + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Khong the luu anh", ex);
        }
    }
}
