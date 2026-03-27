package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.cart.CartItemRequest;
import com.example.Backend_J2EE.dto.cart.CartCheckoutRequest;
import com.example.Backend_J2EE.dto.cart.CartResponse;
import com.example.Backend_J2EE.dto.cart.UpdateCartItemRequest;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.service.AuthService;
import com.example.Backend_J2EE.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Lấy giỏ hàng của user
     * - GET /api/cart
     */
    @GetMapping
    public CartResponse getCart(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return cartService.getCart(accountId, session);
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     * - POST /api/cart/items
     * - Body: { "productId": 1, "sizeId": 2, "quantity": 1 }
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public void addToCart(@RequestBody CartItemRequest request, HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        cartService.addToCart(request, accountId, session);
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ
     * - PUT /api/cart/items/{productId}/{sizeId}
     * - Body: { "quantity": 5 }
     */
    @PutMapping("/items/{productId}/{sizeId}")
    public void updateCartItem(@PathVariable Integer productId,
                               @PathVariable Integer sizeId,
                               @RequestBody UpdateCartItemRequest request,
                               HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        cartService.updateCartItem(productId, sizeId, request, accountId, session);
    }

    /**
     * Xóa sản phẩm khỏi giỏ
     * - DELETE /api/cart/items/{productId}/{sizeId}
     */
    @DeleteMapping("/items/{productId}/{sizeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCartItem(@PathVariable Integer productId,
                               @PathVariable Integer sizeId,
                               HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        cartService.removeCartItem(productId, sizeId, accountId, session);
    }

    /**
     * Xóa toàn bộ giỏ hàng
     * - DELETE /api/cart
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        
        if (accountId != null) {
            cartService.clearCart(accountId);
        } else {
            cartService.clearSessionCart(session);
        }
    }

    @PostMapping("/checkout")
    public OrderDetailResponse checkout(@RequestBody CartCheckoutRequest request, HttpSession session) {
        Integer accountId = (Integer) session.getAttribute(AuthService.SESSION_ACCOUNT_ID);
        return cartService.checkout(accountId, request);
    }
}
