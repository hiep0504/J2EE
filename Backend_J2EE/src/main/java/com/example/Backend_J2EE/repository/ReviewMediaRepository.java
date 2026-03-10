package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Integer> {

    List<ReviewMedia> findByReview_Id(Integer reviewId);

    @Modifying
    @Transactional
    void deleteByReview_Id(Integer reviewId);
}
