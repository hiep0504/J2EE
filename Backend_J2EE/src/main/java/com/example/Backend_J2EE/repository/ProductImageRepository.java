package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    List<ProductImage> findByProduct_Id(Integer productId);

    Optional<ProductImage> findByProduct_IdAndIsMainTrue(Integer productId);

    @Modifying
    @Transactional
    void deleteByProduct_Id(Integer productId);
}
