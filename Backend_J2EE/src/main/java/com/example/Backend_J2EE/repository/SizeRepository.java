package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SizeRepository extends JpaRepository<Size, Integer> {

    Optional<Size> findBySizeName(String sizeName);

    boolean existsBySizeName(String sizeName);
}
