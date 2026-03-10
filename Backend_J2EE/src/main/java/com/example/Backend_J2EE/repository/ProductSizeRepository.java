package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Integer> {

    List<ProductSize> findByProduct_Id(Integer productId);

    Optional<ProductSize> findByProduct_IdAndSize_Id(Integer productId, Integer sizeId);

    boolean existsByProduct_IdAndSize_Id(Integer productId, Integer sizeId);
}
