package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.ProductDTO;
import com.example.Backend_J2EE.dto.ProductSizeDTO;
import com.example.Backend_J2EE.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{productId}/sizes")
    public ResponseEntity<List<ProductSizeDTO>> getProductSizes(@PathVariable Integer productId) {
        return ResponseEntity.ok(productService.getProductSizes(productId));
    }
}

