package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.review.ReviewMediaDto;
import com.example.Backend_J2EE.dto.review.ReviewResponse;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.entity.Review;
import com.example.Backend_J2EE.entity.ReviewMedia;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.repository.ProductRepository;
import com.example.Backend_J2EE.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ReviewController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;

    public ReviewController(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            AccountRepository accountRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<ReviewResponse> getReviewsByProduct(@RequestParam Integer productId) {
        return reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
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
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating phai tu 1 den 5 sao");
        }

        List<MultipartFile> validImages = images == null ? Collections.emptyList()
                : images.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (validImages.size() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Toi da 5 anh cho moi review");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay san pham"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay tai khoan"));

        if (reviewRepository.existsByProduct_IdAndAccount_Id(productId, accountId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ban da danh gia san pham nay roi");
        }

        Review review = Review.builder()
                .product(product)
                .account(account)
                .rating(rating)
                .comment(comment)
                .build();

        List<ReviewMedia> mediaList = new ArrayList<>();
        for (MultipartFile imageFile : validImages) {
            String savedUrl = saveFile(imageFile, "images");
            mediaList.add(ReviewMedia.builder()
                    .review(review)
                    .mediaType(ReviewMedia.MediaType.image)
                    .mediaUrl(savedUrl)
                    .build());
        }

        if (video != null && !video.isEmpty()) {
            String savedUrl = saveFile(video, "videos");
            mediaList.add(ReviewMedia.builder()
                    .review(review)
                    .mediaType(ReviewMedia.MediaType.video)
                    .mediaUrl(savedUrl)
                    .build());
        }

        review.setReviewMediaList(mediaList);
        Review savedReview = reviewRepository.save(review);
        return toResponse(savedReview);
    }

    private String saveFile(MultipartFile file, String subDir) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(subDir);
            Files.createDirectories(uploadPath);
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            String filename = UUID.randomUUID().toString() + ext;
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + subDir + "/" + filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Luu file that bai");
        }
    }

    private ReviewResponse toResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setProductId(review.getProduct() != null ? review.getProduct().getId() : null);
        response.setProductName(review.getProduct() != null ? review.getProduct().getName() : null);
        response.setAccountId(review.getAccount() != null ? review.getAccount().getId() : null);
        response.setAccountUsername(review.getAccount() != null ? review.getAccount().getUsername() : null);
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        List<ReviewMediaDto> media = review.getReviewMediaList() == null
                ? Collections.emptyList()
                : review.getReviewMediaList().stream()
                .map(item -> new ReviewMediaDto(
                        item.getId(),
                        item.getMediaType() != null ? item.getMediaType().name() : null,
                        item.getMediaUrl()
                ))
                .toList();
        response.setMedia(media);
        return response;
    }
}