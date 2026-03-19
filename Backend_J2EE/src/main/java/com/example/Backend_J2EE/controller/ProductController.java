package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.ApiResponse;
import com.example.Backend_J2EE.dto.ProductDTO;
import com.example.Backend_J2EE.dto.ProductDetailDTO;
import com.example.Backend_J2EE.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(productService.searchProducts(keyword)));
    }

    @GetMapping("/category/{id}")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsByCategory(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByCategory(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDTO>> getProductById(@PathVariable Integer id) {
        ProductDetailDTO product = productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.ok(ApiResponse.error("Không tìm thấy sản phẩm với id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
