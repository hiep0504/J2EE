package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.cart.CartItemResponse;
import com.example.Backend_J2EE.dto.cart.CartResponse;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    public static final String SESSION_CART = "CART_ITEMS";

    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public CartResponse getCart(HttpSession session) {
        Map<Integer, Integer> cart = getOrCreateCart(session);
        return buildCartResponse(cart, session);
    }

    public CartResponse addItem(HttpSession session, Integer productId, Integer quantity) {
        if (productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        int qty = normalizeQuantity(quantity);

        // Validate product exists
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        Map<Integer, Integer> cart = getOrCreateCart(session);
        cart.put(productId, cart.getOrDefault(productId, 0) + qty);
        session.setAttribute(SESSION_CART, cart);
        return buildCartResponse(cart, session);
    }

    public CartResponse updateItem(HttpSession session, Integer productId, Integer quantity) {
        if (productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }

        Map<Integer, Integer> cart = getOrCreateCart(session);
        if (!cart.containsKey(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not in cart");
        }

        int qty = normalizeQuantity(quantity);
        cart.put(productId, qty);
        session.setAttribute(SESSION_CART, cart);
        return buildCartResponse(cart, session);
    }

    public CartResponse removeItem(HttpSession session, Integer productId) {
        if (productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        Map<Integer, Integer> cart = getOrCreateCart(session);
        cart.remove(productId);
        session.setAttribute(SESSION_CART, cart);
        return buildCartResponse(cart, session);
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null) {
            return 1;
        }
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be >= 1");
        }
        return quantity;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getOrCreateCart(HttpSession session) {
        Object raw = session.getAttribute(SESSION_CART);
        if (raw instanceof Map<?, ?>) {
            try {
                return (Map<Integer, Integer>) raw;
            } catch (ClassCastException ignored) {
                // fall through to recreate
            }
        }
        Map<Integer, Integer> cart = new LinkedHashMap<>();
        session.setAttribute(SESSION_CART, cart);
        return cart;
    }

    private CartResponse buildCartResponse(Map<Integer, Integer> cart, HttpSession session) {
        if (cart.isEmpty()) {
            return new CartResponse(List.of(), BigDecimal.ZERO);
        }

        List<Integer> ids = new ArrayList<>(cart.keySet());
        List<Product> products = productRepository.findAllById(ids);
        Map<Integer, Product> byId = new HashMap<>();
        for (Product p : products) {
            byId.put(p.getId(), p);
        }

        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Preserve insertion order of cart map
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            Integer productId = entry.getKey();
            Integer qty = entry.getValue();
            Product p = byId.get(productId);
            if (p == null) {
                // product deleted: drop from cart
                continue;
            }

            BigDecimal price = p.getPrice() == null ? BigDecimal.ZERO : p.getPrice();
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
            total = total.add(lineTotal);

            items.add(new CartItemResponse(
                    p.getId(),
                    p.getName(),
                    p.getImage(),
                    price,
                    qty,
                    lineTotal
            ));
        }

        // If any missing products were skipped, rewrite cart to match
        if (items.size() != cart.size()) {
            Map<Integer, Integer> cleaned = new LinkedHashMap<>();
            for (CartItemResponse item : items) {
                cleaned.put(item.getProductId(), item.getQuantity());
            }
            session.setAttribute(SESSION_CART, cleaned);
        }

        return new CartResponse(items, total);
    }
}
