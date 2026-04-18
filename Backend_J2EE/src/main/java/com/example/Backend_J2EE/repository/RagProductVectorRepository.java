package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.RagProductVector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RagProductVectorRepository extends JpaRepository<RagProductVector, Integer> {

    Optional<RagProductVector> findByProductId(Integer productId);

    List<RagProductVector> findByProductIdIn(List<Integer> productIds);

    boolean existsByProductIdIn(List<Integer> productIds);

    void deleteByProductIdNotIn(List<Integer> productIds);
}
