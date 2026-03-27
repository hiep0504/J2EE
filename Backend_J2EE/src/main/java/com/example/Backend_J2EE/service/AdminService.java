package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.admin.*;
import com.example.Backend_J2EE.entity.*;
import com.example.Backend_J2EE.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AdminService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSizeRepository productSizeRepository;
    private final SizeRepository sizeRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final ReviewRepository reviewRepository;

    public AdminService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductImageRepository productImageRepository,
            ProductSizeRepository productSizeRepository,
            SizeRepository sizeRepository,
            OrderRepository orderRepository,
            AccountRepository accountRepository,
            ReviewRepository reviewRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.productSizeRepository = productSizeRepository;
        this.sizeRepository = sizeRepository;
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.reviewRepository = reviewRepository;
    }

    public AdminPageResponse<AdminProductResponse> getProducts(String keyword, Integer categoryId, int page, int size) {
        List<Product> all = productRepository.findAllByOrderByCreatedAtDesc();
        String normalized = normalize(keyword);

        List<AdminProductResponse> mapped = all.stream()
                .filter(product -> categoryId == null || (product.getCategory() != null && Objects.equals(product.getCategory().getId(), categoryId)))
                .filter(product -> normalized.isBlank()
                        || normalize(product.getName()).contains(normalized)
                        || normalize(product.getDescription()).contains(normalized))
                .map(this::toProductResponse)
                .toList();

        return paginate(mapped, page, size);
    }

    @Transactional
    public AdminProductResponse createProduct(AdminProductRequest request) {
        validateProductRequest(request);

        Category category = findCategoryOrThrow(request.getCategoryId());

        Product product = Product.builder()
                .name(request.getName().trim())
                .price(request.getPrice())
                .description(request.getDescription())
                .image(request.getImage())
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        syncProductImages(saved, request.getImages());
        syncProductSizes(saved, request.getSizes());

        Product refreshed = productRepository.findById(saved.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Khong tai lai duoc san pham"));
        return toProductResponse(refreshed);
    }

    @Transactional
    public AdminProductResponse updateProduct(Integer productId, AdminProductRequest request) {
        validateProductRequest(request);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay san pham"));
        Category category = findCategoryOrThrow(request.getCategoryId());

        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setImage(request.getImage());
        product.setCategory(category);

        productRepository.save(product);
        syncProductImages(product, request.getImages());
        syncProductSizes(product, request.getSizes());

        Product refreshed = productRepository.findById(product.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Khong tai lai duoc san pham"));
        return toProductResponse(refreshed);
    }

    @Transactional
    public void deleteProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay san pham"));
        productRepository.delete(product);
    }

    public AdminPageResponse<AdminCategoryResponse> getCategories(String keyword, int page, int size) {
        String normalized = normalize(keyword);
        List<AdminCategoryResponse> items = categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getId).reversed())
                .filter(category -> normalized.isBlank() || normalize(category.getName()).contains(normalized))
                .map(category -> new AdminCategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getDescription(),
                        category.getProducts() != null ? category.getProducts().size() : 0
                ))
                .toList();

        return paginate(items, page, size);
    }

    public AdminCategoryResponse createCategory(AdminCategoryRequest request) {
        validateCategoryRequest(request);

        String name = request.getName().trim();
        if (categoryRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Danh muc da ton tai");
        }

        Category saved = categoryRepository.save(Category.builder()
                .name(name)
                .description(request.getDescription())
                .build());

        return new AdminCategoryResponse(saved.getId(), saved.getName(), saved.getDescription(), 0);
    }

    public List<AdminSizeResponse> getSizes() {
        return sizeRepository.findAll().stream()
                .sorted(Comparator.comparing(Size::getId))
                .map(size -> new AdminSizeResponse(size.getId(), size.getSizeName()))
                .toList();
    }

    public AdminCategoryResponse updateCategory(Integer categoryId, AdminCategoryRequest request) {
        validateCategoryRequest(request);

        Category category = findCategoryOrThrow(categoryId);
        String name = request.getName().trim();

        if (!name.equals(category.getName()) && categoryRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Danh muc da ton tai");
        }

        category.setName(name);
        category.setDescription(request.getDescription());
        Category saved = categoryRepository.save(category);

        return new AdminCategoryResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getProducts() != null ? saved.getProducts().size() : 0
        );
    }

    public void deleteCategory(Integer categoryId) {
        Category category = findCategoryOrThrow(categoryId);
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong the xoa danh muc dang co san pham");
        }
        categoryRepository.delete(category);
    }

    public AdminPageResponse<AdminOrderResponse> getOrders(String keyword, String status, int page, int size) {
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalize(status);

        List<AdminOrderResponse> items = orderRepository.findAllByOrderByOrderDateDesc().stream()
                .filter(order -> normalizedStatus.isBlank() || normalize(order.getStatus() != null ? order.getStatus().name() : "").equals(normalizedStatus))
                .filter(order -> {
                    if (normalizedKeyword.isBlank()) {
                        return true;
                    }
                    String orderIdText = String.valueOf(order.getId());
                    String username = order.getAccount() != null ? normalize(order.getAccount().getUsername()) : "";
                    String phone = normalize(order.getPhone());
                    return orderIdText.contains(normalizedKeyword) || username.contains(normalizedKeyword) || phone.contains(normalizedKeyword);
                })
                .map(this::toOrderResponse)
                .toList();

        return paginate(items, page, size);
    }

    public AdminOrderDetailResponse getOrderDetail(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay don hang"));

        List<AdminOrderItemResponse> items = order.getOrderDetails() == null ? List.of() : order.getOrderDetails().stream()
                .map(detail -> {
                    ProductSize ps = detail.getProductSize();
                    BigDecimal lineTotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
                    return new AdminOrderItemResponse(
                            detail.getId(),
                            ps != null ? ps.getId() : null,
                            ps != null && ps.getProduct() != null ? ps.getProduct().getName() : null,
                            ps != null && ps.getSize() != null ? ps.getSize().getSizeName() : null,
                            detail.getQuantity(),
                            detail.getPrice(),
                            lineTotal
                    );
                })
                .toList();

        return new AdminOrderDetailResponse(
                order.getId(),
                order.getAccount() != null ? order.getAccount().getId() : null,
                order.getAccount() != null ? order.getAccount().getUsername() : null,
                order.getTotalPrice(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getAddress(),
                order.getPhone(),
                order.getOrderDate(),
                items
        );
    }

    public AdminOrderResponse updateOrderStatus(Integer orderId, AdminUpdateOrderStatusRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang thai la bat buoc");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay don hang"));

        order.setStatus(parseOrderStatus(request.getStatus()));
        Order saved = orderRepository.save(order);
        return toOrderResponse(saved);
    }

    public AdminPageResponse<AdminUserResponse> getUsers(String keyword, String role, int page, int size) {
        String normalizedKeyword = normalize(keyword);
        String normalizedRole = normalize(role);

        List<AdminUserResponse> items = accountRepository.findAll().stream()
            .sorted(Comparator.comparing(Account::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .filter(account -> normalizedRole.isBlank() || normalize(account.getRole() != null ? account.getRole().name() : "").equals(normalizedRole))
                .filter(account -> {
                    if (normalizedKeyword.isBlank()) {
                        return true;
                    }
                    return normalize(account.getUsername()).contains(normalizedKeyword)
                            || normalize(account.getEmail()).contains(normalizedKeyword)
                            || normalize(account.getPhone()).contains(normalizedKeyword);
                })
                .map(account -> new AdminUserResponse(
                        account.getId(),
                        account.getUsername(),
                        account.getEmail(),
                        account.getPhone(),
                        account.getRole() != null ? account.getRole().name() : null,
                        Boolean.TRUE.equals(account.getLocked()),
                        account.getCreatedAt()
                ))
                .toList();

        return paginate(items, page, size);
    }

    public AdminUserResponse updateUserRole(Integer userId, AdminUpdateUserRoleRequest request) {
        if (request == null || request.getRole() == null || request.getRole().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role la bat buoc");
        }

        Account account = findAccountOrThrow(userId);
        String roleValue = request.getRole().trim().toLowerCase(Locale.ROOT);
        if (!roleValue.equals("admin") && !roleValue.equals("user")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role khong hop le");
        }

        account.setRole("admin".equals(roleValue) ? Account.Role.admin : Account.Role.user);
        Account saved = accountRepository.save(account);
        return toUserResponse(saved);
    }

    public AdminUserResponse updateUserLock(Integer userId, AdminUpdateUserLockRequest request) {
        if (request == null || request.getLocked() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locked la bat buoc");
        }

        Account account = findAccountOrThrow(userId);
        account.setLocked(request.getLocked());
        Account saved = accountRepository.save(account);
        return toUserResponse(saved);
    }

    public AdminPageResponse<AdminReviewResponse> getReviews(Integer productId, String keyword, int page, int size) {
        String normalizedKeyword = normalize(keyword);

        List<AdminReviewResponse> items = reviewRepository.findAll().stream()
            .sorted(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .filter(review -> productId == null || (review.getProduct() != null && Objects.equals(review.getProduct().getId(), productId)))
                .filter(review -> {
                    if (normalizedKeyword.isBlank()) {
                        return true;
                    }
                    return normalize(review.getComment()).contains(normalizedKeyword)
                            || normalize(review.getProduct() != null ? review.getProduct().getName() : "").contains(normalizedKeyword)
                            || normalize(review.getAccount() != null ? review.getAccount().getUsername() : "").contains(normalizedKeyword);
                })
                .map(this::toReviewResponse)
                .toList();

        return paginate(items, page, size);
    }

    public void deleteReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay review"));
        reviewRepository.delete(review);
    }

    private void syncProductImages(Product product, List<AdminProductImageRequest> imageRequests) {
        List<AdminProductImageRequest> safeImages = imageRequests == null ? List.of() : imageRequests.stream()
                .filter(item -> item.getImageUrl() != null && !item.getImageUrl().isBlank())
                .toList();

        productImageRepository.deleteByProduct_Id(product.getId());

        for (AdminProductImageRequest item : safeImages) {
            productImageRepository.save(ProductImage.builder()
                    .product(product)
                    .imageUrl(item.getImageUrl().trim())
                .isMain(false)
                    .build());
        }
    }

    private void syncProductSizes(Product product, List<AdminProductSizeRequest> sizeRequests) {
        List<AdminProductSizeRequest> safeSizes = sizeRequests == null ? List.of() : sizeRequests.stream()
                .filter(item -> item.getSizeId() != null)
                .toList();

        productSizeRepository.findByProduct_Id(product.getId())
                .forEach(productSizeRepository::delete);

        for (AdminProductSizeRequest item : safeSizes) {
            Size size = sizeRepository.findById(item.getSizeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size khong ton tai: " + item.getSizeId()));

            int quantity = item.getQuantity() == null ? 0 : Math.max(item.getQuantity(), 0);
            productSizeRepository.save(ProductSize.builder()
                    .product(product)
                    .size(size)
                    .quantity(quantity)
                    .build());
        }
    }

    private AdminProductResponse toProductResponse(Product product) {
        List<AdminProductImageResponse> images = product.getProductImages() == null ? List.of() : product.getProductImages().stream()
                .sorted(Comparator.comparing(ProductImage::getId))
                .map(item -> new AdminProductImageResponse(item.getId(), item.getImageUrl(), item.getIsMain()))
                .toList();

        List<AdminProductSizeResponse> sizes = product.getProductSizes() == null ? List.of() : product.getProductSizes().stream()
                .sorted(Comparator.comparing(ProductSize::getId))
                .map(item -> new AdminProductSizeResponse(
                        item.getId(),
                        item.getSize() != null ? item.getSize().getId() : null,
                        item.getSize() != null ? item.getSize().getSizeName() : null,
                        item.getQuantity()
                ))
                .toList();

        return new AdminProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImage(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getCreatedAt(),
                images,
                sizes
        );
    }

    private AdminOrderResponse toOrderResponse(Order order) {
        return new AdminOrderResponse(
                order.getId(),
                order.getAccount() != null ? order.getAccount().getId() : null,
                order.getAccount() != null ? order.getAccount().getUsername() : null,
                order.getTotalPrice(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getAddress(),
                order.getPhone(),
                order.getOrderDate()
        );
    }

    private AdminUserResponse toUserResponse(Account account) {
        return new AdminUserResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getPhone(),
                account.getRole() != null ? account.getRole().name() : null,
                Boolean.TRUE.equals(account.getLocked()),
                account.getCreatedAt()
        );
    }

    private AdminReviewResponse toReviewResponse(Review review) {
        List<AdminReviewMediaResponse> media = review.getReviewMediaList() == null ? List.of() : review.getReviewMediaList().stream()
                .sorted(Comparator.comparing(ReviewMedia::getId))
                .map(item -> new AdminReviewMediaResponse(
                        item.getId(),
                        item.getMediaType() != null ? item.getMediaType().name() : null,
                        item.getMediaUrl()
                ))
                .toList();

        return new AdminReviewResponse(
                review.getId(),
                review.getProduct() != null ? review.getProduct().getId() : null,
                review.getProduct() != null ? review.getProduct().getName() : null,
                review.getAccount() != null ? review.getAccount().getId() : null,
                review.getAccount() != null ? review.getAccount().getUsername() : null,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                media
        );
    }

    private Account findAccountOrThrow(Integer accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay tai khoan"));
    }

    private Category findCategoryOrThrow(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay danh muc"));
    }

    private void validateProductRequest(AdminProductRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thong tin san pham la bat buoc");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten san pham la bat buoc");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gia san pham khong hop le");
        }
        if (request.getCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId la bat buoc");
        }
    }

    private void validateCategoryRequest(AdminCategoryRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten danh muc la bat buoc");
        }
    }

    private Order.OrderStatus parseOrderStatus(String statusText) {
        String normalized = statusText.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pending" -> Order.OrderStatus.pending;
            case "confirmed" -> Order.OrderStatus.confirmed;
            case "processing" -> Order.OrderStatus.processing;
            case "shipping" -> Order.OrderStatus.shipping;
            case "completed" -> Order.OrderStatus.completed;
            case "cancelled" -> Order.OrderStatus.cancelled;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang thai don hang khong hop le");
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private <T> AdminPageResponse<T> paginate(List<T> source, int page, int size) {
        int validPage = Math.max(page, 0);
        int validSize = Math.max(size, 1);

        int fromIndex = validPage * validSize;
        if (fromIndex >= source.size()) {
            return new AdminPageResponse<>(List.of(), source.size(), validPage, validSize, (int) Math.ceil(source.size() / (double) validSize));
        }
        int toIndex = Math.min(fromIndex + validSize, source.size());

        List<T> items = new ArrayList<>(source.subList(fromIndex, toIndex));
        int totalPages = (int) Math.ceil(source.size() / (double) validSize);
        return new AdminPageResponse<>(items, source.size(), validPage, validSize, totalPages);
    }
}
