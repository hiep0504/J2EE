package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.account.AccountOwnedReviewResponse;
import com.example.Backend_J2EE.dto.account.AccountPurchasedProductResponse;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.dto.order.OrderItemResponse;
import com.example.Backend_J2EE.dto.order.OrderSummaryResponse;
import com.example.Backend_J2EE.dto.review.ReviewMediaDto;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.Order;
import com.example.Backend_J2EE.entity.OrderDetail;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.entity.ProductImage;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.entity.Review;
import com.example.Backend_J2EE.entity.ReviewMedia;
import com.example.Backend_J2EE.repository.OrderRepository;
import com.example.Backend_J2EE.repository.ProductImageRepository;
import com.example.Backend_J2EE.repository.ProductRepository;
import com.example.Backend_J2EE.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountPortalService {

    private final AuthService authService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public AccountPortalService(
            AuthService authService,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ReviewRepository reviewRepository
    ) {
        this.authService = authService;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getMyOrders(Integer accountId) {
        authService.getAccountOrThrow(accountId);
        return orderRepository.findByAccount_IdOrderByOrderDateDesc(accountId)
                .stream()
                .map(this::toOrderSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMyOrderDetail(Integer accountId, Integer orderId) {
        authService.getAccountOrThrow(accountId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay don hang"));

        Integer ownerId = order.getAccount() != null ? order.getAccount().getId() : null;
        if (ownerId == null || !ownerId.equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xem don hang nay");
        }

        return toOrderDetailResponse(order);
    }

    @Transactional(readOnly = true)
    public List<AccountPurchasedProductResponse> getMyPurchasedProducts(Integer accountId) {
        authService.getAccountOrThrow(accountId);

        // Chi lay san pham tu don da hoan tat de dam bao dung nghiep vu "da mua".
        List<Order> completedOrders = orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(
                accountId,
                Order.OrderStatus.completed
        );

        Map<Integer, AccountPurchasedProductResponse> uniqueProducts = new LinkedHashMap<>();

        for (Order order : completedOrders) {
            LocalDateTime purchasedAt = order.getOrderDate();
            List<OrderDetail> details = order.getOrderDetails() == null ? List.of() : order.getOrderDetails();

            for (OrderDetail detail : details) {
                ProductSize productSize = detail.getProductSize();
                Product product = productSize != null ? productSize.getProduct() : null;
                if (product == null || product.getId() == null) {
                    continue;
                }

                Integer productId = product.getId();
                AccountPurchasedProductResponse existing = uniqueProducts.get(productId);
                if (existing != null) {
                    continue;
                }

                AccountPurchasedProductResponse item = new AccountPurchasedProductResponse();
                item.setProductId(productId);
                item.setProductName(product.getName());
                item.setPrice(product.getPrice());
                item.setImageUrl(resolveProductImage(product));
                item.setLastPurchasedAt(purchasedAt);
                uniqueProducts.put(productId, item);
            }
        }

        if (uniqueProducts.isEmpty()) {
            return List.of();
        }

        List<Integer> productIds = new ArrayList<>(uniqueProducts.keySet());
        Map<Integer, Review> reviewByProductId = reviewRepository.findByAccount_IdAndProduct_IdIn(accountId, productIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        review -> review.getProduct().getId(),
                        review -> review,
                        (left, right) -> left
                ));

        for (AccountPurchasedProductResponse item : uniqueProducts.values()) {
            Review review = reviewByProductId.get(item.getProductId());
            item.setReview(review == null ? null : toOwnedReviewResponse(review));
        }

        return uniqueProducts.values().stream()
                .sorted(Comparator.comparing(AccountPurchasedProductResponse::getLastPurchasedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public AccountOwnedReviewResponse createMyReview(
            Integer accountId,
            Integer productId,
            Integer rating,
            String comment,
            List<MultipartFile> images,
            MultipartFile video
    ) {
        Account account = authService.getAccountOrThrow(accountId);
        validateMedia(images, video);

        if (reviewRepository.existsByProduct_IdAndAccount_Id(productId, accountId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ban da danh gia san pham nay roi");
        }

        assertCompletedPurchase(accountId, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay san pham"));

        Review review = Review.builder()
                .product(product)
                .account(account)
                .rating(validateRating(rating))
                .comment(comment == null ? "" : comment.trim())
                .build();

        review.setReviewMediaList(buildMediaList(review, images, video));
        Review saved = reviewRepository.save(review);
        return toOwnedReviewResponse(saved);
    }

    @Transactional
    public AccountOwnedReviewResponse updateMyReview(
            Integer accountId,
            Integer reviewId,
            Integer rating,
            String comment,
            List<MultipartFile> images,
            MultipartFile video,
            boolean replaceMedia
    ) {
        authService.getAccountOrThrow(accountId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay review"));

        Integer ownerId = review.getAccount() != null ? review.getAccount().getId() : null;
        if (ownerId == null || !ownerId.equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen sua review nay");
        }

        review.setRating(validateRating(rating));
        review.setComment(comment == null ? "" : comment.trim());

        List<MultipartFile> validImages = filterFiles(images);
        boolean hasVideo = video != null && !video.isEmpty();

        if (replaceMedia) {
            // Cho phep nguoi dung thay the toan bo media cu khi sua review.
            review.getReviewMediaList().clear();
        }

        int existingImageCount = (int) review.getReviewMediaList().stream()
                .filter(item -> item.getMediaType() == ReviewMedia.MediaType.image)
                .count();
        boolean existingVideo = review.getReviewMediaList().stream()
                .anyMatch(item -> item.getMediaType() == ReviewMedia.MediaType.video);

        if (existingImageCount + validImages.size() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Toi da 5 anh cho moi review");
        }
        if ((existingVideo ? 1 : 0) + (hasVideo ? 1 : 0) > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Toi da 1 video cho moi review");
        }

        List<ReviewMedia> appended = buildMediaList(review, validImages, video);
        review.getReviewMediaList().addAll(appended);

        Review saved = reviewRepository.save(review);
        return toOwnedReviewResponse(saved);
    }

    @Transactional
    public void deleteMyReview(Integer accountId, Integer reviewId) {
        authService.getAccountOrThrow(accountId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay review"));

        Integer ownerId = review.getAccount() != null ? review.getAccount().getId() : null;
        if (ownerId == null || !ownerId.equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen xoa review nay");
        }

        reviewRepository.delete(review);
    }

    private List<ReviewMedia> buildMediaList(Review review, List<MultipartFile> images, MultipartFile video) {
        List<MultipartFile> validImages = filterFiles(images);
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

        return mediaList;
    }

    private List<MultipartFile> filterFiles(List<MultipartFile> files) {
        if (files == null) {
            return Collections.emptyList();
        }
        return files.stream().filter(file -> file != null && !file.isEmpty()).toList();
    }

    private void validateMedia(List<MultipartFile> images, MultipartFile video) {
        List<MultipartFile> validImages = filterFiles(images);
        if (validImages.size() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Toi da 5 anh cho moi review");
        }
        if (video != null && !video.isEmpty()) {
            return;
        }
    }

    private Integer validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating phai tu 1 den 5 sao");
        }
        return rating;
    }

    private void assertCompletedPurchase(Integer accountId, Integer productId) {
        // Bat buoc da mua va don o trang thai completed moi duoc review.
        List<Order> completedOrders = orderRepository.findByAccount_IdAndStatusOrderByOrderDateDesc(
                accountId,
                Order.OrderStatus.completed
        );

        boolean purchased = completedOrders.stream().anyMatch(order ->
                order.getOrderDetails() != null && order.getOrderDetails().stream().anyMatch(detail -> {
                    ProductSize productSize = detail.getProductSize();
                    Product product = productSize != null ? productSize.getProduct() : null;
                    return product != null && productId.equals(product.getId());
                })
        );

        if (!purchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban chi co the danh gia san pham da mua va don hang da hoan tat");
        }
    }

    private String saveFile(MultipartFile file, String subDir) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(subDir);
            Files.createDirectories(uploadPath);
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + subDir + "/" + filename;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Luu file that bai");
        }
    }

    private String resolveProductImage(Product product) {
        if (product.getImage() != null && !product.getImage().isBlank()) {
            return product.getImage();
        }

        Optional<ProductImage> mainImage = productImageRepository.findByProduct_IdAndIsMainTrue(product.getId());
        if (mainImage.isPresent() && mainImage.get().getImageUrl() != null) {
            return mainImage.get().getImageUrl();
        }

        return productImageRepository.findByProduct_Id(product.getId())
                .stream()
                .map(ProductImage::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse("");
    }

    private OrderSummaryResponse toOrderSummaryResponse(Order order) {
        OrderSummaryResponse response = new OrderSummaryResponse();
        response.setId(order.getId());
        response.setAccountId(order.getAccount() != null ? order.getAccount().getId() : null);
        response.setOrderDate(order.getOrderDate());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        response.setAddress(order.getAddress());
        response.setPhone(order.getPhone());
        return response;
    }

    private OrderDetailResponse toOrderDetailResponse(Order order) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setId(order.getId());
        response.setAccountId(order.getAccount() != null ? order.getAccount().getId() : null);
        response.setOrderDate(order.getOrderDate());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        response.setAddress(order.getAddress());
        response.setPhone(order.getPhone());

        List<OrderItemResponse> items = order.getOrderDetails() == null
                ? List.of()
                : order.getOrderDetails().stream().map(detail -> {
                    ProductSize ps = detail.getProductSize();
                    BigDecimal lineTotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
                    return new OrderItemResponse(
                            detail.getId(),
                            ps != null ? ps.getId() : null,
                            ps != null && ps.getProduct() != null ? ps.getProduct().getName() : null,
                            ps != null && ps.getProduct() != null ? resolveProductImage(ps.getProduct()) : "",
                            ps != null && ps.getSize() != null ? ps.getSize().getSizeName() : null,
                            detail.getQuantity(),
                            detail.getPrice(),
                            lineTotal
                    );
                }).toList();

        response.setItems(items);
        return response;
    }

    private AccountOwnedReviewResponse toOwnedReviewResponse(Review review) {
        AccountOwnedReviewResponse response = new AccountOwnedReviewResponse();
        response.setId(review.getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        response.setMedia(review.getReviewMediaList() == null
                ? List.of()
                : review.getReviewMediaList().stream()
                .map(item -> new ReviewMediaDto(
                        item.getId(),
                        item.getMediaType() != null ? item.getMediaType().name() : null,
                        item.getMediaUrl()
                ))
                .toList());
        return response;
    }
}
