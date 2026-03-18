package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.ProductDTO;
import com.example.Backend_J2EE.dto.ProductDetailDTO;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ProductDetailDTO getProductById(Integer id) {
        return productRepository.findById(id)
                .map(this::toDetailDTO)
                .orElse(null);
    }

    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .image(product.getImage())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .build();
    }

    private ProductDetailDTO toDetailDTO(Product product) {
        List<ProductDetailDTO.ProductImageDTO> images = product.getProductImages().stream()
                .map(img -> ProductDetailDTO.ProductImageDTO.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .isMain(img.getIsMain())
                        .build())
                .toList();

        List<ProductDetailDTO.ProductSizeDTO> sizes = product.getProductSizes().stream()
                .map(ps -> ProductDetailDTO.ProductSizeDTO.builder()
                        .id(ps.getId())
                        .sizeName(ps.getSize() != null ? ps.getSize().getSizeName() : null)
                        .quantity(ps.getQuantity())
                        .build())
                .toList();

        return ProductDetailDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .image(product.getImage())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .images(images)
                .sizes(sizes)
                .build();
    }
}
