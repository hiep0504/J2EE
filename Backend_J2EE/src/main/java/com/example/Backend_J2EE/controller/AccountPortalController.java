package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.account.AccountOwnedReviewResponse;
import com.example.Backend_J2EE.dto.account.AccountPurchasedProductResponse;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.dto.order.OrderSummaryResponse;
import com.example.Backend_J2EE.service.AccountPortalService;
import com.example.Backend_J2EE.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AccountPortalController {

    private final AccountPortalService accountPortalService;

    public AccountPortalController(AccountPortalService accountPortalService) {
        this.accountPortalService = accountPortalService;
    }

    @GetMapping("/orders")
    public List<OrderSummaryResponse> getMyOrders(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return accountPortalService.getMyOrders(accountId);
    }

    @GetMapping("/orders/{orderId}")
    public OrderDetailResponse getMyOrderDetail(
            @PathVariable Integer orderId,
            HttpSession session
    ) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return accountPortalService.getMyOrderDetail(accountId, orderId);
    }

    @GetMapping("/purchased-products")
    public List<AccountPurchasedProductResponse> getMyPurchasedProducts(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return accountPortalService.getMyPurchasedProducts(accountId);
    }

    @PostMapping(value = "/purchased-products/{productId}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AccountOwnedReviewResponse createMyReview(
            @PathVariable Integer productId,
            @RequestParam Integer rating,
            @RequestParam(required = false, defaultValue = "") String comment,
            @RequestParam(required = false) List<MultipartFile> images,
            @RequestParam(required = false) MultipartFile video,
            HttpSession session
    ) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return accountPortalService.createMyReview(accountId, productId, rating, comment, images, video);
    }

    @PutMapping(value = "/reviews/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AccountOwnedReviewResponse updateMyReview(
            @PathVariable Integer reviewId,
            @RequestParam Integer rating,
            @RequestParam(required = false, defaultValue = "") String comment,
            @RequestParam(required = false) List<MultipartFile> images,
            @RequestParam(required = false) MultipartFile video,
            @RequestParam(required = false, defaultValue = "false") boolean replaceMedia,
            HttpSession session
    ) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return accountPortalService.updateMyReview(accountId, reviewId, rating, comment, images, video, replaceMedia);
    }

    @DeleteMapping("/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyReview(@PathVariable Integer reviewId, HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        accountPortalService.deleteMyReview(accountId, reviewId);
    }
}
