package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.review.ReviewResponse;
import com.example.Backend_J2EE.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public List<ReviewResponse> getReviewsByProduct(@RequestParam Integer productId) {
        return reviewService.getReviewsByProduct(productId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(
            @RequestParam Integer productId,
            @RequestParam Integer accountId,
            @RequestParam Integer rating,
            @RequestParam(required = false, defaultValue = "") String comment,
            @RequestPart(required = false) List<MultipartFile> images,
            @RequestPart(required = false) MultipartFile video
    ) {
        return reviewService.createReview(productId, accountId, rating, comment, images, video);
    }
}