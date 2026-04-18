package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.cart.CartCheckoutRequest;
import com.example.Backend_J2EE.dto.cart.CartItemRequest;
import com.example.Backend_J2EE.dto.cart.CartResponse;
import com.example.Backend_J2EE.dto.cart.UpdateCartItemRequest;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.service.AuthService;
import com.example.Backend_J2EE.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private CartController cartController;

    @Test
    void getCartDelegatesToService() {
        CartResponse response = new CartResponse();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(7);
        when(cartService.getCart(7, session)).thenReturn(response);

        CartResponse result = cartController.getCart(session);

        assertSame(response, result);
        verify(cartService).getCart(7, session);
    }

    @Test
    void addUpdateRemoveAndCheckoutDelegateToService() {
        CartItemRequest addRequest = new CartItemRequest(1, 2, 3);
        UpdateCartItemRequest updateRequest = new UpdateCartItemRequest(2, 4);
        CartCheckoutRequest checkoutRequest = new CartCheckoutRequest();
        OrderDetailResponse checkoutResponse = new OrderDetailResponse();
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(7);
        when(cartService.checkout(7, checkoutRequest)).thenReturn(checkoutResponse);

        cartController.addToCart(addRequest, session);
        cartController.updateCartItem(1, 2, updateRequest, session);
        cartController.removeCartItem(1, 2, session);
        cartController.clearCart(session);
        OrderDetailResponse result = cartController.checkout(checkoutRequest, session);

        assertSame(checkoutResponse, result);
        verify(cartService).addToCart(addRequest, 7, session);
        verify(cartService).updateCartItem(1, 2, updateRequest, 7, session);
        verify(cartService).removeCartItem(1, 2, 7, session);
        verify(cartService).clearCart(7);
        verify(cartService).checkout(7, checkoutRequest);
    }

    @Test
    void clearCartUsesSessionWhenNotLoggedIn() {
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(null);

        cartController.clearCart(session);

        verify(cartService).clearSessionCart(session);
    }
}
