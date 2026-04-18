package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.account.AccountOwnedReviewResponse;
import com.example.Backend_J2EE.dto.account.AccountPurchasedProductResponse;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.dto.order.OrderSummaryResponse;
import com.example.Backend_J2EE.service.AccountPortalService;
import com.example.Backend_J2EE.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountPortalControllerTest {

    @Mock
    private AccountPortalService accountPortalService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AccountPortalController accountPortalController;

    @Test
    void getMyOrdersAndDetailDelegateUsingSessionAccount() {
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(9);

        List<OrderSummaryResponse> orders = List.of(new OrderSummaryResponse());
        OrderDetailResponse orderDetail = new OrderDetailResponse();

        when(accountPortalService.getMyOrders(9)).thenReturn(orders);
        when(accountPortalService.getMyOrderDetail(9, 77)).thenReturn(orderDetail);

        assertSame(orders, accountPortalController.getMyOrders(session));
        assertSame(orderDetail, accountPortalController.getMyOrderDetail(77, session));

        verify(accountPortalService).getMyOrders(9);
        verify(accountPortalService).getMyOrderDetail(9, 77);
    }

    @Test
    void purchasedProductsAndDeleteReviewDelegateUsingSessionAccount() {
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(9);

        List<AccountPurchasedProductResponse> purchasedProducts = List.of(new AccountPurchasedProductResponse());

        when(accountPortalService.getMyPurchasedProducts(9)).thenReturn(purchasedProducts);

        assertSame(purchasedProducts, accountPortalController.getMyPurchasedProducts(session));
        accountPortalController.deleteMyReview(12, session);

        verify(accountPortalService).getMyPurchasedProducts(9);
        verify(accountPortalService).deleteMyReview(9, 12);
    }

    @Test
    void createAndUpdateReviewDelegateUsingSessionAccount() {
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(9);

        AccountOwnedReviewResponse review = new AccountOwnedReviewResponse();
        when(accountPortalService.createMyReview(9, 5, 4, "great", null, null)).thenReturn(review);
        when(accountPortalService.updateMyReview(9, 8, 5, "better", null, null, true)).thenReturn(review);

        assertSame(review, accountPortalController.createMyReview(5, 4, "great", null, null, session));
        assertSame(review, accountPortalController.updateMyReview(8, 5, "better", null, null, true, session));
        verify(accountPortalService).createMyReview(9, 5, 4, "great", null, null);
        verify(accountPortalService).updateMyReview(9, 8, 5, "better", null, null, true);
    }

    @Test
    void missingSessionAccountIdIsPassedThrough() {
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(null);

        List<OrderSummaryResponse> orders = List.of();
        when(accountPortalService.getMyOrders(null)).thenReturn(orders);

        assertSame(orders, accountPortalController.getMyOrders(session));
        verify(accountPortalService).getMyOrders(null);
    }
}
