package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.cart.CartItemRequest;
import com.example.Backend_J2EE.dto.cart.CartResponse;
import com.example.Backend_J2EE.dto.cart.UpdateCartItemRequest;
import com.example.Backend_J2EE.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(HttpSession session) {
        return cartService.getCart(session);
    }

    @PostMapping("/items")
    public CartResponse addItem(@RequestBody CartItemRequest request, HttpSession session) {
        return cartService.addItem(session, request.getProductId(), request.getQuantity());
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItem(
            @PathVariable Integer productId,
            @RequestBody UpdateCartItemRequest request,
            HttpSession session
    ) {
        return cartService.updateItem(session, productId, request.getQuantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable Integer productId, HttpSession session) {
        return cartService.removeItem(session, productId);
    }
}
