package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByProduct_Id(Integer productId);

    List<Review> findByProduct_IdOrderByCreatedAtDesc(Integer productId);

    List<Review> findByAccount_Id(Integer accountId);

    Optional<Review> findByAccount_IdAndProduct_Id(Integer accountId, Integer productId);

    List<Review> findByAccount_IdAndProduct_IdIn(Integer accountId, List<Integer> productIds);

    boolean existsByProduct_IdAndAccount_Id(Integer productId, Integer accountId);
}
