package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.review.ReviewMediaDto;
import com.example.Backend_J2EE.dto.review.ReviewResponse;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.Order;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.entity.Review;
import com.example.Backend_J2EE.entity.ReviewMedia;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.repository.OrderRepository;
import com.example.Backend_J2EE.repository.ProductRepository;
import com.example.Backend_J2EE.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final FileStorageService fileStorageService;

    public List<ReviewResponse> getReviewsByProduct(Integer productId) {
        return reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReviewResponse createReview(
            Integer productId,
            Integer accountId,
            Integer rating,
            String comment,
            List<MultipartFile> images,
            MultipartFile video
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

        Review latestReview = reviewRepository.findByAccount_IdAndProduct_IdOrderByCreatedAtDesc(accountId, productId)
                .stream()
                .findFirst()
                .orElse(null);

        if (latestReview != null && !canReviewAgain(accountId, productId, latestReview)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ban da danh gia san pham nay roi");
        }

        Review review = Review.builder()
                .product(product)
                .account(account)
                .rating(rating)
                .comment(comment)
                .build();
        review.setCreatedAt(LocalDateTime.now());

        List<ReviewMedia> mediaList = new ArrayList<>();
        for (MultipartFile imageFile : validImages) {
            String savedUrl = fileStorageService.store(imageFile, "images");
            mediaList.add(ReviewMedia.builder()
                    .review(review)
                    .mediaType(ReviewMedia.MediaType.image)
                    .mediaUrl(savedUrl)
                    .build());
        }

        if (video != null && !video.isEmpty()) {
            String savedUrl = fileStorageService.storeReviewVideo(video);
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

    private boolean canReviewAgain(Integer accountId, Integer productId, Review latestReview) {
        if (latestReview == null) {
            return true;
        }

        LocalDateTime latestPurchaseAt = orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(
                        accountId,
                        Order.OrderStatus.completed
                )
                .stream()
                .filter(order -> order.getOrderDetails() != null && order.getOrderDetails().stream().anyMatch(detail -> {
                    ProductSize productSize = detail.getProductSize();
                    Product purchasedProduct = productSize != null ? productSize.getProduct() : null;
                    return purchasedProduct != null && productId.equals(purchasedProduct.getId());
                }))
                .map(Order::getOrderDate)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);

        if (latestPurchaseAt == null || latestReview.getCreatedAt() == null) {
            return false;
        }

        return latestPurchaseAt.isAfter(latestReview.getCreatedAt());
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
