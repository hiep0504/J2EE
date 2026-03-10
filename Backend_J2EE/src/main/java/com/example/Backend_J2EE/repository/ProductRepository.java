package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByCategory_Id(Integer categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategory_IdOrderByCreatedAtDesc(Integer categoryId);

    List<Product> findAllByOrderByCreatedAtDesc();
}
