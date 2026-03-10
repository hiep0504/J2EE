package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByProduct_Id(Integer productId);

    List<Review> findByProduct_IdOrderByCreatedAtDesc(Integer productId);

    List<Review> findByAccount_Id(Integer accountId);

    boolean existsByProduct_IdAndAccount_Id(Integer productId, Integer accountId);
}
