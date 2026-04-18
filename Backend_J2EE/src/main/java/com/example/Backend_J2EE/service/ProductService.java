package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.ProductDTO;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.dto.ProductSizeDTO;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.repository.ProductRepository;
import com.example.Backend_J2EE.repository.ProductSizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ProductSizeDTO> getProductSizes(Integer productId) {
        return productSizeRepository.findByProduct_Id(productId)
                .stream()
                .map(this::toProductSizeDTO)
                .toList();
    }

    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .image(product.getImage())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .build();
    }

    private ProductSizeDTO toProductSizeDTO(ProductSize productSize) {
        return new ProductSizeDTO(
                productSize.getId(),
                productSize.getSize().getId(),
                productSize.getSize().getSizeName(),
                productSize.getQuantity()
        );
    }
}
